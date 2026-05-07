package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LandPriceJpaRepository extends JpaRepository<LandPriceEntity, Integer> {

    /** Tìm đơn giá đất mới nhất theo loại đất và khu vực. */
    @Query("SELECT lp FROM LandPriceEntity lp " +
           "WHERE lp.landTypeId = :landTypeId AND lp.areaId = :areaId " +
           "ORDER BY lp.appliedFrom DESC LIMIT 1")
    Optional<LandPriceEntity> findLatestPrice(Integer landTypeId, Integer areaId);

    /** Lọc theo loại đất */
    List<LandPriceEntity> findByLandTypeId(Integer landTypeId);

    /** Lọc theo khu vực */
    List<LandPriceEntity> findByAreaId(Integer areaId);

    /** Lọc theo cả loại đất và khu vực */
    List<LandPriceEntity> findByLandTypeIdAndAreaId(Integer landTypeId, Integer areaId);
}
