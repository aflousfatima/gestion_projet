package com.task.taskservice.Chatbot.DTO;

import com.task.taskservice.Enumeration.WorkItemStatus;

public class TaskStatusResponse {
    private WorkItemStatus status;
    private long count;

    public TaskStatusResponse(WorkItemStatus status, long count) {
        this.status = status;
        this.count = count;
    }

    public WorkItemStatus getStatus() {
        return status;
    }

    public void setStatus(WorkItemStatus status) {
        this.status = status;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}