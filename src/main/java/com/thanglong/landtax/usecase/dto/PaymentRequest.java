package com.thanglong.landtax.usecase.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO cho yêu cầu thanh toán.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotNull(message = "ID bản ghi thuế không được để trống")
    private Long taxRecordId;

    @NotNull(message = "ID công dân không được để trống")
    private Long citizenId;

    @NotNull(message = "Số tiền không được để trống")
    private BigDecimal amount;

    private String paymentMethod;   // PAYOS, BANK_TRANSFER, CASH
    private String returnUrl;
    private String cancelUrl;
}
