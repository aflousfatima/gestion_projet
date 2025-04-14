package com.task.taskservice.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Autowired
    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public Map uploadFile(MultipartFile file) throws IOException {
        Map options = ObjectUtils.asMap(
                "folder", "AGILIA",
                "access_mode", "authenticated", // Ensure files require signed URLs
                "resource_type", "raw" // For PDFs and other non-image files
        );
        return cloudinary.uploader().upload(file.getBytes(), options);
    }

    public Map deleteFile(String publicId) throws IOException {
        return cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    // New method to generate signed URLs
    public String generateSignedUrl(String publicId) {
        return cloudinary.url()
                .resourceType("raw")
                .secure(true)
                .signed(true)
                .publicId(publicId)
                .generate();
    }

}
