package com.task.taskservice.Entity;

import com.task.taskservice.Enumeration.BugSeverity;
import com.task.taskservice.Enumeration.WorkItemPriority;
import jakarta.persistence.Entity;

@Entity
public class Bug extends WorkItem {
    private BugSeverity severity; // Optional: minor, major, critical
    public BugSeverity getSeverity(){
        return severity;
    }

    public void setSeverity(BugSeverity severity) {
        this.severity = severity;
    }
}
