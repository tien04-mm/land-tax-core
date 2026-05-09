package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LandPriceJpaRepository extends JpaRepository<LandPriceEntity, Integer> {

    /** Tim don gia dat moi nhat theo loai dat va khu vuc. */
    @Query("SELECT lp FROM LandPriceEntity lp " +
           "WHERE lp.landTypeId = :landTypeId AND lp.areaId = :areaId " +
           "ORDER BY lp.appliedFrom DESC LIMIT 1")
    Optional<LandPriceEntity> findLatestPrice(Integer landTypeId, Integer areaId);

    /** Loc theo loai dat */
    List<LandPriceEntity> findByLandTypeId(Integer landTypeId);

    /** Loc theo khu vuc */
    List<LandPriceEntity> findByAreaId(Integer areaId);

    /** Loc theo ca loai dat va khu vuc */
    List<LandPriceEntity> findByLandTypeIdAndAreaId(Integer landTypeId, Integer areaId);
}
