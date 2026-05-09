package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AreaEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandTypeEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AreaJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandTypeJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Master Data Controller  cung cap du lieu nen cho Frontend.
 *
 * <ul>
 *   <li>GET /api/master-data/areas       Danh sach Quan/Huyen/Khu vuc</li>
 *   <li>GET /api/master-data/land-types  Danh sach Loai dat (dung cho Combobox)</li>
 * </ul>
 *
 * <p>Cac endpoint nay KHONG yeu cau xac thuc de Frontend co the goi khi render form.</p>
 */
@RestController
@RequestMapping("/api/master-data")
@RequiredArgsConstructor
@Slf4j
public class MasterDataController {

    private final AreaJpaRepository areaJpaRepository;
    private final LandTypeJpaRepository landTypeJpaRepository;

    // 
    // GET /api/master-data/areas
    // 

    /**
     * Tra ve toan bo danh sach Khu vuc (areas) tu DB.
     * Frontend dung de hien thi Combobox chon Quan/Huyen.
     *
     * @param districtCode (tuy chon) loc theo ma quan/huyen
     */
    @GetMapping("/areas")
    public ResponseEntity<?> getAreas(
            @RequestParam(required = false) String districtCode) {

        log.info("GET /api/master-data/areas  districtCode={}", districtCode);

        List<AreaEntity> areas;
        if (districtCode != null && !districtCode.isBlank()) {
            areas = areaJpaRepository.findByDistrictCode(districtCode);
        } else {
            areas = areaJpaRepository.findAll();
        }

        return ResponseEntity.ok(Map.of(
                "data",  areas,
                "total", areas.size()
        ));
    }

    // 
    // GET /api/master-data/land-types
    // 

    /**
     * Tra ve toan bo danh sach Loai dat (land_types) tu DB.
     * Frontend dung de render Combobox "Loai dat" trong form ke khai.
     *
     * @param taxPaymentOnly (tuy chon) neu true  chi tra loai dat phai nop thue
     */
    @GetMapping("/land-types")
    public ResponseEntity<?> getLandTypes(
            @RequestParam(required = false, defaultValue = "false") boolean taxPaymentOnly) {

        log.info("GET /api/master-data/land-types  taxPaymentOnly={}", taxPaymentOnly);

        List<LandTypeEntity> landTypes = landTypeJpaRepository.findAll();

        if (taxPaymentOnly) {
            landTypes = landTypes.stream()
                    .filter(lt -> Boolean.TRUE.equals(lt.getIsTaxPayment()))
                    .toList();
        }

        return ResponseEntity.ok(Map.of(
                "data",  landTypes,
                "total", landTypes.size()
        ));
    }

    /**
     * Tao mot Loai dat (LandType) moi.
     * Chi danh cho ROLE_ADMIN.
     */
    @PostMapping("/land-types")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> createLandType(@RequestBody Map<String, Object> req) {
        log.info("POST /api/master-data/land-types  body={}", req);

        LandTypeEntity entity = LandTypeEntity.builder()
                .typeCode((String) req.get("typeCode"))
                .typeName((String) req.get("typeName"))
                .isTaxPayment((Boolean) req.get("isTaxPayment"))
                .build();

        LandTypeEntity saved = landTypeJpaRepository.save(entity);
        return ResponseEntity.ok(Map.of(
                "data", saved,
                "message", "Tao loai dat thanh cong"
        ));
    }
}
