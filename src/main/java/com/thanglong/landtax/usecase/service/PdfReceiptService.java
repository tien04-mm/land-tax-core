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
 * Service xuat bien lai dien tu (PDF) cho thanh toan thue.
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
     * Tao file PDF bien lai dien tu cho mot khoan thanh toan.
     * Chi cho phep xuat PDF khi thanh toan da co trang thai PAID.
     *
     * @param payId ID ban ghi thanh toan
     * @return mang byte noi dung file PDF
     */
    public byte[] generatePaymentReceipt(Integer payId) {
        log.info("Generating PDF receipt for payId: {}", payId);

        // 1. Tim thong tin thanh toan
        TaxPaymentEntity payment = taxPaymentJpaRepository.findById(payId)
                .orElseThrow(() -> new RuntimeException("Payment transaction not found: " + payId));

        if (!"PAID".equals(payment.getPaymentStatus())) {
            throw new RuntimeException("Receipt can only be generated for successfully paid transactions (PAID).");
        }

        // 2. Tim thong tin ho so va thua dat
        RecordEntity record = recordJpaRepository.findById(payment.getRecordId())
                .orElseThrow(() -> new RuntimeException("Related record not found"));

        LandParcelEntity parcel = landParcelJpaRepository.findById(payment.getLandParcelId())
                .orElseThrow(() -> new RuntimeException("Related land parcel not found"));

        CitizenLocalEntity citizen = citizenLocalJpaRepository.findById(record.getCitizenId())
                .orElseThrow(() -> new RuntimeException("Landowner information not found"));

        // 3. Khoi tao Document PDF (Dung thu vien iText)
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Font mac dinh cua iText khong ho tro Tieng Viet co dau,
            // don gian ta co the cau hinh font Arial hoac viet tieng Viet khong dau (neu khong map font)
            // Trong thuc te can map font ttf, o day demo dung Helvetica
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);

            // Tieu de
            Paragraph title = new Paragraph("BIEN LAI THANH TOAN THUE DAT (ELECTRONIC RECEIPT)", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            // Bang thong tin
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // Cac hang du lieu
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

            // Loi cam on
            Paragraph footer = new Paragraph("Thank you for fulfilling your tax obligations!", normalFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30f);
            document.add(footer);

            document.close();
            
            log.info("PDF receipt generated successfully for payId: {}", payId);
            return out.toByteArray();

        } catch (DocumentException e) {
            log.error("Error generating PDF receipt: {}", e.getMessage());
            throw new RuntimeException("Error during PDF file creation", e);
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
