package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RoleDelegationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleDelegationJpaRepository extends JpaRepository<RoleDelegationEntity, Integer> {
    
    List<RoleDelegationEntity> findByDelegatorAccountId(Integer delegatorAccountId);
    
    List<RoleDelegationEntity> findByDelegateeAccountId(Integer delegateeAccountId);
}
