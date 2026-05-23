package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.domain.model.Complaint;
import com.thanglong.landtax.domain.repository.ComplaintRepository;
import com.thanglong.landtax.domain.service.NotificationService;
import com.thanglong.landtax.infrastructure.adapter.controller.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RecordJpaRepository recordJpaRepository;

    /**
     * Gửi khiếu nại mới (Công dân).
     */
    @Transactional
    public Complaint submitComplaint(Complaint complaint) {
        log.info("Submit new complaint for citizenId={}, recordId={}",
                complaint.getCitizenId(), complaint.getRecordId());

        complaint.setStatus("PENDING");
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setUpdatedAt(LocalDateTime.now());

        Complaint saved = complaintRepository.save(complaint);

        auditLogService.log("SUBMIT_COMPLAINT", "COMPLAINT",
                String.valueOf(saved.getId()),
                "Công dân gửi khiếu nại mới");

        return saved;
    }

    /**
     * Cán bộ phản hồi khiếu nại.
     */
    @Transactional
    public Complaint respondToComplaint(Integer complaintId, String responseNote) {
        log.info("Officer responding to complaintId={}", complaintId);

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Khiếu nại không tồn tại"));

        complaint.setResponseNote(responseNote);
        complaint.setStatus("RESOLVED");
        complaint.setUpdatedAt(LocalDateTime.now());

        Complaint saved = complaintRepository.save(complaint);

        auditLogService.log("RESPOND_COMPLAINT", "COMPLAINT",
                String.valueOf(complaintId),
                "Cán bộ phản hồi khiếu nại");

        // Gửi thông báo tự động cho người dân
        try {
            String notifyContent = complaint.getRecordId() != null
                    ? "Khiếu nại của bạn về hồ sơ #" + complaint.getRecordId() + " đã được phản hồi: " + responseNote
                    : "Khiếu nại của bạn đã được phản hồi: " + responseNote;

            notificationService.createNotification(
                    complaint.getCitizenId(),
                    "Khiếu nại đã được giải quyết",
                    notifyContent,
                    "COMPLAINT_RESOLVED"
            );
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo giải quyết khiếu nại ID={}: {}", complaintId, e.getMessage());
        }

        return saved;
    }

    /**
     * Lấy danh sách khiếu nại của 1 công dân.
     */
    public List<Complaint> getComplaintsByCitizen(Integer citizenId) {
        log.info("Fetching complaints for citizenId={}", citizenId);
        return complaintRepository.findByCitizenId(citizenId);
    }

    /**
     * Lấy toàn bộ danh sách khiếu nại (Cán bộ) có bộ lọc theo vai trò.
     */
    public List<Complaint> getComplaintsWithRoleFilter(String type) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isTaxOfficer = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TAX_OFFICER"));
        boolean isLandOfficer = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_LAND_OFFICER"));

        if (isTaxOfficer) {
            type = "TAX";
        } else if (isLandOfficer) {
            type = "LAND";
        }

        log.info("Fetching complaints with role filter type={}", type);
        return complaintRepository.findByComplaintType(type);
    }

    /**
     * Cập nhật khiếu nại theo phân quyền.
     */
    @Transactional
    public Complaint updateComplaint(Integer id, String status, String responseNote) {
        log.info("Updating complaint id={}", id);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isTaxOfficer = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TAX_OFFICER"));
        boolean isLandOfficer = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_LAND_OFFICER"));

        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khiếu nại không tồn tại"));

        boolean isTaxComplaint = false;
        if (complaint.getRecordId() != null) {
            var recordOpt = recordJpaRepository.findById(complaint.getRecordId());
            if (recordOpt.isPresent() && "TAX_DECLARATION".equals(recordOpt.get().getRecordCategory())) {
                isTaxComplaint = true;
            }
        }

        if (isTaxOfficer) {
            if (!isTaxComplaint) {
                throw new org.springframework.security.access.AccessDeniedException("Cán bộ thuế chỉ được phản hồi khiếu nại về thuế");
            }
            complaint.setResponseNote(responseNote);
            complaint.setStatus("RESOLVED");
        } else if (isLandOfficer) {
            if (isTaxComplaint) {
                throw new org.springframework.security.access.AccessDeniedException("Cán bộ địa chính chỉ được phản hồi khiếu nại về đất");
            }
            complaint.setResponseNote(responseNote);
            complaint.setStatus("RESOLVED");
        } else if (isAdmin) {
            if (status != null) {
                complaint.setStatus(status);
            }
            if (responseNote != null) {
                complaint.setResponseNote(responseNote);
            }
        } else {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền cập nhật khiếu nại này");
        }

        complaint.setUpdatedAt(LocalDateTime.now());
        Complaint saved = complaintRepository.save(complaint);

        auditLogService.log("UPDATE_COMPLAINT", "COMPLAINT", String.valueOf(id),
                "Cập nhật khiếu nại với status=" + saved.getStatus());

        try {
            String notifyContent = saved.getRecordId() != null
                    ? "Khiếu nại của bạn về hồ sơ #" + saved.getRecordId() + " đã được cập nhật: " + responseNote
                    : "Khiếu nại của bạn đã được cập nhật: " + responseNote;

            notificationService.createNotification(
                    saved.getCitizenId(),
                    "Khiếu nại đã được cập nhật",
                    notifyContent,
                    "COMPLAINT_UPDATED"
            );
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo cập nhật khiếu nại ID={}: {}", id, e.getMessage());
        }

        return saved;
    }
}
