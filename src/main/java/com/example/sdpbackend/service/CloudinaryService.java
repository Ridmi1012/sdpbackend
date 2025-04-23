package com.example.sdpbackend.service;


import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryService {
    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret));
    }

    public String uploadImage(MultipartFile file) {
        try {
            // Generate a unique public ID for the image
            String publicId = "design_" + UUID.randomUUID().toString();

            // Upload to Cloudinary
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "folder", "designs"
                    )
            );

            // Return the secure URL
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image to Cloudinary", e);
        }
    }

    public void deleteImage(String imageUrl) {
        // Only attempt to delete if the URL is a Cloudinary URL
        if (imageUrl != null && imageUrl.contains("cloudinary.com")) {
            try {
                // Extract public_id from URL
                String publicId = extractPublicIdFromUrl(imageUrl);
                if (publicId != null) {
                    cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete image from Cloudinary", e);
            }
        }
    }

    private String extractPublicIdFromUrl(String imageUrl) {
        // Example URL: https://res.cloudinary.com/dfbsh7nzm/image/upload/v1234567890/designs/design_abcdef
        if (imageUrl != null && imageUrl.contains("/upload/")) {
            String[] parts = imageUrl.split("/upload/");
            if (parts.length > 1) {
                // Remove version number if present
                String path = parts[1].replaceAll("^v\\d+/", "");
                return path; // This should be something like "designs/design_abcdef"
            }
        }
        return null;
    }
}
