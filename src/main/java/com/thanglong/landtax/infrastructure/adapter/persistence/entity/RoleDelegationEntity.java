package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA Entity mapping với bảng role_delegations trong CSDL.
 */
@Entity
@Table(name = "role_delegations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDelegationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delegation_id")
    private Integer delegationId;

    @Column(name = "delegator_account_id", nullable = false)
    private Integer delegatorAccountId;

    @Column(name = "delegatee_account_id", nullable = false)
    private Integer delegateeAccountId;

    @Column(name = "delegated_role_id", nullable = false)
    private Integer delegatedRoleId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "status", length = 20)
    private String status;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = "ACTIVE";
        }
    }
}
