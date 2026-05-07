package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.usecase.service.CreatePaymentLinkUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller cho thanh toán thuế đất.
 *
 * <p><b>API Endpoints:</b></p>
 * <ul>
 *   <li>POST /api/payments/{payId}/create-link — Tạo link thanh toán PayOS</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final CreatePaymentLinkUseCase createPaymentLinkUseCase;
    private final com.thanglong.landtax.usecase.service.PdfReceiptService pdfReceiptService;

    /**
     * Tạo link thanh toán cho một khoản thuế.
     * Yêu cầu JWT token (người dân đã đăng nhập).
     *
     * @param payId pay_id trong bảng tax_payments
     * @return checkoutUrl, orderCode, amount, qrCode
     */
    @PostMapping("/{payId}/create-link")
    public ResponseEntity<Map<String, Object>> createPaymentLink(@PathVariable Integer payId) {
        Map<String, Object> result = createPaymentLinkUseCase.createPaymentLink(payId);
        return ResponseEntity.ok(result);
    }

    /**
     * Tải xuống biên lai điện tử (PDF) khi đã thanh toán thành công.
     */
    @GetMapping("/{payId}/receipt")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Integer payId) {
        byte[] pdfBytes = pdfReceiptService.generatePaymentReceipt(payId);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "BienLai_ThueDat_" + payId + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
    }
}
