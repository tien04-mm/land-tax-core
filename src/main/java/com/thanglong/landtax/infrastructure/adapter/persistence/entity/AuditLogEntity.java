package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_cccd", length = 20)
    private String userCccd;

    @Column(name = "action", length = 255, nullable = false)
    private String action;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "target_type", length = 100)
    private String targetType;

    @Column(name = "target_id", length = 100)
    private String targetId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
