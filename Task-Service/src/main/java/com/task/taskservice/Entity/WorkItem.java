package com.task.taskservice.Entity;

import com.task.taskservice.Enumeration.WorkItemPriority;
import com.task.taskservice.Enumeration.WorkItemStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public abstract class WorkItem {
    protected Long id;
    protected String title;
    protected String description;
    protected LocalDate creationDate;
    protected LocalDate lastModifiedDate;
    protected LocalDate startDate;
    protected LocalDate dueDate;
    protected LocalDate completedDate;

    protected Long estimationTime;     // in minutes or hours (depending on convention)
    protected Long timeSpent;

    protected WorkItemStatus status;
    protected WorkItemPriority priority;

    protected Double progress;         // 0.0 to 100.0

    protected List<Long> assignedUser;      // L'ID de l'utilisateur (venant du User MS)
    protected Long userStory;         // L'ID de la User Story (venant du User Story MS)

    protected List<WorkItem> dependencies;  // WorkItems this one depends on
    protected List<Comment> comments;
    protected List<FileAttachment> attachments;

    protected Set<String> tags;        // Optional tagging system

}
