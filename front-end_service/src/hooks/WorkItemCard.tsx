/* eslint-disable */
"use client";
import React, { useState, useEffect, useRef } from "react";
import { useAuth } from "../context/AuthContext";
import useAxios from "./useAxios";
import { useWebSocket } from "./useWebSocket";
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

interface TimeForm {
  duration: number;
  type: string;
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

interface TaskSummary {
  id: number;
  title: string;
  status: string;
  projectId: number;
  userStoryId: number;
}

interface WorkItemHistory {
  id: number;
  action: string;
  description: string;
  authorName: string; // Nouveau champ
  timestamp: string;
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
  history?: WorkItemHistory[];
}

interface WorkItemCardProps {
  workItem: WorkItem;
  showDates: boolean;
  toggleDates: () => void;
  onWorkItemUpdate?: (
    updatedWorkItem: Partial<WorkItem> & { deleted?: boolean }
  ) => void;
  displayMode?: "card" | "row";
}

const getActionClass = (action: string) => {
  switch (action.toUpperCase()) {
    case "CREATION":
      return "created";
    case "MISE_A_JOUR":
      return "updated";
    case "SUPPRESSION":
      return "deleted";
    case "AJOUT_DEPENDANCE":
      return "add_dependency";
    case "SUPPRESSION_DEPENDANCE":
      return "remove_dependency";
    case "AJOUT_COMMENTAIRE":
      return "add_comment";
    case "AJOUT_PIECE_JOINTE":
      return "add_attachment";
    case "SUPPRESSION_PIECE_JOINTE":
      return "remove_attachment";
    case "AJOUT_TEMPS":
      return "add_time";
    default:
      return action.toLowerCase().replace(/\s+/g, "_");
  }
};

const getActionLabel = (action: string) => {
  switch (action.toUpperCase()) {
    case "CREATION":
      return "Créé";
    case "MISE_A_JOUR":
      return "Mis à jour";
    case "SUPPRESSION":
      return "Supprimé";
    case "AJOUT_DEPENDANCE":
      return "Dépendance ajoutée";
    case "SUPPRESSION_DEPENDANCE":
      return "Dépendance supprimée";
    case "AJOUT_COMMENTAIRE":
      return "Commentaire ajouté";
    case "AJOUT_PIECE_JOINTE":
      return "Pièce jointe ajoutée";
    case "SUPPRESSION_PIECE_JOINTE":
      return "Pièce jointe supprimée";
    case "AJOUT_TEMPS":
      return "Temps ajouté";
    default:
      return action;
  }
};

const WorkItemCard: React.FC<WorkItemCardProps> = ({
  workItem,
  onWorkItemUpdate,
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
  const [isDeletingWorkItem, setIsDeletingWorkItem] = useState(false);
  const router = useRouter();

  const [showHistoryPopup, setShowHistoryPopup] = useState(false);
  const [history, setHistory] = useState<WorkItemHistory[]>([]);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);
  const [historyError, setHistoryError] = useState<string | null>(null);

  // States for dependency management
  const [showAddDependencyPopup, setShowAddDependencyPopup] = useState(false);
  const [potentialDependencies, setPotentialDependencies] = useState<
    WorkItem[]
  >([]);
  const [isLoadingDependencies, setIsLoadingDependencies] = useState(false);
  const [dependencyError, setDependencyError] = useState<string | null>(null);
  const [showDependenciesPopup, setShowDependenciesPopup] = useState(false);
  const [isRemovingDependency, setIsRemovingDependency] = useState<
    number | null
  >(null);

  // Fetch potential dependencies
  useEffect(() => {
    if (!showAddDependencyPopup || !workItem.id || !accessToken) return;

    const fetchPotentialDependencies = async () => {
      setIsLoadingDependencies(true);
      setDependencyError(null);
      try {
        const endpoint =
          workItem.type === "TASK"
            ? `/tasks/${workItem.id}/potential-dependencies`
            : `/bugs/${workItem.id}/potential-dependencies`;
        const response = await axiosInstance.get(
          `${TASK_SERVICE_URL}/api/project${endpoint}`,
          { headers: { Authorization: `Bearer ${accessToken}` } }
        );
        setPotentialDependencies(response.data);
      } catch (err: any) {
        const errorMessage =
          err.response?.data?.message ||
          "Failed to load potential dependencies";
        setDependencyError(errorMessage);
        console.error("Error fetching potential dependencies:", err);
        toast.error(errorMessage);
      } finally {
        setIsLoadingDependencies(false);
      }
    };

    fetchPotentialDependencies();
  }, [
    showAddDependencyPopup,
    workItem.id,
    workItem.type,
    accessToken,
    axiosInstance,
  ]);

  // Handle adding a dependency
  const handleAddDependency = async (dependencyId: number) => {
    if (!workItem.id || !accessToken) return;

    setDependencyError(null);
    try {
      const endpoint =
        workItem.type === "TASK"
          ? `/tasks/${workItem.id}/dependencies/${dependencyId}/add-dependancy`
          : `/bugs/${workItem.id}/dependencies/${dependencyId}/add-dependancy`;
      const response = await axiosInstance.post(
        `${TASK_SERVICE_URL}/api/project${endpoint}`,
        null,
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );
      onWorkItemUpdate?.(response.data);
      setShowAddDependencyPopup(false);
      toast.success("Dependency added successfully!");
    } catch (err: any) {
      const errorMessage =
        err.response?.data?.message || "Failed to add dependency";
      setDependencyError(errorMessage);
      console.error("Error adding dependency:", err);
      toast.error(errorMessage);
    }
  };

  // Handle removing a dependency
  const handleRemoveDependency = async (dependencyId: number) => {
    if (!workItem.id || !accessToken) return;

    setIsRemovingDependency(dependencyId);
    setDependencyError(null);
    try {
      const endpoint =
        workItem.type === "TASK"
          ? `/tasks/${workItem.id}/dependencies/${dependencyId}`
          : `/bugs/${workItem.id}/dependencies/${dependencyId}`;
      const response = await axiosInstance.delete(
        `${TASK_SERVICE_URL}/api/project${endpoint}`,
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );
      onWorkItemUpdate?.(response.data);
      toast.success("Dependency removed successfully!");
    } catch (err: any) {
      const errorMessage =
        err.response?.data?.message || "Failed to remove dependency";
      setDependencyError(errorMessage);
      console.error("Error removing dependency:", err);
      toast.error(errorMessage);
    } finally {
      setIsRemovingDependency(null);
    }
  };

  // Formulaire pour les commentaires
  const {
    handleSubmit: handleSubmitComment,
    watch,
    setValue,
    reset,
  } = useForm<CommentForm>({
    defaultValues: { content: "" },
  });

  // Formulaire pour ajouter du temps
  const {
    handleSubmit: handleSubmitTime,
    register,
    formState: { errors },
  } = useForm<TimeForm>({
    defaultValues: { duration: 0.1, type: "travail" },
  });

  const [showComments, setShowComments] = useState(false);
  const toggleComments = () => setShowComments(!showComments);
  const { comments, setComments } = useWebSocket(workItem.id, accessToken);
  const [users, setUsers] = useState<Map<string, User>>(new Map());

  const priorityColors: { [key: string]: string } = {
    LOW: "#4caf50",
    MEDIUM: "#ff9800",
    HIGH: "#f44336",
    CRITICAL: "#d81b60",
    "": "#b0bec5",
  };

  const severityColors: { [key: string]: string } = {
    MINOR: "#2196f3",
    MAJOR: "#ff9800",
    CRITICAL: "#f44336",
    BLOCKER: "#d81b60",
    "": "#b0bec5",
  };

  const priorityLabels: { [key: string]: string } = {
    LOW: "Low",
    MEDIUM: "Medium",
    HIGH: "High",
    CRITICAL: "Critical",
    "": "None",
  };

  const severityLabels: { [key: string]: string } = {
    MINOR: "Minor",
    MAJOR: "Major",
    CRITICAL: "Critical",
    BLOCKER: "Blocker",
    "": "None",
  };

  const [displayedTimeSpent, setDisplayedTimeSpent] = useState<number | null>(
    workItem.totalTimeSpent
  );
  const [setDisplayedProgress] = useState<number>(workItem.progress);

  // Récupérer l'historique
  useEffect(() => {
    if (!showHistoryPopup || !workItem.id || !accessToken) return;

    const fetchHistory = async () => {
      setIsLoadingHistory(true);
      setHistoryError(null);
      try {
        const endpoint =
          workItem.type === "TASK"
            ? `/tasks/${workItem.id}/history-with-author-names`
            : `/bugs/${workItem.id}/history-with-author-names`; // À adapter si vous avez un service similaire pour les bugs
        const response = await axiosInstance.get(
          `${TASK_SERVICE_URL}/api/project${endpoint}`,
          { headers: { Authorization: `Bearer ${accessToken}` } }
        );
        setHistory(response.data);
      } catch (err: any) {
        const errorMessage =
          err.response?.data?.message || "Échec du chargement de l'historique";
        setHistoryError(errorMessage);
        console.error("Erreur lors de la récupération de l'historique:", err);
        toast.error(errorMessage);
      } finally {
        setIsLoadingHistory(false);
      }
    };

    fetchHistory();
  }, [showHistoryPopup, workItem.id, workItem.type, accessToken]);

  // Mettre à jour le temps affiché en temps réel si l'élément est en IN_PROGRESS
  useEffect(() => {
    let interval: NodeJS.Timeout | null = null;
    if (workItem.status === "IN_PROGRESS" && workItem.startTime) {
      interval = setInterval(() => {
        const start = new Date(workItem.startTime);
        const now = new Date();
        const elapsedMinutes = Math.round(
          (now.getTime() - start.getTime()) / 1000 / 60
        );
        const totalMinutes = (workItem.totalTimeSpent || 0) + elapsedMinutes;
        setDisplayedTimeSpent(totalMinutes);

        // Calculer la progression
        if (workItem.estimationTime && workItem.estimationTime > 0) {
          const progress = Math.min(
            (totalMinutes / workItem.estimationTime) * 100,
            90
          );
      
        }
      }, 60000); // Mettre à jour chaque minute
    } else {
      setDisplayedTimeSpent(workItem.totalTimeSpent);

    }

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [
    workItem.status,
    workItem.startTime,
    workItem.totalTimeSpent,
    workItem.estimationTime,
    workItem.progress,
  ]);

  const [showAddTimeForm, setShowAddTimeForm] = useState(false);
  const [isAddingTime, setIsAddingTime] = useState(false);

  const onSubmitAddTime = async (data: { duration: number; type: string }) => {
    if (!workItem.id || !accessToken) return;

    setIsAddingTime(true);
    setUploadError(null);

    try {
      const durationInMinutes = Math.round(data.duration * 60);
      const endpoint =
        workItem.type === "TASK"
          ? `/tasks/${workItem.id}/time-entry`
          : `/bugs/${workItem.id}/time-entry`;
      await axiosInstance.post(
        `${TASK_SERVICE_URL}/api/project${endpoint}`,
        null,
        {
          headers: { Authorization: `Bearer ${accessToken}` },
          params: { duration: durationInMinutes, type: data.type },
        }
      );

      // Récupérer l'élément mis à jour
      const fetchEndpoint =
        workItem.type === "TASK"
          ? `/tasks/${workItem.projectId}/${workItem.userStoryId}/${workItem.id}`
          : `/bugs/${workItem.projectId}/${workItem.userStoryId}/${workItem.id}`;
      const response = await axiosInstance.get(
        `${TASK_SERVICE_URL}/api/project${fetchEndpoint}`,
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );
      onWorkItemUpdate?.({ ...response.data, type: workItem.type });
      setShowAddTimeForm(false);
      toast.success("Temps ajouté avec succès !");
    } catch (err: any) {
      let errorMessage = "Échec de l'ajout du temps";
      if (err.response?.status === 400) {
        errorMessage = "La durée doit être positive";
      } else if (err.response?.status === 404) {
        errorMessage =
          workItem.type === "TASK" ? "Tâche non trouvée" : "Bug non trouvé";
      } else if (err.response?.data) {
        errorMessage =
          typeof err.response.data === "string"
            ? err.response.data
            : err.response.data.error ||
              err.response.data.message ||
              errorMessage;
      }
      setUploadError(errorMessage);
      console.error("Erreur lors de l'ajout du temps :", err);
      toast.error(errorMessage);
    } finally {
      setIsAddingTime(false);
    }
  };

  // Rendu de la fenêtre contextuelle pour l'historique
  const renderHistoryPopup = () => (
    <div className="modal-overlay-history">
      <div
        className="modal-content-history"
        ref={popupRef}
        role="dialog"
        aria-labelledby="history-title"
      >
        <h5 id="history-title">
          History of {workItem.type === "TASK" ? "Task" : "le bug"}
        </h5>
        <button
          className="close-history-btn"
          onClick={() => setShowHistoryPopup(false)}
          aria-label="Fermer la fenêtre de l'historique"
        >
          ✕
        </button>
        {isLoadingHistory ? (
          <p className="loading-message">Chargement de l historique...</p>
        ) : history.length === 0 ? (
          <p className="no-history-message">Aucun historique disponible.</p>
        ) : (
          <div className="history-timeline">
            {history.map((entry) => (
              <div
                key={entry.id}
                className={`history-event ${getActionClass(entry.action)}`}
              >
                <div className="event-date">
                  {new Date(entry.timestamp).toLocaleString("fr-FR", {
                    year: "numeric",
                    month: "short",
                    day: "numeric",
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </div>
                <div className="event-details-container">
                  <div className="event-details">
                    <div className="top-row">
                      <div className="action">
                        <i className="fa fa-tag"></i>
                        <span className={getActionClass(entry.action)}>
                          {getActionLabel(entry.action)}
                        </span>
                      </div>
                      <div className="author">
                        <i className="fa fa-user"></i>
                        <span>{entry.authorName}</span>
                      </div>
                    </div>
                    <div className="description-row">
                      <div className="description">
                        <i className="fa fa-info-circle"></i>
                        <span>{entry.description}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
        {historyError && (
          <div className="error-container">
            <span className="error-message">{historyError}</span>
          </div>
        )}
      </div>
    </div>
  );

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
    if (!workItem.id || !accessToken) return;

    const fetchComments = async () => {
      try {
        const endpoint =
          workItem.type === "TASK"
            ? `/task/comments/getComment/${workItem.id}`
            : `/bug/comments/getComment/${workItem.id}`;
        const response = await axiosInstance.get(
          `${TASK_SERVICE_URL}/api/project${endpoint}`,
          { headers: { Authorization: `Bearer ${accessToken}` } }
        );
        setComments(response.data);
      } catch (err) {
        console.error("Error fetching comments:", err);
        toast.error("Failed to load comments");
      }
    };

    fetchComments();
  }, [workItem.id, workItem.type, accessToken, axiosInstance, setComments]);

  const onSubmitComment = async (data: { content: string }) => {
    if (!workItem.id || !accessToken) return;

    const sanitizedContent = sanitizeHtml(data.content, {
      allowedTags: ["p", "b", "i", "u", "ul", "ol", "li", "a", "span"],
      allowedAttributes: { a: ["href"] },
    });

    try {
      const endpoint =
        workItem.type === "TASK"
          ? `/task/comments/createComment`
          : `/bug/comments/createComment`;
      await axiosInstance.post(
        `${TASK_SERVICE_URL}/api/project${endpoint}`,
        { content: sanitizedContent, workItem: { id: workItem.id } },
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );
      reset();
      toast.success("Comment added!");
      const fetchEndpoint =
        workItem.type === "TASK"
          ? `/task/comments/getComment/${workItem.id}`
          : `/bug/comments/getComment/${workItem.id}`;
      const response = await axiosInstance.get(
        `${TASK_SERVICE_URL}/api/project${fetchEndpoint}`,
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
    if (!workItem.id || isTogglingStatus) return;

    setIsTogglingStatus(true);
    setUploadError(null);

    const newStatus = workItem.status === "DONE" ? "TO_DO" : "DONE";

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
      tags: workItem.tags || [],
      assignedUsers: (workItem.assignedUsers || []).map((user) => ({
        id: user.id,
        firstName: user.firstName,
        lastName: user.lastName,
        avatar: user.avatar,
      })),
      attachments: (workItem.attachments || []).map((attachment) => ({
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
      const endpoint =
        workItem.type === "TASK"
          ? `/tasks/${workItem.id}/updateTask`
          : `/bugs/${workItem.id}/updateBug`;
      await axiosInstance.put(
        `${TASK_SERVICE_URL}/api/project${endpoint}`,
        workItemDTO,
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );

      const fetchEndpoint =
        workItem.type === "TASK"
          ? `/tasks/${workItem.projectId}/${workItem.userStoryId}/${workItem.id}`
          : `/bugs/${workItem.projectId}/${workItem.userStoryId}/${workItem.id}`;
      const response = await axiosInstance.get(
        `${TASK_SERVICE_URL}/api/project${fetchEndpoint}`,
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );
      onWorkItemUpdate?.({ ...response.data, type: workItem.type });
    } catch (err: any) {
      let errorMessage = "Failed to toggle work item status";
      if (err.response?.data) {
        errorMessage =
          typeof err.response.data === "string"
            ? err.response.data
            : err.response.data.error ||
              err.response.data.message ||
              errorMessage;
      }
      setUploadError(errorMessage);
      console.error("Error toggling work item status:", {
        status: err.response?.status,
        data: err.response?.data,
        message: err.message,
      });
    } finally {
      setIsTogglingStatus(false);
    }
  };

  const handleDragStart = (e: React.DragEvent<HTMLDivElement>) => {
    if (workItem.id) {
      e.dataTransfer.setData("workItemId", workItem.id.toString());
      e.dataTransfer.setData("workItemType", workItem.type);
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

  const handleDeleteWorkItem = async () => {
    if (!workItem.id || isDeletingWorkItem) return;

    if (
      !confirm(
        `Are you sure you want to delete this ${workItem.type.toLowerCase()}?`
      )
    )
      return;

    setIsDeletingWorkItem(true);
    setUploadError(null);

    try {
      const endpoint =
        workItem.type === "TASK"
          ? `/tasks/${workItem.id}/deleteTask`
          : `/bugs/${workItem.id}/deleteBug`;
      await axiosInstance.delete(`${TASK_SERVICE_URL}/api/project${endpoint}`, {
        headers: { Authorization: `Bearer ${accessToken}` },
      });
      onWorkItemUpdate?.({ id: workItem.id, deleted: true });
    } catch (err: any) {
      let errorMessage = `Failed to delete ${workItem.type.toLowerCase()}`;
      if (err.response?.data) {
        errorMessage =
          typeof err.response.data === "string"
            ? err.response.data
            : err.response.data.error ||
              err.response.data.message ||
              errorMessage;
      }
      setUploadError(errorMessage);
      console.error(`Error deleting ${workItem.type.toLowerCase()}:`, {
        status: err.response?.status,
        data: err.response?.data,
        message: err.message,
      });
    } finally {
      setIsDeletingWorkItem(false);
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
        workItem.attachments.find((att) => att.fileUrl === imageUrl)
          ?.fileName || defaultFileName;
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
    if (!workItem.userStoryId || !workItem.id) {
      console.error("Missing userStory or work item ID");
      return;
    }
    const modalType = workItem.type === "TASK" ? "AddTaskModal" : "AddBugModal";
    router.push(
      `/user/dashboard/tasks/${modalType}/${workItem.projectId}/${workItem.userStoryId}/${workItem.id}`
    );
  };

  const toggleUsersPopup = () => {
    setShowUsersPopup((prev) => !prev);
  };

  const handleFileUpload = async (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    const file = event.target.files?.[0];
    if (!file || !workItem.id) return;

    setUploading(true);
    setUploadError(null);

    const formData = new FormData();
    formData.append("file", file);

    try {
      const endpoint =
        workItem.type === "TASK"
          ? `/tasks/${workItem.id}/attachments`
          : `/bugs/${workItem.id}/attachments`;
      const response = await axiosInstance.post(
        `${TASK_SERVICE_URL}/api/project${endpoint}`,
        formData,
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
            "Content-Type": "multipart/form-data",
          },
        }
      );
      onWorkItemUpdate?.({ ...response.data, type: workItem.type });
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
    if (!workItem.id || !publicId || deleting) return;

    if (!confirm("Are you sure you want to delete this file?")) return;

    setUploading(true);
    setUploadError(null);
    setDeleting(publicId);

    try {
      const endpoint =
        workItem.type === "TASK"
          ? `/tasks/delete?publicId=${encodeURIComponent(publicId)}`
          : `/bugs/delete?publicId=${encodeURIComponent(publicId)}`;
      await axiosInstance.delete(`${TASK_SERVICE_URL}/api/project${endpoint}`, {
        headers: { Authorization: `Bearer ${accessToken}` },
      });

      const updatedWorkItem = {
        ...workItem,
        attachments: workItem.attachments.filter(
          (attachment) => attachment.publicId !== publicId
        ),
      };
      onWorkItemUpdate?.(updatedWorkItem);
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

  // Rendu pour le mode "row"
  if (displayMode === "row") {
    return (
      <div className="work-item-row">
        <div className="work-item-checkbox">
          <input
            type="checkbox"
            checked={workItem.status === "DONE"}
            onChange={handleToggleStatus}
            disabled={isTogglingStatus}
          />
          <span>
            {workItem.title}{" "}
            {workItem.type === "BUG" && <span className="bug-badge">Bug</span>}
          </span>
        </div>
        <div className="work-item-responsible">
          {workItem.assignedUsers.length > 0 ? (
            <div className="responsible-avatars">
              {workItem.assignedUsers.slice(0, 3).map((user, index) => (
                <span
                  key={user.id}
                  className="responsible-circle"
                  style={{ zIndex: workItem.assignedUsers.length - index }}
                  title={`${user.firstName} ${user.lastName}`}
                >
                  {user.firstName[0]}
                  {user.lastName[0]}
                </span>
              ))}
              {workItem.assignedUsers.length > 3 && (
                <span className="responsible-circle more">
                  +{workItem.assignedUsers.length - 3}
                </span>
              )}
            </div>
          ) : (
            <span className="responsible-circle empty">?</span>
          )}
        </div>
        <div className="work-item-due-date">
          {workItem.dueDate
            ? new Date(workItem.dueDate).toLocaleDateString("fr-FR", {
                day: "numeric",
                month: "short",
              })
            : "None"}
        </div>
        <div className="work-item-priority-list">
          {workItem.type === "TASK" ? (
            <span
              className="priority-tag"
              style={{ backgroundColor: priorityColors[workItem.priority] }}
            >
              {priorityLabels[workItem.priority]}
            </span>
          ) : (
            <span
              className="severity-tag"
              style={{
                backgroundColor: severityColors[workItem.severity || ""],
              }}
            >
              {severityLabels[workItem.severity || ""]}
            </span>
          )}
        </div>
        <div className="work-item-tags">
          {workItem.tags && workItem.tags.length > 0
            ? workItem.tags.map((tag, index) => (
                <span
                  key={index}
                  className="tag-pill"
                  style={{ backgroundColor: "#e0e0e0" }}
                >
                  {tag}
                </span>
              ))
            : "None"}
        </div>
        <div className="work-item-files">
          {workItem.attachments && workItem.attachments.length > 0 ? (
            <div className="file-list">
              {workItem.attachments.map((attachment, index) => {
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
          <label
            htmlFor={`file-upload-${workItem.id}`}
            className="file-upload-btn"
          >
            <i className="fa fa-plus"></i> Add File
          </label>
          <input
            id={`file-upload-${workItem.id}`}
            type="file"
            ref={fileInputRef}
            onChange={handleFileUpload}
            style={{ display: "none" }}
            disabled={uploading}
          />
          {uploading && <span className="status-message">Uploading...</span>}
          {uploadError && <span className="error-message">{uploadError}</span>}
        </div>
        <div className="work-item-comments" onClick={toggleComments}>
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
              onSubmit={handleSubmitComment(onSubmitComment)}
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

  // Rendu pour le mode "card"
  return (
    <div
      className="work-item-card"
      key={workItem.id}
      draggable
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
    >
      <div className="work-item-card-header">
        <h4>
          {workItem.title}{" "}
          {workItem.type === "BUG" && <span className="bug-badge">Bug</span>}
        </h4>
        <div className="work-item-menu">
          <button
            className="menu-btn"
            onClick={() => setShowMenu((prev) => !prev)}
          >
            ...
          </button>
          {showMenu && (
            <div className="work-item-menu-dropdown" ref={menuRef}>
              <button className="menu-item" onClick={handleEdit}>
                <i className="fa fa-edit"></i>
                <span>Edit</span>
              </button>
              <button
                className="menu-item"
                onClick={handleDeleteWorkItem}
                disabled={isDeletingWorkItem}
              >
                <i className="fa fa-trash"></i>
                <span>Delete</span>
              </button>
              <button
                className="menu-item"
                onClick={() => setShowAddDependencyPopup(true)}
              >
                <i className="fa fa-link"></i>
                <span>Add Dependency</span>
              </button>
              <button
                className="menu-item"
                onClick={() => setShowHistoryPopup(true)}
              >
                <i className="fa fa-history"></i>
                <span>Historique</span>
              </button>
            </div>
          )}
          {showHistoryPopup && renderHistoryPopup()}
        </div>
      </div>

      {workItem.description && (
        <p className="work-item-description">{workItem.description}</p>
      )}

      <div className="progress-container">
        <p className="progress-text">Progression : {workItem.progress ?? 0}%</p>
        <div className="progress-bar-kanban">
          <div
            className="progress-kanban"
            style={{ width: `${workItem.progress}%` }}
          ></div>
          <div
            className="progress-glow"
            style={{ width: `${workItem.progress}%` }}
          ></div>
          <div className="sparkle"></div>
        </div>
      </div>

      <div className="priority-tags-row">
        {workItem.type === "TASK"
          ? workItem.priority && (
              <span
                className="priority-tag"
                style={{ backgroundColor: priorityColors[workItem.priority] }}
              >
                {workItem.priority}
              </span>
            )
          : workItem.severity && (
              <span
                className="severity-tag"
                style={{ backgroundColor: severityColors[workItem.severity] }}
              >
                {workItem.severity}
              </span>
            )}
        {workItem.tags?.length > 0 && (
          <div className="tags-container">
            {workItem.tags.map((tag, index) => (
              <span key={index} className="work-item-tag">
                {tag}
              </span>
            ))}
          </div>
        )}
      </div>

      <div className="attachment-section">
        <div className="attachment-container">
          <div className="attachments-list">
            {workItem.attachments.length > 0 &&
              workItem.attachments.map((attachment, index) => {
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
            <label
              htmlFor={`file-upload-${workItem.id}`}
              className="btn-attach"
            >
              Attach File
              <i className="fa fa-paperclip"></i>
            </label>
            <input
              id={`file-upload-${workItem.id}`}
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

      <div className="work-item-footer">
        {workItem.assignedUsers?.length > 0 && (
          <div className="assigned-users-container">
            <div className="avatars-stack" onClick={toggleUsersPopup}>
              {workItem.assignedUsers.slice(0, 3).map((user, index) => (
                <div
                  key={user.id}
                  className="avatar-wrapper"
                  style={{ zIndex: workItem.assignedUsers.length - index }}
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
              {workItem.assignedUsers.length > 3 && (
                <div className="avatar-more">
                  +{workItem.assignedUsers.length - 3}
                </div>
              )}
            </div>
            {showUsersPopup && (
              <div className="users-card-popup" ref={popupRef}>
                {workItem.assignedUsers.map((user) => (
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
              workItem.status === "DONE" ? "done" : "to-do"
            }`}
            onClick={handleToggleStatus}
            disabled={isTogglingStatus}
            title={
              workItem.status === "DONE" ? "Mark as To Do" : "Mark as Done"
            }
            data-status={workItem.status}
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
                  onSubmit={handleSubmitComment(onSubmitComment)}
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

          <div className="dependencies-container">
            <button
              className="dependencies-btn"
              onClick={() => setShowDependenciesPopup(true)}
              title="View dependencies"
            >
              <i className="fa fa-link calendar-style"></i>
            </button>
            {showDependenciesPopup && (
              <div className="dependencies-popup" ref={popupRef}>
                <div className="popup-content">
                  <div className="dependencies-header">
                    <h5>Work Item Dependencies</h5>
                    <button
                      className="close-dependencies-btn"
                      onClick={() => setShowDependenciesPopup(false)}
                    >
                      ✕
                    </button>
                  </div>
                  {workItem.dependencies.length === 0 ? (
                    <p className="no-dependencies">
                      No dependencies found for this work item.
                    </p>
                  ) : (
                    <div className="dependencies-table">
                      <table>
                        <thead>
                          <tr>
                            <th>Title</th>
                            <th>Status</th>
                            <th>Blocking</th>
                            <th>Actions</th>
                          </tr>
                        </thead>
                        <tbody>
                          {workItem.dependencies.map((dep) => (
                            <tr key={dep.id}>
                              <td>{dep.title}</td>
                              <td>
                                <span className="status-badge">
                                  {dep.status}
                                </span>
                              </td>
                              <td>
                                {dep.status !== "DONE" ? (
                                  <span className="blocked">Blocked</span>
                                ) : (
                                  <span className="not-blocked">
                                    Not Blocked
                                  </span>
                                )}
                              </td>
                              <td>
                                <button
                                  className="remove-dependency-btn"
                                  onClick={() => handleRemoveDependency(dep.id)}
                                  disabled={isRemovingDependency === dep.id}
                                  title="Remove dependency"
                                >
                                  <i className="fa fa-trash"></i>
                                </button>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                  {dependencyError && (
                    <span className="error-message">{dependencyError}</span>
                  )}
                </div>
              </div>
            )}
          </div>

          {(workItem.creationDate || workItem.dueDate) && (
            <div className="date-container">
              <button
                className="calendar-btn"
                onClick={() => setShowLocalDates((prev) => !prev)}
              >
                <i className="fa fa-calendar-alt calendar-style"></i>
              </button>
              {showLocalDates && (
                <div className="date-popup">
                  {workItem.creationDate && (
                    <p className="date-item created">
                      <span>Created:</span> {workItem.creationDate}
                    </p>
                  )}
                  {workItem.dueDate && (
                    <p className="date-item due">
                      <span>Due:</span> {workItem.dueDate}
                    </p>
                  )}
                </div>
              )}
            </div>
          )}

          {(workItem.estimationTime !== null ||
            displayedTimeSpent !== null) && (
            <div className="estimation-time-wrapper">
              <button
                className="estimation-time-btn"
                onClick={toggleEstimationPopup}
                title="View time details"
              >
                <i className="fa fa-clock calendar-style"></i>
              </button>
              {isEstimationPopupOpen && (
                <div className="estimation-popup-wrapper">
                  <div className="estimation-popup">
                    {displayedTimeSpent !== null && (
                      <p className="time-item spent">
                        <span>Temps passé :</span>{" "}
                        {(workItem.totalTimeSpent / 60).toFixed(1)} heures
                      </p>
                    )}
                    {workItem.estimationTime !== null && (
                      <p className="time-item estimated">
                        <span>Temps estimé :</span>{" "}
                        {(workItem.estimationTime / 60).toFixed(1)} heures
                      </p>
                    )}
                    <button
                      className="add-time-btn"
                      onClick={() => setShowAddTimeForm((prev) => !prev)}
                    >
                      Ajouter du temps
                    </button>
                    {showAddTimeForm && (
                      <form
                        onSubmit={handleSubmitTime(onSubmitAddTime)}
                        className="add-time-form"
                      >
                        <label>
                          Durée (heures) :
                          <input
                            type="number"
                            step="0.1"
                            min="0.1"
                            {...register("duration", {
                              required: "La durée est requise",
                              min: {
                                value: 0.1,
                                message:
                                  "La durée doit être d'au moins 0.1 heure",
                              },
                            })}
                          />
                          {errors.duration && (
                            <span className="error-message">
                              {errors.duration.message}
                            </span>
                          )}
                        </label>
                        <label>
                          Type :
                          <select
                            {...register("type", {
                              required: "Le type est requis",
                            })}
                          >
                            <option value="travail">Travail</option>
                            <option value="réunion">Réunion</option>
                            <option value="autre">Autre</option>
                          </select>
                          {errors.type && (
                            <span className="error-message">
                              {errors.type.message}
                            </span>
                          )}
                        </label>
                        <button type="submit" disabled={isAddingTime}>
                          {isAddingTime ? "Ajout..." : "Ajouter"}
                        </button>
                      </form>
                    )}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {showAddDependencyPopup && (
        <div className="add-dependency-popup" ref={popupRef}>
          <div className="add-dependency-header">
            <h5>Add Dependency</h5>
            <button
              className="close-add-dependency-btn"
              onClick={() => setShowAddDependencyPopup(false)}
            >
              ✕
            </button>
          </div>
          {isLoadingDependencies ? (
            <p className="loading-message">Loading potential dependencies...</p>
          ) : potentialDependencies.length === 0 ? (
            <p className="no-dependencies-message">
              No work items available to add as dependencies.
            </p>
          ) : (
            <div className="potential-dependencies-list">
              {potentialDependencies.map((dep) => (
                <div key={dep.id} className="potential-dependency-item">
                  <span className="dependency-title">
                    {dep.title} ({dep.status}) {dep.type === "BUG" && "(Bug)"}
                  </span>
                  <button
                    className="add-dependency-btn"
                    onClick={() => handleAddDependency(dep.id!)}
                    disabled={!dep.id}
                  >
                    Add
                  </button>
                </div>
              ))}
            </div>
          )}
          {dependencyError && (
            <span className="error-message">{dependencyError}</span>
          )}
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
};

export default WorkItemCard;
