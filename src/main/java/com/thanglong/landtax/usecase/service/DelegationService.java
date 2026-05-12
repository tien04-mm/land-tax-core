package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AccountEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RoleDelegationEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RoleEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AccountJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RoleDelegationJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RoleJpaRepository;
import com.thanglong.landtax.usecase.dto.DelegationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class DelegationService {

        private final RoleDelegationJpaRepository delegationRepository;
        private final CitizenLocalJpaRepository citizenRepository;
        private final AccountJpaRepository accountRepository;
        private final RoleJpaRepository roleRepository;
        private final AuditLogService auditLogService;

        public RoleDelegationEntity delegateRole(Map<String, Object> request) {
                String fromUserCccd = (String) request.get("from_user_cccd");
                String toUserCccd = (String) request.get("to_user_cccd");
                String startDateStr = (String) request.get("start_date");
                String endDateStr = (String) request.get("end_date");
                String roleCode = request.containsKey("role_code") ? (String) request.get("role_code")
                                : "ROLE_LAND_OFFICER";

                log.info("Th c hi n  y quy n t  CCCD: {} sang CCCD: {}", fromUserCccd, toUserCccd);

                // 1. T m delegator
                CitizenLocalEntity delegatorCitizen = citizenRepository.findByCccdNumber(fromUserCccd)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Kh ng t m th y ng i  y quy n (from_user_cccd)"));
                AccountEntity delegatorAccount = accountRepository.findByCitizenId(delegatorCitizen.getCitizenId())
                                .orElseThrow(() -> new IllegalArgumentException("Ng i  y quy n ch a c  t i kho n"));

                // 2. T m delegatee
                CitizenLocalEntity delegateeCitizen = citizenRepository.findByCccdNumber(toUserCccd)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Kh ng t m th y ng i  c  y quy n (to_user_cccd)"));
                AccountEntity delegateeAccount = accountRepository.findByCitizenId(delegateeCitizen.getCitizenId())
                                .orElseThrow(() -> new IllegalArgumentException("Ng i  c  y quy n ch a c  t i kho n"));

                // 3. T m Role
                RoleEntity role = roleRepository.findByRoleCode(roleCode)
                                .orElseGet(() -> {
                                        log.warn("Kh ng t m th y Role {}, t o m i", roleCode);
                                        RoleEntity newRole = RoleEntity.builder().roleCode(roleCode).roleName(roleCode)
                                                        .build();
                                        return roleRepository.save(newRole);
                                });

                // 4. L u y quy n
                RoleDelegationEntity delegation = RoleDelegationEntity.builder()
                                .delegatorAccountId(delegatorAccount.getAccountId())
                                .delegateeAccountId(delegateeAccount.getAccountId())
                                .delegatedRoleId(role.getRoleId())
                                .startTime(LocalDateTime.parse(startDateStr))
                                .endTime(LocalDateTime.parse(endDateStr))
                                .status("ACTIVE")
                                .build();

                RoleDelegationEntity saved = delegationRepository.save(delegation);

                auditLogService.log("CREATE_DELEGATION", "ROLE_DELEGATION",
                                saved.getDelegationId() != null ? String.valueOf(saved.getDelegationId()) : toUserCccd,
                                String.format(" y quy n %s t  %s cho %s  n %s", roleCode, fromUserCccd, toUserCccd,
                                                endDateStr));

                return saved;
        }

        public List<DelegationDTO> getAllDelegations() {
                return delegationRepository.findAll().stream()
                                .map(entity -> DelegationDTO.builder()
                                                .id(entity.getDelegationId())
                                                .delegatorAccountId(entity.getDelegatorAccountId())
                                                .delegateeAccountId(entity.getDelegateeAccountId())
                                                .delegatedRoleId(entity.getDelegatedRoleId())
                                                .startTime(entity.getStartTime())
                                                .endTime(entity.getEndTime())
                                                .status(entity.getStatus())
                                                .build())
                                .collect(Collectors.toList());
        }
}
