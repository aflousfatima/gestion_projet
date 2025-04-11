package com.task.taskservice.Entity;

import jakarta.persistence.Entity;

@Entity
public class SubTask extends WorkItem {
    protected Long parentTaskId;  // Référence à la tâche principale

}
