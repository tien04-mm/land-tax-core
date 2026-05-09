package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandParcelJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxDeclarationRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxPaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Service xu ly thong ke, bao cao Dashboard cho Admin.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class StatisticsService {

    private final TaxPaymentJpaRepository taxPaymentJpaRepository;
    private final TaxDeclarationRepository taxDeclarationRepository;
    private final LandParcelJpaRepository landParcelJpaRepository;

    /**
     * Lay cac chi so thong ke tong quan cho Dashboard.
     *
     * @return Map chua cac chi so thong ke
     */
    public Map<String, Object> getDashboardStatistics() {
        log.info("StatisticsService.getDashboardStatistics() called");
        Map<String, Object> stats = new HashMap<>();

        // 1. Tong so tien thue da thu trong nam (totalTax)
        int currentYear = LocalDate.now().getYear();
        BigDecimal totalTax = taxPaymentJpaRepository.sumPaidAmountByYear(currentYear);
        if (totalTax == null) {
            totalTax = BigDecimal.ZERO;
        }
        stats.put("totalTax", totalTax);

        // 2. Tinh trang ho so (pendingDeclarations)
        long pendingDeclarations = taxDeclarationRepository.countByStatus("PENDING");
        stats.put("pendingDeclarations", pendingDeclarations);
        
        // 3. Tong so thua dat (totalParcels)
        long totalParcels = landParcelJpaRepository.count();
        stats.put("totalParcels", totalParcels);

        // Bo sung cac key khac neu can cho UI
        stats.put("currentYear", currentYear);

        log.info("Generated dashboard statistics: totalTax={}, pending={}, totalParcels={}",
                totalTax, pendingDeclarations, totalParcels);

        return stats;
    }
}
