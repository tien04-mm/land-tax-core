package com.thanglong.landtax.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * JWT Filter xác thực token từ VNeID Auth Service gửi sang.
 *
 * <p><b>Luồng xử lý:</b></p>
 * <ol>
 *   <li>Trích xuất token từ Header: {@code Authorization: Bearer <token>}</li>
 *   <li>Verify chữ ký bằng shared secret key (cùng key với vneid-auth-service)</li>
 *   <li>Trích xuất {@code cccd_number} từ Subject của token</li>
 *   <li>Trích xuất {@code role} từ custom claim</li>
 *   <li>Đặt Authentication vào SecurityContext với principal = cccd_number</li>
 * </ol>
 *
 * <p>Sau khi filter chạy xong, các controller có thể lấy cccd_number qua:</p>
 * <pre>{@code
 * String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
 * }</pre>
 *
 * <p>Hoặc inject trực tiếp trong controller:</p>
 * <pre>{@code
 * @GetMapping("/me")
 * public ResponseEntity<?> getMe(@AuthenticationPrincipal String cccdNumber) { ... }
 * }</pre>
 */
@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        log.info("JwtFilter initialized - shared secret key loaded");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                // Verify token bằng shared secret key
                Claims claims = Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // ===== TRÍCH XUẤT CCCD_NUMBER TỪ TOKEN =====
                // Cách 1: Lấy từ Subject (primary)
                String cccdNumber = claims.getSubject();

                // Cách 2 (fallback): Lấy từ custom claim "cccd" nếu Subject rỗng
                if (!StringUtils.hasText(cccdNumber)) {
                    cccdNumber = claims.get("cccd", String.class);
                }

                // Trích xuất các thông tin bổ sung
                String activeRole = claims.get("activeRole", String.class);
                if (!StringUtils.hasText(activeRole)) {
                    activeRole = claims.get("role", String.class); // Fallback
                }
                
                String email = claims.get("email", String.class);
                Long citizenId = claims.get("citizenId", Long.class);
                @SuppressWarnings("unchecked")
                java.util.List<String> roles = claims.get("roles", java.util.List.class);

                if (StringUtils.hasText(cccdNumber) && StringUtils.hasText(activeRole)) {
                    // Tạo Authentication object với principal = cccd_number
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    cccdNumber,                 // Principal = Số CCCD
                                    null,                       // Credentials (không cần)
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + activeRole))
                            );

                    // Lưu thêm thông tin vào details để controller có thể truy cập
                    authentication.setDetails(new JwtUserDetails(cccdNumber, email, activeRole, roles, citizenId));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("Authenticated user - CCCD: {}, email: {}, activeRole: {}", cccdNumber, email, activeRole);
                }

            } catch (ExpiredJwtException e) {
                log.warn("JWT token expired: {}", e.getMessage());
            } catch (UnsupportedJwtException e) {
                log.warn("Unsupported JWT token: {}", e.getMessage());
            } catch (MalformedJwtException e) {
                log.warn("Malformed JWT token: {}", e.getMessage());
            } catch (SecurityException e) {
                log.warn("Invalid JWT signature: {}", e.getMessage());
            } catch (IllegalArgumentException e) {
                log.warn("JWT claims string is empty: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Trích xuất Bearer token từ Header Authorization.
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Inner record để lưu trữ thông tin user đã xác thực từ JWT.
     */
    public record JwtUserDetails(String cccdNumber, String email, String activeRole, java.util.List<String> roles, Long citizenId) {
    }
}
