package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AccountEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RoleDelegationEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RoleEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AccountJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RoleDelegationJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RoleJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DelegationService {

    private final RoleDelegationJpaRepository delegationRepository;
    private final CitizenLocalJpaRepository citizenRepository;
    private final AccountJpaRepository accountRepository;
    private final RoleJpaRepository roleRepository;

    public RoleDelegationEntity delegateRole(Map<String, Object> request) {
        String fromUserCccd = (String) request.get("from_user_cccd");
        String toUserCccd = (String) request.get("to_user_cccd");
        String startDateStr = (String) request.get("start_date");
        String endDateStr = (String) request.get("end_date");
        String roleCode = request.containsKey("role_code") ? (String) request.get("role_code") : "ROLE_LAND_OFFICER";

        log.info("Thực hiện ủy quyền từ CCCD: {} sang CCCD: {}", fromUserCccd, toUserCccd);

        // 1. Tìm delegator
        CitizenLocalEntity delegatorCitizen = citizenRepository.findByCccdNumber(fromUserCccd)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người ủy quyền (from_user_cccd)"));
        AccountEntity delegatorAccount = accountRepository.findByCitizenId(delegatorCitizen.getCitizenId())
                .orElseThrow(() -> new IllegalArgumentException("Người ủy quyền chưa có tài khoản"));

        // 2. Tìm delegatee
        CitizenLocalEntity delegateeCitizen = citizenRepository.findByCccdNumber(toUserCccd)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người được ủy quyền (to_user_cccd)"));
        AccountEntity delegateeAccount = accountRepository.findByCitizenId(delegateeCitizen.getCitizenId())
                .orElseThrow(() -> new IllegalArgumentException("Người được ủy quyền chưa có tài khoản"));

        // 3. Tìm Role
        RoleEntity role = roleRepository.findByRoleCode(roleCode)
                .orElseGet(() -> {
                    log.warn("Không tìm thấy Role {}, tạo mới", roleCode);
                    RoleEntity newRole = RoleEntity.builder().roleCode(roleCode).roleName(roleCode).build();
                    return roleRepository.save(newRole);
                });

        // 4. Lưu ủy quyền
        RoleDelegationEntity delegation = RoleDelegationEntity.builder()
                .delegatorAccountId(delegatorAccount.getAccountId())
                .delegateeAccountId(delegateeAccount.getAccountId())
                .delegatedRoleId(role.getRoleId())
                .startTime(LocalDateTime.parse(startDateStr))
                .endTime(LocalDateTime.parse(endDateStr))
                .status("ACTIVE")
                .build();

        return delegationRepository.save(delegation);
    }
}
