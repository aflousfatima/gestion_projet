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

interface Commit {
  sha: string;
  commit: {
    message: string;
    author: { name: string; date: string };
  };
  html_url: string;
}

interface Branch {
  name: string;
  commit: { sha: string };
  protected: boolean;
}

const ProjectManagement: React.FC = () => {
  const { accessToken, isLoading } = useAuth();
  const axiosInstance = useAxios();
  const params = useParams();
  const projectId = params.projectId as string;

  const [project, setProject] = useState<Project | null>(null);
  const [repositoryUrl, setRepositoryUrl] = useState<string>(""); // Fixed syntax
  const [linkedRepository, setLinkedRepository] = useState<string>("");
  const [commits, setCommits] = useState<Commit[]>([]);
  const [branches, setBranches] = useState<Branch[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);

  // Extract owner and repo from repository URL
  const extractOwnerAndRepo = (url: string): [string, string] | null => {
    const regex = /^https:\/\/github\.com\/([a-zA-Z0-9-]+)\/([a-zA-Z0-9-_]+)$/;
    const match = url.match(regex);
    if (match) {
      return [match[1], match[2]];
    }
    return null;
  };

  // Load project details, linked repository, commits, and branches
  useEffect(() => {
    const fetchProjectDetails = async () => {
      if (!accessToken || isLoading || !projectId) return;
      try {
        setLoading(true);
        setError(null);

        // Fetch project details
        const projectResponse = await axiosInstance.get(
          `${PROJECT_SERVICE_URL}/api/projects/${projectId}`,
          { headers: { Authorization: `Bearer ${accessToken}` } }
        );
        setProject({
          id: Number(projectId),
          name: projectResponse.data.name || "Projet Inconnu",
        });

        // Fetch linked repository URL
        const githubResponse = await axiosInstance.get(
          `http://localhost:8085/api/${projectId}/github-link`,
          { headers: { Authorization: `Bearer ${accessToken}` } }
        );
        const repoUrl = githubResponse.data.repositoryUrl || "";
        setLinkedRepository(repoUrl);

        // Fetch commits and branches if repository is linked
        if (repoUrl) {
          const repoDetails = extractOwnerAndRepo(repoUrl);
          if (repoDetails) {
            const [owner, repo] = repoDetails;

            // Fetch commits
            const commitsResponse = await axiosInstance.get(
              `http://localhost:8087/fetch_data/repos/${owner}/${repo}/commits`,
              { headers: { Authorization: `Bearer ${accessToken}` } }
            );
            setCommits(commitsResponse.data || []);

            // Fetch branches
            const branchesResponse = await axiosInstance.get(
              `http://localhost:8087/fetch_data/repos/${owner}/${repo}/branches`,
              { headers: { Authorization: `Bearer ${accessToken}` } }
            );
            setBranches(branchesResponse.data || []);
          }
        }
      } catch (err: any) {
        console.error("Erreur lors du chargement:", err);
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
      const response = await axiosInstance.post(
        `http://localhost:8085/api/${projectId}/github-link`,
        { repositoryUrl },
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );
      setSuccessMessage(response.data.message || "Dépôt lié avec succès !");
      setLinkedRepository(repositoryUrl);
      setRepositoryUrl("");

      // Fetch commits and branches after linking
      const repoDetails = extractOwnerAndRepo(repositoryUrl);
      if (repoDetails) {
        const [owner, repo] = repoDetails;
        const commitsResponse = await axiosInstance.get(
          `http://localhost:8087/fetch_data/repos/${owner}/${repo}/commits`,
          { headers: { Authorization: `Bearer ${accessToken}` } }
        );
        setCommits(commitsResponse.data || []);
        const branchesResponse = await axiosInstance.get(
          `http://localhost:8087/fetch_data/repos/${owner}/${repo}/branches`,
          { headers: { Authorization: `Bearer ${accessToken}` } }
        );
        setBranches(branchesResponse.data || []);
      }
    } catch (err: any) {
      console.error("Erreur lors de la liaison du dépôt:", err);
      setError(
        err.response?.data?.error ||
          "Erreur lors de la liaison du dépôt GitHub."
      );
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
      <div className="project-card-manag" data-tilt>
        {error && <div className="alert alert-error">{error}</div>}
        {successMessage && (
          <div className="alert alert-success">{successMessage}</div>
        )}

        <div className="github-section">
          <h2 className="section-title">Link a Github Repo</h2>
          {linkedRepository ? (
            <div className="linked-repo">
              <span className="linked-icon">✔</span> Lié :{" "}
              <a href={linkedRepository} target="_blank" rel="noopener noreferrer">
                {linkedRepository}
              </a>
            </div>
          ) : (
            <p className="section-description">
              Entrez l URL du dépôt GitHub à lier à ce projet (ex. :
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
              "Update the Repo"
            ) : (
              "Lier le dépôt"
            )}
          </button>
        </div>

        {linkedRepository && (
          <>
            <div className="commits-section">
              <h2 className="section-title">Commits</h2>
              {commits.length > 0 ? (
                <ul className="commit-list">
                  {commits.map((commit) => (
                    <li key={commit.sha} className="commit-item">
                      <a
                        href={commit.html_url}
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        {commit.commit.message}
                      </a>{" "}
                      by {commit.commit.author.name} on{" "}
                      {new Date(
                        commit.commit.author.date
                      ).toLocaleDateString()}
                    </li>
                  ))}
                </ul>
              ) : (
                <p>No commits found.</p>
              )}
            </div>

            <div className="branches-section">
              <h2 className="section-title">Branches</h2>
              {branches.length > 0 ? (
                <ul className="branch-list">
                  {branches.map((branch) => (
                    <li key={branch.name} className="branch-item">
                      {branch.name} {branch.protected && "(Protected)"}
                    </li>
                  ))}
                </ul>
              ) : (
                <p>No branches found.</p>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default ProjectManagement;