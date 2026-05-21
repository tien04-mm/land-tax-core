package com.thanglong.landtax;

import com.thanglong.landtax.domain.model.Complaint;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository;
import com.thanglong.landtax.usecase.service.ComplaintService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test kiểm thử luồng nghiệp vụ khiếu nại (Complaints) hoạt động chính xác.
 */
@SpringBootTest
@Transactional
public class ComplaintIntegrationTest {

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private CitizenLocalJpaRepository citizenLocalJpaRepository;

    @Test
    public void testSubmitAndResolveComplaint() {
        List<CitizenLocalEntity> citizens = citizenLocalJpaRepository.findAll();
        if (citizens.isEmpty()) {
            System.out.println("No citizens found in DB, skipping test details");
            return;
        }

        CitizenLocalEntity citizen = citizens.get(0);
        Integer citizenId = citizen.getCitizenId();

        // 1. Submit Complaint
        Complaint complaint = Complaint.builder()
                .citizenId(citizenId)
                .content("Test khiếu nại về tiền thuế đất")
                .build();

        Complaint saved = complaintService.submitComplaint(complaint);
        assertNotNull(saved.getId());
        assertEquals("PENDING", saved.getStatus());
        assertEquals("Test khiếu nại về tiền thuế đất", saved.getContent());
        assertNotNull(saved.getCreatedAt());

        // 2. Query My Complaints
        List<Complaint> myComplaints = complaintService.getComplaintsByCitizen(citizenId);
        assertFalse(myComplaints.isEmpty());
        assertTrue(myComplaints.stream().anyMatch(c -> c.getId().equals(saved.getId())));

        // 3. Officer Response
        Complaint resolved = complaintService.respondToComplaint(saved.getId(), "Phản hồi giải quyết khiếu nại");
        assertEquals("RESOLVED", resolved.getStatus());
        assertEquals("Phản hồi giải quyết khiếu nại", resolved.getResponseNote());
        assertNotNull(resolved.getUpdatedAt());
    }
}
