package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA Entity mapping chinh xac voi bang accounts trong land_tax_management.sql.
 */
@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Integer accountId;

    @Column(name = "citizen_id", nullable = false)
    private Integer citizenId;

    @Column(name = "role_id", nullable = false)
    private Integer roleId;

    @Column(name = "account_status", length = 20)
    private String accountStatus;

    @Column(name = "status_note", columnDefinition = "TEXT")
    private String statusNote;
}
