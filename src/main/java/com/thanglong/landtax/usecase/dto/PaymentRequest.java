package com.thanglong.landtax.usecase.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO cho yeu cau thanh toan.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotNull(message = "ID ban ghi thue khong duoc de trong")
    private Long taxRecordId;

    @NotNull(message = "ID cong dan khong duoc de trong")
    private Long citizenId;

    @NotNull(message = "So tien khong duoc de trong")
    private BigDecimal amount;

    private String paymentMethod;   // PAYOS, BANK_TRANSFER, CASH
    private String returnUrl;
    private String cancelUrl;
}
