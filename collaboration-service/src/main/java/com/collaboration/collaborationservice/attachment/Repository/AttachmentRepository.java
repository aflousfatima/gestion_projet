package com.collaboration.collaborationservice.attachment.Repository;

import com.collaboration.collaborationservice.attachment.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByChannelId(Long channelId);
}
