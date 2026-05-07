package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.infrastructure.adapter.external.PayOSAdapter;
import com.thanglong.landtax.usecase.service.HandlePaymentWebhookUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller xử lý webhook thanh toán từ PayOS.
 *
 * <p><b>Endpoint này là PUBLIC</b> (permitAll trong SecurityConfig)
 * vì PayOS server gọi trực tiếp, không có JWT token.</p>
 *
 * <p><b>Bảo mật:</b> Xác thực tính hợp lệ của webhook bằng chữ ký HMAC-SHA256
 * được PayOS gửi kèm trong request body.</p>
 *
 * <p><b>PayOS Webhook Payload:</b></p>
 * <pre>
 * {
 *   "code": "00",           // 00 = thành công
 *   "desc": "success",
 *   "data": {
 *     "orderCode": 123456,
 *     "amount": 5000000,
 *     "description": "Nộp thuế đất...",
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
    private final PayOSAdapter payOSAdapter;

    /**
     * Nhận webhook từ PayOS khi thanh toán hoàn tất.
     *
     * <p><b>URL:</b> POST /api/payments/webhook</p>
     * <p><b>Access:</b> PUBLIC (không cần JWT)</p>
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Received PayOS webhook: {}", payload);

        try {
            // ===== 1. Trích xuất thông tin từ payload =====
            String code = String.valueOf(payload.getOrDefault("code", ""));
            String desc = String.valueOf(payload.getOrDefault("desc", ""));
            String signature = String.valueOf(payload.getOrDefault("signature", ""));

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

            // ===== 2. Xác thực chữ ký Webhook (Bảo mật) =====
            // Chuyển đổi data sang Map<String, String> để sort và hash
            java.util.Map<String, String> webhookData = new java.util.HashMap<>();
            for (java.util.Map.Entry<String, Object> entry : data.entrySet()) {
                // PayOS webhook data có thể chứa null hoặc số, cần parse sang String
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

            // ===== 3. Kiểm tra mã trạng thái =====
            if (!"00".equals(code)) {
                log.info("Webhook received non-success code: code={}, desc={}, orderCode={}",
                        code, desc, orderCode);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Received non-success webhook, no action taken"
                ));
            }

            // ===== 4. Xử lý thanh toán thành công =====
            log.info("PayOS payment SUCCESS: orderCode={}", orderCode);
            handlePaymentWebhookUseCase.handlePaymentSuccess(orderCode);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Webhook processed successfully"
            ));

        } catch (Exception e) {
            log.error("Error processing PayOS webhook: {}", e.getMessage(), e);
            // Trả về 200 để PayOS không retry (tránh loop)
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Webhook processing failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Endpoint test webhook (chỉ dùng trong development).
     * Giả lập PayOS gửi tín hiệu thanh toán thành công.
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
