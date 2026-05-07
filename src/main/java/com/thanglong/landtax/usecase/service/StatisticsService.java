package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RecordEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxPaymentEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RecordJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxPaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service xử lý thống kê, báo cáo Dashboard cho Admin.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {

    private final TaxPaymentJpaRepository taxPaymentJpaRepository;
    private final RecordJpaRepository recordJpaRepository;

    /**
     * Lấy các chỉ số thống kê tổng quan cho Dashboard.
     *
     * @return Map chứa các chỉ số thống kê
     */
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 1. Tổng số tiền thuế đã thu trong năm
        int currentYear = LocalDate.now().getYear();
        List<TaxPaymentEntity> allPayments = taxPaymentJpaRepository.findAll();
        
        BigDecimal totalCollected = allPayments.stream()
                .filter(p -> "PAID".equals(p.getPaymentStatus()) && p.getTaxYear() != null && p.getTaxYear() == currentYear)
                .map(TaxPaymentEntity::getTotalAmountDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        stats.put("totalTaxCollectedCurrentYear", totalCollected);

        // 2. Tình trạng hồ sơ (Records)
        List<RecordEntity> allRecords = recordJpaRepository.findAll();
        
        long pendingRecords = allRecords.stream()
                .filter(r -> "PENDING".equals(r.getCurrentStatus()))
                .count();
        stats.put("pendingRecordsCount", pendingRecords);
        
        long fraudWarningRecords = allRecords.stream()
                .filter(r -> "WARNING_FRAUD".equals(r.getCurrentStatus()))
                .count();
        stats.put("warningFraudRecordsCount", fraudWarningRecords);

        // 3. Danh sách các thửa đất nợ thuế quá hạn
        LocalDate now = LocalDate.now();
        List<Map<String, Object>> overdueParcels = allPayments.stream()
                .filter(p -> "UNPAID".equals(p.getPaymentStatus()) || "AWAITING_PAYMENT".equals(p.getPaymentStatus()))
                .filter(p -> p.getDueDate() != null && p.getDueDate().isBefore(now))
                .map(p -> Map.<String, Object>of(
                        "payId", p.getPayId(),
                        "landParcelId", p.getLandParcelId(),
                        "taxYear", p.getTaxYear(),
                        "amountDue", p.getTotalAmountDue(),
                        "dueDate", p.getDueDate()
                ))
                .collect(Collectors.toList());
                
        stats.put("overdueTaxParcels", overdueParcels);
        stats.put("overdueTaxParcelsCount", overdueParcels.size());

        log.info("Generated dashboard statistics: collected={}, pending={}, fraud={}, overdue={}",
                totalCollected, pendingRecords, fraudWarningRecords, overdueParcels.size());

        return stats;
    }
}
