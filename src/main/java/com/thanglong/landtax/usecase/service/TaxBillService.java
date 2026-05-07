package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxBillEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxBillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxBillService {

    private final TaxBillRepository taxBillRepository;

    public List<TaxBillEntity> getUnpaidBills(String cccd) {
        log.info("Lấy danh sách hóa đơn chưa thanh toán cho CCCD: {}", cccd);
        
        List<TaxBillEntity> bills = taxBillRepository.findByCccdNumberAndStatus(cccd, "UNPAID");

        if (bills.isEmpty() && "001190000101".equals(cccd)) {
            log.info("Trả về dữ liệu mock hóa đơn cho CCCD test: 001190000101");
            TaxBillEntity mockBill = TaxBillEntity.builder()
                    .billId(999) // Sử dụng payId giả định cho mock
                    .cccdNumber(cccd)
                    .amount(new BigDecimal("1500000"))
                    .status("UNPAID")
                    .description("Nộp thuế đất năm 2025 - Thửa đất P001")
                    .declarationId(101)
                    .calculationFormula("120m2 x 12.500đ/m2 x 1.0")
                    .basePrice(new BigDecimal("12500"))
                    .coefficient(new BigDecimal("1.0"))
                    .build();
            return List.of(mockBill);
        }

        return bills;
    }

    public List<TaxBillEntity> getPaidBills(String cccd) {
        log.info("Lấy danh sách hóa đơn ĐÃ thanh toán cho CCCD: {}", cccd);
        
        List<TaxBillEntity> bills = taxBillRepository.findByCccdNumberAndStatus(cccd, "PAID");

        if (bills.isEmpty() && "001190000101".equals(cccd)) {
            log.info("Trả về dữ liệu mock hóa đơn PAID cho CCCD test: 001190000101");
            TaxBillEntity mockBill = TaxBillEntity.builder()
                    .billId(888)
                    .cccdNumber(cccd)
                    .amount(new BigDecimal("2000000"))
                    .status("PAID")
                    .description("Nộp thuế đất năm 2024 - Thửa đất P001")
                    .declarationId(100)
                    .calculationFormula("120m2 x 12.500đ/m2 x 1.0")
                    .basePrice(new BigDecimal("12500"))
                    .coefficient(new BigDecimal("1.0"))
                    .build();
            return List.of(mockBill);
        }

        return bills;
    }
}
