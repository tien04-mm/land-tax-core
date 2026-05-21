package com.thanglong.landtax;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.ComplaintEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RecordEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxDeclarationEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.ComplaintJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RecordJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxDeclarationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
public class DatabaseMappingIntegrationTest {

    @Autowired
    private RecordJpaRepository recordJpaRepository;

    @Autowired
    private TaxDeclarationRepository taxDeclarationRepository;

    @Autowired
    private ComplaintJpaRepository complaintJpaRepository;

    @Test
    public void testDatabaseMappings() {
        // Query Records
        List<RecordEntity> records = recordJpaRepository.findAll();
        assertNotNull(records, "Records list should not be null");
        if (!records.isEmpty()) {
            System.out.println("✅ Found " + records.size() + " records. Example category: " + records.get(0).getRecordCategory());
        }

        // Query Tax Declarations
        List<TaxDeclarationEntity> declarations = taxDeclarationRepository.findAll();
        assertNotNull(declarations, "Declarations list should not be null");
        if (!declarations.isEmpty()) {
            System.out.println("✅ Found " + declarations.size() + " tax declarations. Example area: " + declarations.get(0).getDeclaredArea());
        }

        // Query Complaints
        List<ComplaintEntity> complaints = complaintJpaRepository.findAll();
        assertNotNull(complaints, "Complaints list should not be null");
        if (!complaints.isEmpty()) {
            System.out.println("✅ Found " + complaints.size() + " complaints. Example status: " + complaints.get(0).getStatus());
        }
    }
}
