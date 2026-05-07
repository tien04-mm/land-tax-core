package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AttachmentEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AttachmentJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * File Controller – Quản lý upload/download file vật lý.
 *
 * <ul>
 *   <li>POST /api/files/upload     – Upload file, lưu vào /uploads, tạo bản ghi attachments</li>
 *   <li>GET  /api/files/{filename} – Download/xem file đã upload</li>
 *   <li>GET  /api/files/my-files   – Danh sách file của người dùng hiện tại</li>
 *   <li>DELETE /api/files/{id}     – Xóa file (xóa bản ghi + file vật lý)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final AttachmentJpaRepository attachmentJpaRepository;

    /** Thư mục lưu file vật lý, cấu hình trong application.yml. Mặc định: ./uploads */
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /** Base URL của server, dùng để tạo file_url trả về cho Frontend */
    @Value("${app.server.base-url:http://localhost:8080}")
    private String serverBaseUrl;

    // Các định dạng file được phép upload
    private static final long MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024L; // 20 MB
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    // ──────────────────────────────────────────────────────────────────
    // POST /api/files/upload
    // ──────────────────────────────────────────────────────────────────

    /**
     * Upload file lên server.
     *
     * <ol>
     *   <li>Validate file (kích thước, loại MIME)</li>
     *   <li>Tạo tên file duy nhất (UUID + extension)</li>
     *   <li>Lưu file vào thư mục /uploads trên server</li>
     *   <li>Tạo bản ghi trong bảng attachments</li>
     *   <li>Trả về file_url để Frontend sử dụng</li>
     * </ol>
     *
     * @param file             File cần upload (multipart/form-data)
     * @param relatedEntityType Loại thực thể liên kết (LAND_PARCEL, RECORD,...) - tuỳ chọn
     * @param relatedEntityId   ID thực thể liên kết - tuỳ chọn
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "relatedEntityType", required = false) String relatedEntityType,
            @RequestParam(value = "relatedEntityId",   required = false) Long relatedEntityId) {

        String uploaderCccd = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        log.info("POST /api/files/upload — uploader={}, originalName={}, size={}",
                uploaderCccd, file.getOriginalFilename(), file.getSize());

        // ── 1. Validate file ──────────────────────────────────────────
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File không được để trống"));
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", String.format("File vượt quá kích thước tối đa %d MB", MAX_FILE_SIZE_BYTES / 1024 / 1024)
            ));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Định dạng file không được hỗ trợ. Chỉ chấp nhận: ảnh, PDF, Word, Excel"
            ));
        }

        // ── 2. Tạo tên file duy nhất ──────────────────────────────────
        String originalFilename = file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "unknown";
        String extension = "";
        if (originalFilename != null && !originalFilename.isEmpty()) {
            extension = originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
        }
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String storedFilename = timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

        // ── 3. Lưu file vào thư mục /uploads ─────────────────────────
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            Path targetPath = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Đã lưu file vật lý: {}", targetPath);

        } catch (IOException e) {
            log.error("Lỗi lưu file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Không thể lưu file lên server: " + e.getMessage()
            ));
        }

        // ── 4. Tạo bản ghi trong bảng attachments ────────────────────
        String fileUrl = serverBaseUrl + "/api/files/" + storedFilename;

        AttachmentEntity attachment = AttachmentEntity.builder()
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .fileUrl(fileUrl)
                .contentType(contentType)
                .fileSize(file.getSize())
                .uploadedBy(uploaderCccd)
                .uploadedAt(LocalDateTime.now())
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .build();

        AttachmentEntity saved = attachmentJpaRepository.save(attachment);
        log.info("Đã tạo bản ghi attachment id={}, fileUrl={}", saved.getAttachmentId(), fileUrl);

        // ── 5. Trả về kết quả ─────────────────────────────────────────
        return ResponseEntity.ok(Map.of(
                "attachmentId",    saved.getAttachmentId(),
                "originalFilename", saved.getOriginalFilename(),
                "file_url",        fileUrl,          // key "file_url" như spec yêu cầu
                "fileUrl",         fileUrl,
                "contentType",     contentType,
                "fileSize",        file.getSize(),
                "uploadedAt",      saved.getUploadedAt().toString(),
                "message",         "Upload file thành công"
        ));
    }

    // ──────────────────────────────────────────────────────────────────
    // GET /api/files/{filename}
    // ──────────────────────────────────────────────────────────────────

    /**
     * Download hoặc xem file đã upload theo tên file đã lưu trên server.
     * Frontend có thể dùng URL này để hiển thị ảnh hoặc tải tài liệu.
     */
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        log.info("GET /api/files/{}", filename);

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath   = uploadPath.resolve(filename).normalize();

            // Ngăn path traversal attack
            if (!filePath.startsWith(uploadPath)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("URL không hợp lệ: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("Lỗi đọc file: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // GET /api/files/my-files
    // ──────────────────────────────────────────────────────────────────

    /** Lấy danh sách file mà người dùng hiện tại đã upload. */
    @GetMapping("/my-files")
    public ResponseEntity<?> getMyFiles() {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("GET /api/files/my-files — cccd={}", cccd);

        List<AttachmentEntity> myFiles = attachmentJpaRepository.findByUploadedBy(cccd);
        return ResponseEntity.ok(Map.of("data", myFiles, "total", myFiles.size()));
    }

    // ──────────────────────────────────────────────────────────────────
    // DELETE /api/files/{id}
    // ──────────────────────────────────────────────────────────────────

    /** Xóa file (bản ghi DB + file vật lý). Chỉ chủ sở hữu hoặc ADMIN. */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable Long id) {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN"));

        log.info("DELETE /api/files/delete/{} — requester={}", id, cccd);

        return attachmentJpaRepository.findById(id)
                .<ResponseEntity<?>>map(attachment -> {
                    // Kiểm tra quyền
                    if (!isAdmin && !cccd.equals(attachment.getUploadedBy())) {
                        return ResponseEntity.status(403).body(Map.of(
                                "error", "Bạn không có quyền xóa file này"
                        ));
                    }

                    // Xóa file vật lý
                    try {
                        Path filePath = Paths.get(uploadDir).toAbsolutePath()
                                .resolve(attachment.getStoredFilename()).normalize();
                        Files.deleteIfExists(filePath);
                        log.info("Đã xóa file vật lý: {}", filePath);
                    } catch (IOException e) {
                        log.warn("Không thể xóa file vật lý {}: {}", attachment.getStoredFilename(), e.getMessage());
                    }

                    // Xóa bản ghi DB
                    attachmentJpaRepository.deleteById(id);
                    log.info("Đã xóa bản ghi attachment id={}", id);

                    return ResponseEntity.ok(Map.of(
                            "deletedId", id,
                            "message",   "Xóa file thành công"
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
