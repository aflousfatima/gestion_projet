import { useState, useEffect, useRef } from "react";
import { useAuth } from "../context/AuthContext";
import useAxios from "./useAxios";
import { TASK_SERVICE_URL } from "../config/useApi";

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
  status: "TO_DO" | "IN_PROGRESS" | "DONE" | "BLOCKED" | "ARCHIVED" | "CANCELLED" | "";
  priority: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL" | "";
  userStory: number | null;
  createdBy: string | null;
  projectId: number;
  tags: string[];
  assignedUsers: User[];
  attachments: FileAttachment[]; // Default to empty array
}

interface UseTaskProps {
  projectId?: string; // Optional for fetching tasks by project
  taskId?: number; // Optional for fetching a single task
}

interface UseTaskResult {
  tasks: Task[];
  task: Task | null;
  loading: boolean;
  error: string | null;
  uploading: { [key: number]: boolean };
  uploadError: { [key: number]: string | null };
  showUsersPopup: { [key: number]: boolean };
  fetchTasks: () => Promise<void>;
  fetchTask: (taskId: number) => Promise<void>;
  uploadFile: (taskId: number, event: React.ChangeEvent<HTMLInputElement>) => Promise<void>;
  toggleUsersPopup: (taskId: number) => void;
  setShowUsersPopup: React.Dispatch<React.SetStateAction<{ [key: number]: boolean }>>;
  popupRefs: React.MutableRefObject<{ [key: number]: HTMLDivElement | null }>;
  fileInputRefs: React.MutableRefObject<{ [key: number]: HTMLInputElement | null }>;
}

export const useTask = ({ projectId, taskId }: UseTaskProps = {}): UseTaskResult => {
  const { accessToken, isLoading: authLoading } = useAuth();
  const axiosInstance = useAxios();
  const [tasks, setTasks] = useState<Task[]>([]);
  const [task, setTask] = useState<Task | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [uploading, setUploading] = useState<{ [key: number]: boolean }>({});
  const [uploadError, setUploadError] = useState<{ [key: number]: string | null }>({});
  const [showUsersPopup, setShowUsersPopup] = useState<{ [key: number]: boolean }>({});
  const popupRefs = useRef<{ [key: number]: HTMLDivElement | null }>({});
  const fileInputRefs = useRef<{ [key: number]: HTMLInputElement | null }>({});

  // Handle click outside to close user popup
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      Object.keys(popupRefs.current).forEach((id) => {
        const ref = popupRefs.current[parseInt(id)];
        if (ref && !ref.contains(event.target as Node)) {
          setShowUsersPopup((prev) => ({ ...prev, [id]: false }));
        }
      });
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Fetch all tasks for a project
  const fetchTasks = async () => {
    if (!projectId || authLoading || !accessToken) return;

    setLoading(true);
    setError(null);
    try {
      const response = await axiosInstance.get(
        `${TASK_SERVICE_URL}/api/project/tasks/active_sprint/${projectId}`,
        {
          headers: { Authorization: `Bearer ${accessToken}` },
        }
      );
      const data = Array.isArray(response.data)
        ? response.data.map((task: Task) => ({
            ...task,
            attachments: task.attachments || [],
          }))
        : [];
      setTasks(data);
    } catch (err) {
      const errorMessage = err.response?.data || "Failed to fetch tasks";
      setError(errorMessage);
      console.error("Error fetching tasks:", errorMessage);
      setTasks([]);
    } finally {
      setLoading(false);
    }
  };

  // Fetch a single task
  const fetchTask = async (id: number) => {
    if (authLoading || !accessToken) return;

    setLoading(true);
    setError(null);
    try {
      const response = await axiosInstance.get(`${TASK_SERVICE_URL}/api/tasks/${id}`, {
        headers: { Authorization: `Bearer ${accessToken}` },
      });
      const data = { ...response.data, attachments: response.data.attachments || [] };
      setTask(data);
    } catch (err) {
      const errorMessage = err.response?.data || "Failed to fetch task";
      setError(errorMessage);
      console.error("Error fetching task:", errorMessage);
      setTask(null);
    } finally {
      setLoading(false);
    }
  };

  // Upload file to a task
  const uploadFile = async (taskId: number, event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file || !taskId) {
      console.error("No file selected or task ID is undefined:", { file, taskId });
      return;
    }

    setUploading((prev) => ({ ...prev, [taskId]: true }));
    setUploadError((prev) => ({ ...prev, [taskId]: null }));

    const formData = new FormData();
    formData.append("file", file);

    try {
      const response = await axiosInstance.post(
        `${TASK_SERVICE_URL}/api/tasks/${taskId}/attachments`,
        formData,
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
            "Content-Type": "multipart/form-data",
          },
        }
      );
      // Update tasks or task state
      if (projectId) {
        setTasks((prev) =>
          prev.map((t) => (t.id === taskId ? { ...t, ...response.data } : t))
        );
      } else {
        setTask((prev) => (prev ? { ...prev, ...response.data } : response.data));
      }
    } catch (err) {
      let errorMessage = "Failed to upload file";
      if (err.response?.data) {
        errorMessage =
          typeof err.response.data === "string"
            ? err.response.data
            : err.response.data.error || err.response.data.message || errorMessage;
      }
      setUploadError((prev) => ({ ...prev, [taskId]: errorMessage }));
      console.error("Error uploading file:", err.response?.data || err);
    } finally {
      setUploading((prev) => ({ ...prev, [taskId]: false }));
      if (fileInputRefs.current[taskId]) {
        fileInputRefs.current[taskId]!.value = "";
      }
    }
  };

  // Toggle users popup
  const toggleUsersPopup = (taskId: number) => {
    setShowUsersPopup((prev) => ({ ...prev, [taskId]: !prev[taskId] }));
  };

  // Auto-fetch tasks or task if IDs are provided
  useEffect(() => {
    if (projectId) {
      fetchTasks();
    } else if (taskId) {
      fetchTask(taskId);
    }
  }, [projectId, taskId, accessToken, authLoading]);

  return {
    tasks,
    task,
    loading,
    error,
    uploading,
    uploadError,
    showUsersPopup,
    fetchTasks,
    fetchTask,
    uploadFile,
    toggleUsersPopup,
    setShowUsersPopup,
    popupRefs,
    fileInputRefs,
  };
};