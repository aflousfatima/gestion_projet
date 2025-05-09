import { useState, useEffect } from "react";
import useAxios from "./useAxios";
import { useAuth } from "../context/AuthContext";
import { AUTH_SERVICE_URL, PROJECT_SERVICE_URL } from "../config/useApi";

export interface Project {
  id?: number;
  name: string;
  description: string;
  companyId?: string;
  managerId?: string;
  creationDate?: string;
  startDate: string;
  deadline: string;
  status: "START" | "IN_PROGRESS" | "IN_PAUSE" | "DONE" | "CANCEL" | "ARCHIVE" | "";
  phase:
    | "PLANIFICATION"
    | "DESIGN"
    | "DEVELOPPEMENT"
    | "TEST"
    | "DEPLOY"
    | "MAINTENANCE"
    | "CLOSE"
    | "";
  priority: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL" | "";
}

interface UseProjectsResult {
  projects: Project[];
  setProjects: React.Dispatch<React.SetStateAction<Project[]>>;
  loading: boolean;
  error: string | null;
}

export const useProjects = (): UseProjectsResult => {
  const { accessToken, isLoading: authLoading } = useAuth();
  const axiosInstance = useAxios();
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchProjects = async () => {
      if (authLoading) return;
      if (!accessToken) {
        setLoading(false);
        setError("Aucun token d'accès disponible.");
        return;
      }

      try {
        setLoading(true);
        setError(null);

        const authIdResponse = await axiosInstance.get(
          `${AUTH_SERVICE_URL}/api/user-id`
        );
        const authId = authIdResponse.data;

        // Appeler le nouvel endpoint pour récupérer les projets de l'utilisateur
        const projectsResponse = await axiosInstance.get(
          `${PROJECT_SERVICE_URL}/api/projects/by-user?authId=${authId}`
        );
        const { projects } = projectsResponse.data;
        console.log("Projets renvoyés par l'API :", projects);
        setProjects(projects);
      } catch (err) {
        console.error("Erreur lors de la récupération des projets:", err);
        setError("Impossible de charger les projets.");
      } finally {
        setLoading(false);
      }
    };

    fetchProjects();
  }, [accessToken, authLoading, axiosInstance]);

  return { projects, setProjects, loading, error };
};