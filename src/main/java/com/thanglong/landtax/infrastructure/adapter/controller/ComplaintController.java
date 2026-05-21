package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.domain.model.Complaint;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository;
import com.thanglong.landtax.usecase.service.ComplaintService;
import com.thanglong.landtax.infrastructure.adapter.controller.exception.ResourceNotFoundException;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller cho các API liên quan đến Khiếu nại (Complaints).
 */
@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;
    private final CitizenLocalJpaRepository citizenLocalJpaRepository;

    /**
     * Công dân gửi khiếu nại (yêu cầu Auth).
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CITIZEN')")
    public ResponseEntity<Complaint> submitComplaint(@RequestBody SubmitRequest request) {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        CitizenLocalEntity citizen = citizenLocalJpaRepository.findByCccdNumber(cccd)
                .orElseThrow(() -> new ResourceNotFoundException("Công dân không tồn tại với số CCCD này"));

        Complaint complaint = Complaint.builder()
                .citizenId(citizen.getCitizenId())
                .recordId(request.getRecordId())
                .content(request.getContent())
                .build();

        Complaint saved = complaintService.submitComplaint(complaint);
        return ResponseEntity.ok(saved);
    }

    /**
     * Công dân xem danh sách khiếu nại của chính mình.
     */
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_CITIZEN')")
    public ResponseEntity<List<Complaint>> getMyComplaints() {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        CitizenLocalEntity citizen = citizenLocalJpaRepository.findByCccdNumber(cccd)
                .orElseThrow(() -> new ResourceNotFoundException("Công dân không tồn tại với số CCCD này"));

        List<Complaint> myComplaints = complaintService.getComplaintsByCitizen(citizen.getCitizenId());
        return ResponseEntity.ok(myComplaints);
    }

    /**
     * Cán bộ phản hồi khiếu nại.
     */
    @PutMapping("/{id}/respond")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LAND_OFFICER', 'ROLE_TAX_OFFICER')")
    public ResponseEntity<Complaint> respondToComplaint(
            @PathVariable Integer id,
            @RequestBody RespondRequest request) {
        Complaint updated = complaintService.respondToComplaint(id, request.getResponseNote());
        return ResponseEntity.ok(updated);
    }

    /**
     * Cán bộ xem toàn bộ danh sách khiếu nại.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LAND_OFFICER', 'ROLE_TAX_OFFICER')")
    public ResponseEntity<List<Complaint>> getAllComplaints() {
        List<Complaint> complaints = complaintService.getAllComplaints();
        return ResponseEntity.ok(complaints);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitRequest {
        private Integer recordId;
        private String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RespondRequest {
        private String responseNote;
    }
}
