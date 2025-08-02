package com.auth.authentification_service.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "agendas")
public class Agenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @ElementCollection
    @CollectionTable(name = "agenda_available_slots", joinColumns = @JoinColumn(name = "agenda_id"))
    @JsonProperty("available_slots")
    private List<Slot> availableSlots = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "agenda_blocked_slots", joinColumns = @JoinColumn(name = "agenda_id"))
    @JsonProperty("blocked_slots")
    private List<Slot> blockedSlots = new ArrayList<>();

    @Embeddable
    public static class Slot {
        @Column(name = "day")
        private String day;

        @Column(name = "start_time")
        @JsonProperty("start")
        private String startTime;

        @Column(name = "end_time")
        @JsonProperty("end")
        private String endTime;

        // Getters and Setters
        public String getDay() { return day; }
        public void setDay(String day) { this.day = day; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<Slot> getAvailableSlots() { return availableSlots; }
    public void setAvailableSlots(List<Slot> availableSlots) { this.availableSlots = availableSlots != null ? availableSlots : new ArrayList<>(); }
    public List<Slot> getBlockedSlots() { return blockedSlots; }
    public void setBlockedSlots(List<Slot> blockedSlots) { this.blockedSlots = blockedSlots != null ? blockedSlots : new ArrayList<>(); }
}