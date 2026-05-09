package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.client.VneidServiceClient;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AccountEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RoleDelegationEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RoleEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AccountJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RoleDelegationJpaRepository;
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

import java.util.ArrayList;
import java.util.List;

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
    private final RoleDelegationJpaRepository roleDelegationJpaRepository;
    private final JwtProvider jwtProvider;

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

        // 3. Tim tat ca cac vai tro
        List<String> roles = getAllRolesForAccount(account);
        String activeRole = roles.get(0); // Mac dinh lay vai tro dau tien (thuong la vai tro goc)

        // 4. Sinh JWT Token
        String token = jwtProvider.generateToken(cccdNumber, citizen.getEmail(), activeRole, roles, citizenId);

        return AuthResponse.builder()
                .token(token)
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

        return AuthResponse.builder()
                .token(token)
                .userId(citizen.getCitizenId())
                .fullName(citizen.getFullName())
                .cccdNumber(cccdNumber)
                .activeRole(targetRole)
                .roles(roles)
                .build();
    }

    private List<String> getAllRolesForAccount(AccountEntity account) {
        List<String> roles = new ArrayList<>();
        
        // 1. Vai tro goc
        RoleEntity baseRole = roleJpaRepository.findById(account.getRoleId())
                .orElseThrow(() -> new RuntimeException("Invalid role"));
        roles.add(baseRole.getRoleCode());

        // 2. Vai tro duoc uy quyen (dang con hieu luc)
        List<RoleDelegationEntity> delegations = roleDelegationJpaRepository.findByDelegateeAccountId(account.getAccountId());
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        for (RoleDelegationEntity delegation : delegations) {
            if ("ACTIVE".equals(delegation.getStatus()) && 
                delegation.getStartTime().isBefore(now) && 
                delegation.getEndTime().isAfter(now)) {
                
                roleJpaRepository.findById(delegation.getDelegatedRoleId())
                        .ifPresent(r -> {
                            if (!roles.contains(r.getRoleCode())) {
                                roles.add(r.getRoleCode());
                            }
                        });
            }
        }
        
        return roles;
    }
}
