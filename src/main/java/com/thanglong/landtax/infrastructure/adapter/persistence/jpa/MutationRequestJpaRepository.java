package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.MutationRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MutationRequestJpaRepository extends JpaRepository<MutationRequestEntity, Long> {

    Page<MutationRequestEntity> findByStatus(String status, Pageable pageable);

    @Query("SELECT m FROM MutationRequestEntity m, CitizenLocalEntity c WHERE m.submittedBy = CAST(c.citizenId as long) AND c.cccdNumber = :requesterCccd")
    Page<MutationRequestEntity> findByRequesterCccd(@Param("requesterCccd") String requesterCccd, Pageable pageable);
}
