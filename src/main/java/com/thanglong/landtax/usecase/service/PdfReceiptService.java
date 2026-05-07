package com.thanglong.landtax.usecase.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandParcelEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RecordEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxPaymentEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandParcelJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RecordJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxPaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Service xuất biên lai điện tử (PDF) cho thanh toán thuế.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class PdfReceiptService {

    private final TaxPaymentJpaRepository taxPaymentJpaRepository;
    private final RecordJpaRepository recordJpaRepository;
    private final LandParcelJpaRepository landParcelJpaRepository;
    private final CitizenLocalJpaRepository citizenLocalJpaRepository;

    /**
     * Tạo file PDF biên lai điện tử cho một khoản thanh toán.
     * Chỉ cho phép xuất PDF khi thanh toán đã có trạng thái PAID.
     *
     * @param payId ID bản ghi thanh toán
     * @return mảng byte nội dung file PDF
     */
    public byte[] generatePaymentReceipt(Integer payId) {
        log.info("Generating PDF receipt for payId: {}", payId);

        // 1. Tìm thông tin thanh toán
        TaxPaymentEntity payment = taxPaymentJpaRepository.findById(payId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch thanh toán: " + payId));

        if (!"PAID".equals(payment.getPaymentStatus())) {
            throw new RuntimeException("Chỉ có thể xuất biên lai cho các giao dịch đã thanh toán thành công (PAID).");
        }

        // 2. Tìm thông tin hồ sơ và thửa đất
        RecordEntity record = recordJpaRepository.findById(payment.getRecordId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ liên quan"));

        LandParcelEntity parcel = landParcelJpaRepository.findById(payment.getLandParcelId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thửa đất liên quan"));

        CitizenLocalEntity citizen = citizenLocalJpaRepository.findById(record.getCitizenId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin chủ đất"));

        // 3. Khởi tạo Document PDF (Dùng thư viện iText)
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Font mặc định của iText không hỗ trợ Tiếng Việt có dấu,
            // Để đơn giản ta có thể cấu hình font Arial hoặc viết tiếng Việt không dấu (nếu không map font)
            // Trong thực tế cần map font ttf, ở đây demo dùng Helvetica
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);

            // Tiêu đề
            Paragraph title = new Paragraph("BIEN LAI THANH TOAN THUE DAT (ELECTRONIC RECEIPT)", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            // Bảng thông tin
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // Các hàng dữ liệu
            addTableRow(table, "Ma ho so (Record ID):", String.valueOf(record.getRecordId()), headerFont, normalFont);
            addTableRow(table, "Chu dat (Landowner):", citizen.getFullName() + " (CCCD: " + citizen.getCccdNumber() + ")", headerFont, normalFont);
            addTableRow(table, "Ma thua dat (Parcel Number):", parcel.getParcelNumber(), headerFont, normalFont);
            addTableRow(table, "Nam tinh thue (Tax Year):", String.valueOf(payment.getTaxYear()), headerFont, normalFont);
            
            String amountStr = String.format("%,.0f VND", payment.getTotalAmountDue());
            addTableRow(table, "So tien da nop (Amount Paid):", amountStr, headerFont, normalFont);
            
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String paidAtStr = payment.getPaidAt() != null ? payment.getPaidAt().format(dtf) : "N/A";
            addTableRow(table, "Thoi gian thanh toan (Paid At):", paidAtStr, headerFont, normalFont);
            
            addTableRow(table, "Ma giao dich PayOS (Transaction):", payment.getTransactionCode(), headerFont, normalFont);

            document.add(table);

            // Lời cảm ơn
            Paragraph footer = new Paragraph("Cam on ban da hoan thanh nghia vu thue!", normalFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30f);
            document.add(footer);

            document.close();
            
            log.info("PDF receipt generated successfully for payId: {}", payId);
            return out.toByteArray();

        } catch (DocumentException e) {
            log.error("Error generating PDF receipt: {}", e.getMessage());
            throw new RuntimeException("Lỗi trong quá trình tạo file PDF", e);
        }
    }

    private void addTableRow(PdfPTable table, String header, String value, Font headerFont, Font normalFont) {
        PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerCell.setPaddingBottom(10f);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, normalFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(10f);

        table.addCell(headerCell);
        table.addCell(valueCell);
    }
}
