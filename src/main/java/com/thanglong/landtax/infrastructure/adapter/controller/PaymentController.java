package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.usecase.service.CreatePaymentLinkUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller cho thanh toan thue dat.
 *
 * <p><b>API Endpoints:</b></p>
 * <ul>
 *   <li>POST /api/payments/{payId}/create-link  Tao link thanh toan PayOS</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final CreatePaymentLinkUseCase createPaymentLinkUseCase;
    private final com.thanglong.landtax.usecase.service.PdfReceiptService pdfReceiptService;

    /**
     * Tao link thanh toan cho mot khoan thue.
     * Yeu cau JWT token (nguoi dan da dang nhap).
     *
     * @param payId pay_id trong bang tax_payments
     * @return checkoutUrl, orderCode, amount, qrCode
     */
    @PostMapping("/{payId}/create-link")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<Map<String, Object>> createPaymentLink(@PathVariable Integer payId) {
        Map<String, Object> result = createPaymentLinkUseCase.createPaymentLink(payId);
        return ResponseEntity.ok(result);
    }

    /**
     * Tai xuong bien lai dien tu (PDF) khi da thanh toan thanh cong.
     */
    @GetMapping("/{payId}/receipt")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Integer payId) {
        byte[] pdfBytes = pdfReceiptService.generatePaymentReceipt(payId);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "BienLai_ThueDat_" + payId + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
    }
}
