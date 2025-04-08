package com.project.project_service.Entity;

import jakarta.persistence.Entity;

@Entity
public class UserStoryHistory extends History {

    private Long userStoryId;

    public Long getUserStoryId() {
        return userStoryId;
    }

    public void setUserStoryId(Long userStoryId) {
        this.userStoryId = userStoryId;
    }
}