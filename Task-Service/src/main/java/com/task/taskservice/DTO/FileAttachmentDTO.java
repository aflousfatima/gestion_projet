package com.task.taskservice.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class FileAttachmentDTO {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("fileName")
    private String fileName;

    @JsonProperty("fileType")
    private String fileType;

    @JsonProperty("fileSize")
    private Long fileSize;

    @JsonProperty("fileUrl")
    private String fileUrl;

    @JsonProperty("publicId")
    private String publicId;

    @JsonProperty("uploadedBy")
    private String uploadedBy;

    @JsonProperty("uploadedAt")
    private LocalDateTime uploadedAt;

    // Constructors
    public FileAttachmentDTO() {}

    public FileAttachmentDTO(Long id, String fileName, String fileType, Long fileSize,
                             String fileUrl, String publicId, String uploadedBy, LocalDateTime uploadedAt) {
        this.id = id;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.fileUrl = fileUrl;
        this.publicId = publicId;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}