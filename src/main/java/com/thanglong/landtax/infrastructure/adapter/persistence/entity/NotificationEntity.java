package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA Entity mapping chính xác với bảng notifications trong land_tax_management.sql.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "noti_id")
    private Integer notiId;

    @Column(name = "account_id", nullable = false)
    private Integer accountId;                  // FK → accounts.account_id

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "noti_type", nullable = false, length = 50)
    private String notiType;                    // TAX_APPROVED, TAX_REJECTED, PAYMENT_DUE, ...

    @Column(name = "is_read")
    private Boolean isRead;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "cccd_number", length = 20)
    private String cccdNumber;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @PrePersist
    protected void onCreate() {
        if (isRead == null) {
            isRead = false;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
