"use client";
import React, { useState } from "react";
import { useTasks } from "../../../../../../hooks/useTask";
import TaskCard from "../../../../../../hooks/TaskCard";
import "../../../../../../styles/Dashboard-Task-Kanban.css";
import { useAuth } from "../../../../../../context/AuthContext";
import useAxios from "../../../../../../hooks/useAxios";
import { TASK_SERVICE_URL } from "../../../../../../config/useApi";

interface Task {
  id?: number;
  title: string;
  description: string | null;
  creationDate: string;
  startDate: string | null;
  dueDate: string | null;
  estimationTime: number | null;
  status:
    | "TO_DO"
    | "IN_PROGRESS"
    | "DONE"
    | "BLOCKED"
    | "ARCHIVED"
    | "CANCELLED"
    | "";
  priority: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL" | "";
  userStoryId: number | null;
  createdBy: string | null;
  projectId: number;
  tags: string[];
  assignedUsers: { id: string; firstName: string; lastName: string; avatar?: string }[];
  attachments: {
    id?: number;
    fileName: string;
    fileType: string;
    fileSize: number;
    fileUrl: string;
    publicId: string;
    uploadedBy: string;
    uploadedAt: string;
  }[];
}

export default function Kanban() {
  const { tasks, loading, error, handleTaskUpdate } = useTasks();
  const { accessToken } = useAuth();
  const axiosInstance = useAxios();
  const [showDates, setShowDates] = useState(false);
  const [draggingTaskId, setDraggingTaskId] = useState<number | null>(null);
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);
  const [dragOverColumn, setDragOverColumn] = useState<string | null>(null);

  const columns = [
    { status: "TO_DO", title: "To Do" },
    { status: "IN_PROGRESS", title: "In Progress" },
    { status: "DONE", title: "Done" },
    { status: "BLOCKED", title: "Blocked" },
    { status: "ARCHIVED", title: "Archived" },
    { status: "CANCELLED", title: "Cancelled" },
  ];

  const handleDragOver = (
    e: React.DragEvent<HTMLDivElement>,
    columnStatus: string
  ) => {
    e.preventDefault();
    e.stopPropagation();
    setDragOverColumn(columnStatus);
  };

  const handleDragLeave = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setDragOverColumn(null);
  };

  const handleDrop = async (
    e: React.DragEvent<HTMLDivElement>,
    newStatus: Task["status"]
  ) => {
    e.preventDefault();
    e.stopPropagation();

    setDragOverColumn(null);

    if (isUpdatingStatus) return;

    const taskId = e.dataTransfer.getData("taskId");
    const task = tasks.find((t) => t.id?.toString() === taskId);

    if (!task || !task.id || task.status === newStatus) return;

    setIsUpdatingStatus(true);
    setDraggingTaskId(null);

    const originalTasks = [...tasks];
    handleTaskUpdate({ id: task.id, status: newStatus });

    const taskDTO = {
      id: task.id,
      title: task.title,
      description: task.description,
      creationDate: task.creationDate,
      startDate: task.startDate,
      dueDate: task.dueDate,
      estimationTime: task.estimationTime,
      status: newStatus,
      priority: task.priority,
      userStoryId: task.userStoryId,
      createdBy: task.createdBy,
      projectId: task.projectId,
      tags: task.tags,
      assignedUsers: task.assignedUsers,
      attachments: task.attachments,
    };

    try {
      const response = await axiosInstance.put(
        `${TASK_SERVICE_URL}/api/project/tasks/${task.id}/updateTask`,
        taskDTO,
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );
      handleTaskUpdate(response.data);
    } catch (err: any) {
      let errorMessage = "Failed to update task status";
      if (err.response?.data) {
        errorMessage =
          typeof err.response.data === "string"
            ? err.response.data
            : err.response.data.error ||
              err.response.data.message ||
              errorMessage;
      }
      handleTaskUpdate({ id: task.id, status: task.status });
      console.error("Error updating task status:", err.response?.data || err);
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  return (
    <div className="kanban-container">
      {loading && (
        <p>
          <img src="/loading.svg" alt="Loading" className="loading-img" />
        </p>
      )}
      {error && <p style={{ color: "red" }}>Error: {error}</p>}
      {!loading && !error && tasks.length === 0 && <p>No tasks found.</p>}
      {!loading && !error && tasks.length > 0 && (
        <div className="kanban-board">
          {columns.map((column) => (
            <div
              key={column.status}
              className={`kanban-column ${
                dragOverColumn === column.status ? "drag-active" : ""
              }`}
              onDragOver={(e) => handleDragOver(e, column.status)}
              onDragLeave={handleDragLeave}
              onDrop={(e) => handleDrop(e, column.status as Task["status"])}
            >
              <div className={`column-header ${column.status.toLowerCase()}`}>
                <h5>{column.title}</h5>
                <div className="parameters-kanban">
                  <button className="add-task-btn">+</button>
                  <button className="ellipsis-btn">...</button>
                </div>
              </div>
              <div className="task-list">
                {tasks
                  .filter((task) => task.status === column.status)
                  .map((task) => (
                    <TaskCard
                      key={task.id}
                      task={task}
                      showDates={showDates}
                      toggleDates={() => setShowDates(!showDates)}
                      onTaskUpdate={handleTaskUpdate}
                    />
                  ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}