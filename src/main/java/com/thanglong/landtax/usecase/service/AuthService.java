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

        // 1. Gọi sang Port 9090 để thực hiện Handshake/Login bằng QR Token
        ApiResponse<VneidAuthResponse> vneidResponse = vneidServiceClient.loginByQr(
                AuthRequest.QrLoginRequest.builder().qrToken(qrToken).build()
        );

        if (vneidResponse == null || !vneidResponse.isSuccess() || vneidResponse.getData() == null) {
            String errorMsg = vneidResponse != null ? vneidResponse.getMessage() : "Không nhận được phản hồi từ VNeID";
            throw new RuntimeException("Xác thực QR thất bại: " + errorMsg);
        }

        String cccdNumber = vneidResponse.getData().getUserId();
        log.info("CCCD trích xuất từ VNeID: {}", cccdNumber);
        log.info("QR login successful at VNeID for CCCD: {}", cccdNumber);

        // 2. Đồng bộ/Tìm User trong hệ thống 8080
        Integer citizenId = syncUserFromVneidUseCase.syncAndGetCitizenId(cccdNumber);
        CitizenLocalEntity citizen = citizenLocalJpaRepository.findById(citizenId)
                .orElseThrow(() -> new RuntimeException("Lỗi hệ thống: Không tìm thấy công dân sau khi đồng bộ"));

        AccountEntity account = accountJpaRepository.findByCitizenId(citizenId)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        // 3. Tìm tất cả các vai trò
        List<String> roles = getAllRolesForAccount(account);
        String activeRole = roles.get(0); // Mặc định lấy vai trò đầu tiên (thường là vai trò gốc)

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
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        AccountEntity account = accountJpaRepository.findByCitizenId(citizen.getCitizenId())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        List<String> roles = getAllRolesForAccount(account);

        if (!roles.contains(targetRole)) {
            log.warn("User {} attempted to switch to unauthorized role: {}", cccdNumber, targetRole);
            throw new RuntimeException("Bạn không sở hữu vai trò này: " + targetRole);
        }

        // Sinh JWT Token mới với activeRole mới
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
        
        // 1. Vai trò gốc
        RoleEntity baseRole = roleJpaRepository.findById(account.getRoleId())
                .orElseThrow(() -> new RuntimeException("Vai trò không hợp lệ"));
        roles.add(baseRole.getRoleCode());

        // 2. Vai trò được ủy quyền (đang còn hiệu lực)
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
