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
 * JWT Filter xac thuc token tu VNeID Auth Service gui sang.
 *
 * <p><b>Luong xu ly:</b></p>
 * <ol>
 *   <li>Trich xuat token tu Header: {@code Authorization: Bearer <token>}</li>
 *   <li>Verify chu ky bang shared secret key (cung key voi vneid-auth-service)</li>
 *   <li>Trich xuat {@code cccd_number} tu Subject cua token</li>
 *   <li>Trich xuat {@code role} tu custom claim</li>
 *   <li>Dat Authentication vao SecurityContext voi principal = cccd_number</li>
 * </ol>
 *
 * <p>Sau khi filter chay xong, cac controller co the lay cccd_number qua:</p>
 * <pre>{@code
 * String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
 * }</pre>
 *
 * <p>Hoac inject truc tiep trong controller:</p>
 * <pre>{@code
 * @GetMapping("/me")
 * public ResponseEntity<?> getMe(@AuthenticationPrincipal String cccdNumber) { ... }
 * }</pre>
 */
@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    @org.springframework.beans.factory.annotation.Autowired
    private com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AccountJpaRepository accountJpaRepository;

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
                // Verify token bang shared secret key
                Claims claims = Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // ===== TRICH XUAT CCCD_NUMBER TU TOKEN =====
                // Cach 1: Lay tu Subject (primary)
                String cccdNumber = claims.getSubject();

                // Cach 2 (fallback): Lay tu custom claim "cccd" neu Subject rong
                if (!StringUtils.hasText(cccdNumber)) {
                    cccdNumber = claims.get("cccd", String.class);
                }

                // Trich xuat cac thong tin bo sung
                String activeRole = claims.get("activeRole", String.class);
                if (!StringUtils.hasText(activeRole)) {
                    activeRole = claims.get("role", String.class); // Fallback
                }

                // --- LOGIC MỚI CẦN THAY THẾ ---
                java.util.List<org.springframework.security.core.GrantedAuthority> authorities = new java.util.ArrayList<>();
                boolean dbRoleFound = false;

                // 1. Kiểm tra DB trước
                if (StringUtils.hasText(cccdNumber)) {
                    java.util.List<String> localRoles = accountJpaRepository.findRoleCodesByCccdNumber(cccdNumber);
                    if (localRoles != null && !localRoles.isEmpty()) {
                        for (String localRole : localRoles) {
                            String authority = localRole.startsWith("ROLE_") ? localRole : "ROLE_" + localRole;
                            authorities.add(new SimpleGrantedAuthority(authority));
                        }
                        dbRoleFound = true;
                        log.info(">>> [AUTH] Role assigned based on DB: {}", authorities);
                    }
                }

                // 2. Fallback sang Token nếu DB không có role
                if (!dbRoleFound && StringUtils.hasText(activeRole)) {
                    String jwtAuthority = activeRole.startsWith("ROLE_") ? activeRole : "ROLE_" + activeRole;
                    authorities.add(new SimpleGrantedAuthority(jwtAuthority));
                    log.info(">>> [AUTH] Role assigned based on Token (Fallback): {}", authorities);
                }
                
                String email = claims.get("email", String.class);
                Long citizenId = claims.get("citizenId", Long.class);
                @SuppressWarnings("unchecked")
                java.util.List<String> roles = claims.get("roles", java.util.List.class);

                if (StringUtils.hasText(cccdNumber) && !authorities.isEmpty()) {
                    log.info("JWT Claims: {}", claims);
                    log.info("Assigning authorities: {} to user: {}", authorities, cccdNumber);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    cccdNumber,                 // Principal = So CCCD
                                    null,                       // Credentials (khong can)
                                    authorities
                            );

                    // Luu them thong tin vao details de controller co the truy cap
                    authentication.setDetails(new JwtUserDetails(cccdNumber, email, activeRole, roles, citizenId));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.info("Authenticated user - CCCD: {}, merged authorities: {}", cccdNumber, authorities);
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
     * Trich xuat Bearer token tu Header Authorization.
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Inner record de luu tru thong tin user da xac thuc tu JWT.
     */
    public record JwtUserDetails(String cccdNumber, String email, String activeRole, java.util.List<String> roles, Long citizenId) {
    }
}
