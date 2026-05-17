package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandOwnerHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LandOwnerHistoryJpaRepository extends JpaRepository<LandOwnerHistoryEntity, Long> {
    List<LandOwnerHistoryEntity> findByParcelIdOrderByEffectiveDateDesc(Integer parcelId);
}
