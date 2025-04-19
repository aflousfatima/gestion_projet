import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
import { useAuth } from "../context/AuthContext";
import useAxios from "../hooks/useAxios";
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

interface UseTasksReturn {
  tasks: Task[];
  loading: boolean;
  error: string | null;
  handleTaskUpdate: (updatedTask: Partial<Task> & { deleted?: boolean }) => void;
  fetchTasks: () => Promise<void>;
}

export const useTasks = (): UseTasksReturn => {
  const { accessToken, isLoading: authLoading } = useAuth();
  const axiosInstance = useAxios();
  const params = useParams();
  const projectId = params.projectId as string;
  const [tasks, setTasks] = useState<Task[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const fetchTasks = async () => {
    if (authLoading || !accessToken || !projectId) return;

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
      const data = Array.isArray(response.data)
        ? response.data.map((task: Task) => ({
            ...task,
            attachments: task.attachments || [],
          }))
        : [];
      setTasks(data);
    } catch (err: any) {
      const errorMessage = err.response?.data || "Failed to fetch tasks";
      setError(errorMessage);
      setTasks([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTasks();
  }, [projectId, accessToken, authLoading, axiosInstance]);

  const handleTaskUpdate = (
    updatedTask: Partial<Task> & { deleted?: boolean }
  ) => {
    setTasks((prevTasks) => {
      if (updatedTask.deleted && updatedTask.id) {
        return prevTasks.filter((task) => task.id !== updatedTask.id);
      }
      return prevTasks.map((task) =>
        task.id === updatedTask.id ? { ...task, ...updatedTask } : task
      );
    });
  };

  return { tasks, loading, error, handleTaskUpdate, fetchTasks };
};