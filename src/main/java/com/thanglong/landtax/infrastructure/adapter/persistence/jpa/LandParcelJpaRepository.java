package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandParcelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LandParcelJpaRepository extends JpaRepository<LandParcelEntity, Integer> {

    Optional<LandParcelEntity> findByParcelNumber(String parcelNumber);

    List<LandParcelEntity> findByLandTypeId(Integer landTypeId);

    List<LandParcelEntity> findByAreaId(Integer areaId);

    List<LandParcelEntity> findByOwnerCccd(String ownerCccd);

    List<LandParcelEntity> findByMapSheetNumberAndParcelNumber(String mapSheetNumber, String parcelNumber);

    List<LandParcelEntity> findByMapSheetNumber(String mapSheetNumber);

    Optional<LandParcelEntity> findByParcelNumberAndMapSheetNumber(String parcelNumber, String mapSheetNumber);
}
