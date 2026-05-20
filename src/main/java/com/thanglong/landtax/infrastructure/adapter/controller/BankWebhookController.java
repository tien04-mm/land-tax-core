package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.usecase.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Controller tiep nhan webhook thanh toan tu dong tu ngan hang.
 */
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class BankWebhookController {

    private final PaymentService paymentService;

    /**
     * API nhan webhook tu ngan hang (hoan toan public, khong can token).
     *
     * <p>Body JSON example:</p>
     * <pre>
     * {
     *   "transactionCode": "TX-1234567890",
     *   "amount": 1500000.00,
     *   "bankName": "Vietcombank"
     * }
     * </pre>
     */
    @PostMapping("/bank-transfer")
    public ResponseEntity<?> handleBankWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Received bank transfer webhook payload: {}", payload);

        try {
            String transactionCode = (String) payload.get("transactionCode");
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            String bankName = (String) payload.getOrDefault("bankName", "Unknown Bank");

            if (transactionCode == null || transactionCode.isBlank() || amount == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "transactionCode and amount are required"));
            }

            paymentService.processBankTransferWebhook(transactionCode, amount, bankName);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Bank transfer webhook processed successfully"
            ));
        } catch (Exception e) {
            log.error("Error processing bank transfer webhook: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
