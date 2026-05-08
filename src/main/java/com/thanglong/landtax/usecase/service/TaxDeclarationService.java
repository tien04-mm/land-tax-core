package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxDeclarationEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxDeclarationRepository;
import com.thanglong.landtax.usecase.dto.TaxDeclarationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxDeclarationService {

    private final TaxDeclarationRepository repository;

    /** Expose repository để TaxController có thể gọi trực tiếp (update-info) */
    public TaxDeclarationRepository getRepository() { return repository; }

    public List<TaxDeclarationResponse> getMyHistory(String cccd) {
        log.info("Lấy lịch sử tờ khai cho CCCD: {}", cccd);

        List<TaxDeclarationEntity> entities = repository.findBySenderCccdOrderBySubmittedAtDesc(cccd);

        if (entities.isEmpty() && ("001190000101".equals(cccd) || "001190000104".equals(cccd))) {
            log.info("Trả về dữ liệu mock tờ khai cho CCCD test: {}", cccd);
            return buildMockData(cccd);
        }

        return entities.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public TaxDeclarationResponse getDeclarationById(Integer id, String currentCccd) {
        log.info("Lấy chi tiết tờ khai {} cho CCCD: {}", id, currentCccd);

        TaxDeclarationEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tờ khai không tồn tại"));

        if (!java.util.Objects.equals(entity.getSenderCccd(), currentCccd)) {
            log.warn("Cảnh báo bảo mật: CCCD {} cố xem tờ khai {} của {}", currentCccd, id, entity.getSenderCccd());
            throw new AccessDeniedException("Bạn không có quyền xem hồ sơ này");
        }

        return mapToResponse(entity);
    }

    private TaxDeclarationResponse mapToResponse(TaxDeclarationEntity entity) {
        return TaxDeclarationResponse.builder()
                .recordId(entity.getId())
                .citizenId(entity.getCitizenId())
                .parcelId(entity.getParcelId())
                .taxYear(entity.getTaxYear())
                .declaredArea(entity.getDeclaredArea())
                .actualArea(entity.getActualArea())
                .declaredPurpose(entity.getDeclaredPurpose())
                .status(entity.getStatus())
                .reviewNote(entity.getReviewNote())
                .calculatedTaxAmount(entity.getCalculatedTaxAmount())
                .unitPrice(entity.getUnitPrice())
                .taxRate(entity.getTaxRate())
                .submittedAt(entity.getSubmittedAt())
                .build();
    }

    private List<TaxDeclarationResponse> buildMockData(String cccd) {
        List<TaxDeclarationResponse> mocks = new ArrayList<>();
        
        mocks.add(TaxDeclarationResponse.builder()
                .recordId(101)
                .citizenId(1)
                .parcelId(1)
                .taxYear(2025)
                .declaredArea(new BigDecimal("120.5"))
                .actualArea(new BigDecimal("120.5"))
                .declaredPurpose("Đất ở")
                .status("PENDING")
                .reviewNote(null)
                .calculatedTaxAmount(new BigDecimal("1500000"))
                .unitPrice(new BigDecimal("10000000"))
                .taxRate(new BigDecimal("0.03"))
                .submittedAt(LocalDateTime.now().minusDays(1))
                .build());

        mocks.add(TaxDeclarationResponse.builder()
                .recordId(102)
                .citizenId(1)
                .parcelId(2)
                .taxYear(2024)
                .declaredArea(new BigDecimal("80.0"))
                .actualArea(new BigDecimal("80.0"))
                .declaredPurpose("Đất ở")
                .status("APPROVED")
                .reviewNote("Hợp lệ")
                .calculatedTaxAmount(new BigDecimal("1200000"))
                .unitPrice(new BigDecimal("8000000"))
                .taxRate(new BigDecimal("0.03"))
                .submittedAt(LocalDateTime.now().minusDays(30))
                .build());
                
        return mocks;
    }

    public void cancelDeclaration(Integer id, String cccd) {
        if (id == null || cccd == null) {
            throw new IllegalArgumentException("Dữ liệu đầu vào không hợp lệ (null)");
        }

        TaxDeclarationEntity entity = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tờ khai không tồn tại"));
            
        if (!java.util.Objects.equals(cccd, entity.getSenderCccd())) {
            throw new RuntimeException("Bạn không có quyền hủy tờ khai này");
        }
        
        if (!"PENDING".equals(entity.getStatus())) {
            throw new IllegalArgumentException("Chỉ được hủy tờ khai ở trạng thái PENDING");
        }
        
        entity.setStatus("CANCELLED");
        repository.save(entity);
        log.info("Đã hủy tờ khai {} của CCCD {}", id, cccd);
    }
}
