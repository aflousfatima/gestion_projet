package com.task.taskservice.Entity;

import com.task.taskservice.Enumeration.BugSeverity;

public class Bug extends WorkItem {
    private Long reportedBy; // Distinct from assignedTo
    private BugSeverity severity; // Optional: minor, major, critical
}
