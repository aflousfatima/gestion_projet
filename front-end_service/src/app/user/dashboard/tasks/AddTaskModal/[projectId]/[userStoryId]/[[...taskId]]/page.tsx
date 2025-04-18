// app/user/dashboard/tasks/AddTaskModal/[projectId]/[userStoryId]/[[...taskId]]/page.tsx

"use client";
import { useParams, useRouter } from "next/navigation";
import React, { useState, useEffect } from "react";
import "../../../../../../../../styles/Dashboard-Add-Task.css";
import { useAuth } from "../../../../../../../../context/AuthContext";
import useAxios from "../../../../../../../../hooks/useAxios";
import {
  TASK_SERVICE_URL,
  AUTH_SERVICE_URL,
} from "../../../../../../../../config/useApi";

interface TeamMember {
  id: string;
  firstName: string;
  lastName: string;
  role: string;
  project: string;
  avatar: string;
}

interface Task {
  id: string;
  title: string;
  description: string | null;
  startDate: string | null;
  dueDate: string | null;
  estimationTime: number | null;
  status: string;
  priority: string | null;
  assignedUser: string[]; // Changed from assignedUsers: { id: string }[]  
  tags: string[];
  type: "TASK" | "BUG";
  severity: string | null;
}

export default function AddTaskModalPage() {
  const { accessToken, isLoading: authLoading } = useAuth();
  const axiosInstance = useAxios();
  const params = useParams();
  const projectId = params.projectId as string;
  const userStoryId = params.userStoryId as string;
  const taskId = params.taskId ? params.taskId[0] : null;
  console.log("Params:", params);
  console.log("Extracted projectId:", projectId);
  console.log("Extracted userStoryId:", userStoryId);
  console.log("Extracted taskId:", taskId);
  console.log("Is Editing:", !!taskId);

  const [teamMembers, setTeamMembers] = useState<TeamMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();

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

  const isEditing = !!taskId;

  // Fetch team members
  useEffect(() => {
    const fetchTeamMembers = async () => {
      if (authLoading || !accessToken || !projectId) {
        console.log("Skipping fetchTeamMembers: missing dependencies", {
          authLoading,
          accessToken,
          projectId,
        });
        return;
      }
      try {
        setLoading(true);
        setError(null);
        console.log("Fetching team members for project:", projectId);
        const teamResponse = await axiosInstance.get(
          `${AUTH_SERVICE_URL}/api/team-members/${projectId}`,
          {
            headers: { Authorization: `Bearer ${accessToken}` },
          }
        );
        console.log("Team members received:", teamResponse.data);
        setTeamMembers(teamResponse.data);
      } catch (err: any) {
        console.error(
          "❌ Error fetching team members:",
          err.response?.data || err.message
        );
        setError("Unable to load team members.");
      } finally {
        setLoading(false);
      }
    };

    fetchTeamMembers();
  }, [accessToken, authLoading, axiosInstance, projectId]);

  // Add state for toggling team list visibility
  const [isTeamListOpen, setIsTeamListOpen] = useState(false);

  // Toggle team list
  const toggleTeamList = () => {
    setIsTeamListOpen((prev) => !prev);
  };

  // Close team list (optional, for clicking outside)
  const closeTeamList = () => {
    setIsTeamListOpen(false);
  };
  useEffect(() => {
    const handleOutsideClick = (e: MouseEvent) => {
      const target = e.target as Node;
      const teamSelection = document.querySelector(".team-selection");
      if (teamSelection && !teamSelection.contains(target)) {
        closeTeamList();
      }
    };
    document.addEventListener("click", handleOutsideClick);
    return () => document.removeEventListener("click", handleOutsideClick);
  }, []);

  // Fetch task data if editing
  useEffect(() => {
    const fetchTask = async () => {
      if (!taskId || !accessToken || !projectId || !userStoryId) {
        console.log("Skipping fetchTask: missing dependencies", {
          taskId,
          accessToken,
          projectId,
          userStoryId,
        });
        return;
      }

      try {
        setLoading(true);
        setError(null);

        const response = await axiosInstance.get(
          `${TASK_SERVICE_URL}/api/project/tasks/${projectId}/${userStoryId}/${taskId}`,
          {
            headers: { Authorization: `Bearer ${accessToken}` },
          }
        );
        console.log("Raw response:", response);
        const taskData = {
          ...response.data,
          assignedUsers: response.data.assignedUser
            ? response.data.assignedUser.map((id: string) => ({ id }))
            : [],
        };
        console.log("Transformed task data:", JSON.stringify(taskData, null, 2));
  
        const task: Task = taskData;
        if (!task) {
          throw new Error("No task data received from API");
        }
        setWorkItemType(task.type || "TASK");
        setWorkItem({
          title: task.title || "",
          description: task.description || "",
          startDate: task.startDate ? task.startDate.split("T")[0] : "",
          dueDate: task.dueDate ? task.dueDate.split("T")[0] : "",
          estimationTime: task.estimationTime?.toString() || "",
          status: task.status || "TO_DO",
          priority: task.priority || "",          
          assignedUser: task.assignedUser || [], // Changed from task.assignedUsers?.map((user) => user.id)
          tags: task.tags || [],
          severity: task.severity || "",
        });
        console.log("WorkItem updated with:", {
          title: task.title,
          description: task.description,
          startDate: task.startDate,
          dueDate: task.dueDate,
          estimationTime: task.estimationTime,
          status: task.status,
          priority: task.priority,
          assignedUser: task.assignedUser?.map((user) => user.id),
          tags: task.tags,
          severity: task.severity,
        });
      } catch (err: any) {
        console.error("Error fetching task:", {
          message: err.message,
          status: err.response?.status,
          data: err.response?.data,
        });
        setError("Unable to load task data. Please try again.");
      } finally {
        setLoading(false);
      }
    };

    fetchTask();
  }, [taskId, accessToken, axiosInstance, projectId, userStoryId]);
  
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
    try {
      const payload = {
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
      };
      console.log("Submitting payload:", payload);

      if (isEditing) {
        const response = await axiosInstance.put(
          `${TASK_SERVICE_URL}/api/project/tasks/${taskId}/updateTask`,
          payload,
          {
            headers: { Authorization: `Bearer ${accessToken}` },
          }
        );
        if (response.status === 200) {
          console.log("Task updated successfully");
          router.back();
        } else {
          throw new Error("Failed to update task");
        }
      } else {
        const response = await axiosInstance.post(
          `${TASK_SERVICE_URL}/api/project/tasks/${projectId}/${userStoryId}/createTask`,
          payload,
          {
            headers: { Authorization: `Bearer ${accessToken}` },
          }
        );
        if (response.status === 201) {
          console.log("Task created successfully");
          router.back();
        } else {
          throw new Error("Failed to create task");
        }
      }
    } catch (error: any) {
      console.error(
        "Error submitting task:",
        error.response?.data || error.message
      );
      alert(
        isEditing
          ? "Une erreur s'est produite lors de la mise à jour."
          : "Une erreur s'est produite lors de la création."
      );
    }
  };

  const handleClose = () => {
    router.back();
  };

  const tagSuggestions = ["frontend", "backend", "urgent", "design", "testing"];

  if (loading) {
    return <div>Loading...</div>;
  }

  if (error) {
    return <div>Error: {error}</div>;
  }

  return (
    <div className="modal-overlay">
      <div className="modal-content-task">
        <button className="close-btn" onClick={handleClose}>
          ×
        </button>
        <div className="modal-header">
          <h2>
            {isEditing
              ? workItemType === "TASK"
                ? "Edit Task"
                : "Edit Bug"
              : workItemType === "TASK"
              ? "New Task"
              : "New Bug"}
          </h2>
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
                <h3>Start Date</h3>
                <input
                  type="date"
                  name="startDate"
                  value={workItem.startDate}
                  onChange={handleInputChange}
                />
              </div>
              <div className="form-group timeline-item">
                <h3>Due Date</h3>
                <input
                  type="date"
                  name="dueDate"
                  value={workItem.dueDate}
                  onChange={handleInputChange}
                />
              </div>
              <div className="form-group timeline-item">
                <h3>Estimation Time</h3>
                <input
                  type="number"
                  name="estimationTime"
                  value={workItem.estimationTime}
                  onChange={handleInputChange}
                  placeholder="Hours"
                  min="0"
                />
              </div>
            </div>

            {/* Combined Tags, Priority, Team, and Severity (for Bugs) */}
            <div className="options-row">
              {/* Priority */}
              <div className="options-group priority-group">
                <h3>Priority</h3>
        
  <div className="priority-options">
    {[
      {
        value: "LOW",
        label: "Low",
        color: "#4ade80",
        icon: (
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M12 5v14m0 0l-7-7m7 7l7-7" />
          </svg>
        ),
      },
      {
        value: "MEDIUM",
        label: "Medium",
        color: "#fb923c",
        icon: (
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M5 12h14m0 0l-7-7m7 7l-7 7" />
          </svg>
        ),
      },
      {
        value: "HIGH",
        label: "High",
        color: "#ef4444",
        icon: (
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M12 19V5m0 0l7 7m-7-7l-7 7" />
          </svg>
        ),
      },
    ].map((opt) => (
      <button
        key={opt.value}
        type="button"
        className={`priority-btn ${workItem.priority === opt.value ? "active" : ""}`}
        onClick={() => handlePrioritySelect(opt.value as "LOW" | "MEDIUM" | "HIGH")}
        style={{ '--priority-color': opt.color } as React.CSSProperties}
      >
        <span className="priority-icon">{opt.icon}</span>
        <span className="priority-label">{opt.label}</span>
      </button>
    ))}
  </div>
</div>


              {/* Severity (for Bugs) */}
              {workItemType === "BUG" && (
  <div className="options-group severity-group">
    <h3>Severity</h3>
    <div className="severity-options">
      {[
        {
          value: "MINOR",
          label: "Minor",
          color: "#4ade80",
          icon: (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
            </svg>
          ),
        },
        {
          value: "MAJOR",
          label: "Major",
          color: "#fb923c",
          icon: (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M12 2L2 19h20L12 2z" />
              <path d="M12 8v4" />
              <circle cx="12" cy="16" r="1" />
            </svg>
          ),
        },
        {
          value: "CRITICAL",
          label: "Critical",
          color: "#ef4444",
          icon: (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M12 2l9 15H3l9-15z" />
              <path d="M12 9v4" />
              <circle cx="12" cy="17" r="1" />
            </svg>
          ),
        },
      ].map((opt) => (
        <button
          key={opt.value}
          type="button"
          className={`severity-btn ${workItem.severity === opt.value ? "active" : ""}`}
          onClick={() => setWorkItem((prev) => ({ ...prev, severity: opt.value }))}
          style={{
            '--severity-color': opt.color,
          } as React.CSSProperties}
        >
          <span className="severity-icon">{opt.icon}</span>
          <span className="severity-label">{opt.label}</span>
        </button>
      ))}
    </div>
  </div>
)}
              
              {/* Tags */}
              <div className="options-group tags-group">
              <h3>Tags</h3>
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
                        <button type="button" onClick={() => removeTag(index)}>
                          ×
                        </button>
                      </span>
                    ))}
                  </div>
                </div>
              </div>

              {/* Team */}
              <div className="options-group team-group">
                <h3>Responsible</h3>
                <div className="team-selection">
                  <div
                    className="select-members"
                    onClick={(e) => {
                      e.stopPropagation(); // Prevent closing when clicking inside
                      toggleTeamList();
                    }}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" || e.key === " ") {
                        e.stopPropagation();
                        toggleTeamList();
                      }
                    }}
                  >
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
                      <span className="placeholder1">Select Members</span>
                    )}
                    <span className="dropdown-arrow">
                      {isTeamListOpen ? "▲" : "▼"}
                    </span>
                  </div>
                  {isTeamListOpen && (
                    <div className="team-list-task">
                      {loading ? (
                        <span>Loading team members...</span>
                      ) : error ? (
                        <span>{error}</span>
                      ) : teamMembers.length === 0 ? (
                        <span>No team members found</span>
                      ) : (
                        teamMembers.map((user) => (
                          <label key={user.id} className="team-member">
                            <input
                              type="checkbox"
                              checked={workItem.assignedUser.includes(user.id)}
                              onChange={() => handleUserSelection(user.id)}
                              style={{ display: "none" }}
                            />
                            <div
                              className={`team-member-inner ${
                                workItem.assignedUser.includes(user.id)
                                  ? "selected"
                                  : ""
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
                  )}
                </div>
              </div>

            </div>
          </div>

          <div className="modal-actions-task">
            <button type="submit" className="btn-task primary">
              {isEditing ? "Update" : "Create"}
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
