package com.thanglong.landtax.domain.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Domain model dai dien cho ho so/bien ban.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Record {

    private Long id;
    private String recordCode;
    private String recordType;           // DECLARATION, INSPECTION, VIOLATION
    private Long landParcelId;
    private Long citizenId;
    private String content;
    private String attachmentUrl;        // URL tai lieu dinh kem (Cloudinary)
    private String status;              // DRAFT, SUBMITTED, REVIEWED, CLOSED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
