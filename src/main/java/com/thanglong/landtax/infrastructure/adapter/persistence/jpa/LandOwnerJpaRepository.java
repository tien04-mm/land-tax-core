package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandOwnerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LandOwnerJpaRepository extends JpaRepository<LandOwnerEntity, Integer> {

    List<LandOwnerEntity> findByCitizenId(Integer citizenId);

    List<LandOwnerEntity> findByLandParcelId(Integer landParcelId);
}
