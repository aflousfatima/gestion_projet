"use client";
import React, { useState } from "react";
import { useTasks } from "../../../../../../hooks/useTask"; // Corrigé l'importation
import TaskCard from "../../../../../../hooks/TaskCard";
import "../../../../../../styles/Dashboard-Task-List.css";

export default function ListView() {
  const { tasks, loading, error, handleTaskUpdate } = useTasks();
  const [showDates, setShowDates] = useState(false);

  const sections = [
    { status: "TO_DO", title: "TO DO" },
    { status: "IN_PROGRESS", title: "IN PROGRESS" },
    { status: "DONE", title: "DONE" },
  ];

  return (
    <div className="list-container-task">
      {loading && (
        <p>
          <img src="/loading.svg" alt="Loading" className="loading-img" />
        </p>
      )}
      {error && <p style={{ color: "red" }}>Error: {error}</p>}
      {!loading && !error && tasks.length === 0 && <p>No tasks found.</p>}
      {!loading && !error && tasks.length > 0 && (
        <div className="tasks-wrapper">
          {/* En-tête de la liste */}
          <div className="tasks-list-header">
            <span className="tasks-list-header-item-principal">
              Task{"'"}s Name
            </span>
            <span className="tasks-list-header-item">Responsable</span>
            <span className="tasks-list-header-item">Due Date</span>
            <span className="tasks-list-header-item">Priority</span>
            <span className="tasks-list-header-item">Tags</span>
            <span className="tasks-list-header-item">Files</span>
            <span className="tasks-list-header-item">Comments</span>
          </div>

          {sections.map((section) => {
            const sectionTasks = tasks.filter(
              (task) => task.status === section.status
            );
            return (
              <div key={section.status} className="task-section">
                <h3 className="section-task-title">{section.title}</h3>
                <div className="task-details">
                  {sectionTasks.map((task) => (
                    <TaskCard
                      key={task.id}
                      task={task}
                      showDates={showDates}
                      toggleDates={() => setShowDates(!showDates)}
                      onTaskUpdate={handleTaskUpdate}
                      displayMode="row"
                    />
                  ))}
                  <div className="tasks-add-subtask">Add Task...</div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
