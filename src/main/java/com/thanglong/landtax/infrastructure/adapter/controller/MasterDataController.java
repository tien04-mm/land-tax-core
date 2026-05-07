package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AreaEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandTypeEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AreaJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandTypeJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Master Data Controller – cung cấp dữ liệu nền cho Frontend.
 *
 * <ul>
 *   <li>GET /api/master-data/areas      – Danh sách Quận/Huyện/Khu vực</li>
 *   <li>GET /api/master-data/land-types – Danh sách Loại đất (dùng cho Combobox)</li>
 * </ul>
 *
 * <p>Các endpoint này KHÔNG yêu cầu xác thực để Frontend có thể gọi khi render form.</p>
 */
@RestController
@RequestMapping("/api/master-data")
@RequiredArgsConstructor
@Slf4j
public class MasterDataController {

    private final AreaJpaRepository areaJpaRepository;
    private final LandTypeJpaRepository landTypeJpaRepository;

    // ──────────────────────────────────────────────────────────────────
    // GET /api/master-data/areas
    // ──────────────────────────────────────────────────────────────────

    /**
     * Trả về toàn bộ danh sách Khu vực (areas) từ DB.
     * Frontend dùng để hiển thị Combobox chọn Quận/Huyện.
     *
     * @param districtCode (tuỳ chọn) lọc theo mã quận/huyện
     */
    @GetMapping("/areas")
    public ResponseEntity<?> getAreas(
            @RequestParam(required = false) String districtCode) {

        log.info("GET /api/master-data/areas — districtCode={}", districtCode);

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

    // ──────────────────────────────────────────────────────────────────
    // GET /api/master-data/land-types
    // ──────────────────────────────────────────────────────────────────

    /**
     * Trả về toàn bộ danh sách Loại đất (land_types) từ DB.
     * Frontend dùng để render Combobox "Loại đất" trong form kê khai.
     *
     * @param taxPaymentOnly (tuỳ chọn) nếu true → chỉ trả loại đất phải nộp thuế
     */
    @GetMapping("/land-types")
    public ResponseEntity<?> getLandTypes(
            @RequestParam(required = false, defaultValue = "false") boolean taxPaymentOnly) {

        log.info("GET /api/master-data/land-types — taxPaymentOnly={}", taxPaymentOnly);

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
}
