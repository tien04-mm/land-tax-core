package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CitizenLocalJpaRepository extends JpaRepository<CitizenLocalEntity, Integer> {

    Optional<CitizenLocalEntity> findByCccdNumber(String cccdNumber);

    Optional<CitizenLocalEntity> findByEmail(String email);

    boolean existsByCccdNumber(String cccdNumber);
}
