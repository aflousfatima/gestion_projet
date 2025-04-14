// app/add-task-modal/[userStoryId]/page.tsx
"use client";
import { useParams } from "next/navigation";
import React, { useState, useEffect } from "react";
import "../../../../../../../styles/Dashboard-Add-Task.css";
import { useAuth } from "../../../../../../../context/AuthContext";
import useAxios from "../../../../../../../hooks/useAxios";
import {
  TASK_SERVICE_URL,
  AUTH_SERVICE_URL,
} from "../../../../../../../config/useApi";
import { useRouter } from "next/navigation";

interface TeamMember {
  id: string;
  firstName: string;
  lastName: string;
  role: string;
  project: string;
  avatar: string;
}
export default function AddTaskModalPage() {
  const { accessToken  , isLoading: authLoading } = useAuth();
  const axiosInstance = useAxios();
  const params = useParams();
  const projectId = params.projectId as string;
  const userStoryId = params.userStoryId as string;
  const [teamMembers, setTeamMembers] = useState<TeamMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const router = useRouter();

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

  // Fetch team members

  useEffect(() => {
    const fetchTeamMembers = async () => {
      if (authLoading || !accessToken || !projectId) return; // Wait for auth
      try {
        setLoading(true);
        setError(null);
  
        const teamResponse = await axiosInstance.get(
          `${AUTH_SERVICE_URL}/api/team-members/${projectId}`,
          {
            headers: {
              Authorization: `Bearer ${accessToken}`,
            },
          }
        );
        console.log("Team members response:", teamResponse.data); // Debug log
        setTeamMembers(teamResponse.data);
      } catch (err) {
        console.error("❌ Error fetching team members:", err);
        setError("Unable to load team members.");
      } finally {
        setLoading(false);
      }
    };
  
    fetchTeamMembers();
  }, [accessToken, authLoading, axiosInstance, projectId]);
  
  const handleInputChange = (
    e: React.ChangeEvent<
      HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
    >
  ) => {
    const { name, value } = e.target;
    setWorkItem((prev) => ({ ...prev, [name]: value }));
  };
  const [tagInput, setTagInput] = useState("");
  const handleTagAdd = () => {
    if (tagInput.trim()) {
      setWorkItem((prev) => ({
        ...prev,
        tags: [...prev.tags, tagInput.trim()],
      }));
      setTagInput("");
    }
  };
  const removeTag = (index: number) => {
    setWorkItem((prev) => ({
      ...prev,
      tags: prev.tags.filter((_, i) => i !== index),
    }));
  };
  const handlePrioritySelect = (priority: "LOW" | "MEDIUM" | "HIGH" | "") => {
    setWorkItem((prev) => ({ ...prev, priority }));
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
          ...(workItemType === "BUG" && {
            severity: workItem.severity || null,
          }),
        },
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
          withCredentials: true, // Include cookies
        }
      );

      if (response.status === 201) {
        // Check for CREATED status
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
  const [activeSection, setActiveSection] = useState<string | null>(null);
  const toggleSection = (section: string) => {
    setActiveSection(activeSection === section ? null : section);
  };
  const tagSuggestions = ["frontend", "backend", "urgent", "design", "testing"];
  return (
    <div className="modal-overlay">
      <div className="modal-content-task">
        <button className="close-btn" onClick={handleClose}>
          ×
        </button>
        <div className="modal-header">
          <h2>{workItemType === "TASK" ? "New Task" : "New Bug"}</h2>
          <div className="type-toggle">
            <button
              type="button"
              className={`toggle-btn ${
                workItemType === "TASK" ? "active" : ""
              }`}
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
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-main">
            <div className="form-group">
              <input
                type="text"
                name="title"
                value={workItem.title}
                onChange={handleInputChange}
                required
                placeholder="Task or Bug Title"
                className="title-input"
              />
            </div>
            <div className="form-group">
              <textarea
                name="description"
                value={workItem.description}
                onChange={handleInputChange}
                placeholder="Add a description..."
                className="description-input"
              />
            </div>
            <div className="timeline-group">
              <div className="form-group timeline-item">
                <label>Start</label>
                <input
                  type="date"
                  name="startDate"
                  value={workItem.startDate}
                  onChange={handleInputChange}
                />
              </div>
              <div className="form-group timeline-item">
                <label>Due</label>
                <input
                  type="date"
                  name="dueDate"
                  value={workItem.dueDate}
                  onChange={handleInputChange}
                />
              </div>
              <div className="form-group timeline-item">
                <label>Hours</label>
                <input
                  type="number"
                  name="estimationTime"
                  value={workItem.estimationTime}
                  onChange={handleInputChange}
                  placeholder="Est."
                  min="0"
                />
              </div>
            </div>
            <div className="priority-group">
              <h3>Priority</h3>
              <div className="priority-options">
                {[
                  { value: "LOW", label: "Low", color: "#4ade80", icon: "↓" },
                  {
                    value: "MEDIUM",
                    label: "Medium",
                    color: "#fb923c",
                    icon: "↔",
                  },
                  { value: "HIGH", label: "High", color: "#ef4444", icon: "↑" },
                ].map((opt) => (
                  <button
                    key={opt.value}
                    type="button"
                    className={`priority-btn ${
                      workItem.priority === opt.value ? "active" : ""
                    }`}
                    style={{ borderColor: opt.color }}
                    onClick={() =>
                      handlePrioritySelect(
                        opt.value as "LOW" | "MEDIUM" | "HIGH"
                      )
                    }
                  >
                    <span
                      className="priority-icon"
                      style={{ color: opt.color }}
                    >
                      {opt.icon}
                    </span>
                    {opt.label}
                  </button>
                ))}
              </div>
            </div>
          </div>

          <div className="form-sections">
            <div className="accordion-section">
              <button
                type="button"
                className="accordion-btn"
                onClick={() => toggleSection("team")}
              >
                Team {activeSection === "team" ? "−" : "+"}
              </button>
              {activeSection === "team" && (
  <div className="accordion-content">
    <div className="user-selection">
      <div className="selected-users">
        {workItem.assignedUser.length > 0 ? (
          <div className="avatar-stack">
            {workItem.assignedUser.slice(0, 3).map((userId) => {
              const user = teamMembers.find((u) => u.id === userId);
              return user ? (
                <img
                  key={user.id}
                  src={user.avatar}
                  alt={`${user.firstName} ${user.lastName}`}
                  className="user-avatar-stack"
                />
              ) : null;
            })}
            {workItem.assignedUser.length > 3 && (
              <span className="user-avatar-stack more">
                +{workItem.assignedUser.length - 3}
              </span>
            )}
          </div>
        ) : (
          <span>No users assigned</span>
        )}
      </div>
      <div className="user-grid">
        {loading ? (
          <span>Loading team members...</span>
        ) : error ? (
          <span>{error}</span>
        ) : teamMembers.length === 0 ? (
          <span>No team members found</span>
        ) : (
          teamMembers.map((user) => (
            <label key={user.id} className="user-card">
              <input
                type="checkbox"
                checked={workItem.assignedUser.includes(user.id)}
                onChange={() => handleUserSelection(user.id)}
                style={{ display: "none" }}
              />
              <div
                className={`user-card-inner ${
                  workItem.assignedUser.includes(user.id) ? "selected" : ""
                }`}
              >
                <img
                  src={user.avatar}
                  alt={`${user.firstName} ${user.lastName}`}
                  className="user-avatar"
                />
                <span className="user-name">{`${user.firstName} ${user.lastName}`}</span>
              </div>
            </label>
          ))
        )}
      </div>
    </div>
  </div>
)}
            </div>
            <div className="accordion-section">
              <button
                type="button"
                className="accordion-btn"
                onClick={() => toggleSection("tags")}
              >
                Tags {activeSection === "tags" ? "−" : "+"}
              </button>
              {activeSection === "tags" && (
                <div className="accordion-content">
                  <div className="tags-input">
                    <input
                      type="text"
                      value={tagInput}
                      onChange={(e) => setTagInput(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === "Enter" || e.key === ",") {
                          e.preventDefault();
                          handleTagAdd();
                        }
                      }}
                      placeholder="Add a tag..."
                    />
                    {tagInput && (
                      <div className="tag-suggestions">
                        {tagSuggestions
                          .filter((tag) =>
                            tag.toLowerCase().includes(tagInput.toLowerCase())
                          )
                          .map((tag) => (
                            <button
                              key={tag}
                              type="button"
                              className="tag-suggestion"
                              onClick={() => {
                                setWorkItem((prev) => ({
                                  ...prev,
                                  tags: [...prev.tags, tag],
                                }));
                                setTagInput("");
                              }}
                            >
                              {tag}
                            </button>
                          ))}
                      </div>
                    )}
                    <div className="tag-chips">
                      {workItem.tags.map((tag, index) => (
                        <span key={index} className="tag-chip">
                          {tag}
                          <button
                            type="button"
                            onClick={() => removeTag(index)}
                          >
                            ×
                          </button>
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </div>
            {workItemType === "BUG" && (
              <div className="accordion-section">
                <button
                  type="button"
                  className="accordion-btn"
                  onClick={() => toggleSection("bug")}
                >
                  Bug Details {activeSection === "bug" ? "−" : "+"}
                </button>
                {activeSection === "bug" && (
                  <div className="accordion-content">
                    <div className="severity-group">
                      <h3>Severity</h3>
                      <div className="severity-options">
                        {[
                          {
                            value: "MINOR",
                            label: "Minor",
                            color: "#4ade80",
                            icon: "⚪",
                          },
                          {
                            value: "MAJOR",
                            label: "Major",
                            color: "#fb923c",
                            icon: "⚠️",
                          },
                          {
                            value: "CRITICAL",
                            label: "Critical",
                            color: "#ef4444",
                            icon: "🔥",
                          },
                        ].map((opt) => (
                          <button
                            key={opt.value}
                            type="button"
                            className={`severity-btn ${
                              workItem.severity === opt.value ? "active" : ""
                            }`}
                            style={{ borderColor: opt.color }}
                            onClick={() =>
                              setWorkItem((prev) => ({
                                ...prev,
                                severity: opt.value as
                                  | "MINOR"
                                  | "MAJOR"
                                  | "CRITICAL",
                              }))
                            }
                          >
                            <span
                              className="severity-icon"
                              style={{ color: opt.color }}
                            >
                              {opt.icon}
                            </span>
                            {opt.label}
                          </button>
                        ))}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>

          <div className="modal-actions-task">
            <button type="submit" className="btn-task primary">
              Create
            </button>
            <button
              type="button"
              className="btn-task secondary"
              onClick={handleClose}
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
