"use client";
import React, { useState, useEffect, useRef } from "react";
import { useAuth } from "../context/AuthContext";
import useAxios from "../hooks/useAxios";
import { useWebSocket } from "../hooks/useWebSocket";
import { AUTH_SERVICE_URL, TASK_SERVICE_URL } from "../config/useApi";
import sanitizeHtml from "sanitize-html";
import { Document, Page, pdfjs } from "react-pdf";
import "react-pdf/dist/esm/Page/AnnotationLayer.css";
import "react-pdf/dist/esm/Page/TextLayer.css";
import { useRouter } from "next/navigation";
import { toast } from "react-toastify";
import { useForm } from "react-hook-form";
import ReactQuill from "react-quill-new";
import "react-quill-new/dist/quill.snow.css";

pdfjs.GlobalWorkerOptions.workerSrc = `//unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.mjs`;

const quillModules = {
  toolbar: [
    [{ header: [1, 2, false] }],
    ["bold", "italic", "underline"],
    [{ list: "ordered" }, { list: "bullet" }],
    ["link"],
    ["clean"],
  ],
};

interface CommentForm {
  content: string;
}

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
  attachments: FileAttachment[];
}

interface TaskCardProps {
  task: Task;
  showDates: boolean;
  toggleDates: () => void;
  onTaskUpdate?: (updatedTask: Partial<Task> & { deleted?: boolean }) => void;
  displayMode?: "card" | "row"; // Nouveau prop pour choisir le mode d'affichage
}

const TaskCard: React.FC<TaskCardProps> = ({
  task,
  onTaskUpdate,
  displayMode = "card",
}) => {
  const { accessToken } = useAuth();
  const axiosInstance = useAxios();
  const [showUsersPopup, setShowUsersPopup] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState<string | null>(null);
  const popupRef = useRef<HTMLDivElement>(null);
  const [showLocalDates, setShowLocalDates] = useState(false);
  const [showMenu, setShowMenu] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const [numPages, setNumPages] = useState<number | null>(null);
  const [enlargedImage, setEnlargedImage] = useState<string | null>(null);
  const [isDeletingTask, setIsDeletingTask] = useState(false);
  const router = useRouter();
  const { handleSubmit, watch, setValue, reset } = useForm<CommentForm>({
    defaultValues: { content: "" },
  });
  const [showComments, setShowComments] = useState(false);
  const toggleComments = () => setShowComments(!showComments);
  const { comments, setComments } = useWebSocket(task.id, accessToken);
  const [users, setUsers] = useState<Map<string, User>>(new Map());

  const priorityColors: { [key: string]: string } = {
    LOW: "#4caf50",
    MEDIUM: "#ff9800",
    HIGH: "#f44336",
    CRITICAL: "#d81b60",
    "": "#b0bec5",
  };

  const priorityLabels: { [key: string]: string } = {
    LOW: "Faible",
    MEDIUM: "Moyenne",
    HIGH: "Élevée",
    CRITICAL: "Critique",
    "": "Aucune",
  };

  useEffect(() => {
    const fetchUsers = async () => {
      if (!comments.length || !accessToken) return;

      const userIds = [...new Set(comments.map((comment) => comment.author))];
      try {
        const response = await axiosInstance.post<User[]>(
          `${AUTH_SERVICE_URL}/api/tasks_reponsibles/by-ids`,
          userIds,
          { headers: { Authorization: `Bearer ${accessToken}` } }
        );
        const userMap = new Map<string, User>();
        response.data.forEach((user) => userMap.set(user.id, user));
        setUsers(userMap);
      } catch (error) {
        console.error(
          "Erreur lors de la récupération des utilisateurs :",
          error
        );
      }
    };

    fetchUsers();
  }, [comments, accessToken, axiosInstance]);

  useEffect(() => {
    if (!task.id || !accessToken) return;

    const fetchComments = async () => {
      try {
        const response = await axiosInstance.get(
          `${TASK_SERVICE_URL}/api/project/task/comments/getComment/${task.id}`,
          { headers: { Authorization: `Bearer ${accessToken}` } }
        );
        setComments(response.data);
      } catch (err) {
        console.error("Error fetching comments:", err);
        toast.error("Failed to load comments");
      }
    };

    fetchComments();
  }, [task.id, accessToken, axiosInstance, setComments]);

  const onSubmitComment = async (data: { content: string }) => {
    if (!task.id || !accessToken) return;

    const sanitizedContent = sanitizeHtml(data.content, {
      allowedTags: ["p", "b", "i", "u", "ul", "ol", "li", "a", "span"],
      allowedAttributes: { a: ["href"] },
    });

    try {
      await axiosInstance.post(
        `${TASK_SERVICE_URL}/api/project/task/comments/createComment`,
        { content: sanitizedContent, workItem: { id: task.id } },
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );
      reset();
      toast.success("Comment added!");
      // Rafraîchir les commentaires après l'ajout
      const response = await axiosInstance.get(
        `${TASK_SERVICE_URL}/api/project/task/comments/getComment/${task.id}`,
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );
      setComments(response.data);
    } catch (err) {
      console.error("Error adding comment:", err);
      toast.error("Failed to add comment");
    }
  };

  const [isTogglingStatus, setIsTogglingStatus] = useState(false);
  const [isEstimationPopupOpen, setIsEstimationPopupOpen] = useState(false);
  const toggleEstimationPopup = () => {
    setIsEstimationPopupOpen((prev) => !prev);
  };

  const handleToggleStatus = async () => {
    if (!task.id || isTogglingStatus) return;

    setIsTogglingStatus(true);
    setUploadError(null);

    const newStatus = task.status === "DONE" ? "TO_DO" : "DONE";

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
      tags: task.tags || [],
      assignedUsers: (task.assignedUsers || []).map((user) => ({
        id: user.id,
        firstName: user.firstName,
        lastName: user.lastName,
        avatar: user.avatar,
      })),
      attachments: (task.attachments || []).map((attachment) => ({
        id: attachment.id,
        fileName: attachment.fileName,
        fileType: attachment.fileType,
        fileSize: attachment.fileSize,
        fileUrl: attachment.fileUrl,
        publicId: attachment.publicId,
        uploadedBy: attachment.uploadedBy,
        uploadedAt: attachment.uploadedAt,
      })),
    };

    try {
      const response = await axiosInstance.put(
        `${TASK_SERVICE_URL}/api/project/tasks/${task.id}/updateTask`,
        taskDTO,
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );
      onTaskUpdate?.(response.data);
    } catch (err: any) {
      let errorMessage = "Failed to toggle task status";
      if (err.response?.data) {
        errorMessage =
          typeof err.response.data === "string"
            ? err.response.data
            : err.response.data.error ||
              err.response.data.message ||
              errorMessage;
      }
      setUploadError(errorMessage);
      console.error("Error toggling task status:", {
        status: err.response?.status,
        data: err.response?.data,
        message: err.message,
      });
    } finally {
      setIsTogglingStatus(false);
    }
  };

  const handleDragStart = (e: React.DragEvent<HTMLDivElement>) => {
    if (task.id) {
      e.dataTransfer.setData("taskId", task.id.toString());
      e.currentTarget.style.opacity = "0.5";
    }
  };

  const handleDragEnd = (e: React.DragEvent<HTMLDivElement>) => {
    e.currentTarget.style.opacity = "1";
  };

  const openModal = (url: string) => {
    setEnlargedImage(url);
  };

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setShowMenu(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  const handleDeleteTask = async () => {
    if (!task.id || isDeletingTask) return;

    if (!confirm("Are you sure you want to delete this task?")) return;

    setIsDeletingTask(true);
    setUploadError(null);

    try {
      await axiosInstance.delete(
        `${TASK_SERVICE_URL}/api/project/tasks/${task.id}/deleteTask`,
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );
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

  const handleImageDownload = async (
    imageUrl: string,
    defaultFileName: string
  ) => {
    try {
      const response = await fetch(imageUrl, { mode: "cors" });
      if (!response.ok) throw new Error("Failed to fetch image");
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
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
    if (!task.userStoryId || !task.id) {
      console.error("Missing userStory or task ID");
      return;
    }
    router.push(
      `/user/dashboard/tasks/AddTaskModal/${task.projectId}/${task.userStoryId}/${task.id}`
    );
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
      onTaskUpdate?.(response.data);
    } catch (err: any) {
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

  const handleFileDownload = async (fileUrl: string, fileName: string) => {
    try {
      const response = await fetch(fileUrl, { mode: "cors" });
      if (!response.ok) throw new Error("Failed to fetch file");
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = fileName || "attachment";
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error("Error downloading file:", err);
      setUploadError("Impossible de télécharger le fichier.");
    }
  };

  const handleDeleteFile = async (publicId: string) => {
    if (!task.id || !publicId || deleting) return;

    if (!confirm("Are you sure you want to delete this file?")) return;

    setUploading(true);
    setUploadError(null);
    setDeleting(publicId);

    try {
      await axiosInstance.delete(
        `${TASK_SERVICE_URL}/api/project/tasks/delete?publicId=${encodeURIComponent(
          publicId
        )}`,
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );

      const updatedTask = {
        ...task,
        attachments: task.attachments.filter(
          (attachment) => attachment.publicId !== publicId
        ),
      };
      onTaskUpdate?.(updatedTask);
    } catch (err: any) {
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

  // Rendu pour le mode "row" (utilisé dans la vue Liste)
  if (displayMode === "row") {
    return (
      <div className="task-row">
        <div className="task-checkbox">
          <input
            type="checkbox"
            checked={task.status === "DONE"}
            onChange={handleToggleStatus}
            disabled={isTogglingStatus}
          />
          <span>{task.title}</span>
        </div>
        <div className="task-responsible">
          {task.assignedUsers.length > 0 ? (
            <div className="responsible-avatars">
              {task.assignedUsers.slice(0, 3).map((user, index) => (
                <span
                  key={user.id}
                  className="responsible-circle"
                  style={{ zIndex: task.assignedUsers.length - index }}
                  title={`${user.firstName} ${user.lastName}`}
                >
                  {user.firstName[0]}
                  {user.lastName[0]}
                </span>
              ))}
              {task.assignedUsers.length > 3 && (
                <span className="responsible-circle more">
                  +{task.assignedUsers.length - 3}
                </span>
              )}
            </div>
          ) : (
            <span className="responsible-circle empty">?</span>
          )}
         
        </div>
        <div className="task-due-date">
          {task.dueDate
            ? new Date(task.dueDate).toLocaleDateString("fr-FR", {
                day: "numeric",
                month: "short",
              })
            : "Aucune"}
        </div>
        <div className="task-priority-list">
          <span
            className="priority-tag"
            style={{ backgroundColor: priorityColors[task.priority] }}
          >
            {priorityLabels[task.priority]}
          </span>
        </div>
        <div className="task-tags">
          {task.tags && task.tags.length > 0
            ? task.tags.map((tag, index) => (
                <span
                  key={index}
                  className="tag-pill"
                  style={{ backgroundColor: "#e0e0e0" }}
                >
                  {tag}
                </span>
              ))
            : "Aucun"}
        </div>
        <div className="task-files">
          {task.attachments && task.attachments.length > 0 ? (
            <div className="file-list">
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
                  <div key={index} className="attachment-item-list">
                    {attachment.fileType.startsWith("image/") ? (
                      <div className="attachment-preview">
                        <img
                          src={attachment.fileUrl}
                          alt={attachment.fileName}
                          className="attachment-image-list"
                          onClick={() => openModal(attachment.fileUrl)}
                        />
                      </div>
                    ) : attachment.fileType === "application/pdf" ? (
                      <div className="attachment-preview-pdf-list">
                         <Document
                          file={attachment.fileUrl}
                          onLoadSuccess={onDocumentLoadSuccess}
                          onLoadError={(error) => {
                            console.error("PDF Load Error:", error);
                            setUploadError("Failed to load PDF preview.");
                          }}
                          className="pdf-preview-list"
                        >
                          <Page
                            pageNumber={1}
                            width={100}
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
                      </div>
                    ) : (
                      <div className="file-info">
                        <span>
                          {attachment.fileName} (
                          {(attachment.fileSize / 1024).toFixed(2)} KB)
                        </span>
                      </div>
                    )}
                    <div className="attachment-actions-list">
                      <button
                        className="download-btn-list"
                        onClick={() =>
                          handleFileDownload(
                            attachment.fileUrl,
                            attachment.fileName
                          )
                        }
                        title="Download file"
                      >
                        <i className="fa fa-download download-file-style"></i>
                      </button>
                      <button
                        className="delete-file-btn-list"
                        onClick={() => handleDeleteFile(attachment.publicId)}
                        disabled={uploading || deleting === attachment.publicId}
                        title="Delete file"
                      >
                        <i className="fa fa-trash delete-file-style"></i>
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            "Aucun"
          )}
          <label htmlFor={`file-upload-${task.id}`} className="file-upload-btn">
            <i className="fa fa-plus"></i> Add File
          </label>
          <input
            id={`file-upload-${task.id}`}
            type="file"
            ref={fileInputRef}
            onChange={handleFileUpload}
            style={{ display: "none" }}
            disabled={uploading}
          />
          {uploading && <span className="status-message">Uploading...</span>}
          {uploadError && <span className="error-message">{uploadError}</span>}
        </div>
        <div className="task-comments" onClick={toggleComments}>
          <span className="comment-counter">
            <i className="fa fa-comment"></i> {comments.length || 0}
          </span>
        </div>
        {showComments && (
          <div className="comments-popup">
            <div className="comments-header">
              <h5>Comments</h5>
              <button className="close-comments-btn" onClick={toggleComments}>
                ✕
              </button>
            </div>
            {comments.length === 0 ? (
              <p className="no-comments">Aucun commentaire pour l instant.</p>
            ) : (
              <div className="comments-list">
                {comments.map((comment) => {
                  const user = users.get(comment.author);
                  const authorName = user
                    ? `${user.firstName} ${user.lastName}`
                    : "Utilisateur inconnu";
                  return (
                    <div key={comment.id} className="comment-item">
                      <div
                        className="comment-content"
                        dangerouslySetInnerHTML={{ __html: comment.content }}
                      />
                      <div className="comment-meta">
                        <small>
                          By {authorName} the{" "}
                          {new Date(comment.createdAt).toLocaleString()}
                        </small>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
            <form
              onSubmit={handleSubmit(onSubmitComment)}
              className="comment-form"
            >
              <ReactQuill
                value={watch("content")}
                onChange={(value) => setValue("content", value)}
                modules={quillModules}
                placeholder="Add a comment ..."
                className="comment-input"
              />
              <button type="submit" className="submit-comment-btn">
                Envoyer
              </button>
            </form>
          </div>
        )}
        {enlargedImage && (
          <div className="image-modal" onClick={() => setEnlargedImage(null)}>
            <div
              className="image-modal-content"
              onClick={(e) => e.stopPropagation()}
            >
              <img
                src={enlargedImage}
                alt="Enlarged attachment"
                className="enlarged-image"
              />
              <div className="modal-actions">
                <button
                  className="download-image-btn"
                  onClick={() =>
                    handleImageDownload(enlargedImage, "attachment.jpg")
                  }
                >
                  <i className="fa fa-download"></i>
                </button>
                <button
                  className="close-modal-btn"
                  onClick={() => setEnlargedImage(null)}
                >
                  ✕
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }

  // Rendu pour le mode "card" (utilisé dans Kanban)
  return (
    <div
      className="task-card"
      key={task.id}
      draggable
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
    >
      <div className="task-card-header">
        <h4>{task.title}</h4>
        <div className="task-menu">
          <button
            className="menu-btn"
            onClick={() => setShowMenu((prev) => !prev)}
          >
            ...
          </button>
          {showMenu && (
            <div className="task-menu-dropdown" ref={menuRef}>
              <button className="menu-item" onClick={handleEdit}>
                <i className="fa fa-edit calendar-style"></i> Modifier
              </button>
              <button
                className="menu-item"
                onClick={handleDeleteTask}
                disabled={isDeletingTask}
              >
                <i className="fa fa-trash"></i> Supprimer
              </button>
            </div>
          )}
        </div>
      </div>

      {task.description && (
        <p className="task-description">{task.description}</p>
      )}

      <div className="priority-tags-row">
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
      </div>

      <div className="attachment-section">
        <div className="attachment-container">
          <div className="attachments-list">
            {task.attachments.length > 0 &&
              task.attachments.map((attachment, index) => {
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
                          onClick={() => openModal(attachment.fileUrl)}
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
                      </div>
                    ) : (
                      <div className="file-info">
                        <span>
                          {attachment.fileName} (
                          {(attachment.fileSize / 1024).toFixed(2)} KB)
                        </span>
                      </div>
                    )}
                    <div className="attachment-actions">
                      <button
                        className="download-btn"
                        onClick={() =>
                          handleFileDownload(
                            attachment.fileUrl,
                            attachment.fileName
                          )
                        }
                        title="Download file"
                      >
                        <i className="fa fa-download download-file-style"></i>
                      </button>
                      <button
                        className="delete-file-btn"
                        onClick={() => handleDeleteFile(attachment.publicId)}
                        disabled={uploading || deleting === attachment.publicId}
                        title="Delete file"
                      >
                        <i className="fa fa-trash delete-file-style"></i>
                      </button>
                    </div>
                  </div>
                );
              })}
          </div>
          <div className="attachment-controls">
            <label htmlFor={`file-upload-${task.id}`} className="btn-attach">
              Attach File
              <i className="fa fa-paperclip"></i>
            </label>
            <input
              id={`file-upload-${task.id}`}
              type="file"
              ref={fileInputRef}
              onChange={handleFileUpload}
              style={{ display: "none" }}
              disabled={uploading}
            />
          </div>
        </div>
        {uploading && <span className="status-message">Uploading...</span>}
        {uploadError && <span className="error-message">{uploadError}</span>}
      </div>

      <div className="task-footer">
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
        <div className="right-container">
          <button
            className={`toggle-status-btn ${
              task.status === "DONE" ? "done" : "to-do"
            }`}
            onClick={handleToggleStatus}
            disabled={isTogglingStatus}
            title={task.status === "DONE" ? "Mark as To Do" : "Mark as Done"}
            data-status={task.status}
          >
            {isTogglingStatus ? (
              <i className="fa fa-spinner fa-spin status-icon"></i>
            ) : (
              <i className="fa fa-check status-icon"></i>
            )}
          </button>

          <div className="comments-container">
            <button
              className="comments-btn"
              onClick={toggleComments}
              title="View comments"
            >
              <i className="fa fa-comment calendar-style"></i>
            </button>
            {showComments && (
              <div className="comments-popup">
                <div className="comments-header">
                  <h5>Comments</h5>
                  <button
                    className="close-comments-btn"
                    onClick={toggleComments}
                  >
                    ✕
                  </button>
                </div>
                {comments.length === 0 ? (
                  <p className="no-comments">
                    Aucun commentaire pour l instant.
                  </p>
                ) : (
                  <div className="comments-list">
                    {comments.map((comment) => {
                      const user = users.get(comment.author);
                      const authorName = user
                        ? `${user.firstName} ${user.lastName}`
                        : "Utilisateur inconnu";
                      return (
                        <div key={comment.id} className="comment-item">
                          <div
                            className="comment-content"
                            dangerouslySetInnerHTML={{
                              __html: comment.content,
                            }}
                          />
                          <div className="comment-meta">
                            <small>
                              By {authorName} the{" "}
                              {new Date(comment.createdAt).toLocaleString()}
                            </small>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
                <form
                  onSubmit={handleSubmit(onSubmitComment)}
                  className="comment-form"
                >
                  <ReactQuill
                    value={watch("content")}
                    onChange={(value) => setValue("content", value)}
                    modules={quillModules}
                    placeholder="Add a comment ..."
                    className="comment-input"
                  />
                  <button type="submit" className="submit-comment-btn">
                    Envoyer
                  </button>
                </form>
              </div>
            )}
          </div>

          {(task.creationDate || task.dueDate) && (
            <div className="date-container">
              <button
                className="calendar-btn"
                onClick={() => setShowLocalDates((prev) => !prev)}
              >
                <i className="fa fa-calendar-alt calendar-style"></i>
              </button>
              {showLocalDates && (
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
            <div className="estimation-time-wrapper">
              <button
                className="estimation-time-btn"
                onClick={toggleEstimationPopup}
                title="View estimation time"
              >
                <i className="fa fa-clock calendar-style"></i>
              </button>
              {isEstimationPopupOpen && (
                <div className="estimation-popup-wrapper">
                  <div className="estimation-popup">
                    <span>{task.estimationTime} hours</span>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
      {enlargedImage && (
        <div className="image-modal" onClick={() => setEnlargedImage(null)}>
          <div
            className="image-modal-content"
            onClick={(e) => e.stopPropagation()}
          >
            <img
              src={enlargedImage}
              alt="Enlarged attachment"
              className="enlarged-image"
            />
            <div className="modal-actions">
              <button
                className="download-image-btn"
                onClick={() =>
                  handleImageDownload(enlargedImage, "attachment.jpg")
                }
              >
                <i className="fa fa-download"></i>
              </button>
              <button
                className="close-modal-btn"
                onClick={() => setEnlargedImage(null)}
              >
                ✕
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TaskCard;
