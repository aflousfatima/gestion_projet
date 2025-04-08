package com.project.project_service.Entity;

import jakarta.persistence.Entity;

@Entity
public class SprintHistory extends History {

    private Long sprintId;


    public Long getSprintId() {
        return sprintId;
    }

    public void setSprintId(Long sprintId) {
        this.sprintId = sprintId;
    }
}