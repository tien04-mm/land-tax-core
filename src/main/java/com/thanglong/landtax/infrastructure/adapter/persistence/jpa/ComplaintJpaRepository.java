package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.ComplaintEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintJpaRepository extends JpaRepository<ComplaintEntity, Integer> {
    List<ComplaintEntity> findByCccdNumberOrderByCreatedAtDesc(String cccdNumber);
}
