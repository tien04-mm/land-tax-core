package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.ComplaintEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository cho ComplaintEntity.
 */
@Repository
public interface ComplaintJpaRepository extends JpaRepository<ComplaintEntity, Integer> {

    List<ComplaintEntity> findByCitizenCitizenId(Integer citizenId);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM ComplaintEntity c WHERE " +
           "(:type IS NULL) OR " +
           "(:type = 'TAX' AND c.record.recordCategory = 'TAX_DECLARATION') OR " +
           "(:type = 'LAND' AND (c.record IS NULL OR c.record.recordCategory <> 'TAX_DECLARATION'))")
    List<ComplaintEntity> findByComplaintType(@org.springframework.data.repository.query.Param("type") String type);
}
