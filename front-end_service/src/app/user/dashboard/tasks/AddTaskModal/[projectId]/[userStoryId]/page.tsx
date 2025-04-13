// app/add-task-modal/[userStoryId]/page.tsx
"use client";
import { useParams } from "next/navigation";
import React, { useState } from "react";
import "../../../../../../../styles/Dashboard-Add-Task.css";
import { useAuth } from "../../../../../../../context/AuthContext";
import useAxios from "../../../../../../../hooks/useAxios";
import {
  TASK_SERVICE_URL,
} from "../../../../../../../config/useApi";
import { useRouter } from "next/navigation";
export default function AddTaskModalPage() {
   const { accessToken } = useAuth();
    const axiosInstance = useAxios();
  const params = useParams();
  const projectId = params.projectId as string;
  const userStoryId = params.userStoryId as string;
  const router = useRouter();

  // Données fictives pour simuler les utilisateurs
  const mockUsers = [
    { id: "1", name: "John Doe", avatar: "👨‍💼" },
    { id: "2", name: "Jane Smith", avatar: "👩‍💻" },
    { id: "3", name: "Alice Johnson", avatar: "👩‍🎤" },
    { id: "4", name: "Bob Brown", avatar: "🧑‍🚀" },
  ];

  // Simuler l'access token (remplace par ta méthode réelle, par exemple localStorage.getItem("token"))

  // URL de base pour l'API (à remplacer par ta vraie URL)

  const [workItemType, setWorkItemType] = useState<"TASK" | "BUG">("TASK");
  const [workItem, setWorkItem] = useState({
    title: "",
    description: "",
    startDate: "",
    dueDate: "",
    estimationTime: "",
    status: "TO_DO",
    priority: "",
    assignedUser: [] as string[],
    tags: [] as string[],
    severity: "",
  });

  const handleInputChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    const { name, value } = e.target;
    setWorkItem((prev) => ({ ...prev, [name]: value }));
  };

  const handleArrayInput = (name: string, value: string) => {
    setWorkItem((prev) => ({
      ...prev,
      [name]: value.split(",").map((item) => item.trim()).filter(Boolean),
    }));
  };

  const handleUserSelection = (userId: string) => {
    setWorkItem((prev) => {
      const assignedUser = prev.assignedUser.includes(userId)
        ? prev.assignedUser.filter((id) => id !== userId)
        : [...prev.assignedUser, userId];
      return { ...prev, assignedUser };
    });
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    console.log("Payload avant envoi :", workItem);

    try {
        const response = await axiosInstance.post(
            `${TASK_SERVICE_URL}/api/project/tasks/${projectId}/${userStoryId}/createTask`,
            
            {
                title: workItem.title,
                description: workItem.description,
                startDate: workItem.startDate || null,
                dueDate: workItem.dueDate || null,
                estimationTime: parseInt(workItem.estimationTime) || null,
                status: workItem.status,
                priority: workItem.priority || null,
                assignedUser: workItem.assignedUser,
                tags: workItem.tags,
                type: workItemType,
                ...(workItemType === "BUG" && { severity: workItem.severity || null }),
            },
            {
                headers: {
                    Authorization: `Bearer ${accessToken}`,
                },
                withCredentials: true, // Include cookies
            }
        );

        if (response.status === 201) { // Check for CREATED status
            console.log("Work item created successfully");
            router.back();
        } else {
            console.error("Failed to create work item:", response.data);
            alert("Erreur lors de la création du work item.");
        }
    } catch (error) {
        console.error("Erreur lors de la création du work item :", error);
        alert("Une erreur s'est produite. Veuillez réessayer.");
    }
};

  const handleClose = () => {
    router.back();
  };

  return (
    <div className="modal-overlay">
      <div className="modal-content-task">
        <h2>Create a New {workItemType === "TASK" ? "Task" : "Bug"}</h2>
        <div className="type-toggle">
          <button
            type="button"
            className={`toggle-btn ${workItemType === "TASK" ? "active" : ""}`}
            onClick={() => setWorkItemType("TASK")}
          >
            Task
          </button>
          <button
            type="button"
            className={`toggle-btn ${workItemType === "BUG" ? "active" : ""}`}
            onClick={() => setWorkItemType("BUG")}
          >
            Bug
          </button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="form-section">
            <h3>General</h3>
            <div className="form-group">
              <label>Title</label>
              <input
                type="text"
                name="title"
                value={workItem.title}
                onChange={handleInputChange}
                required
                placeholder="What's the task or bug?"
              />
            </div>
            <div className="form-group">
              <label>Description</label>
              <textarea
                name="description"
                value={workItem.description}
                onChange={handleInputChange}
                placeholder="Add some details..."
              />
            </div>
          </div>

          <div className="form-section">
            <h3>Timeline</h3>
            <div className="form-group">
              <label>Start Date</label>
              <input
                type="date"
                name="startDate"
                value={workItem.startDate}
                onChange={handleInputChange}
              />
            </div>
            <div className="form-group">
              <label>Due Date</label>
              <input
                type="date"
                name="dueDate"
                value={workItem.dueDate}
                onChange={handleInputChange}
              />
            </div>
            <div className="form-group">
              <label>Estimation Time (hours)</label>
              <input
                type="number"
                name="estimationTime"
                value={workItem.estimationTime}
                onChange={handleInputChange}
                placeholder="e.g., 4"
                min="0"
              />
            </div>
          </div>

          <div className="form-section">
            <h3>Details</h3>
            <div className="form-group">
              <label>Status</label>
              <div className="status-toggle">
                <button
                  type="button"
                  className={`status-btn ${workItem.status === "TO_DO" ? "active" : ""}`}
                  onClick={() => setWorkItem((prev) => ({ ...prev, status: "TO_DO" }))}
                >
                  To Do
                </button>
                <button
                  type="button"
                  className={`status-btn ${workItem.status === "IN_PROGRESS" ? "active" : ""}`}
                  onClick={() => setWorkItem((prev) => ({ ...prev, status: "IN_PROGRESS" }))}
                >
                  In Progress
                </button>
                <button
                  type="button"
                  className={`status-btn ${workItem.status === "DONE" ? "active" : ""}`}
                  onClick={() => setWorkItem((prev) => ({ ...prev, status: "DONE" }))}
                >
                  Done
                </button>
              </div>
            </div>
            <div className="form-group">
              <label>Priority</label>
              <select name="priority" value={workItem.priority} onChange={handleInputChange}>
                <option value="">Select Priority</option>
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
              </select>
            </div>
            
          </div>

          <div className="form-section">
            <h3>Team</h3>
            <div className="form-group">
              <label>Assigned Users</label>
              <div className="user-selection">
                {mockUsers.map((user) => (
                  <label key={user.id} className="user-checkbox">
                    <input
                      type="checkbox"
                      checked={workItem.assignedUser.includes(user.id)}
                      onChange={() => handleUserSelection(user.id)}
                    />
                    <span className="user-avatar">{user.avatar}</span>
                    {user.name}
                  </label>
                ))}
              </div>
            </div>
          </div>

          <div className="form-section">
            <h3>Tags</h3>
            <div className="form-group">
              <label>Tags (comma-separated)</label>
              <input
                type="text"
                value={workItem.tags.join(",")}
                onChange={(e) => handleArrayInput("tags", e.target.value)}
                placeholder="e.g., frontend,urgent"
              />
            </div>
          </div>

          {workItemType === "BUG" && (
            <div className="form-section">
              <h3>Bug Details</h3>
              <div className="form-group">
                <label>Severity</label>
                <select name="severity" value={workItem.severity} onChange={handleInputChange}>
                  <option value="">Select Severity</option>
                  <option value="MINOR">Minor</option>
                  <option value="MAJOR">Major</option>
                  <option value="CRITICAL">Critical</option>
                </select>
              </div>
            </div>
          )}

          <div className="modal-actions-task">
            <button type="submit" className="btn-task primary">Create</button>
            <button type="button" className="btn-task secondary" onClick={handleClose}>
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}