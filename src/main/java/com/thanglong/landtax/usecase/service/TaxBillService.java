package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxBillEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxBillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxBillService {

    private final TaxBillRepository taxBillRepository;

    public List<TaxBillEntity> getUnpaidBills(String cccd) {
        log.info("Fetching unpaid bills for CCCD: {}", cccd);
        return taxBillRepository.findByCccdNumberAndStatus(cccd, "UNPAID");
    }

    public List<TaxBillEntity> getPaidBills(String cccd) {
        log.info("Fetching paid bills for CCCD: {}", cccd);
        return taxBillRepository.findByCccdNumberAndStatus(cccd, "PAID");
    }
}
