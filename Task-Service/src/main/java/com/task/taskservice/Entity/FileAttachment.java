package com.task.taskservice.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;
@Entity
public class FileAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fileName; // e.g., "document.pdf"
    private String contentType; // e.g., "application/pdf"
    private Long fileSize; // Size in bytes
    private String fileUrl; // URL or path to the stored file
    private String publicId; // ID unique Cloudinary pour suppression/modif
    private String uploadedBy; // Username or ID of the user who uploaded the file
    private LocalDateTime uploadedAt;

    // Constructor (empty for deserialization)

    public FileAttachment() {
    }
    // Full constructor with all fields
    public FileAttachment(Long id, String fileName, String contentType, Long fileSize,
                             String fileUrl, String publicId, String uploadedBy, LocalDateTime uploadedAt) {
        this.id = id;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.fileUrl = fileUrl;
        this.publicId = publicId;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
    }

    // Partial constructor for creating new attachments (without id and uploadedAt)
    public FileAttachment(String fileName, String contentType, Long fileSize,
                             String fileUrl, String publicId, String uploadedBy) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.fileUrl = fileUrl;
        this.publicId = publicId;
        this.uploadedBy = uploadedBy;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }


    @Override
    public String toString() {
        return "FileAttachment{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", contentType='" + contentType + '\'' +
                ", fileSize=" + fileSize +
                ", fileUrl='" + fileUrl + '\'' +
                ", publicId='" + publicId + '\'' +
                ", uploadedBy='" + uploadedBy + '\'' +
                ", uploadedAt=" + uploadedAt +
                '}';
    }
}
