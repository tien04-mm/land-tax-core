package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandOwnerHistoryEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandParcelEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.MutationRequestEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandOwnerHistoryJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandParcelJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.MutationRequestJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository;
import com.thanglong.landtax.usecase.dto.MutationResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class MutationService {

    private final MutationRequestJpaRepository mutationRequestJpaRepository;
    private final LandParcelJpaRepository landParcelJpaRepository;
    private final LandOwnerHistoryJpaRepository landOwnerHistoryJpaRepository;
    private final CitizenLocalJpaRepository citizenLocalJpaRepository;

    @Transactional
    public MutationRequestEntity createMutationRequest(MutationRequestEntity request) {
        if (request == null || request.getParcelId() == null) {
            throw new IllegalArgumentException("Mutation request and parcel ID cannot be null");
        }
        log.info("Creating mutation request for parcel ID: {}", request.getParcelId());
        request.setStatus("PENDING");
        
        // Cap nhat trang thai thua dat sang IN_MUTATION
        if (!landParcelJpaRepository.existsById(request.getParcelId())) {
                throw new RuntimeException("Land parcel not found with ID " + request.getParcelId());
        }
        
        // Gia su co truong status trong LandParcelEntity. Neu khong co ta se bo qua buoc nay hoac cap nhat notes
        log.info("Changing land parcel {} status to IN_MUTATION", request.getParcelId());
        // parcel.setStatus("IN_MUTATION"); // Uncomment if status exists
        
        return mutationRequestJpaRepository.save(request);
    }

    @Transactional
    public MutationRequestEntity needMoreDocs(Long mutationId, String note) {
        log.info("Requesting additional documents for mutation ID: {}", mutationId);
        MutationRequestEntity request = mutationRequestJpaRepository.findById(mutationId)
                .orElseThrow(() -> new RuntimeException("Mutation request not found with ID " + mutationId));
        
        request.setStatus("NEED_MORE_DOCS");
        request.setReviewNote(note);
        request.setReviewedAt(LocalDateTime.now());
        
        return mutationRequestJpaRepository.save(request);
    }

    @Transactional
    public MutationRequestEntity approveMutation(Long mutationId) {
        log.info("Approving mutation ID: {}", mutationId);
        MutationRequestEntity request = mutationRequestJpaRepository.findById(mutationId)
                .orElseThrow(() -> new RuntimeException("Mutation request not found with ID " + mutationId));
        
        if ("APPROVED".equals(request.getStatus())) {
            return request;
        }

        LandParcelEntity parcel = landParcelJpaRepository.findById(request.getParcelId())
                .orElseThrow(() -> new RuntimeException("Land parcel not found with ID " + request.getParcelId()));

        String oldOwner = parcel.getOwnerCccd();
        String newOwner = request.getNewOwnerCccd();

        // 1. Cap nhat chu so huu moi vao bang land_parcels
        log.info("Updating landowner for parcel {} from {} to {}", parcel.getParcelNumber(), oldOwner, newOwner);
        parcel.setOwnerCccd(newOwner);
        // parcel.setStatus("ACTIVE"); // Return to normal active status
        landParcelJpaRepository.save(parcel);

        // 2. Tao ban ghi lich su trong bang land_owner_history
        LandOwnerHistoryEntity history = LandOwnerHistoryEntity.builder()
                .parcelId(parcel.getLandParcelId())
                .oldOwnerCccd(oldOwner)
                .newOwnerCccd(newOwner)
                .mutationType(request.getMutationType())
                .note(request.getDescription())
                .build();
        landOwnerHistoryJpaRepository.save(history);

        // 3. Cap nhat trang thai yeu cau bien dong
        request.setStatus("APPROVED");
        request.setReviewedAt(LocalDateTime.now());
        
        return mutationRequestJpaRepository.save(request);
    }

    /**
     * Lay danh sach yeu cau bien dong dat dai ho tro phan trang va loc.
     */
    public Page<MutationResponseDTO> getMutationRequests(String status, int page, int size) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated");
        }

        String cccd = auth.getName();
        boolean isCitizenOnly = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CITIZEN".equals(a.getAuthority()))
                && auth.getAuthorities().stream()
                .noneMatch(a -> "ROLE_LAND_OFFICER".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority()));

        log.info("getMutationRequests: user={}, isCitizenOnly={}, status={}, page={}, size={}", cccd, isCitizenOnly, status, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<MutationRequestEntity> entityPage;

        if (isCitizenOnly) {
            entityPage = mutationRequestJpaRepository.findByRequesterCccd(cccd, pageable);
        } else {
            if (status != null && !status.trim().isEmpty()) {
                entityPage = mutationRequestJpaRepository.findByStatus(status, pageable);
            } else {
                entityPage = mutationRequestJpaRepository.findAll(pageable);
            }
        }

        return entityPage.map(this::convertToResponseDTO);
    }

    private MutationResponseDTO convertToResponseDTO(MutationRequestEntity entity) {
        String cccd = null;
        if (entity.getSubmittedBy() != null) {
            var citizenOpt = citizenLocalJpaRepository.findById(entity.getSubmittedBy().intValue());
            if (citizenOpt.isPresent()) {
                cccd = citizenOpt.get().getCccdNumber();
            }
        }
        return MutationResponseDTO.builder()
                .id(entity.getMutationId())
                .parcelId(entity.getParcelId())
                .requesterCccd(cccd)
                .mutationType(entity.getMutationType())
                .status(entity.getStatus())
                .note(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
