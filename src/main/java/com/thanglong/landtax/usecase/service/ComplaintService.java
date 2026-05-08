package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.ComplaintEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.ComplaintJpaRepository;
import com.thanglong.landtax.usecase.dto.ComplaintResponse;
import com.thanglong.landtax.usecase.dto.ResolveComplaintRequest;
import com.thanglong.landtax.usecase.mapper.ComplaintMapper;
import com.thanglong.landtax.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class ComplaintService {

    private final ComplaintJpaRepository complaintRepository;
    private final NotificationService notificationService;
    private final SyncUserFromVneidUseCase syncUserFromVneidUseCase;
    private final ComplaintMapper mapper;
    private final AuditLogService auditLogService;

    public ComplaintEntity createComplaint(String cccd, Map<String, Object> request) {
        log.info("Tạo khiếu nại mới cho CCCD: {}", cccd);
        ComplaintEntity complaint = ComplaintEntity.builder()
                .cccdNumber(cccd)
                .title((String) request.get("title"))
                .content((String) request.get("content"))
                .taxDeclarationId((Integer) request.get("taxDeclarationId"))
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        ComplaintEntity saved = complaintRepository.save(complaint);
        
        auditLogService.log("CREATE_COMPLAINT", "COMPLAINT", 
            String.valueOf(saved.getId()), 
            "Công dân gửi khiếu nại với tiêu đề: " + request.get("title"));
            
        return saved;
    }

    public List<ComplaintEntity> getMyComplaints(String cccd) {
        log.info("Lấy danh sách khiếu nại của CCCD: {}", cccd);
        return complaintRepository.findByCccdNumberOrderByCreatedAtDesc(cccd);
    }

    public List<ComplaintEntity> getAllComplaints() {
        log.info("Admin lấy toàn bộ danh sách khiếu nại");
        return complaintRepository.findAll();
    }

    @Transactional
    public ComplaintResponse resolveComplaint(Integer id, ResolveComplaintRequest request) {
        log.info("Admin xử lý khiếu nại ID: {}", id);
        ComplaintEntity complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khiếu nại"));
        
        complaint.setStatus(request.getResolutionStatus());
        complaint.setAdminResponse(request.getAdminResponse());
        
        ComplaintEntity savedComplaint = complaintRepository.save(complaint);
        
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.log("RESOLVE_COMPLAINT", "COMPLAINT", 
            String.valueOf(id), 
            "Cán bộ địa chính " + cccd + " đã phản hồi khiếu nại " + id + ". Nội dung: " + request.getAdminResponse());

        // Gửi thông báo tự động cho người dân
        try {
            Integer citizenId = syncUserFromVneidUseCase.syncAndGetCitizenId(complaint.getCccdNumber());
            notificationService.createNotification(
                citizenId,
                complaint.getCccdNumber(),
                "Kết quả giải quyết khiếu nại",
                "Khiếu nại '" + complaint.getTitle() + "' đã được xử lý. Nội dung: " + request.getAdminResponse(),
                "COMPLAINT_RESOLVED"
            );
        } catch (Exception e) {
            log.error("Lỗi gửi thông báo cho khiếu nại {}: {}", id, e.getMessage());
        }

        return mapper.toResponse(savedComplaint);
    }
}
