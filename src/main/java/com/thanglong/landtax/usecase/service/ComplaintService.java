package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.domain.model.Complaint;
import com.thanglong.landtax.domain.repository.ComplaintRepository;
import com.thanglong.landtax.domain.service.NotificationService;
import com.thanglong.landtax.infrastructure.adapter.controller.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

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
                    null,
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
     * Lấy toàn bộ danh sách khiếu nại (Cán bộ).
     */
    public List<Complaint> getAllComplaints() {
        log.info("Fetching all complaints");
        return complaintRepository.findAll();
    }
}
