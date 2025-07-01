"use client";
import React, { useEffect, useState } from "react";
import { useAuth } from "../../../../../../context/AuthContext";
import useAxios from "../../../../../../hooks/useAxios";
import "../../../../../../styles/ProjectManagement.css";
import { useParams } from "next/navigation";
import { PROJECT_SERVICE_URL } from "../../../../../../config/useApi";
import { AxiosError } from "axios"; 
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
  author?: { avatar_url: string };
}

interface CommitDetails {
  sha: string;
  files: Array<{
    filename: string;
    additions: number;
    deletions: number;
    patch: string;
  }>;
  stats: {
    total: number;
    additions: number;
    deletions: number;
  };
}

interface Branch {
  name: string;
  commit: { sha: string; commit: { author: { name: string }; message: string; date: string } };
  protected: boolean;
  merged: boolean;
}

interface PullRequest {
  number: number;
  title: string;
  state: string;
  html_url: string;
  user: { login: string; avatar_url: string };
  created_at: string;
  head: { ref: string };
  base: { ref: string };
}

interface PullRequestDetails {
  commits: Commit[];
  files: Array<{
    filename: string;
    additions: number;
    deletions: number;
    patch: string;
  }>;
  reviews: Array<{
    user: { login: string };
    state: string;
    submitted_at: string;
  }>;
  events: Array<{
    event: string;
    actor: { login: string };
    created_at: string;
  }>;
}

const ProjectManagement: React.FC = () => {
  const { accessToken, isLoading } = useAuth();
  const axiosInstance = useAxios();
  const params = useParams();
  const projectId = params.projectId as string;
  const [project, setProject] = useState<Project | null>(null);
  const [repositoryUrl, setRepositoryUrl] = useState<string>("");
  const [linkedRepository, setLinkedRepository] = useState<string>("");
  const [commits, setCommits] = useState<Commit[]>([]);
  const [branches, setBranches] = useState<Branch[]>([]);
  const [pullRequests, setPullRequests] = useState<PullRequest[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [commitDetails, setCommitDetails] = useState<Map<string, CommitDetails>>(new Map());
  const [prDetails, setPrDetails] = useState<Map<number, PullRequestDetails>>(new Map());
  const [commitFilter, setCommitFilter] = useState<{ author: string; branch: string; since: string; until: string }>({
    author: "",
    branch: "",
    since: "",
    until: "",
  });
  const [prState, setPrState] = useState<string>("open");
  const [isSidePanelOpen, setIsSidePanelOpen] = useState<boolean>(false);
  const [activeTab, setActiveTab] = useState<"commits" | "branches" | "pulls">("commits");

  const extractOwnerAndRepo = (url: string): [string, string] | null => {
    const regex = /^https:\/\/github\.com\/([a-zA-Z0-9-]+)\/([a-zA-Z0-9-_]+)$/;
    const match = url.match(regex);
    if (match) {
      return [match[1], match[2]];
    }
    return null;
  };

  const formatDateTime = (date: string): string => {
    return new Date(date).toLocaleString("fr-FR", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  };

  const fetchCommitDetails = async (owner: string, repo: string, sha: string) => {
    try {
      const response = await axiosInstance.get(
        `http://localhost:8087/fetch_data/repos/${owner}/${repo}/commits/${sha}`,
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );
      setCommitDetails((prev) => new Map(prev).set(sha, response.data));
    } catch (err) {
      console.error("Error fetching commit details:", err);
    }
  };

  const fetchPullRequestDetails = async (owner: string, repo: string, pullNumber: string) => {
    try {
      const [commitsResponse, filesResponse, reviewsResponse, eventsResponse] = await Promise.all([
        axiosInstance.get(
          `http://localhost:8087/fetch_data/repos/${owner}/${repo}/pulls/${pullNumber}/commits`,
          { headers: { Authorization: `Bearer ${accessToken}` } }
        ),
        axiosInstance.get(
          `http://localhost:8087/fetch_data/repos/${owner}/${repo}/pulls/${pullNumber}/files`,
          { headers: { Authorization: `Bearer ${accessToken}` } }
        ),
        axiosInstance.get(
          `http://localhost:8087/fetch_data/repos/${owner}/${repo}/pulls/${pullNumber}/reviews`,
          { headers: { Authorization: `Bearer ${accessToken}` } }
        ),
        axiosInstance.get(
          `http://localhost:8087/fetch_data/repos/${owner}/${repo}/pulls/${pullNumber}/events`,
          { headers: { Authorization: `Bearer ${accessToken}` } }
        ),
      ]);
      setPrDetails((prev) =>
        new Map(prev).set(Number(pullNumber), {
          commits: commitsResponse.data || [],
          files: filesResponse.data || [],
          reviews: reviewsResponse.data || [],
          events: eventsResponse.data || [],
        })
      );
    } catch (err) {
      console.error("Error fetching PR details:", err);
    }
  };

  useEffect(() => {
    const fetchProjectDetails = async () => {
      if (!accessToken || isLoading || !projectId) return;
      try {
        setLoading(true);
        setError(null);

        const projectResponse = await axiosInstance.get(
          `${PROJECT_SERVICE_URL}/api/projects/${projectId}`,
          { headers: { Authorization: `Bearer ${accessToken}` } }
        );
        setProject({
          id: Number(projectId),
          name: projectResponse.data.name || "Projet Inconnu",
        });

        const githubResponse = await axiosInstance.get(
          `http://localhost:8085/api/${projectId}/github-link`,
          { headers: { Authorization: `Bearer ${accessToken}` } }
        );
        const repoUrl = githubResponse.data.repositoryUrl || "";
        setLinkedRepository(repoUrl);

        if (repoUrl) {
          const repoDetails = extractOwnerAndRepo(repoUrl);
          if (repoDetails) {
            const [owner, repo] = repoDetails;

            const commitsResponse = await axiosInstance.get(
              `http://localhost:8087/fetch_data/repos/${owner}/${repo}/commits`,
              {
                headers: { Authorization: `Bearer ${accessToken}` },
                params: {
                  branch: commitFilter.branch || undefined,
                  author: commitFilter.author || undefined,
                  since: commitFilter.since || undefined,
                  until: commitFilter.until || undefined,
                },
              }
            );
            setCommits(commitsResponse.data || []);

            const branchesResponse = await axiosInstance.get(
              `http://localhost:8087/fetch_data/repos/${owner}/${repo}/branches`,
              { headers: { Authorization: `Bearer ${accessToken}` } }
            );
            const branchesData = branchesResponse.data || [];
            const enrichedBranches = await Promise.all(
              branchesData.map(async (branch: { name: string }) => {
                const branchDetailsResponse = await axiosInstance.get(
                  `http://localhost:8087/fetch_data/repos/${owner}/${repo}/branches/${branch.name}`,
                  { headers: { Authorization: `Bearer ${accessToken}` } }
                );
                return branchDetailsResponse.data;
              })
            );
            setBranches(enrichedBranches);

            const pullRequestsResponse = await axiosInstance.get(
              `http://localhost:8087/fetch_data/repos/${owner}/${repo}/pulls`,
              {
                headers: { Authorization: `Bearer ${accessToken}` },
                params: { state: prState },
              }
            );
            setPullRequests(pullRequestsResponse.data || []);
          }
        }
      } catch (err: unknown) {
        const errorMessage =
          err instanceof AxiosError
            ? err.response?.data?.error ||
              "Erreur lors du chargement des détails du projet"
            : "Erreur lors du chargement des détails du projet";
        setError(errorMessage);
        setProject({ id: Number(projectId), name: "Projet Inconnu" });
      } finally {
        setLoading(false);
      }
    };

    fetchProjectDetails();
  }, [accessToken, isLoading, axiosInstance, projectId, commitFilter, prState]);

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

      const repoDetails = extractOwnerAndRepo(repositoryUrl);
      if (repoDetails) {
        const [owner, repo] = repoDetails;
        const commitsResponse = await axiosInstance.get(
          `http://localhost:8087/fetch_data/repos/${owner}/${repo}/commits`,
          {
            headers: { Authorization: `Bearer ${accessToken}` },
            params: {
              branch: commitFilter.branch || undefined,
              author: commitFilter.author || undefined,
              since: commitFilter.since || undefined,
              until: commitFilter.until || undefined,
            },
          }
        );
        setCommits(commitsResponse.data || []);

        const branchesResponse = await axiosInstance.get(
          `http://localhost:8087/fetch_data/repos/${owner}/${repo}/branches`,
          { headers: { Authorization: `Bearer ${accessToken}` } }
        );
        const branchesData = branchesResponse.data || [];
        const enrichedBranches = await Promise.all(
          branchesData.map(async (branch: { name: string }) => {
            const branchDetailsResponse = await axiosInstance.get(
              `http://localhost:8087/fetch_data/repos/${owner}/${repo}/branches/${branch.name}`,
              { headers: { Authorization: `Bearer ${accessToken}` } }
            );
            return branchDetailsResponse.data;
          })
        );
        setBranches(enrichedBranches);

        const pullRequestsResponse = await axiosInstance.get(
          `http://localhost:8087/fetch_data/repos/${owner}/${repo}/pulls`,
          {
            headers: { Authorization: `Bearer ${accessToken}` },
            params: { state: prState },
          }
        );
        setPullRequests(pullRequestsResponse.data || []);
      }
    } catch (err: unknown) {
      const errorMessage =
        err instanceof AxiosError
          ? err.response?.data?.error ||
            "Erreur lors de la liaison du dépôt GitHub."
          : "Erreur lors de la liaison du dépôt GitHub.";
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const parseIssueReferences = (message: string): string => {
    const issueRegex = /(fixes|closes|resolves)\s+#(\d+)/gi;
    return message.replace(issueRegex, (match, verb, issueNumber) => {
      const repoDetails = extractOwnerAndRepo(linkedRepository);
      if (repoDetails) {
        const [owner, repo] = repoDetails;
        return `${verb} <a href="https://github.com/${owner}/${repo}/issues/${issueNumber}" target="_blank" rel="noopener noreferrer">#${issueNumber}</a>`;
      }
      return match;
    });
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
      <nav className="top-nav">
        <button
          className="manage-repo-button"
          onClick={() => setIsSidePanelOpen(!isSidePanelOpen)}
        >
          <i className="fas fa-cog"></i> Manage Repo
        </button>
      </nav>
      <div className={`side-panel ${isSidePanelOpen ? "open" : ""}`}>
        <div className="side-panel-content">
          <button
            className="close-panel"
            onClick={() => setIsSidePanelOpen(false)}
          >
            <i className="fas fa-times"></i>
          </button>
          {error && <div className="alert alert-error">{error}</div>}
          {successMessage && (
            <div className="alert alert-success">{successMessage}</div>
          )}
          <div className="github-section">
            <h2 className="section-title-manag">Link a Github Repo</h2>
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
                Enter the GitHub repository URL to link to this project (e.g. :
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
        </div>
      </div>
      <div className="project-content">
        {linkedRepository && (
          <>
            <div className="tabs">
              <button
                className={`tab-github ${
                  activeTab === "commits" ? "active" : ""
                }`}
                onClick={() => setActiveTab("commits")}
              >
                <i className="fas fa-code-commit"></i> Commits
              </button>
              <button
                className={`tab-github ${
                  activeTab === "branches" ? "active" : ""
                }`}
                onClick={() => setActiveTab("branches")}
              >
                <i className="fas fa-code-branch"></i> Branches
              </button>
              <button
                className={`tab-github ${
                  activeTab === "pulls" ? "active" : ""
                }`}
                onClick={() => setActiveTab("pulls")}
              >
                <i className="fas fa-code-pull-request"></i> Pull Requests
              </button>
            </div>
            {activeTab === "commits" && (
              <div className="commits-section">
                <div className="filter-section">
                  <input
                    type="text"
                    placeholder="Sort by author"
                    value={commitFilter.author}
                    onChange={(e) =>
                      setCommitFilter({
                        ...commitFilter,
                        author: e.target.value,
                      })
                    }
                    className="filter-input"
                  />
                  <select
                    value={commitFilter.branch}
                    onChange={(e) =>
                      setCommitFilter({
                        ...commitFilter,
                        branch: e.target.value,
                      })
                    }
                    className="filter-select"
                  >
                    <option value="">All the branches</option>
                    {branches.map((branch) => (
                      <option key={branch.name} value={branch.name}>
                        {branch.name}
                      </option>
                    ))}
                  </select>
                  <input
                    type="datetime-local"
                    value={commitFilter.since}
                    onChange={(e) =>
                      setCommitFilter({
                        ...commitFilter,
                        since: e.target.value,
                      })
                    }
                    className="filter-input"
                  />
                  <input
                    type="datetime-local"
                    value={commitFilter.until}
                    onChange={(e) =>
                      setCommitFilter({
                        ...commitFilter,
                        until: e.target.value,
                      })
                    }
                    className="filter-input"
                  />
                </div>
                {commits.length > 0 ? (
                  <div className="commit-grid">
                    {commits.map((commit) => (
                      <div key={commit.sha} className="commit-card">
                        <div className="commit-header">
                          <img
                            src={
                              commit.author?.avatar_url ||
                              "https://via.placeholder.com/40"
                            }
                            alt="Author"
                            className="commit-avatar"
                          />
                          <div className="commit-info">
                            <a
                              href={commit.html_url}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="commit-message"
                            >
                              <span
                                dangerouslySetInnerHTML={{
                                  __html: parseIssueReferences(
                                    commit.commit.message
                                  ),
                                }}
                              />
                            </a>
                            <div className="commit-meta">
                              <span className="commit-sha">
                                {commit.sha.slice(0, 7)}
                              </span>
                              <span>by {commit.commit.author.name}</span>
                              <span>
                                on {formatDateTime(commit.commit.author.date)}
                              </span>
                            </div>
                          </div>
                          <button
                            onClick={() => {
                              const repoDetails =
                                extractOwnerAndRepo(linkedRepository);
                              if (repoDetails) {
                                const [owner, repo] = repoDetails;
                                fetchCommitDetails(owner, repo, commit.sha);
                              }
                            }}
                            className="details-button"
                          >
                            {commitDetails.has(commit.sha) ? "Hide" : "Details"}
                          </button>
                        </div>
                        {commitDetails.has(commit.sha) && (
                          <div className="commit-details">
                            <h4>Fichiers modifiés:</h4>
                            <ul>
                              {commitDetails
                                .get(commit.sha)
                                ?.files.map((file) => (
                                  <li key={file.filename} className="file-item">
                                    <span>{file.filename}</span>
                                    <span className="file-stats">
                                      (+{file.additions}/-{file.deletions})
                                    </span>
                                    <pre className="diff">{file.patch}</pre>
                                  </li>
                                ))}
                            </ul>
                            <p className="commit-stats">
                              Total: +
                              {commitDetails.get(commit.sha)?.stats.additions}/-
                              {commitDetails.get(commit.sha)?.stats.deletions}
                            </p>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="no-data">No commits yet.</p>
                )}
              </div>
            )}
            {activeTab === "branches" && (
              <div className="branches-section">
                {branches.length > 0 ? (
                  <div className="branch-tree">
                    <div className="tree-trunk">
                      <span className="trunk-label">Main</span>
                    </div>
                    <div className="branch-nodes">
                      {branches.map((branch, index) => (
                        <div
                          key={branch.name}
                          className="branch-node"
                          style={{ top: `${index * 80 + 50}px` }}
                        >
                          <div
                            className="branch-connector"
                            style={{ height: `${index * 80 + 30}px` }}
                          ></div>
                          <div
                            className={`node ${
                              branch.name.toLowerCase().includes("main") ||
                              branch.name.toLowerCase().includes("master")
                                ? "main-branch"
                                : branch.protected
                                ? "protected-branch"
                                : branch.merged
                                ? "merged-branch"
                                : ""
                            }`}
                          >
                            <span className="branch-name">{branch.name}</span>
                            <div className="branch-badges">
                              {branch.protected && (
                                <span className="badge protected">
                                  Protected
                                </span>
                              )}
                              {branch.merged && (
                                <span className="badge merged">Merged</span>
                              )}
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                ) : (
                  <p className="no-data">No branches found.</p>
                )}
              </div>
            )}
            {activeTab === "pulls" && (
              <div className="pull-requests-section">
                <div className="filter-section">
                  <select
                    value={prState}
                    onChange={(e) => setPrState(e.target.value)}
                    className="filter-select"
                  >
                    <option value="open">Open</option>
                    <option value="closed">Closed</option>
                    <option value="all">All</option>
                  </select>
                </div>
                {pullRequests.length > 0 ? (
                  <div className="pr-grid">
                    {pullRequests.map((pr) => (
                      <div key={pr.number} className="pr-card">
                        <div className="pr-header">
                          <img
                            src={pr.user.avatar_url}
                            alt={pr.user.login}
                            className="pr-avatar"
                          />
                          <div className="pr-info">
                            <a
                              href={pr.html_url}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="pr-title"
                            >
                              #{pr.number} {pr.title}
                            </a>
                            <div className="pr-meta">
                              <span
                                className={`pr-status ${pr.state.toLowerCase()}`}
                              >
                                {pr.state}
                              </span>
                              <span>by {pr.user.login}</span>
                              <span>on {formatDateTime(pr.created_at)}</span>
                              <span>
                                {" "}
                                From {pr.head.ref} To {pr.base.ref}
                              </span>
                            </div>
                          </div>
                          <button
                            onClick={() => {
                              const repoDetails =
                                extractOwnerAndRepo(linkedRepository);
                              if (repoDetails) {
                                const [owner, repo] = repoDetails;
                                fetchPullRequestDetails(
                                  owner,
                                  repo,
                                  pr.number.toString()
                                );
                              }
                            }}
                            className="details-button"
                          >
                            {prDetails.has(pr.number) ? "Hide" : "Details"}
                          </button>
                        </div>
                        {prDetails.has(pr.number) && (
                          <div className="pr-details">
                            <h4>Commits:</h4>
                            <ul>
                              {prDetails
                                .get(pr.number)
                                ?.commits.map((commit) => (
                                  <li key={commit.sha} className="commit-item">
                                    {commit.sha.slice(0, 7)}:{" "}
                                    {commit.commit.message}
                                  </li>
                                ))}
                            </ul>
                            <h4>Fichiers modifiés:</h4>
                            <ul>
                              {prDetails.get(pr.number)?.files.map((file) => (
                                <li key={file.filename} className="file-item">
                                  <span>{file.filename}</span>
                                  <span className="file-stats">
                                    (+{file.additions}/-{file.deletions})
                                  </span>
                                  <pre className="diff">{file.patch}</pre>
                                </li>
                              ))}
                            </ul>
                            <h4>Révisions:</h4>
                            <ul>
                              {prDetails
                                .get(pr.number)
                                ?.reviews.map((review, index) => (
                                  <li key={index} className="review-item">
                                    {review.user.login}: {review.state} (
                                    {formatDateTime(review.submitted_at)})
                                  </li>
                                ))}
                            </ul>
                            <h4>Historique:</h4>
                            <ul>
                              {prDetails
                                .get(pr.number)
                                ?.events.map((event, index) => (
                                  <li key={index} className="event-item">
                                    {event.event} par {event.actor.login} le{" "}
                                    {formatDateTime(event.created_at)}
                                  </li>
                                ))}
                            </ul>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="no-data">No pull requests found.</p>
                )}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default ProjectManagement;