package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandParcelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LandParcelJpaRepository extends JpaRepository<LandParcelEntity, Integer> {

    Optional<LandParcelEntity> findByParcelNumber(String parcelNumber);

    List<LandParcelEntity> findByLandTypeId(Integer landTypeId);

    List<LandParcelEntity> findByAreaId(Integer areaId);

    @Query("SELECT lp FROM LandParcelEntity lp " +
           "JOIN LandOwnerEntity lo ON lp.landParcelId = lo.landParcelId " +
           "JOIN CitizenLocalEntity c ON lo.citizenId = c.citizenId " +
           "WHERE c.cccdNumber = :cccd")
    List<LandParcelEntity> findByOwnerCccd(@Param("cccd") String cccd);

    List<LandParcelEntity> findByMapSheetNumberAndParcelNumber(String mapSheetNumber, String parcelNumber);

    List<LandParcelEntity> findByMapSheetNumber(String mapSheetNumber);

    Optional<LandParcelEntity> findByParcelNumberAndMapSheetNumber(String parcelNumber, String mapSheetNumber);
}
