package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandParcelEntity;
import com.thanglong.landtax.usecase.service.LandParcelImportService;
import com.thanglong.landtax.usecase.service.LandParcelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/land-parcels")
@RequiredArgsConstructor
@Slf4j
public class LandParcelController {

    private final LandParcelService landParcelService;
    private final LandParcelImportService landParcelImportService;

    @GetMapping("/my-assets")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<?> getMyLandParcels() {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("GET /api/land-parcels/my-assets  owner_cccd={}", cccd);
        List<LandParcelEntity> myParcels = landParcelService.getMyLandParcels(cccd);
        return ResponseEntity.ok(Map.of(
                "data", myParcels,
                "message", "Lay danh sach thua dat thanh cong"
        ));
    }

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    @com.thanglong.landtax.infrastructure.config.aop.AuditLog(action = "Import Excel thua dat")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAND_OFFICER')")
    public ResponseEntity<?> importLandParcels(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File khong duoc de trong"));
        }
        try {
            int count = landParcelImportService.importFromExcel(file);
            return ResponseEntity.ok(Map.of("message", "Import thanh cong", "imported", count));
        } catch (Exception e) {
            log.error("Loi import Excel: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Loi xu ly file Excel: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchParcels(
            @RequestParam(required = false) String mapSheet,
            @RequestParam(required = false) String parcelNumber) {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isOfficer = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN") || a.getAuthority().contains("OFFICER") || a.getAuthority().contains("LAND_OFFICER"));

        List<LandParcelEntity> results = landParcelService.getAllParcels();
        
        if (mapSheet != null) {
            results = results.stream().filter(p -> mapSheet.equals(p.getMapSheetNumber())).collect(Collectors.toList());
        }
        if (parcelNumber != null) {
            results = results.stream().filter(p -> parcelNumber.equals(p.getParcelNumber())).collect(Collectors.toList());
        }

        if (!isOfficer) {
            results = results.stream()
                    .filter(p -> cccd.equals(p.getOwnerCccd()))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(Map.of("data", results, "total", results.size()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAND_OFFICER')")
    public ResponseEntity<?> getAllParcelsForOfficer() {
        List<LandParcelEntity> allParcels = landParcelService.getAllParcels();
        return ResponseEntity.ok(Map.of("data", allParcels, "total", allParcels.size()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LAND_OFFICER')")
    public ResponseEntity<?> getAllParcels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<LandParcelEntity> allParcels = landParcelService.getAllParcels();
        return ResponseEntity.ok(Map.of("content", allParcels, "totalElements", allParcels.size()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getParcelById(@PathVariable Integer id) {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isOfficer = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN") || a.getAuthority().contains("LAND_OFFICER"));

        Optional<LandParcelEntity> parcelOpt = landParcelService.getParcelById(id);
        if (parcelOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        LandParcelEntity parcel = parcelOpt.get();
        if (!isOfficer && !cccd.equals(parcel.getOwnerCccd())) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden", "message", "Ban khong co quyen xem thua dat nay"));
        }

        return ResponseEntity.ok(Map.of("data", parcel));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LAND_OFFICER')")
    public ResponseEntity<?> createParcel(@RequestBody LandParcelEntity entity) {
        log.info("Tao moi thua dat: {}", entity.getParcelNumber());
        LandParcelEntity created = landParcelService.createParcel(entity);
        return ResponseEntity.ok(Map.of("data", created, "message", "Tao thua dat thanh cong"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAND_OFFICER')")
    public ResponseEntity<?> updateParcel(@PathVariable Integer id, @RequestBody LandParcelEntity updatedEntity) {
        log.info("Cap nhat thua dat ID: {}", id);
        LandParcelEntity updated = landParcelService.updateParcel(id, updatedEntity);
        return ResponseEntity.ok(Map.of("data", updated, "message", "Cap nhat thanh cong"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @com.thanglong.landtax.infrastructure.config.aop.AuditLog(action = "Xoa thua dat")
    public ResponseEntity<?> deleteParcel(@PathVariable Integer id) {
        landParcelService.deleteParcel(id);
        return ResponseEntity.ok(Map.of("message", "Xoa thanh cong"));
    }
}
