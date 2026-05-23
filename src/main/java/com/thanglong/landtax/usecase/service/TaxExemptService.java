package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxExemptSubjectEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AccountJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxExemptSubjectRepository;
import com.thanglong.landtax.usecase.dto.TaxExemptRequestDTO;
import com.thanglong.landtax.usecase.dto.TaxExemptSubjectResponseDTO;
import com.thanglong.landtax.infrastructure.adapter.controller.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxExemptService {

    private final TaxExemptSubjectRepository taxExemptSubjectRepository;
    private final CitizenLocalJpaRepository citizenLocalJpaRepository;
    private final AccountJpaRepository accountJpaRepository;
    private final AuditLogService auditLogService;

    /**
     * Công dân nộp yêu cầu miễn giảm thuế.
     */
    @Transactional
    public TaxExemptSubjectResponseDTO submitExemption(TaxExemptRequestDTO request, String cccd) {
        log.info("Submitting tax exemption for cccd={}", cccd);
        
        var citizen = citizenLocalJpaRepository.findByCccdNumber(cccd)
                .orElseThrow(() -> new ResourceNotFoundException("Công dân không tồn tại"));

        // Kiểm tra xem đã tồn tại yêu cầu cho năm áp dụng này chưa
        var existing = taxExemptSubjectRepository.findByCitizenIdAndAppliedYear(citizen.getCitizenId(), request.getAppliedYear());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Công dân đã có hồ sơ miễn giảm cho năm " + request.getAppliedYear());
        }

        var account = accountJpaRepository.findByCitizenId(citizen.getCitizenId())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản công dân không tồn tại"));

        TaxExemptSubjectEntity entity = new TaxExemptSubjectEntity();
        entity.setCitizenId(citizen.getCitizenId());
        entity.setExemptionReason(request.getExemptionReason());
        entity.setDiscountRate(request.getDiscountRate());
        entity.setAppliedYear(request.getAppliedYear());
        entity.setFullName(citizen.getFullName());
        entity.setStatus("PENDING");
        entity.setUploadedByAccount(account.getAccountId());

        TaxExemptSubjectEntity saved = taxExemptSubjectRepository.save(entity);

        auditLogService.log("SUBMIT_TAX_EXEMPTION", "TAX_EXEMPTION", String.valueOf(saved.getExemptId()), "Gửi yêu cầu miễn giảm thuế");

        return mapToResponseDTO(saved);
    }

    /**
     * Công dân xem danh sách miễn giảm của mình.
     */
    public List<TaxExemptSubjectResponseDTO> getExexemptionsByCitizen(String cccd) {
        log.info("Fetching tax exemptions for citizen cccd={}", cccd);
        var citizen = citizenLocalJpaRepository.findByCccdNumber(cccd)
                .orElseThrow(() -> new ResourceNotFoundException("Công dân không tồn tại"));

        return taxExemptSubjectRepository.findByCitizenId(citizen.getCitizenId()).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách đối tượng miễn giảm thuế với bộ lọc theo vai trò.
     */
    public List<TaxExemptSubjectResponseDTO> getTaxExemptions(Integer appliedYear, String status) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isTaxOfficer = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TAX_OFFICER"));

        if (isTaxOfficer) {
            status = "PENDING"; // Tax officer only sees PENDING requests
        }

        log.info("Fetching tax exemptions for appliedYear={}, status={}", appliedYear, status);
        return taxExemptSubjectRepository.filterExemptions(appliedYear, status).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Duyệt hoặc từ chối hồ sơ miễn giảm.
     */
    @Transactional
    public TaxExemptSubjectResponseDTO approveExemption(Integer id, String status) {
        log.info("Approving tax exemption id={} with status={}", id, status);

        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new IllegalArgumentException("Trạng thái phê duyệt không hợp lệ. Chỉ chấp nhận APPROVED hoặc REJECTED");
        }

        TaxExemptSubjectEntity entity = taxExemptSubjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ miễn giảm"));

        // Lấy thông tin tài khoản cán bộ đang phê duyệt
        String officerCccd = SecurityContextHolder.getContext().getAuthentication().getName();
        var officerCitizen = citizenLocalJpaRepository.findByCccdNumber(officerCccd)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin cán bộ"));
        var officerAccount = accountJpaRepository.findByCitizenId(officerCitizen.getCitizenId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản cán bộ"));

        entity.setStatus(status);
        entity.setUploadedByAccount(officerAccount.getAccountId());

        TaxExemptSubjectEntity saved = taxExemptSubjectRepository.save(entity);

        auditLogService.log("APPROVE_TAX_EXEMPTION", "TAX_EXEMPTION", String.valueOf(saved.getExemptId()), "Phê duyệt miễn giảm với status=" + status);

        return mapToResponseDTO(saved);
    }

    private TaxExemptSubjectResponseDTO mapToResponseDTO(TaxExemptSubjectEntity entity) {
        if (entity == null) {
            return null;
        }
        return TaxExemptSubjectResponseDTO.builder()
                .exemptId(entity.getExemptId())
                .citizenId(entity.getCitizenId())
                .uploadedByAccount(entity.getUploadedByAccount())
                .fullName(entity.getFullName())
                .exemptionReason(entity.getExemptionReason())
                .discountRate(entity.getDiscountRate())
                .appliedYear(entity.getAppliedYear())
                .uploadedAt(entity.getUploadedAt())
                .status(entity.getStatus())
                .build();
    }
}
