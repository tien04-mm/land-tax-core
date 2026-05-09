package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA Entity cho bang attachments.
 * Luu thong tin ve file duoc upload len server.
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

    /** Ten file goc do nguoi dung upload */
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    /** Ten file da luu tren server (co the duoc doi ten de tranh xung dot) */
    @Column(name = "stored_filename", nullable = false, length = 255)
    private String storedFilename;

    /** URL hoan chinh de truy cap file */
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    /** MIME type cua file (image/jpeg, application/pdf, ...) */
    @Column(name = "content_type", length = 100)
    private String contentType;

    /** Kich thuoc file (byte) */
    @Column(name = "file_size")
    private Long fileSize;

    /** CCCD cua nguoi upload */
    @Column(name = "uploaded_by", length = 20)
    private String uploadedBy;

    /** Thoi gian upload */
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    /** ID cua thuc the lien ket (vi du: land_parcel_id, record_id) - tuy chon */
    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    /** Loai thuc the lien ket (LAND_PARCEL, RECORD, ...) - tuy chon */
    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;
}
