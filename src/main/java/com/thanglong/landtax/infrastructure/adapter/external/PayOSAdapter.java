package com.thanglong.landtax.infrastructure.adapter.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

@Component
@Slf4j
public class PayOSAdapter {

    private final PayOS payOS;

    public PayOSAdapter(
            @Value("${payos.client-id:}") String clientId,
            @Value("${payos.api-key:}") String apiKey,
            @Value("${payos.checksum-key:}") String checksumKey) {
        
        // Trim de chong loi khoang trang an
        this.payOS = new PayOS(
            clientId != null ? clientId.trim() : "", 
            apiKey != null ? apiKey.trim() : "", 
            checksumKey != null ? checksumKey.trim() : ""
        );
    }

    public PaymentLinkResult createPaymentLink(int orderCode, int amount, String description, String returnUrl, String cancelUrl) {
        try {
            // Su dung doi tuong CreatePaymentLinkRequest cua ban 2.0.1 (Thay cho PaymentData)
            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                    .orderCode((long) orderCode)
                    .amount((long) amount)
                    .description(description)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .build();

            // Goi qua paymentRequests() service cua ban 2.x
            CreatePaymentLinkResponse data = payOS.paymentRequests().create(paymentData);

            return new PaymentLinkResult(
                    String.valueOf(data.getOrderCode()),
                    data.getAmount() != null ? data.getAmount() : 0L,
                    data.getCheckoutUrl(),
                    data.getQrCode(),
                    data.getDescription()
            );

        } catch (Exception e) {
            log.error("Loi tao link thanh toan PayOS ban 2.0.1", e);
            throw new RuntimeException("Khong the ket noi voi cong thanh toan. Vui long thu lai sau.", e);
        }
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class PaymentLinkResult {
        private final String orderCode;
        private final long amount;
        private final String checkoutUrl;
        private final String qrCode;
        private final String description;
    }
}
