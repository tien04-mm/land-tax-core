package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.ReconciliationStatementEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxBillEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.ReconciliationStatementRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxBillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service doi soat sao ke ngan hang voi hoa don thue trong he thong.
 *
 * <p><b>Dinh dang file CSV mau (header row):</b></p>
 * <pre>
 * transactionRef,amount,transactionDate,description
 * ORDER-001,1500000,2025-01-10,Nop thue dat
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class ReconciliationService {

    private final ReconciliationStatementRepository reconciliationRepository;
    private final TaxBillRepository taxBillRepository;

    /**
     * Doc file CSV sao ke, parse va so khop voi bang tax_bills.
     *
     * @param file File CSV duoc upload boi TAX_OFFICER
     * @return Map thong ke ket qua: tong dong, MATCHED, DISCREPANCY, UNMATCHED
     */
    @Transactional
    public Map<String, Object> processReconciliation(MultipartFile file) throws Exception {
        String sourceFileName = java.util.Optional.ofNullable(file.getOriginalFilename()).orElse("unknown.csv");
        log.info("Starting bank reconciliation: file={}, size={}KB", sourceFileName, file.getSize() / 1024);

        List<ReconciliationStatementEntity> statements = parseCsv(file, sourceFileName);

        int matched = 0, discrepancy = 0, unmatched = 0;

        for (ReconciliationStatementEntity stmt : statements) {
            // Tim hoa don theo transactionRef (orderCode) hoac billId embedded trong noi dung
            Optional<TaxBillEntity> billOpt = findMatchingBill(stmt);

            if (billOpt.isPresent()) {
                TaxBillEntity bill = billOpt.get();
                stmt.setMatchedBillId(bill.getBillId());

                // Kiem tra so tien khop khong (+/- 1000 VND tolerance)
                BigDecimal diff = stmt.getBankAmount().subtract(bill.getAmount()).abs();
                if (diff.compareTo(new BigDecimal("1000")) <= 0) {
                    stmt.setMatchStatus("MATCHED");
                    matched++;
                    log.debug("MATCHED: ref={}, billId={}", stmt.getBankTransactionRef(), bill.getBillId());
                } else {
                    stmt.setMatchStatus("DISCREPANCY");
                    stmt.setDiscrepancyNote(String.format(
                            "Amount discrepancy: bank=%s, bill=%s, diff=%s",
                            stmt.getBankAmount(), bill.getAmount(), diff));
                    discrepancy++;
                    log.warn("DISCREPANCY: ref={}, billId={}, diff={}", stmt.getBankTransactionRef(), bill.getBillId(), diff);
                }
            } else {
                stmt.setMatchStatus("UNMATCHED");
                unmatched++;
                log.debug("UNMATCHED: ref={}", stmt.getBankTransactionRef());
            }
        }

        reconciliationRepository.saveAll(statements);

        Map<String, Object> result = Map.of(
                "sourceFile", sourceFileName,
                "totalRows", statements.size(),
                "matched", matched,
                "discrepancy", discrepancy,
                "unmatched", unmatched,
                "message", "Reconciliation completed"
        );

        log.info("Reconciliation results: total={}, matched={}, discrepancy={}, unmatched={}",
                statements.size(), matched, discrepancy, unmatched);
        return result;
    }

    /**
     * Lay danh sach cac dong doi soat co sai lech de can bo xu ly thu cong.
     */
    public List<ReconciliationStatementEntity> getDiscrepancies() {
        return reconciliationRepository.findByMatchStatus("DISCREPANCY");
    }

    // ===== Private helpers =====

    private List<ReconciliationStatementEntity> parseCsv(MultipartFile file, String sourceFileName) throws Exception {
        List<ReconciliationStatementEntity> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isHeader = true;
            int lineNum = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (isHeader) { isHeader = false; continue; } // skip header
                if (line.isBlank()) continue;

                try {
                    String[] cols = line.split(",", -1);
                    if (cols.length < 3) {
                        log.warn("Line {} missing columns, skipping: {}", lineNum, line);
                        continue;
                    }

                    String txRef = cols[0].trim();
                    BigDecimal amount = new BigDecimal(cols[1].trim());
                    String description = cols.length > 3 ? cols[3].trim() : "";

                    ReconciliationStatementEntity stmt = ReconciliationStatementEntity.builder()
                            .bankTransactionRef(txRef)
                            .bankAmount(amount)
                            .bankTransactionDate(LocalDateTime.now()) // simplified
                            .bankDescription(description)
                            .sourceFile(sourceFileName)
                            .build();
                    list.add(stmt);
                } catch (Exception e) {
                    log.warn("Error parsing line {}: {} - {}", lineNum, line, e.getMessage());
                }
            }
        }

        if (list.isEmpty()) throw new IllegalArgumentException("Invalid CSV file data");
        return list;
    }

    private Optional<TaxBillEntity> findMatchingBill(ReconciliationStatementEntity stmt) {
        // Try parsing billId from transactionRef (e.g. "BILL-123" or "123")
        try {
            String ref = stmt.getBankTransactionRef();
            String numStr = ref.replaceAll("[^0-9]", "");
            if (!numStr.isEmpty()) {
                Integer billId = Integer.parseInt(numStr);
                Optional<TaxBillEntity> byId = taxBillRepository.findByBillId(billId);
                if (byId.isPresent()) return byId;
            }
        } catch (NumberFormatException ignored) {}

        // Find by description containing billId
        String desc = java.util.Optional.ofNullable(stmt.getBankDescription()).orElse("");
        List<TaxBillEntity> allBills = taxBillRepository.findAll();
        return allBills.stream()
                .filter(b -> desc.contains(String.valueOf(b.getBillId())))
                .findFirst();
    }

}
