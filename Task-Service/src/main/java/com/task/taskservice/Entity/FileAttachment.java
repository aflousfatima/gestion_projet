package com.task.taskservice.Entity;

import java.time.LocalDateTime;

public class FileAttachment {
    private Long id;
    private String filename;         // Nom original du fichier (ex: "specs.pdf")
    private String url;              // URL renvoyée par Cloudinary
    private String publicId;         // ID unique Cloudinary pour suppression/modif
    private LocalDateTime uploadedAt;
    private Long uploadedBy;
}
