package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.domain.model.Complaint;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository;
import com.thanglong.landtax.usecase.dto.ComplaintRequestDTO;
import com.thanglong.landtax.usecase.dto.ComplaintResponseDTO;
import com.thanglong.landtax.usecase.dto.ComplaintUpdateRequestDTO;
import com.thanglong.landtax.usecase.service.ComplaintService;
import com.thanglong.landtax.infrastructure.adapter.controller.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller cho các API liên quan đến Khiếu nại (Complaints).
 */
@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
@Slf4j
public class ComplaintController {

    private final ComplaintService complaintService;
    private final CitizenLocalJpaRepository citizenLocalJpaRepository;

    /**
     * Công dân gửi khiếu nại (yêu cầu Auth).
     */
    @PostMapping
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ComplaintResponseDTO> submitComplaint(@RequestBody ComplaintRequestDTO request) {
        log.info("POST /api/complaints");
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        CitizenLocalEntity citizen = citizenLocalJpaRepository.findByCccdNumber(cccd)
                .orElseThrow(() -> new ResourceNotFoundException("Công dân không tồn tại với số CCCD này"));

        Complaint complaint = Complaint.builder()
                .citizenId(citizen.getCitizenId())
                .recordId(request.getRecordId())
                .content(request.getContent())
                .build();

        Complaint saved = complaintService.submitComplaint(complaint);
        return ResponseEntity.ok(mapToResponseDTO(saved));
    }

    /**
     * Công dân xem danh sách khiếu nại của chính mình (đường dẫn /my).
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<List<ComplaintResponseDTO>> getMyComplaints() {
        log.info("GET /api/complaints/my");
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        CitizenLocalEntity citizen = citizenLocalJpaRepository.findByCccdNumber(cccd)
                .orElseThrow(() -> new ResourceNotFoundException("Công dân không tồn tại với số CCCD này"));

        List<ComplaintResponseDTO> myComplaints = complaintService.getComplaintsByCitizen(citizen.getCitizenId()).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(myComplaints);
    }

    /**
     * Công dân xem danh sách khiếu nại của chính mình (đường dẫn /me).
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<List<ComplaintResponseDTO>> getMyComplaintsMe() {
        log.info("GET /api/complaints/me");
        return getMyComplaints();
    }

    /**
     * Cán bộ xem toàn bộ hoặc lọc theo loại khiếu nại.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TAX_OFFICER', 'LAND_OFFICER')")
    public ResponseEntity<List<ComplaintResponseDTO>> getComplaints(
            @RequestParam(required = false) String type) {
        log.info("GET /api/complaints - type={}", type);
        List<ComplaintResponseDTO> complaints = complaintService.getComplaintsWithRoleFilter(type).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(complaints);
    }

    /**
     * Cập nhật khiếu nại (Admin, Tax Officer, Land Officer).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TAX_OFFICER', 'LAND_OFFICER')")
    public ResponseEntity<ComplaintResponseDTO> updateComplaint(
            @PathVariable Integer id,
            @RequestBody ComplaintUpdateRequestDTO request) {
        log.info("PUT /api/complaints/{} - status={}, responseNote={}", id, request.getStatus(), request.getResponseNote());
        Complaint updated = complaintService.updateComplaint(id, request.getStatus(), request.getResponseNote());
        return ResponseEntity.ok(mapToResponseDTO(updated));
    }

    private ComplaintResponseDTO mapToResponseDTO(Complaint complaint) {
        if (complaint == null) {
            return null;
        }
        return ComplaintResponseDTO.builder()
                .id(complaint.getId())
                .citizenId(complaint.getCitizenId())
                .recordId(complaint.getRecordId())
                .content(complaint.getContent())
                .status(complaint.getStatus())
                .responseNote(complaint.getResponseNote())
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .build();
    }
}
