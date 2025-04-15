"use client";
import { useParams } from "next/navigation";
import React, { useState, useEffect, useRef } from "react";
import { useAuth } from "../../../../../../context/AuthContext";
import useAxios from "../../../../../../hooks/useAxios";
import { TASK_SERVICE_URL } from "../../../../../../config/useApi";
import "../../../../../../styles/Dashboard-Task-Kanban.css";
import { Document, Page, pdfjs } from "react-pdf";
import "react-pdf/dist/esm/Page/AnnotationLayer.css";
import "react-pdf/dist/esm/Page/TextLayer.css";
import { useRouter } from "next/navigation";
// Set up the PDF.js worker
pdfjs.GlobalWorkerOptions.workerSrc = `//unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.mjs`;

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
  assignedUsers: User[];
  attachments: FileAttachment[]; // Changed to non-optional
}

interface TaskCardProps {
  task: Task;
  showDates: boolean;
  toggleDates: () => void;
  onTaskUpdate?: (updatedTask: Task) => void;
}

const TaskCard: React.FC<TaskCardProps> = ({
  task,
  showDates,
  toggleDates,
  onTaskUpdate,
}) => {
  const { accessToken } = useAuth();
  const axiosInstance = useAxios();
  const [showUsersPopup, setShowUsersPopup] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState<string | null>(null);
  const popupRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [numPages, setNumPages] = useState<number | null>(null);
  const [enlargedImage, setEnlargedImage] = useState<string | null>(null);
  const [isDeletingTask, setIsDeletingTask] = useState(false); // Nouvel état pour la suppression de la tâche
  const router = useRouter();
  const priorityColors: { [key: string]: string } = {
    LOW: "#4caf50",
    MEDIUM: "#ff9800",
    HIGH: "#f44336",
    CRITICAL: "#d81b60",
    "": "#b0bec5",
  };
  useEffect(() => {
    const handleClickOutsideModal = (event: MouseEvent) => {
      if (enlargedImage && !event.target.closest(".image-modal-content")) {
        console.log("Clicked outside modal, closing");
        setEnlargedImage(null);
      }
    };
  
    document.addEventListener("mousedown", handleClickOutsideModal);
    return () => {
      document.removeEventListener("mousedown", handleClickOutsideModal);
    };
  }, [enlargedImage]);

  // Fonction pour ouvrir le modal
  const openModal = (url: string) => {
    console.log("Opening modal with URL:", url, "Task ID:", task.id, "Current enlargedImage:", enlargedImage);
    setEnlargedImage(url);
  };


  
  const handleDeleteTask = async () => {
    if (!task.id || isDeletingTask) return;
  
    if (!confirm("Are you sure you want to delete this task?")) return;
  
    setIsDeletingTask(true);
    setUploadError(null);
  
    try {
      console.log("Deleting task with ID:", task.id);
      await axiosInstance.delete(
        `${TASK_SERVICE_URL}/api/project/tasks/${task.id}/deleteTask`,
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );
      console.log("Task deleted successfully");
      // Passer l'ID de la tâche avec une propriété deleted
      onTaskUpdate?.({ id: task.id, deleted: true } as any);
    } catch (err: any) {
      let errorMessage = "Failed to delete task";
      if (err.response?.data) {
        errorMessage =
          typeof err.response.data === "string"
            ? err.response.data
            : err.response.data.error ||
              err.response.data.message ||
              errorMessage;
      }
      setUploadError(errorMessage);
      console.error("Error deleting task:", {
        status: err.response?.status,
        data: err.response?.data,
        message: err.message,
      });
    } finally {
      setIsDeletingTask(false);
    }
  };




  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      const modalContent = document.querySelector(".image-modal-content");
      if (enlargedImage && modalContent && !modalContent.contains(event.target as Node)) {
        console.log("Clicked outside modal content, closing modal");
        setEnlargedImage(null);
      }
    };
  
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [enlargedImage]);

  const handleImageDownload = async (imageUrl: string, defaultFileName: string) => {
    try {
      const response = await fetch(imageUrl, { mode: "cors" });
      if (!response.ok) throw new Error("Failed to fetch image");
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      // Essaie de récupérer le nom de fichier depuis l'attachment si disponible
      const fileName =
        task.attachments.find((att) => att.fileUrl === imageUrl)?.fileName ||
        defaultFileName;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error("Error downloading image:", err);
      setUploadError("Impossible de télécharger l'image.");
    }
  };


  const handleEdit = () => {
    console.log("Editing task:", task.id, task.userStoryId); // Pour déboguer
    if (!task.userStoryId || !task.id) {
      console.error("Missing userStory or task ID");
      return;
    }
    // Supposons que userStoryId est disponible dans task.userStory
    router.push(`/user/dashboard/tasks/AddTaskModal/${task.projectId}/${task.userStoryId}/${task.id}`);
  };

  const toggleUsersPopup = () => {
    setShowUsersPopup((prev) => !prev);
  };

  const handleFileUpload = async (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    const file = event.target.files?.[0];
    if (!file || !task.id) return;

    setUploading(true);
    setUploadError(null);

    const formData = new FormData();
    formData.append("file", file);

    try {
      const response = await axiosInstance.post(
        `${TASK_SERVICE_URL}/api/project/tasks/${task.id}/attachments`,
        formData,
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
            "Content-Type": "multipart/form-data",
          },
        }
      );
      console.log("File Upload Response:", response.data);
      onTaskUpdate?.(response.data);
    } catch (err) {
      let errorMessage = "Failed to upload file";
      if (err.response?.data) {
        errorMessage =
          typeof err.response.data === "string"
            ? err.response.data
            : err.response.data.error ||
              err.response.data.message ||
              errorMessage;
      }
      setUploadError(errorMessage);
      console.error("Error uploading file:", err.response?.data || err);
    } finally {
      setUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

  const handleDeleteFile = async (publicId: string) => {
    if (!task.id || !publicId || deleting) return;

    if (!confirm("Are you sure you want to delete this file?")) return;

    setUploading(true);
    setUploadError(null);
    setDeleting(publicId);

    try {
      console.log("Deleting file with publicId:", publicId);
      await axiosInstance.delete(
        `${TASK_SERVICE_URL}/api/project/tasks/delete?publicId=${encodeURIComponent(publicId)}`, 
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );
      

      const updatedTask = {
        ...task,
        attachments: task.attachments.filter(
          (attachment) => attachment.publicId !== publicId
        ),
      };
      onTaskUpdate?.(updatedTask);
    } catch (err) {
      let errorMessage = "Failed to delete file";
      if (err.response?.data) {
        errorMessage =
          typeof err.response.data === "string"
            ? err.response.data
            : err.response.data.error ||
              err.response.data.message ||
              errorMessage;
      }
      setUploadError(errorMessage);
      console.error("Error deleting file:", {
        status: err.response?.status,
        data: err.response?.data,
        message: err.message,
      });
    } finally {
      setUploading(false);
      setDeleting(null);
    }
  };

  const onDocumentLoadSuccess = ({ numPages }: { numPages: number }) => {
    setNumPages(numPages);
  };

  return (
    <div className="task-card" key={task.id}>
      <h4>{task.title}</h4>
      {task.description && (
        <p className="task-description">{task.description}</p>
      )}
      {task.priority && (
        <span
          className="priority-tag"
          style={{ backgroundColor: priorityColors[task.priority] }}
        >
          {task.priority}
        </span>
      )}
      {task.tags?.length > 0 && (
        <div className="tags-container">
          {task.tags.map((tag, index) => (
            <span key={index} className="task-tag">
              {tag}
            </span>
          ))}
        </div>
      )}
      {task.assignedUsers?.length > 0 && (
        <div className="assigned-users-container">
          <div className="avatars-stack" onClick={toggleUsersPopup}>
            {task.assignedUsers.slice(0, 3).map((user, index) => (
              <div
                key={user.id}
                className="avatar-wrapper"
                style={{ zIndex: task.assignedUsers.length - index }}
              >
                {user.avatar ? (
                  <img
                    src={user.avatar}
                    alt={`${user.firstName} ${user.lastName}`}
                    className="user-avatar-stack"
                  />
                ) : (
                  <div className="user-avatar-placeholder">
                    {user.firstName.charAt(0)}
                    {user.lastName.charAt(0)}
                  </div>
                )}
              </div>
            ))}
            {task.assignedUsers.length > 3 && (
              <div className="avatar-more">
                +{task.assignedUsers.length - 3}
              </div>
            )}
          </div>
          {showUsersPopup && (
            <div className="users-card-popup" ref={popupRef}>
              {task.assignedUsers.map((user) => (
                <div key={user.id} className="user-card-item">
                  <div className="user-card-avatar">
                    {user.avatar ? (
                      <img
                        src={user.avatar}
                        alt={`${user.firstName} ${user.lastName}`}
                        className="user-card-avatar-img"
                      />
                    ) : (
                      <div className="user-card-avatar-placeholder">
                        {user.firstName.charAt(0)}
                        {user.lastName.charAt(0)}
                      </div>
                    )}
                  </div>
                  <div className="user-card-info">
                    <span className="user-card-name">
                      {user.firstName} {user.lastName}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}


      <div className="task-actions">
        <button className="edit-btn" onClick={handleEdit}>
          ✏️ Edit
        </button>
        <button
          className="delete-btn"
          onClick={handleDeleteTask}
          disabled={isDeletingTask}
        >
          🗑️ Delete
        </button>
      </div>

      <div className="attachment-section">
        <label htmlFor={`file-upload-${task.id}`} className="attach-file-btn">
          📎 Attach File
        </label>
        <input
          id={`file-upload-${task.id}`}
          type="file"
          ref={fileInputRef}
          onChange={handleFileUpload}
          style={{ display: "none" }}
          disabled={uploading}
        />
        {uploading && <span>Uploading...</span>}
        {uploadError && <span style={{ color: "red" }}>{uploadError}</span>}
        {task.attachments.length > 0 && (
  <div className="attachments-list">
    {task.attachments.map((attachment, index) => {
      if (
        !attachment?.fileName ||
        !attachment?.fileUrl ||
        !attachment?.fileType ||
        !attachment?.publicId
      ) {
        console.warn("Skipping invalid attachment:", attachment);
        return null;
      }
      return (
<div key={index} className="attachment-item">
{attachment.fileType.startsWith("image/") ? (
  <div className="attachment-preview">
  <img
    src={attachment.fileUrl}
    alt={attachment.fileName}
    className="attachment-image"
    onClick={() => {
      console.log("Opening image modal for:", attachment.fileUrl);
      openModal(attachment.fileUrl);
    }}
  />
</div>
                  ) : attachment.fileType === "application/pdf" ? (
                    <div className="attachment-preview">
                      <Document
                        file={attachment.fileUrl}
                        onLoadSuccess={onDocumentLoadSuccess}
                        onLoadError={(error) => {
                          console.error("PDF Load Error:", error);
                          setUploadError("Failed to load PDF preview.");
                        }}
                        className="pdf-preview"
                      >
                        <Page
                          pageNumber={1}
                          width={150}
                          renderTextLayer={false}
                          renderAnnotationLayer={false}
                          onLoadError={(error) =>
                            console.error("Page Load Error:", error)
                          }
                        />
                      </Document>
                      {numPages === null && !uploadError && (
                        <span>Loading PDF...</span>
                      )}
                      {uploadError && (
                        <a
                          href={attachment.fileUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="download-btn"
                        >
                          Download PDF
                        </a>
                      )}
                      {!uploadError && (
                        <a
                          href={attachment.fileUrl}
                          download={attachment.fileName}
                          className="download-btn"
                          title="Download PDF"
                        >
                          ⬇️
                        </a>
                      )}
                    </div>
                  ) : (
                    <a
                      href={attachment.fileUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      {attachment.fileName} (
                      {(attachment.fileSize / 1024).toFixed(2)} KB)
                    </a>
                  )}
                  <button
                    className="delete-file-btn"
                    onClick={() => handleDeleteFile(attachment.publicId)}
                    disabled={uploading || deleting === attachment.publicId}
                    title="Delete file"
                  >
                    🗑️
                  </button>
                </div>
              );
            })}
          </div>
        )}
      </div>
      {enlargedImage && (
  <div className="image-modal">
    <div className="image-modal-content">
      <img
        src={enlargedImage}
        alt="Enlarged attachment"
        className="enlarged-image"
      />
      <div className="modal-actions">
        <button
          className="download-image-btn"
          onClick={(e) => {
            e.stopPropagation(); // Empêche la propagation au modal
            console.log("Downloading image:", enlargedImage);
            handleImageDownload(enlargedImage, "attachment.jpg");
          }}
        >
          ⬇️ Download
        </button>
        <button
          className="close-modal-btn"
          onClick={(e) => {
            e.stopPropagation(); // Empêche la propagation au modal
            console.log("Clicked close button, closing modal");
            setEnlargedImage(null);
          }}
        >
          ✕
        </button>
      </div>
    </div>
  </div>
)}
      <div className="task-footer">
        {(task.creationDate || task.dueDate) && (
          <div className="date-container">
            <button className="calendar-btn" onClick={toggleDates}>
              📅
            </button>
            {showDates && (
              <div className="date-popup">
                {task.creationDate && (
                  <p className="date-item created">
                    <span>Created:</span> {task.creationDate}
                  </p>
                )}
                {task.dueDate && (
                  <p className="date-item due">
                    <span>Due:</span> {task.dueDate}
                  </p>
                )}
              </div>
            )}
          </div>
        )}
        {task.estimationTime !== null && (
          <span className="estimation-time">
            ⏱️ {task.estimationTime} hours
          </span>
        )}
      </div>
    </div>
  );
};
export default function Kanban() {
  const { accessToken, isLoading: authLoading } = useAuth();
  const axiosInstance = useAxios();
  const params = useParams();
  const projectId = params.projectId as string;
  const [tasks, setTasks] = useState<Task[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [showDates, setShowDates] = useState(false);

  const columns = [
    { status: "TO_DO", title: "To Do" },
    { status: "IN_PROGRESS", title: "In Progress" },
    { status: "DONE", title: "Done" },
    { status: "BLOCKED", title: "Blocked" },
    { status: "ARCHIVED", title: "Archived" },
    { status: "CANCELLED", title: "Cancelled" },
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
        console.log("Raw API Response:", response.data); // Add this log
        const data = Array.isArray(response.data)
          ? response.data.map((task: Task) => ({
              ...task,
              attachments: task.attachments || [],
            }))
          : [];
        setTasks(data);
        console.log("Tasks:", data);
      } catch (err) {
        const errorMessage = err.response?.data || "Failed to fetch tasks";
        setError(errorMessage);
        console.error("Error fetching tasks:", errorMessage);
        setTasks([]);
      } finally {
        setLoading(false);
      }
    }

    fetchTasksByProjectId();
  }, [projectId, accessToken, authLoading, axiosInstance]);

  // Handle task update after file upload or deletion
  const handleTaskUpdate = (updatedTask: Partial<Task> & { deleted?: boolean }) => {
    setTasks((prevTasks) => {
      if (updatedTask.deleted && updatedTask.id) {
        console.log("Removing task with ID:", updatedTask.id);
        return prevTasks.filter((task) => task.id !== updatedTask.id);
      }
      console.log("Updating task with ID:", updatedTask.id);
      return prevTasks.map((task) =>
        task.id === updatedTask.id ? { ...task, ...updatedTask } : task
      );
    });
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
            <div key={column.status} className="kanban-column">
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
