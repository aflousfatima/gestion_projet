package com.collaboration.collaborationservice.message.service;


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

    public Map uploadAudio(MultipartFile file) throws IOException {
        Map options = ObjectUtils.asMap(
                "folder", "AGILIA/audio", // Dossier spécifique pour les audios
                "access_mode", "authenticated",
                "resource_type", "video" // Utiliser "video" pour les fichiers audio
        );
        return cloudinary.uploader().upload(file.getBytes(), options);
    }

    public Map deleteFile(String publicId) throws IOException {
        return cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "video"));
    }

    public String generateSignedUrl(String publicId) {
        return cloudinary.url()
                .resourceType("video")
                .secure(true)
                .signed(true)
                .publicId(publicId)
                .generate();
    }
}