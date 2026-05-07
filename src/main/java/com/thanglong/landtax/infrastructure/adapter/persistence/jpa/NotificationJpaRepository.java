package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, Integer> {

    List<NotificationEntity> findByAccountIdOrderByCreatedAtDesc(Integer accountId);

    List<NotificationEntity> findByAccountIdAndIsReadFalseOrderByCreatedAtDesc(Integer accountId);

    long countByAccountIdAndIsReadFalse(Integer accountId);

    List<NotificationEntity> findByCccdNumberOrderByCreatedAtDesc(String cccdNumber);
}
