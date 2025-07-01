/* eslint-disable */
import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
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

interface TaskSummary {
  id: number;
  title: string;
  status: string;
  projectId: number;
  userStoryId: number;
}

interface WorkItem {
  id?: number;
  type: "TASK" | "BUG"; // Distingue tâche et bug
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
  severity?: "MINOR" | "MAJOR" | "CRITICAL" | "BLOCKER" | ""; // Spécifique aux bugs
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

interface UseWorkItemsReturn {
  workItems: WorkItem[];
  loading: boolean;
  error: string | null;
  handleWorkItemUpdate: (
    updatedWorkItem: Partial<WorkItem> & { deleted?: boolean }
  ) => void;
  fetchWorkItems: () => Promise<void>;
}

export const useWorkItems = (): UseWorkItemsReturn => {
  const { accessToken, isLoading: authLoading } = useAuth();
  const axiosInstance = useAxios();
  const params = useParams();
  const projectId = params.projectId as string;
  const [workItems, setWorkItems] = useState<WorkItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const fetchWorkItems = async () => {
    if (authLoading || !accessToken || !projectId) return;

    setLoading(true);
    setError(null);
    try {
      // Récupérer les tâches
      const tasksResponse = await axiosInstance.get(
        `${TASK_SERVICE_URL}/api/project/tasks/active_sprint/${projectId}`,
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );
      const tasksData = Array.isArray(tasksResponse.data)
        ? tasksResponse.data.map((task: WorkItem) => ({
            ...task,
            type: "TASK",
            attachments: task.attachments || [],
          }))
        : [];

      // Récupérer les bugs
      const bugsResponse = await axiosInstance.get(
        `${TASK_SERVICE_URL}/api/project/bugs/active_sprint/${projectId}`,
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );
      const bugsData = Array.isArray(bugsResponse.data)
  ? bugsResponse.data.map(bug => ({
      ...bug,
      type: "BUG" as "BUG", 
      attachments: bug.attachments || [],
    }))
  : [];


     // Combiner tâches et bugs
    setWorkItems([...tasksData, ...bugsData]);
  } catch (err: any) {
    // Extraire un message d'erreur approprié
    const errorMessage =
      err.response?.data?.message ||
      err.response?.data?.error ||
      err.message ||
      "Failed to fetch work items";
    setError(errorMessage);
    setWorkItems([]);
  } finally {
    setLoading(false);
  }
};

  useEffect(() => {
    fetchWorkItems();
  }, [projectId, accessToken, authLoading, axiosInstance]);

  const handleWorkItemUpdate = (
    updatedWorkItem: Partial<WorkItem> & { deleted?: boolean }
  ) => {
    setWorkItems((prevWorkItems) => {
      if (updatedWorkItem.deleted && updatedWorkItem.id) {
        return prevWorkItems.filter(
          (item) => item.id !== updatedWorkItem.id
        );
      }
      return prevWorkItems.map((item) =>
        item.id === updatedWorkItem.id
          ? { ...item, ...updatedWorkItem }
          : item
      );
    });
  };

  return {
    workItems,
    loading,
    error,
    handleWorkItemUpdate,
    fetchWorkItems,
  };
};