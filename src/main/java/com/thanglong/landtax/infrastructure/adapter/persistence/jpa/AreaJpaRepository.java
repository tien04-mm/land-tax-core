package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AreaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AreaJpaRepository extends JpaRepository<AreaEntity, Integer> {

    /** Tim tat ca khu vuc theo ma quan */
    List<AreaEntity> findByDistrictCode(String districtCode);
}
