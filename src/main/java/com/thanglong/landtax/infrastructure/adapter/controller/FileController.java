package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RecordDocumentEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RecordDocumentJpaRepository;
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
 * File Controller — Quan ly upload/download file vat ly.
 *
 * <ul>
 *   <li>POST /api/files/upload     — Upload file, luu vao /uploads, tao ban ghi record_documents</li>
 *   <li>GET  /api/files/{filename} — Download/xem file da upload</li>
 *   <li>GET  /api/files/my-files   — Danh sach file cua nguoi dung hien tai</li>
 *   <li>DELETE /api/files/{id}     — Xoa file (xoa ban ghi + file vat ly)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class FileController {

    private final RecordDocumentJpaRepository recordDocumentJpaRepository;

    /** Thu moc luu file vat ly, cau hinh trong application.yml. Mac dinh: ./uploads */
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /** Base URL cua server, dung de tao file_url tra ve cho Frontend */
    @Value("${app.server.base-url:http://localhost:8080}")
    private String serverBaseUrl;

    // Cac dinh dang file duoc phep upload
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
     * Upload file len server.
     *
     * <ol>
     *   <li>Validate file (kich thuoc, loai MIME)</li>
     *   <li>Tao ten file duy nhat (UUID + extension)</li>
     *   <li>Luu file vao thu moc /uploads tren server</li>
     *   <li>Tao ban ghi trong bang record_documents</li>
     *   <li>Tra ve file_url de Frontend su dung</li>
     * </ol>
     *
     * @param file             File can upload (multipart/form-data)
     * @param relatedEntityType Loai thuc the lien ket (LAND_PARCEL, RECORD,...) - tuy chon
     * @param relatedEntityId   ID thuc the lien ket - tuy chon
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "relatedEntityType", required = false) String relatedEntityType,
            @RequestParam(value = "relatedEntityId",   required = false) Long relatedEntityId) {

        String uploaderCccd = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        log.info("POST /api/files/upload - uploader={}, originalName={}, size={}",
                uploaderCccd, file.getOriginalFilename(), file.getSize());

        // — 1. Validate file ——————————————————————————————————————
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File khong duoc de trong"));
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", String.format("File vuot qua kich thuoc toi da %d MB", MAX_FILE_SIZE_BYTES / 1024 / 1024)
            ));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Dinh dang file khong duoc ho tro. Chi chap nhan: anh, PDF, Word, Excel"
            ));
        }

        // — 2. Tao ten file duy nhat ————————————————————————————
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

        // — 3. Luu file vao thu moc /uploads ————————————————————
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            Path targetPath = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Da luu file vat ly: {}", targetPath);

        } catch (IOException e) {
            log.error("Loi luu file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Khong the luu file len server: " + e.getMessage()
            ));
        }

        // — 4. Tao ban ghi trong bang record_documents ——————————
        String fileUrl = serverBaseUrl + "/api/files/" + storedFilename;

        RecordDocumentEntity document = RecordDocumentEntity.builder()
                .fileName(storedFilename)
                .fileUrl(fileUrl)
                .fileType(contentType)
                .build();

        RecordDocumentEntity saved = recordDocumentJpaRepository.save(document);
        log.info("Da tao ban ghi record_document id={}, fileUrl={}", saved.getDocumentId(), fileUrl);

        // — 5. Tra ve ket qua —
        return ResponseEntity.ok(Map.of(
                "documentId",       saved.getDocumentId(),
                "originalFilename", originalFilename,
                "fileName",         saved.getFileName(),
                "file_url",         fileUrl,
                "fileUrl",          fileUrl,
                "contentType",      contentType,
                "fileSize",         file.getSize(),
                "uploadedAt",       LocalDateTime.now().toString(),
                "message",          "Upload file thanh cong"
        ));
    }

    // ──────────────────────────────────────────────────────────────────
    // GET /api/files/{filename}
    // ──────────────────────────────────────────────────────────────────

    /**
     * Download hoac xem file da upload theo ten file da luu tren server.
     * Frontend co the dung URL nay de hien thi anh hoac tai tai lieu.
     */
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        log.info("GET /api/files/{}", filename);

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath   = uploadPath.resolve(filename).normalize();

            // Ngan path traversal attack
            if (!filePath.startsWith(uploadPath)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String detectedContentType = Files.probeContentType(filePath);
            if (detectedContentType == null) detectedContentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(detectedContentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + (resource.getFilename() != null ? resource.getFilename() : "file") + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("URL khong hop le: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("Loi doc file: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // GET /api/files/my-files
    // ──────────────────────────────────────────────────────────────────

    /** Lay danh sach file ma nguoi dung hien tai da upload. */
    @GetMapping("/my-files")
    public ResponseEntity<?> getMyFiles() {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("GET /api/files/my-files - cccd={}", cccd);

        List<RecordDocumentEntity> myFiles = recordDocumentJpaRepository.findAll();
        return ResponseEntity.ok(Map.of("data", myFiles, "total", myFiles.size()));
    }

    // ──────────────────────────────────────────────────────────────────
    // DELETE /api/files/{id}
    // ──────────────────────────────────────────────────────────────────

    /** Xoa file (ban ghi DB + file vat ly). */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable Long id) {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("DELETE /api/files/delete/{} - requester={}", id, cccd);

        return recordDocumentJpaRepository.findById(id)
                .<ResponseEntity<?>>map(document -> {
                    // Xoa file vat ly
                    try {
                        Path filePath = Paths.get(uploadDir).toAbsolutePath()
                                .resolve(document.getFileName()).normalize();
                        Files.deleteIfExists(filePath);
                        log.info("Da xoa file vat ly: {}", filePath);
                    } catch (IOException e) {
                        log.warn("Khong the xoa file vat ly {}: {}", document.getFileName(), e.getMessage());
                    }

                    // Xoa ban ghi DB
                    recordDocumentJpaRepository.deleteById(id);
                    log.info("Da xoa ban ghi record_document id={}", id);

                    return ResponseEntity.ok(Map.of(
                            "deletedId", id,
                            "message",   "Xoa file thanh cong"
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
