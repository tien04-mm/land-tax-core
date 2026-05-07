package com.thanglong.landtax.domain.repository;

import com.thanglong.landtax.domain.model.LandParcel;

import java.util.List;
import java.util.Optional;

/**
 * Interface repository cho LandParcel.
 */
public interface LandParcelRepository {

    Optional<LandParcel> findById(Long id);

    Optional<LandParcel> findByParcelCode(String parcelCode);

    List<LandParcel> findByOwnerId(Long ownerId);

    List<LandParcel> findAll();

    LandParcel save(LandParcel landParcel);

    void deleteById(Long id);
}
