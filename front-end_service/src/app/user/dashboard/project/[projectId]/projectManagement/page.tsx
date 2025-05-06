"use client";
import React, { useEffect, useState } from "react";
import { useAuth } from "../../../../../../context/AuthContext";
import useAxios from "../../../../../../hooks/useAxios";
import "../../../../../../styles/ProjectManagement.css";
import { useParams } from "next/navigation";
import { PROJECT_SERVICE_URL } from "../../../../../../config/useApi";

interface Project {
  id: number;
  name: string;
}

const ProjectManagement: React.FC = () => {
  const { accessToken, isLoading } = useAuth();
  const axiosInstance = useAxios();
  const params = useParams();
  const projectId = params.projectId as string;

  const [project, setProject] = useState<Project | null>(null);
  const [repositoryUrl, setRepositoryUrl] = useState<string>("");
  const [linkedRepository, setLinkedRepository] = useState<string>("");
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);

  // Load project details and linked repository URL
  useEffect(() => {
    const fetchProjectDetails = async () => {
      if (!accessToken || isLoading || !projectId) return;
      try {
        setLoading(true);
        setError(null);

        // Fetch project details
        const projectResponse = await axiosInstance.get(
          `${PROJECT_SERVICE_URL}/api/projects/${projectId}`,
          {
            headers: { Authorization: `Bearer ${accessToken}` },
          }
        );
        console.log("✅ Réponse de /projects:", projectResponse.data);
        setProject({
          id: Number(projectId),
          name: projectResponse.data.name || "Projet Inconnu",
        });

        // Fetch linked repository URL
        const githubResponse = await axiosInstance.get(
          `http://localhost:8085/api/${projectId}/github-link`,
          {
            headers: { Authorization: `Bearer ${accessToken}` },
          }
        );
        console.log("✅ Réponse de /github-link:", githubResponse.data);
        setLinkedRepository(githubResponse.data.repositoryUrl || "");
      } catch (err: any) {
        console.error("Erreur lors du chargement des détails du projet:", err);
        setError(
          err.response?.data?.error ||
            "Erreur lors du chargement des détails du projet"
        );
        setProject({ id: Number(projectId), name: "Projet Inconnu" });
      } finally {
        setLoading(false);
      }
    };

    fetchProjectDetails();
  }, [accessToken, isLoading, axiosInstance, projectId]);

  // Handle repository linking
  const handleLinkRepository = async () => {
    setLoading(true);
    setError(null);
    setSuccessMessage(null);
    try {
      if (!repositoryUrl.trim()) {
        throw new Error("L'URL du dépôt est requise");
      }
      console.log("🔄 Liaison du dépôt GitHub:", repositoryUrl);
      const response = await axiosInstance.post(
        `http://localhost:8085/api/${projectId}/github-link`,
        { repositoryUrl },
        {
          headers: { Authorization: `Bearer ${accessToken}` },
        }
      );
      console.log("✅ Réponse de /github-link:", response.data);
      setSuccessMessage(
        response.data.message || "Dépôt lié avec succès !"
      );
      setLinkedRepository(repositoryUrl);
      setRepositoryUrl("");
    } catch (err: any) {
      console.error("Erreur lors de la liaison du dépôt:", err);
      const errorMessage =
        err.response?.data?.error ||
        "Erreur lors de la liaison du dépôt GitHub. Vérifiez que l'URL est correcte et que le dépôt est accessible.";
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  if (loading || !project) {
    return (
      <div className="loading-container">
        <div className="cyber-spinner"></div>
      </div>
    );
  }

  return (
    <div className="project-management-page">
      <img
        src="/github_integration.png"
        alt="Intégration GitHub"
        className="github-integ"
      />
      <div className="project-card-manag" data-tilt>
        {error && <div className="alert alert-error">{error}</div>}
        {successMessage && (
          <div className="alert alert-success">{successMessage}</div>
        )}

        <div className="github-section">
          <h2 className="section-title">Lier un dépôt GitHub</h2>
          {linkedRepository ? (
            <div className="linked-repo">
              <span className="linked-icon">✔</span> Lié :{" "}
              <a
                href={linkedRepository}
                target="_blank"
                rel="noopener noreferrer"
              >
                {linkedRepository}
              </a>
            </div>
          ) : (
            <p className="section-description">
              Entrez lURL du dépôt GitHub à lier à ce projet (ex. :
              https://github.com/owner/repo).
            </p>
          )}
          <input
            type="text"
            value={repositoryUrl}
            onChange={(e) => setRepositoryUrl(e.target.value)}
            placeholder="ex: https://github.com/owner/repo"
            className="repo-input"
            disabled={loading}
          />
          <button
            onClick={handleLinkRepository}
            disabled={loading || !repositoryUrl.trim()}
            className="link-button"
          >
            {loading ? (
              <span className="button-loading">Liaison en cours...</span>
            ) : linkedRepository ? (
              "Mettre à jour le dépôt"
            ) : (
              "Lier le dépôt"
            )}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ProjectManagement;