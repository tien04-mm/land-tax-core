package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentJpaRepository extends JpaRepository<AttachmentEntity, Long> {

    /** Lấy danh sách file theo người upload */
    List<AttachmentEntity> findByUploadedBy(String uploadedBy);

    /** Lấy danh sách file liên kết với một thực thể */
    List<AttachmentEntity> findByRelatedEntityTypeAndRelatedEntityId(
            String relatedEntityType, Long relatedEntityId);
}
