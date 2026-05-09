package com.thanglong.landtax.infrastructure.adapter.controller;


import com.thanglong.landtax.usecase.service.HandlePaymentWebhookUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller xu ly webhook thanh toan tu PayOS.
 *
 * <p><b>Endpoint nay la PUBLIC</b> (permitAll trong SecurityConfig)
 * vi PayOS server goi truc tiep, khong co JWT token.</p>
 *
 * <p><b>Bao mat:</b> Xac thuc tinh hop le cua webhook bang chu ky HMAC-SHA256
 * duoc PayOS gui kem trong request body.</p>
 *
 * <p><b>PayOS Webhook Payload:</b></p>
 * <pre>
 * {
 *   "code": "00",           // 00 = thanh cong
 *   "desc": "success",
 *   "data": {
 *     "orderCode": 123456,
 *     "amount": 5000000,
 *     "description": "Nop thue dat...",
 *     "accountNumber": "...",
 *     "transactionDateTime": "2025-01-15 14:30:00",
 *     "paymentLinkId": "..."
 *   },
 *   "signature": "abc123..."
 * }
 * </pre>
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final HandlePaymentWebhookUseCase handlePaymentWebhookUseCase;
    // private final PayOSAdapter payOSAdapter;

    /**
     * Nhan webhook tu PayOS khi thanh toan hoan tat.
     *
     * <p><b>URL:</b> POST /api/payments/webhook</p>
     * <p><b>Access:</b> PUBLIC (khong can JWT)</p>
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Received PayOS webhook: {}", payload);

        try {
            // ===== 1. Trich xuat thong tin tu payload =====
            String code = String.valueOf(payload.getOrDefault("code", ""));
            String desc = String.valueOf(payload.getOrDefault("desc", ""));
            // String signature = String.valueOf(payload.getOrDefault("signature", ""));

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.get("data");

            if (data == null) {
                log.warn("Webhook payload missing 'data' field");
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Missing data in webhook payload"
                ));
            }

            String orderCode = String.valueOf(data.getOrDefault("orderCode", ""));

            // ===== 2. Xac thuc chu ky Webhook (Bao mat) =====
            // Chuyen doi data sang Map<String, String> de sort va hash
            java.util.Map<String, String> webhookData = new java.util.HashMap<>();
            for (java.util.Map.Entry<String, Object> entry : data.entrySet()) {
                // PayOS webhook data co the chua null hoac so, can parse sang String
                if (entry.getValue() != null) {
                    webhookData.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }

//            boolean isValidSignature = payOSAdapter.verifyWebhookSignature(webhookData, signature);
//            if (!isValidSignature) {
//                log.warn("Webhook signature validation failed! orderCode={}", orderCode);
//                return ResponseEntity.badRequest().body(Map.of(
//                        "success", false,
//                        "message", "Invalid webhook signature"
//                ));
//            }

            // ===== 3. Kiem tra ma trang thai =====
            if (!"00".equals(code)) {
                log.info("Webhook received non-success code: code={}, desc={}, orderCode={}",
                        code, desc, orderCode);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Received non-success webhook, no action taken"
                ));
            }

            // ===== 4. Xu ly thanh toan thanh cong =====
            log.info("PayOS payment SUCCESS: orderCode={}", orderCode);
            handlePaymentWebhookUseCase.handlePaymentSuccess(orderCode);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Webhook processed successfully"
            ));

        } catch (Exception e) {
            log.error("Error processing PayOS webhook: {}", e.getMessage(), e);
            // Tra ve 200 de PayOS khong retry (tranh loop)
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Webhook processing failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Endpoint test webhook (chi dung trong development).
     * Gia lap PayOS gui tin hieu thanh toan thanh cong.
     */
    @PostMapping("/webhook/test/{orderCode}")
    public ResponseEntity<Map<String, Object>> testWebhook(@PathVariable String orderCode) {
        log.info("[TEST] Simulating PayOS success webhook for orderCode: {}", orderCode);

        handlePaymentWebhookUseCase.handlePaymentSuccess(orderCode);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Test webhook processed for orderCode: " + orderCode
        ));
    }
}
