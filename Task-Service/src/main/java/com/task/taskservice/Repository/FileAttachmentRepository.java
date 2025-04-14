package com.task.taskservice.Repository;

import com.task.taskservice.Entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {
    void deleteByPublicId(String publicId);

    Optional<FileAttachment> findByPublicId(String publicId);

}
