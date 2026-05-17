package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.usecase.dto.CalculateTaxRequest;
import com.thanglong.landtax.usecase.dto.CalculateTaxResponse;
import com.thanglong.landtax.usecase.service.TaxCalculatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/taxes")
@RequiredArgsConstructor
@Slf4j
public class TaxCalculationController {

    private final TaxCalculatorService taxCalculatorService;

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyAuthority('ROLE_CITIZEN', 'ROLE_TAX_OFFICER', 'ROLE_ADMIN')")
    public ResponseEntity<CalculateTaxResponse> calculateTax(@RequestBody CalculateTaxRequest request) {
        log.info("POST /api/taxes/calculate: request={}", request);
        CalculateTaxResponse response = taxCalculatorService.calculateEstimatedTax(request);
        return ResponseEntity.ok(response);
    }
}
