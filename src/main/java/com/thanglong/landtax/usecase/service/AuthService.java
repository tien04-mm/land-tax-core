package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.client.VneidServiceClient;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AccountEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RefreshTokenEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RoleEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AccountJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RefreshTokenJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RoleJpaRepository;
import com.thanglong.landtax.infrastructure.security.JwtProvider;
import com.thanglong.landtax.usecase.dto.ApiResponse;
import com.thanglong.landtax.usecase.dto.AuthRequest;
import com.thanglong.landtax.usecase.dto.AuthResponse;
import com.thanglong.landtax.usecase.dto.VneidAuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class AuthService {

    private final VneidServiceClient vneidServiceClient;
    private final SyncUserFromVneidUseCase syncUserFromVneidUseCase;
    private final CitizenLocalJpaRepository citizenLocalJpaRepository;
    private final AccountJpaRepository accountJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Transactional
    public AuthResponse qrLogin(String qrToken) {
        log.info("Processing QR login with token: {}", qrToken);

        // 1. Goi sang Port 9090 de thuc hien Handshake/Login bang QR Token
        ApiResponse<VneidAuthResponse> vneidResponse = vneidServiceClient.loginByQr(
                AuthRequest.QrLoginRequest.builder().qrToken(qrToken).build()
        );

        if (vneidResponse == null || !vneidResponse.isSuccess() || vneidResponse.getData() == null) {
            String errorMsg = vneidResponse != null ? vneidResponse.getMessage() : "No response from VNeID";
            throw new RuntimeException("QR Authentication failed: " + errorMsg);
        }

        String cccdNumber = vneidResponse.getData().getUserId();
        log.info("CCCD extracted from VNeID: {}", cccdNumber);
        log.info("QR login successful at VNeID for CCCD: {}", cccdNumber);

        // 2. Dong bo/Tim User trong he thong 8080
        Integer citizenId = syncUserFromVneidUseCase.syncAndGetCitizenId(cccdNumber);
        CitizenLocalEntity citizen = citizenLocalJpaRepository.findById(citizenId)
                .orElseThrow(() -> new RuntimeException("System error: Citizen not found after sync"));

        AccountEntity account = accountJpaRepository.findByCitizenId(citizenId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // 3. Tim tat ca cac vai tro (chi lay role goc tu bang roles)
        List<String> roles = getAllRolesForAccount(account);
        String activeRole = roles.get(0);

        // 4. Sinh JWT Token
        String token = jwtProvider.generateToken(cccdNumber, citizen.getEmail(), activeRole, roles, citizenId);

        // 5. Sinh Refresh Token
        String refreshToken = createRefreshToken(account.getAccountId());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(citizenId)
                .fullName(citizen.getFullName())
                .cccdNumber(cccdNumber)
                .activeRole(activeRole)
                .roles(roles)
                .build();
    }

    @Transactional
    public AuthResponse switchRole(String targetRole) {
        String cccdNumber = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("User {} is switching role to: {}", cccdNumber, targetRole);

        CitizenLocalEntity citizen = citizenLocalJpaRepository.findByCccdNumber(cccdNumber)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AccountEntity account = accountJpaRepository.findByCitizenId(citizen.getCitizenId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        List<String> roles = getAllRolesForAccount(account);

        if (!roles.contains(targetRole)) {
            log.warn("User {} attempted to switch to unauthorized role: {}", cccdNumber, targetRole);
            throw new RuntimeException("You do not own this role: " + targetRole);
        }

        // Sinh JWT Token moi voi activeRole moi
        String token = jwtProvider.generateToken(cccdNumber, citizen.getEmail(), targetRole, roles, citizen.getCitizenId());

        // Sinh Refresh Token moi
        String refreshToken = createRefreshToken(account.getAccountId());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(citizen.getCitizenId())
                .fullName(citizen.getFullName())
                .cccdNumber(cccdNumber)
                .activeRole(targetRole)
                .roles(roles)
                .build();
    }

    /**
     * Sinh Refresh Token cho account.
     */
    @Transactional
    public String createRefreshToken(Integer accountId) {
        refreshTokenJpaRepository.deleteByAccountId(accountId);

        String token = UUID.randomUUID().toString();
        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .token(token)
                .accountId(accountId)
                .expiresAt(LocalDateTime.now().plusDays(7)) // Het han sau 7 ngay
                .isRevoked(false)
                .build();
        refreshTokenJpaRepository.save(entity);
        return token;
    }

    /**
     * Lam moi Access Token bang Refresh Token.
     */
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        log.info("Refreshing access token using refresh token");

        RefreshTokenEntity tokenEntity = refreshTokenJpaRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token khong ton tai"));

        if (tokenEntity.isRevoked()) {
            throw new RuntimeException("Refresh token da bi thu hoi (revoked)");
        }

        if (tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token da het han");
        }

        AccountEntity account = accountJpaRepository.findById(tokenEntity.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account khong ton tai"));

        CitizenLocalEntity citizen = citizenLocalJpaRepository.findById(account.getCitizenId())
                .orElseThrow(() -> new RuntimeException("Citizen khong ton tai"));

        List<String> roles = getAllRolesForAccount(account);
        String activeRole = roles.get(0);

        String newAccessToken = jwtProvider.generateToken(
                citizen.getCccdNumber(),
                citizen.getEmail(),
                activeRole,
                roles,
                citizen.getCitizenId()
        );

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(refreshToken)
                .userId(citizen.getCitizenId())
                .fullName(citizen.getFullName())
                .cccdNumber(citizen.getCccdNumber())
                .activeRole(activeRole)
                .roles(roles)
                .build();
    }

    /**
     * Dang xuat (thu hoi Refresh Token).
     */
    @Transactional
    public void logout(String refreshToken) {
        log.info("Logging out — revoking refresh token");
        refreshTokenJpaRepository.findByToken(refreshToken).ifPresent(entity -> {
            entity.setRevoked(true);
            refreshTokenJpaRepository.save(entity);
            log.info("Refresh token {} has been revoked", refreshToken);
        });
    }

    /**
     * Lay danh sach vai tro cua account.
     * Chi lay role goc tu bang roles.
     */
    private List<String> getAllRolesForAccount(AccountEntity account) {
        List<String> roles = new ArrayList<>();

        RoleEntity baseRole = roleJpaRepository.findById(account.getRoleId())
                .orElseThrow(() -> new RuntimeException("Invalid role"));
        roles.add(baseRole.getRoleCode());

        return roles;
    }
}
