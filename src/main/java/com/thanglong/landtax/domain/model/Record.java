package com.thanglong.landtax.domain.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Domain model đại diện cho hồ sơ/biên bản.
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
    private String attachmentUrl;        // URL tài liệu đính kèm (Cloudinary)
    private String status;              // DRAFT, SUBMITTED, REVIEWED, CLOSED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
