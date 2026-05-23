package com.thanglong.landtax.domain.repository;

import com.thanglong.landtax.domain.model.Complaint;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface cho Khiếu nại (Complaint).
 */
public interface ComplaintRepository {

    Optional<Complaint> findById(Integer id);

    List<Complaint> findByCitizenId(Integer citizenId);

    List<Complaint> findByComplaintType(String type);

    List<Complaint> findAll();

    Complaint save(Complaint complaint);
}
