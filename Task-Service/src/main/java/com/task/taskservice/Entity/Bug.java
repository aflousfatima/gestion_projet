package com.task.taskservice.Entity;

import com.task.taskservice.Enumeration.BugSeverity;
import com.task.taskservice.Enumeration.WorkItemPriority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
public class Bug extends WorkItem {
    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private BugSeverity severity; // Optional: minor, major, critical
    public BugSeverity getSeverity(){
        return severity;
    }

    public void setSeverity(BugSeverity severity) {
        this.severity = severity;
    }
}
