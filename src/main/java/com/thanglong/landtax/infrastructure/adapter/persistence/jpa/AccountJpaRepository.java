package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountJpaRepository extends JpaRepository<AccountEntity, Integer> {

    Optional<AccountEntity> findByCitizenId(Integer citizenId);

    boolean existsByCitizenId(Integer citizenId);
}
