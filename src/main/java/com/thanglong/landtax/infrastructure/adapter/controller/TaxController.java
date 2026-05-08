package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.usecase.dto.ReviewDeclarationRequest;
import com.thanglong.landtax.usecase.dto.TaxDeclarationRequest;
import com.thanglong.landtax.usecase.dto.TaxDeclarationResponse;
import com.thanglong.landtax.usecase.service.ApproveDeclarationUseCase;
import com.thanglong.landtax.usecase.service.RejectDeclarationUseCase;
import com.thanglong.landtax.usecase.service.SubmitDeclarationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * REST Controller cho quản lý thuế đất.
 *
 * <p><b>API Endpoints:</b></p>
 * <ul>
 *   <li>POST /api/tax/declarations — Nộp tờ khai (Citizen)</li>
 *   <li>PUT /api/tax/declarations/{id}/approve — Duyệt tờ khai (TAX_OFFICER/ADMIN)</li>
 *   <li>PUT /api/tax/declarations/{id}/reject — Từ chối tờ khai (TAX_OFFICER/ADMIN)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/tax")
@RequiredArgsConstructor
@SuppressWarnings("null")
@Slf4j
public class TaxController {

    private final SubmitDeclarationUseCase submitDeclarationUseCase;
    private final ApproveDeclarationUseCase approveDeclarationUseCase;
    private final RejectDeclarationUseCase rejectDeclarationUseCase;
    private final com.thanglong.landtax.usecase.service.TaxDeclarationService taxDeclarationService;
    private final com.thanglong.landtax.usecase.service.TaxBillService taxBillService;
    private final com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RecordJpaRepository recordJpaRepository;
    private final com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxBillRepository taxBillRepository;
    private final com.thanglong.landtax.usecase.service.AuditLogService auditLogService;

    /**
     * Nộp tờ khai thuế đất.
     * JWT token trong Header Authorization → JwtFilter giải mã → cccd_number → citizen_id.
     */
    @Operation(summary = "Nộp tờ khai thuế đất", description = "Người dân nộp tờ khai thuế đất mới")
    @ApiResponse(responseCode = "200", description = "Nộp tờ khai thành công")
    @PostMapping("/declarations")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<TaxDeclarationResponse> submitDeclaration(
            @Valid @RequestBody TaxDeclarationRequest request) {
        log.info("User: {}, Authorities: {}", SecurityContextHolder.getContext().getAuthentication().getName(), SecurityContextHolder.getContext().getAuthentication().getAuthorities());
        TaxDeclarationResponse response = submitDeclarationUseCase.submitDeclaration(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách hồ sơ thuế đã được Địa chính xác nhận (VERIFIED).
     */
    @Operation(summary = "Danh sách hồ sơ chờ duyệt", description = "Lấy danh sách các hồ sơ đã được xác nhận (VERIFIED)")
    @GetMapping("/records/verified")
    @PreAuthorize("hasRole('TAX_OFFICER')")
    public ResponseEntity<?> getVerifiedRecords() {
        log.info("TAX_OFFICER lấy danh sách hồ sơ VERIFIED");
        List<com.thanglong.landtax.infrastructure.adapter.persistence.entity.RecordEntity> records = 
            recordJpaRepository.findByCurrentStatus("VERIFIED");
        return ResponseEntity.ok(records);
    }

    /**
     * Duyệt tờ khai thuế đất.
     * Chỉ TAX_OFFICER mới có quyền.
     *
     * @param id      record_id trong bảng records
     * @param request Ghi chú của cán bộ (tùy chọn)
     */
    @Operation(summary = "Duyệt tờ khai thuế đất", description = "Cán bộ thuế duyệt hồ sơ VERIFIED")
    @ApiResponse(responseCode = "200", description = "Duyệt hồ sơ thành công")
    @PutMapping("/records/{id}/approve")
    @PreAuthorize("hasRole('TAX_OFFICER')")
    public ResponseEntity<Map<String, Object>> approveRecord(
            @PathVariable Integer id,
            @RequestBody(required = false) ReviewDeclarationRequest request) {
        Map<String, Object> result = approveDeclarationUseCase.approveDeclaration(id, request);
        return ResponseEntity.ok(result);
    }

    /**
     * Từ chối tờ khai thuế đất.
     * Chỉ TAX_OFFICER mới có quyền.
     *
     * @param id      record_id trong bảng records
     * @param request Lý do từ chối (bắt buộc)
     */
    @Operation(summary = "Từ chối tờ khai thuế đất", description = "Cán bộ thuế từ chối hồ sơ")
    @ApiResponse(responseCode = "200", description = "Từ chối hồ sơ thành công")
    @PutMapping("/records/{id}/reject")
    @PreAuthorize("hasRole('TAX_OFFICER')")
    public ResponseEntity<Map<String, Object>> rejectRecord(
            @PathVariable Integer id,
            @RequestBody ReviewDeclarationRequest request) {
        Map<String, Object> result = rejectDeclarationUseCase.rejectDeclaration(id, request);
        return ResponseEntity.ok(result);
    }

    /**
     * Xem lịch sử tờ khai thuế đất.
     * Dành cho công dân tra cứu tờ khai của chính mình.
     */
    @Operation(summary = "Xem lịch sử tờ khai", description = "Người dân xem lịch sử các tờ khai đã nộp")
    @ApiResponse(responseCode = "200", description = "Lấy lịch sử thành công")
    @GetMapping("/declarations/my-history")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<List<TaxDeclarationResponse>> getMyHistory() {
        String cccd = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        List<TaxDeclarationResponse> history = taxDeclarationService.getMyHistory(cccd);
        return ResponseEntity.ok(history);
    }

    /**
     * Lấy chi tiết một tờ khai thuế.
     * Chỉ cho phép người dân xem tờ khai của chính mình.
     */
    @Operation(summary = "Xem chi tiết tờ khai", description = "Người dân xem chi tiết tờ khai đã nộp")
    @ApiResponse(responseCode = "200", description = "Lấy chi tiết thành công")
    @GetMapping("/declarations/{id}")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<TaxDeclarationResponse> getDeclarationById(@PathVariable Integer id) {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        TaxDeclarationResponse response = taxDeclarationService.getDeclarationById(id, cccd);
        return ResponseEntity.ok(response);
    }

    /**
     * Hủy tờ khai thuế đất.
     * Chỉ cho phép hủy nếu trạng thái là PENDING.
     */
    @Operation(summary = "Hủy tờ khai", description = "Người dân hủy tờ khai đang ở trạng thái PENDING")
    @ApiResponse(responseCode = "200", description = "Hủy tờ khai thành công")
    @DeleteMapping("/declarations/{id}/cancel")
    public ResponseEntity<?> cancelDeclaration(@PathVariable Integer id) {
        String cccd = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        taxDeclarationService.cancelDeclaration(id, cccd);
        return ResponseEntity.ok(Map.of("message", "Hủy tờ khai thành công"));
    }

    /**
     * Lấy danh sách hóa đơn thuế chưa thanh toán.
     * Dành cho công dân xem các khoản cần nộp tiền.
     */
    @Operation(summary = "Xem hóa đơn chưa thanh toán", description = "Người dân xem các hóa đơn thuế cần nộp")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách hóa đơn thành công")
    @GetMapping("/bills/unpaid")
    public ResponseEntity<List<com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxBillEntity>> getUnpaidBills() {
        String cccd = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        List<com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxBillEntity> unpaidBills = taxBillService.getUnpaidBills(cccd);
        return ResponseEntity.ok(unpaidBills);
    }

    /**
     * Lấy danh sách hóa đơn thuế ĐÃ thanh toán.
     * Dành cho công dân xem lịch sử nộp tiền.
     */
    @Operation(summary = "Xem hóa đơn đã thanh toán", description = "Người dân xem lịch sử hóa đơn thuế đã nộp")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách hóa đơn thành công")
    @GetMapping("/bills/paid")
    public ResponseEntity<List<com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxBillEntity>> getPaidBills() {
        String cccd = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        List<com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxBillEntity> paidBills = taxBillService.getPaidBills(cccd);
        return ResponseEntity.ok(paidBills);
    }

    /**
     * Cập nhật thông tin tờ khai trong quá trình kiểm duyệt (dành cho Cán bộ thuế).
     * Chỉ TAX_OFFICER/ADMIN mới có quyền sửa thông tin nhỏ trong tờ khai.
     */
    @PutMapping("/declarations/{id}/update-info")
    @PreAuthorize("hasAnyRole('ADMIN', 'TAX_OFFICER')")
    @com.thanglong.landtax.infrastructure.config.aop.AuditLog(action = "Cập nhật thông tin tờ khai")
    public ResponseEntity<?> updateDeclarationInfo(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> updates) {

        com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxDeclarationEntity entity =
            taxDeclarationService.getRepository().findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tờ khai"));

        // Chỉ cho phép sửa các trường an toàn khi đang kiểm duyệt
        if (updates.containsKey("declaredPurpose")) {
            entity.setDeclaredPurpose((String) updates.get("declaredPurpose"));
        }
        if (updates.containsKey("reviewNote")) {
            entity.setReviewNote((String) updates.get("reviewNote"));
        }
        if (updates.containsKey("phoneNumber")) {
            entity.setPhoneNumber((String) updates.get("phoneNumber"));
        }
        if (updates.containsKey("address")) {
            entity.setAddress((String) updates.get("address"));
        }

        taxDeclarationService.getRepository().save(entity);
        return ResponseEntity.ok(Map.of("message", "Cập nhật thông tin tờ khai thành công", "id", id));
    }

    /**
     * Xem toàn bộ hóa đơn thuế.
     */
    @Operation(summary = "Danh sách hóa đơn", description = "Cán bộ thuế xem danh sách tất cả hóa đơn")
    @GetMapping("/bills/all")
    @PreAuthorize("hasRole('TAX_OFFICER')")
    public ResponseEntity<List<com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxBillEntity>> getAllBills() {
        return ResponseEntity.ok(taxBillRepository.findAll());
    }

    /**
     * Xuất dữ liệu hồ sơ thuế.
     */
    @Operation(summary = "Xuất dữ liệu", description = "Cán bộ thuế xuất dữ liệu ra file CSV")
    @GetMapping(value = "/export/data", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasRole('TAX_OFFICER')")
    public ResponseEntity<byte[]> exportData() {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Cán bộ thuế {} đang xuất dữ liệu hồ sơ thuế", cccd);

        StringBuilder csvContent = new StringBuilder();
        csvContent.append("ID,CCCD,Trạng thái,Thời gian nộp\n");

        List<com.thanglong.landtax.infrastructure.adapter.persistence.entity.RecordEntity> records = recordJpaRepository.findAll();
        for (com.thanglong.landtax.infrastructure.adapter.persistence.entity.RecordEntity r : records) {
            csvContent.append(r.getRecordId()).append(",")
                      .append(r.getCitizenId()).append(",")
                      .append(r.getCurrentStatus()).append(",")
                      .append(r.getSubmittedAt()).append("\n");
        }

        auditLogService.log("EXPORT_DATA", "RECORDS", "ALL", "Cán bộ thuế " + cccd + " đã thực hiện xuất dữ liệu hồ sơ thuế");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tax_records_export.csv\"")
                .body(csvContent.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
