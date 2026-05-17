package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RecordJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxPaymentJpaRepository;
import com.thanglong.landtax.usecase.dto.DossierStatusReportDTO;
import com.thanglong.landtax.usecase.dto.RevenueReportDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final TaxPaymentJpaRepository taxPaymentJpaRepository;
    private final RecordJpaRepository recordJpaRepository;

    /**
     * Lấy báo cáo tổng doanh thu theo tháng cho một năm cụ thể.
     * Đảm bảo trả về đủ 12 tháng (mặc định doanh thu = 0.0).
     */
    public List<RevenueReportDTO> getRevenueReport(Integer year) {
        log.info("Calculating monthly revenue report for year {}", year);

        Map<Integer, Double> monthlyMap = new HashMap<>();
        for (int i = 1; i <= 12; i++) {
            monthlyMap.put(i, 0.0);
        }

        List<Object[]> results = taxPaymentJpaRepository.getMonthlyRevenue(year);
        if (results != null) {
            for (Object[] row : results) {
                if (row[0] != null && row[1] != null) {
                    Number monthNum = (Number) row[0];
                    Number revenueNum = (Number) row[1];
                    monthlyMap.put(monthNum.intValue(), revenueNum.doubleValue());
                }
            }
        }

        List<RevenueReportDTO> report = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            report.add(new RevenueReportDTO(i, monthlyMap.get(i)));
        }

        return report;
    }

    /**
     * Lấy báo cáo số lượng hồ sơ theo từng trạng thái kèm theo tỷ lệ phần trăm (phần trăm làm tròn 2 chữ số).
     */
    public List<DossierStatusReportDTO> getDossierStatusReport() {
        log.info("Calculating dossier status breakdown report");

        List<Object[]> results = recordJpaRepository.countRecordsByStatus();
        long totalCount = 0;

        if (results != null) {
            for (Object[] row : results) {
                if (row[1] != null) {
                    totalCount += ((Number) row[1]).longValue();
                }
            }
        }

        List<DossierStatusReportDTO> report = new ArrayList<>();
        if (results != null) {
            for (Object[] row : results) {
                String status = row[0] != null ? (String) row[0] : "UNKNOWN";
                long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                double percentage = 0.0;
                if (totalCount > 0) {
                    percentage = (double) count / totalCount * 100.0;
                    // Làm tròn đến 2 chữ số thập phân
                    percentage = Math.round(percentage * 100.0) / 100.0;
                }
                report.add(new DossierStatusReportDTO(status, count, percentage));
            }
        }

        return report;
    }
}
