package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.controller.exception.ResourceNotFoundException;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AccountEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RoleEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AccountJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RoleJpaRepository;
import com.thanglong.landtax.usecase.dto.CreateUserRequest;
import com.thanglong.landtax.usecase.dto.RoleDTO;
import com.thanglong.landtax.usecase.dto.UpdateRoleRequest;
import com.thanglong.landtax.usecase.dto.UserAdminDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class AdminService {

    private final AccountJpaRepository accountJpaRepository;
    private final CitizenLocalJpaRepository citizenLocalJpaRepository;
    private final RoleJpaRepository roleJpaRepository;

    public List<UserAdminDTO> getAllUsers(String search) {
        return accountJpaRepository.findAllWithCitizenInfo(search);
    }

    /**
     * Tạo mới một user (Citizen + Account).
     */
    @Transactional
    public CitizenLocalEntity createUser(CreateUserRequest request) {
        log.info("Admin creating user: CCCD={}, Name={}, RoleId={}", 
                request.getCccdNumber(), request.getFullName(), request.getRoleId());

        if (citizenLocalJpaRepository.existsByCccdNumber(request.getCccdNumber())) {
            throw new IllegalArgumentException("Công dân với số CCCD này đã tồn tại");
        }

        RoleEntity role = roleJpaRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role không tồn tại"));

        // Tạo mới CitizenLocalEntity
        CitizenLocalEntity citizen = CitizenLocalEntity.builder()
                .cccdNumber(request.getCccdNumber())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .build();
        citizen = citizenLocalJpaRepository.save(citizen);

        // Tạo mới AccountEntity
        AccountEntity account = AccountEntity.builder()
                .citizenId(citizen.getCitizenId())
                .roleId(role.getRoleId())
                .accountStatus("ACTIVE")
                .build();
        accountJpaRepository.save(account);

        log.info("User created successfully: citizenId={}, accountId={}", citizen.getCitizenId(), account.getAccountId());
        return citizen;
    }

    /**
     * Lấy toàn bộ danh sách Role.
     */
    public List<RoleDTO> getAllRoles() {
        log.info("Fetching all roles");
        return roleJpaRepository.findAll().stream()
                .map(role -> RoleDTO.builder()
                        .id(role.getRoleId())
                        .roleCode(role.getRoleCode())
                        .roleName(role.getRoleName())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Cập nhật thông tin một Role.
     */
    @Transactional
    public RoleDTO updateRole(Integer roleId, UpdateRoleRequest request) {
        log.info("Updating role: roleId={}, newName={}, newCode={}", roleId, request.getRoleName(), request.getRoleCode());

        RoleEntity role = roleJpaRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role không tồn tại"));

        if (request.getRoleName() != null) {
            role.setRoleName(request.getRoleName());
        }
        if (request.getRoleCode() != null) {
            role.setRoleCode(request.getRoleCode());
        }

        role = roleJpaRepository.save(role);

        return RoleDTO.builder()
                .id(role.getRoleId())
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .build();
    }
}
