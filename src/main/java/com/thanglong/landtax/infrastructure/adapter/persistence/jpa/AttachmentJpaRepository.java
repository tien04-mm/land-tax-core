package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentJpaRepository extends JpaRepository<AttachmentEntity, Long> {

    /** Lay danh sach file theo nguoi upload */
    List<AttachmentEntity> findByUploadedBy(String uploadedBy);

    /** Lay danh sach file lien ket voi mot thuc the */
    List<AttachmentEntity> findByRelatedEntityTypeAndRelatedEntityId(
            String relatedEntityType, Long relatedEntityId);
}
