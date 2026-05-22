package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountJpaRepository extends JpaRepository<AccountEntity, Integer> {

    Optional<AccountEntity> findByCitizenId(Integer citizenId);

    boolean existsByCitizenId(Integer citizenId);

    @org.springframework.data.jpa.repository.Query("SELECT r.roleCode " +
            "FROM AccountEntity a " +
            "JOIN CitizenLocalEntity c ON a.citizenId = c.citizenId " +
            "JOIN RoleEntity r ON a.roleId = r.roleId " +
            "WHERE c.cccdNumber = :cccdNumber")
    java.util.List<String> findRoleCodesByCccdNumber(@org.springframework.data.repository.query.Param("cccdNumber") String cccdNumber);

    @org.springframework.data.jpa.repository.Query("SELECT new com.thanglong.landtax.usecase.dto.UserAdminDTO(" +
            "c.cccdNumber, c.fullName, r.roleCode, a.accountStatus, c.phoneNumber, c.email) " +
            "FROM AccountEntity a " +
            "JOIN CitizenLocalEntity c ON a.citizenId = c.citizenId " +
            "JOIN RoleEntity r ON a.roleId = r.roleId " +
            "WHERE (:search IS NULL OR c.cccdNumber LIKE %:search% OR c.fullName LIKE %:search%)")
    java.util.List<com.thanglong.landtax.usecase.dto.UserAdminDTO> findAllWithCitizenInfo(@org.springframework.data.repository.query.Param("search") String search);
}
