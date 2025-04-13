"use client";
import { useParams } from "next/navigation";
import React, { useState, useEffect } from "react";
import { useAuth } from "../../../../../../context/AuthContext";
import useAxios from "../../../../../../hooks/useAxios";
import { TASK_SERVICE_URL } from "../../../../../../config/useApi";
import "../../../../../../styles/Dashboard-Task-Kanban.css"; // Import the standard CSS file

export interface Task {
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
  userStory: number | null;
  createdBy: string | null;
  projectId: number;
  tags: string[];
}

export default function Kanban() {
  const { accessToken, isLoading: authLoading } = useAuth();
  const axiosInstance = useAxios();
  const params = useParams();
  const projectId = params.projectId as string;
  const [tasks, setTasks] = useState<Task[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // Define Kanban columns with background colors
  const columns = [
    { status: "TO_DO", title: "To Do"},
    { status: "IN_PROGRESS", title: "In Progress"}, // Gold
    { status: "DONE", title: "Done"}, // Light green
    { status: "BLOCKED", title: "Blocked" }, // Tomato
    { status: "ARCHIVED", title: "Archived" }, // Light gray
    { status: "CANCELLED", title: "Cancelled"}, // Light pink
  ];

  useEffect(() => {
    if (authLoading || !accessToken) return;

    async function fetchTasksByProjectId() {
      setLoading(true);
      setError(null);
      try {
        const response = await axiosInstance.get(
          `${TASK_SERVICE_URL}/api/project/tasks/active_sprint/${projectId}`,
          {
            headers: {
              Authorization: `Bearer ${accessToken}`,
            },
          }
        );

        const data = Array.isArray(response.data) ? response.data : [];
        setTasks(data);
        console.log("Tasks:", data);
      } catch (err) {
        const errorMessage = err.response
          ? err.response.data
          : "Failed to fetch tasks";
        setError(errorMessage);
        console.error("Error fetching tasks:", errorMessage);
        setTasks([]);
      } finally {
        setLoading(false);
      }
    }

    fetchTasksByProjectId();
  }, [projectId, accessToken, authLoading, axiosInstance]);

  // Render a single task card
  const renderTaskCard = (task: Task) => (
    <div className="task-card" key={task.id}>
      <h4>{task.title}</h4>
      <p>
        <strong>Priority:</strong> {task.priority || "None"}
      </p>
      <p>
        <strong>Description:</strong> {task.description || "None"}
      </p>
      <p>
        <strong>Tags:</strong>{" "}
        {task.tags?.length > 0 ? task.tags.join(", ") : "None"}
      </p>
      <p>
        <strong>Created:</strong> {task.creationDate}
      </p>
      {task.dueDate && (
        <p>
          <strong>Due:</strong> {task.dueDate}
        </p>
      )}
      {task.estimationTime !== null && (
        <p>
          <strong>Estimated:</strong> {task.estimationTime} min
        </p>
      )}
    </div>
  );

  return (
    <div className="kanban-container">
      {loading && <p>Loading tasks...</p>}
      {error && <p style={{ color: "red" }}>Error: {error}</p>}
      {!loading && !error && tasks.length === 0 && <p>No tasks found.</p>}
      {!loading && !error && tasks.length > 0 && (
        <>
          <div className="kanban-board">
            {columns.map((column) => (
              <div key={column.status} className="kanban-column">
                <div
                  className={`column-header ${column.status.toLowerCase()}`}
                >
                  <h5>{column.title}</h5>
                  <div className="parameters-kanban">
                  <button className="add-task-btn">+</button>
                  <button className="ellipsis-btn">...</button>
                  </div>

                </div>
                <div className="task-list">
                  {tasks
                    .filter((task) => task.status === column.status)
                    .map((task) => renderTaskCard(task))}
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
