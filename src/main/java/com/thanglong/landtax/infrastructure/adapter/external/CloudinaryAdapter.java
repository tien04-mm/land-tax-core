package com.thanglong.landtax.infrastructure.adapter.external;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Tích hợp Cloudinary cho upload và quản lý hình ảnh.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryAdapter {

    private final Cloudinary cloudinary;

    /**
     * Upload file lên Cloudinary.
     */
    @SuppressWarnings("unchecked")
    public String uploadFile(MultipartFile file, String folder) {
        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "auto"
                    )
            );
            String url = (String) uploadResult.get("secure_url");
            log.info("File uploaded to Cloudinary: {}", url);
            return url;
        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary: {}", e.getMessage());
            throw new RuntimeException("Upload file thất bại: " + e.getMessage());
        }
    }

    /**
     * Xóa file từ Cloudinary.
     */
    public void deleteFile(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("File deleted from Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.error("Failed to delete file from Cloudinary: {}", e.getMessage());
        }
    }
}
