package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA Entity cho bảng attachments.
 * Lưu thông tin về file được upload lên server.
 */
@Entity
@Table(name = "attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long attachmentId;

    /** Tên file gốc do người dùng upload */
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    /** Tên file đã lưu trên server (có thể được đổi tên để tránh xung đột) */
    @Column(name = "stored_filename", nullable = false, length = 255)
    private String storedFilename;

    /** URL hoàn chỉnh để truy cập file */
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    /** MIME type của file (image/jpeg, application/pdf, ...) */
    @Column(name = "content_type", length = 100)
    private String contentType;

    /** Kích thước file (byte) */
    @Column(name = "file_size")
    private Long fileSize;

    /** CCCD của người upload */
    @Column(name = "uploaded_by", length = 20)
    private String uploadedBy;

    /** Thời gian upload */
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    /** ID của thực thể liên kết (ví dụ: land_parcel_id, record_id) - tùy chọn */
    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    /** Loại thực thể liên kết (LAND_PARCEL, RECORD, ...) - tùy chọn */
    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;
}
