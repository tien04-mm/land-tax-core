package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RoleDelegationEntity;
import com.thanglong.landtax.usecase.service.DelegationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/delegations")
@RequiredArgsConstructor
public class DelegationController {

    private final DelegationService delegationService;


    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllDelegations() {
        return ResponseEntity.ok(delegationService.getAllDelegations());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delegateRole(@RequestBody Map<String, Object> request) {
        try {
            RoleDelegationEntity delegation = delegationService.delegateRole(request);
            return ResponseEntity.ok(Map.of(
                    "data", delegation,
                    "message", "Uy quyen thanh cong"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
