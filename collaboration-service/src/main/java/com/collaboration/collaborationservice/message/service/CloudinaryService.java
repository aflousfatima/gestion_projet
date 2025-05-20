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
                "folder", "AGILIA/audio",
                "access_mode", "authenticated",
                "resource_type", "video"
        );
        return cloudinary.uploader().upload(file.getBytes(), options);
    }

    public Map uploadImage(MultipartFile file) throws IOException {
        Map options = ObjectUtils.asMap(
                "folder", "AGILIA/images",
                "access_mode", "authenticated",
                "resource_type", "image"
        );
        return cloudinary.uploader().upload(file.getBytes(), options);
    }

    public Map uploadFile(MultipartFile file) throws IOException {
        Map options = ObjectUtils.asMap(
                "folder", "AGILIA/files",
                "access_mode", "authenticated",
                "resource_type", "raw"
        );
        return cloudinary.uploader().upload(file.getBytes(), options);
    }

    public Map deleteFile(String publicId) throws IOException {
        return cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "video"));
    }

    public Map deleteImage(String publicId) throws IOException {
        return cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
    }

    public Map deleteRawFile(String publicId) throws IOException {
        return cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "raw"));
    }

    public String generateSignedUrl(String publicId) {
        return cloudinary.url()
                .resourceType("video")
                .secure(true)
                .signed(true)
                .publicId(publicId)
                .generate();
    }

    public String generateSignedImageUrl(String publicId) {
        return cloudinary.url()
                .resourceType("image")
                .secure(true)
                .signed(true)
                .publicId(publicId)
                .generate();
    }

    public String generateSignedRawUrl(String publicId) {
        return cloudinary.url()
                .resourceType("raw")
                .secure(true)
                .signed(true)
                .publicId(publicId)
                .generate();
    }
}