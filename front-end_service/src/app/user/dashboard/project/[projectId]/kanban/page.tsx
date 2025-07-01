"use client";
import React, { useState } from "react";
import { useWorkItems } from "../../../../../../hooks/useWorkItems";
import WorkItemCard from "../../../../../../hooks/WorkItemCard";
import "../../../../../../styles/Dashboard-Task-Kanban.css";
import { useAuth } from "../../../../../../context/AuthContext";
import useAxios from "../../../../../../hooks/useAxios";
import { TASK_SERVICE_URL } from "../../../../../../config/useApi";
import { AxiosError } from "axios"; 
interface User {
  id: string;
  firstName: string;
  lastName: string;
  avatar?: string;
}

interface FileAttachment {
  id?: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  fileUrl: string;
  publicId: string;
  uploadedBy: string;
  uploadedAt: string;
}

interface TaskSummary {
  id: number;
  title: string;
  status: string;
  projectId: number;
  userStoryId: number;
}

interface WorkItem {
  id?: number;
  type: "TASK" | "BUG";
  title: string;
  description: string | null;
  creationDate: string;
  startDate: string | null;
  dueDate: string | null;
  estimationTime: number | null;
  totalTimeSpent: number;
  startTime: string;
  status:
    | "TO_DO"
    | "IN_PROGRESS"
    | "DONE"
    | "BLOCKED"
    | "ARCHIVED"
    | "CANCELLED"
    | "";
  priority: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL" | "";
  severity?: "MINOR" | "MAJOR" | "CRITICAL" | "BLOCKER" | "";
  userStoryId: number | null;
  createdBy: string | null;
  projectId: number;
  tags: string[];
  assignedUsers: User[];
  attachments: FileAttachment[];
  progress: number;
  dependencyIds: number[];
  dependencies: TaskSummary[];
}

export default function Kanban() {
  const { workItems, loading, error, handleWorkItemUpdate } = useWorkItems();
  const { accessToken } = useAuth();
  const axiosInstance = useAxios();
  const [showDates, setShowDates] = useState(false);
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
    newStatus: WorkItem["status"]
  ) => {
    e.preventDefault();
    e.stopPropagation();

    setDragOverColumn(null);

    if (isUpdatingStatus) return;

    const workItemId = e.dataTransfer.getData("workItemId");
    const workItemType = e.dataTransfer.getData("workItemType") as "TASK" | "BUG";
    const workItem = workItems.find((t) => t.id?.toString() === workItemId);

    if (!workItem || !workItem.id || workItem.status === newStatus) return;

    setIsUpdatingStatus(true);
    handleWorkItemUpdate({ id: workItem.id, status: newStatus });

    const workItemDTO = {
      id: workItem.id,
      title: workItem.title,
      description: workItem.description,
      creationDate: workItem.creationDate,
      startDate: workItem.startDate,
      dueDate: workItem.dueDate,
      estimationTime: workItem.estimationTime,
      totalTimeSpent: workItem.totalTimeSpent,
      status: newStatus,
      priority: workItem.priority,
      severity: workItem.severity,
      userStoryId: workItem.userStoryId,
      createdBy: workItem.createdBy,
      projectId: workItem.projectId,
      tags: workItem.tags,
      assignedUsers: workItem.assignedUsers,
      attachments: workItem.attachments,
    };

    try {
      const endpoint =
        workItemType === "TASK"
          ? `/tasks/${workItem.id}/updateTask`
          : `/bugs/${workItem.id}/updateBug`;
      const response = await axiosInstance.put(
        `${TASK_SERVICE_URL}/api/project${endpoint}`,
        workItemDTO,
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );
      handleWorkItemUpdate({ ...response.data, type: workItemType });
    } catch (err: unknown) {
      handleWorkItemUpdate({ id: workItem.id, status: workItem.status });
      console.error(
        "Error updating work item status:",
        err instanceof AxiosError ? err.response?.data : err
      );
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
      {!loading && !error && workItems.length === 0 && <p>No work items found.</p>}
      {!loading && !error && workItems.length > 0 && (
        <div className="kanban-board">
          {columns.map((column) => (
            <div
              key={column.status}
              className={`kanban-column ${
                dragOverColumn === column.status ? "drag-active" : ""
              }`}
              onDragOver={(e) => handleDragOver(e, column.status)}
              onDragLeave={handleDragLeave}
              onDrop={(e) => handleDrop(e, column.status as WorkItem["status"])}
            >
              <div className={`column-header ${column.status.toLowerCase()}`}>
                <h5>{column.title}</h5>
                <div className="parameters-kanban">
                  <button className="add-task-btn">+</button>
                  <button className="ellipsis-btn">...</button>
                </div>
              </div>
              <div className="work-item-list">
                {workItems
                  .filter((item) => item.status === column.status)
                  .map((item) => (
                    <WorkItemCard
                      key={item.id}
                      workItem={item}
                      showDates={showDates}
                      toggleDates={() => setShowDates(!showDates)}
                      onWorkItemUpdate={handleWorkItemUpdate}
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
