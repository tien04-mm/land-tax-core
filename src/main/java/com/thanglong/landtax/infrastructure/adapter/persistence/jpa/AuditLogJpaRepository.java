package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, Integer> {

    @Query("SELECT a FROM AuditLogEntity a WHERE " +
           "(:userCccd IS NULL OR a.userCccd = :userCccd) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:fromDate IS NULL OR a.timestamp >= :fromDate) AND " +
           "(:toDate IS NULL OR a.timestamp <= :toDate) " +
           "ORDER BY a.timestamp DESC")
    List<AuditLogEntity> findWithFilters(@Param("userCccd") String userCccd,
                                         @Param("action") String action,
                                         @Param("fromDate") LocalDateTime fromDate,
                                         @Param("toDate") LocalDateTime toDate);
}
