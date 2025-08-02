package com.collaboration.collaborationservice.meeting.repository;
import com.collaboration.collaborationservice.meeting.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findByStatus(String status);
    List<Meeting> findByDate(String date);
    long countByStatus(String status);
    long countByRescheduled(boolean rescheduled);
}
