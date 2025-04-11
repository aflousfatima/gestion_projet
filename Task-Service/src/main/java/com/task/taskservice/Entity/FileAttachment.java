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
    private String filename;         // Nom original du fichier (ex: "specs.pdf")
    private String url;              // URL renvoyée par Cloudinary
    private String publicId;         // ID unique Cloudinary pour suppression/modif
    private LocalDateTime uploadedAt;
    private Long uploadedBy;
}
