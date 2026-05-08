package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.ComplaintEntity;
import com.thanglong.landtax.usecase.service.ComplaintService;
import com.thanglong.landtax.usecase.dto.ComplaintResponse;
import com.thanglong.landtax.usecase.dto.ResolveComplaintRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping("/complaints")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ComplaintEntity> createComplaint(@RequestBody Map<String, Object> request) {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        ComplaintEntity created = complaintService.createComplaint(cccd, request);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/complaints/me")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<List<ComplaintEntity>> getMyComplaints() {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        List<ComplaintEntity> complaints = complaintService.getMyComplaints(cccd);
        return ResponseEntity.ok(complaints);
    }

    @GetMapping("/admin/complaints")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LAND_OFFICER')")
    public ResponseEntity<List<ComplaintEntity>> getAllComplaints() {
        List<ComplaintEntity> complaints = complaintService.getAllComplaints();
        return ResponseEntity.ok(complaints);
    }

    @PutMapping("/admin/complaints/{id}/resolve")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LAND_OFFICER')")
    public ResponseEntity<ComplaintResponse> resolveComplaint(@PathVariable Integer id, @RequestBody ResolveComplaintRequest request) {
        ComplaintResponse updated = complaintService.resolveComplaint(id, request);
        return ResponseEntity.ok(updated);
    }
}
