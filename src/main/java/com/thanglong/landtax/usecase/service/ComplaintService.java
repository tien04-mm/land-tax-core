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
        log.info("T o khi u n i m i cho CCCD: {}", cccd);
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
            "C ng d n g i khi u n i v i ti u  : " + request.get("title"));
            
        return saved;
    }

    public List<ComplaintEntity> getMyComplaints(String cccd) {
        log.info("L y danh s ch khi u n i c a CCCD: {}", cccd);
        return complaintRepository.findByCccdNumberOrderByCreatedAtDesc(cccd);
    }

    public List<ComplaintEntity> getAllComplaints() {
        log.info("Admin l y to n b  danh s ch khi u n i");
        return complaintRepository.findAll();
    }

    @Transactional
    public ComplaintResponse resolveComplaint(Integer id, ResolveComplaintRequest request) {
        log.info("Admin x  l  khi u n i ID: {}", id);
        ComplaintEntity complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kh ng t m th y khi u n i"));
        
        complaint.setStatus(request.getResolutionStatus());
        complaint.setAdminResponse(request.getAdminResponse());
        
        ComplaintEntity savedComplaint = complaintRepository.save(complaint);
        
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogService.log("RESOLVE_COMPLAINT", "COMPLAINT", 
            String.valueOf(id), 
            "C n b   a ch nh " + cccd + "   ph n h i khi u n i " + id + ". N i dung: " + request.getAdminResponse());

        // G i th ng b o t   ng cho ng i d n
        try {
            Integer citizenId = syncUserFromVneidUseCase.syncAndGetCitizenId(complaint.getCccdNumber());
            notificationService.createNotification(
                citizenId,
                complaint.getCccdNumber(),
                "K t qu  gi i quy t khi u n i",
                "Khi u n i '" + complaint.getTitle() + "'    c x  l . N i dung: " + request.getAdminResponse(),
                "COMPLAINT_RESOLVED"
            );
        } catch (Exception e) {
            log.error("L i g i th ng b o cho khi u n i {}: {}", id, e.getMessage());
        }

        return mapper.toResponse(savedComplaint);
    }
}

