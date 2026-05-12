package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AccountJpaRepository;
import com.thanglong.landtax.usecase.dto.UserAdminDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AccountJpaRepository accountJpaRepository;

    public List<UserAdminDTO> getAllUsers(String search) {
        return accountJpaRepository.findAllWithCitizenInfo(search);
    }
}
