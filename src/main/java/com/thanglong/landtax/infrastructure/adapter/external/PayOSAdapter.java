package com.thanglong.landtax.infrastructure.adapter.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Infrastructure adapter tích hợp PayOS cho thanh toán trực tuyến.
 *
 * <p><b>Chức năng:</b></p>
 * <ul>
 *   <li>Tạo link thanh toán (checkout URL) gửi đến PayOS API</li>
 *   <li>Xác thực chữ ký webhook (HMAC-SHA256) từ PayOS callback</li>
 *   <li>Kiểm tra trạng thái thanh toán</li>
 * </ul>
 *
 * <p><b>Lưu ý:</b> Trong môi trường demo, adapter trả về mock data.
 * Khi đi production, bỏ comment phần gọi HTTP thực tế đến PayOS API.</p>
 */
@Service
@Slf4j
public class PayOSAdapter {

    @Value("${payos.client-id:}")
    private String clientId;

    @Value("${payos.api-key:}")
    private String apiKey;

    @Value("${payos.checksum-key:}")
    private String checksumKey;

    // private static final String PAYOS_API_URL = "https://api-merchant.payos.vn/v2/payment-requests";

    /**
     * Tạo link thanh toán qua PayOS.
     *
     * @param orderCode   Mã đơn hàng (sử dụng pay_id hoặc timestamp unique)
     * @param amount      Số tiền thanh toán (VNĐ, số nguyên)
     * @param description Mô tả giao dịch (hiển thị trên PayOS)
     * @param returnUrl   URL redirect khi thanh toán thành công
     * @param cancelUrl   URL redirect khi hủy thanh toán
     * @return Map chứa checkoutUrl, orderCode, amount, qrCode
     */
    public PaymentLinkResult createPaymentLink(long orderCode, long amount, String description,
                                                String returnUrl, String cancelUrl) {
        log.info("Creating PayOS payment link: orderCode={}, amount={} VNĐ, description='{}'",
                orderCode, amount, description);

        // Tạo chữ ký checksum cho request
        // String rawSignature = String.format("amount=%d&cancelUrl=%s&description=%s&orderCode=%d&returnUrl=%s",
        //         amount, cancelUrl, description, orderCode, returnUrl);
        // String signature = generateHmacSHA256(rawSignature, checksumKey);

        // ===== GỌI PAYOS API =====
        // Trong môi trường demo, trả về mock checkout URL
        // Production: gọi HTTP POST đến PAYOS_API_URL với body JSON
        /*
        Map<String, Object> requestBody = Map.of(
                "orderCode", orderCode,
                "amount", amount,
                "description", description,
                "returnUrl", returnUrl,
                "cancelUrl", cancelUrl,
                "signature", signature
        );
        // RestTemplate hoặc WebClient gọi đến PAYOS_API_URL
        */

        String checkoutUrl = "https://pay.payos.vn/web/" + orderCode;
        String qrCode = "https://api-merchant.payos.vn/qr/" + orderCode;

        log.info("PayOS payment link created: orderCode={}, checkoutUrl={}", orderCode, checkoutUrl);

        return PaymentLinkResult.builder()
                .checkoutUrl(checkoutUrl)
                .orderCode(orderCode)
                .amount(amount)
                .qrCode(qrCode)
                .description(description)
                .build();
    }

    /**
     * Xác thực chữ ký webhook từ PayOS (HMAC-SHA256).
     *
     * <p>PayOS gửi webhook kèm chữ ký trong field "signature".
     * Ta tính lại chữ ký từ data và so sánh để đảm bảo tính xác thực.</p>
     *
     * @param webhookData Dữ liệu webhook từ PayOS (các field cần sort theo key)
     * @param signature   Chữ ký PayOS gửi kèm
     * @return true nếu chữ ký hợp lệ
     */
    public boolean verifyWebhookSignature(Map<String, String> webhookData, String signature) {
        // Sort keys alphabetically và tạo raw string
        String rawData = new TreeMap<>(webhookData).entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        String computedSignature = generateHmacSHA256(rawData, checksumKey);

        boolean isValid = computedSignature.equals(signature);
        if (!isValid) {
            log.warn("Invalid PayOS webhook signature! Expected: {}, Got: {}", computedSignature, signature);
        }

        return isValid;
    }

    /**
     * Kiểm tra trạng thái thanh toán trên PayOS.
     */
    public Map<String, Object> getPaymentStatus(long orderCode) {
        log.info("Checking PayOS payment status: orderCode={}", orderCode);
        // Production: GET https://api-merchant.payos.vn/v2/payment-requests/{orderCode}
        return Map.of("orderCode", orderCode, "status", "PENDING");
    }

    /**
     * Sinh HMAC-SHA256 signature.
     */
    private String generateHmacSHA256(String data, String key) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to generate HMAC-SHA256: {}", e.getMessage());
            throw new RuntimeException("Lỗi tạo chữ ký HMAC-SHA256", e);
        }
    }

    // ===== Result DTO =====

    @lombok.Builder
    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class PaymentLinkResult {
        private final String checkoutUrl;
        private final long orderCode;
        private final long amount;
        private final String qrCode;
        private final String description;
    }
}
