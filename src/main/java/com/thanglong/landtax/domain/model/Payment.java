package com.thanglong.landtax.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model đại diện cho giao dịch thanh toán.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    private Long id;
    private Long taxRecordId;
    private Long citizenId;
    private BigDecimal amount;
    private String paymentMethod;        // PAYOS, BANK_TRANSFER, CASH
    private String transactionId;        // Mã giao dịch từ PayOS
    private String status;              // PENDING, SUCCESS, FAILED, REFUNDED
    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;
}
