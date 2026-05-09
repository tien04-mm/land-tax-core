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
 * File Controller a Quan ly upload/download file vat ly.
 *
 * <ul>
 *   <li>POST /api/files/upload     a Upload file, luu v o /uploads, tao ban ghi attachments</li>
 *   <li>GET  /api/files/{filename} a Download/xem file dA upload</li>
 *   <li>GET  /api/files/my-files   a Danh sach file coa nguoi dAng hien tai</li>
 *   <li>DELETE /api/files/{id}     a Xoa file (xoa ban ghi + file vat ly)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class FileController {

    private final AttachmentJpaRepository attachmentJpaRepository;

    /** Thu moc luu file vat ly, cau hinh trong application.yml. Mac dinh: ./uploads */
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /** Base URL coa server, dAng de tao file_url tra vo cho Frontend */
    @Value("${app.server.base-url:http://localhost:8080}")
    private String serverBaseUrl;

    // Cac dinh dang file duoc phAp upload
    private static final long MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024L; // 20 MB
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
    // POST /api/files/upload
    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

    /**
     * Upload file lAn server.
     *
     * <ol>
     *   <li>Validate file (kAch thuoc, loai MIME)</li>
     *   <li>Tao tAn file duy nhat (UUID + extension)</li>
     *   <li>Luu file v o thu moc /uploads trAn server</li>
     *   <li>Tao ban ghi trong bang attachments</li>
     *   <li>Tra vo file_url de Frontend so dong</li>
     * </ol>
     *
     * @param file             File can upload (multipart/form-data)
     * @param relatedEntityType Loai thoc the liAn kat (LAND_PARCEL, RECORD,...) - tuy chon
     * @param relatedEntityId   ID thoc the liAn kat - tuy chon
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

        // aa 1. Validate file aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File khAng duoc de trong"));
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", String.format("File vuot qua kAch thuoc toi da %d MB", MAX_FILE_SIZE_BYTES / 1024 / 1024)
            ));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "inh dang file khAng duoc ho tro. Cho chap nhan: anh, PDF, Word, Excel"
            ));
        }

        // aa 2. Tao tAn file duy nhat aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
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

        // aa 3. Luu file v o thu moc /uploads aaaaaaaaaaaaaaaaaaaaaaaaa
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            Path targetPath = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("A luu file vat ly: {}", targetPath);

        } catch (IOException e) {
            log.error("Loi luu file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "KhAng the luu file lAn server: " + e.getMessage()
            ));
        }

        // aa 4. Tao ban ghi trong bang attachments aaaaaaaaaaaaaaaaaaaa
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
        log.info("A tao ban ghi attachment id={}, fileUrl={}", saved.getAttachmentId(), fileUrl);

        // aa 5. Tra vo kat qua aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        return ResponseEntity.ok(Map.of(
                "attachmentId",    saved.getAttachmentId(),
                "originalFilename", saved.getOriginalFilename(),
                "file_url",        fileUrl,          // key "file_url" nhu spec yAu cau
                "fileUrl",         fileUrl,
                "contentType",     contentType,
                "fileSize",        file.getSize(),
                "uploadedAt",      saved.getUploadedAt().toString(),
                "message",         "Upload file th nh cAng"
        ));
    }

    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
    // GET /api/files/{filename}
    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

    /**
     * Download hoac xem file dA upload theo tAn file dA luu trAn server.
     * Frontend co the dAng URL n y de hien thi anh hoac tai t i lieu.
     */
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        log.info("GET /api/files/{}", filename);

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath   = uploadPath.resolve(filename).normalize();

            // Ngn path traversal attack
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
                            "inline; filename=\"" + (resource.getFilename() != null ? resource.getFilename() : "file") + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("URL khAng hop le: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("Loi doc file: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
    // GET /api/files/my-files
    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

    /** Lay danh sach file m  nguoi dAng hien tai dA upload. */
    @GetMapping("/my-files")
    public ResponseEntity<?> getMyFiles() {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("GET /api/files/my-files - cccd={}", cccd);

        List<AttachmentEntity> myFiles = attachmentJpaRepository.findByUploadedBy(cccd);
        return ResponseEntity.ok(Map.of("data", myFiles, "total", myFiles.size()));
    }

    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
    // DELETE /api/files/{id}
    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

    /** Xoa file (ban ghi DB + file vat ly). Cho cho so huu hoac ADMIN. */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable Long id) {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN"));

        log.info("DELETE /api/files/delete/{} - requester={}", id, cccd);

        return attachmentJpaRepository.findById(id)
                .<ResponseEntity<?>>map(attachment -> {
                    // Kiem tra quyon
                    if (!isAdmin && !cccd.equals(attachment.getUploadedBy())) {
                        return ResponseEntity.status(403).body(Map.of(
                                "error", "Ban khAng co quyon xoa file n y"
                        ));
                    }

                    // Xoa file vat ly
                    try {
                        Path filePath = Paths.get(uploadDir).toAbsolutePath()
                                .resolve(attachment.getStoredFilename()).normalize();
                        Files.deleteIfExists(filePath);
                        log.info("A xoa file vat ly: {}", filePath);
                    } catch (IOException e) {
                        log.warn("KhAng the xoa file vat ly {}: {}", attachment.getStoredFilename(), e.getMessage());
                    }

                    // Xoa ban ghi DB
                    attachmentJpaRepository.deleteById(id);
                    log.info("A xoa ban ghi attachment id={}", id);

                    return ResponseEntity.ok(Map.of(
                            "deletedId", id,
                            "message",   "Xoa file th nh cAng"
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

