"use client";
import { useEffect, useState } from "react";
import { usePathname } from "next/navigation";
import Link from "next/link";
import "../../../styles/UserDashboard.css";
import { useAuth } from "@/context/AuthContext";
import { useRouter } from "next/navigation";
import axios from "axios";
import ProtectedRoute from "../../../components/ProtectedRoute";
import useAxios from "../../../hooks/useAxios";
import { PROJECT_SERVICE_URL } from "../../../config/useApi";
import { useProjects, Project } from "../../../hooks/useProjects";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const handleInvite = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    try {
      await axios.post("http://localhost:8083/api/invitations", {
        email,
        role,
        entrepriseId: "123",
      });

      setMessage("Invitation envoyée avec succès !");
      setEmail("");
      setRole("DEVELOPER");
      setIsModalOpen(false);
    } catch (error) {
      setMessage(
        "Erreur lors de l'envoi de l'invitation : " + (error as Error).message
      );
    }
  };

  const { projects, setProjects, loading, error } = useProjects();
  const axiosInstance = useAxios();
  const currentPath = usePathname();
  const [user, setUser] = useState({ firstName: "", lastName: "" });
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const { accessToken, isLoading } = useAuth();
  const router = useRouter();
  const [isAddMenuOpen, setIsAddMenuOpen] = useState(false);
  const [isProjectModalOpen, setIsProjectModalOpen] = useState(false);
  const [projectData, setProjectData] = useState({ name: "", description: "" });
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [email, setEmail] = useState("");
  const [role, setRole] = useState("DEVELOPER");
  const [message, setMessage] = useState("");
  const [showProjectsList, setShowProjectsList] = useState(false);
  const [editingProject, setEditingProject] = useState<string | null>(null);

  const handleProjectSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    try {
      if (editingProject) {
        await axiosInstance.put(
          `${PROJECT_SERVICE_URL}/api/projects`,
          {
            oldName: editingProject,
            newName: projectData.name,
            description: projectData.description,
          },
          {
            headers: {
              Authorization: `Bearer ${accessToken}`,
            },
          }
        );

        const updatedProjects = projects.map((p) =>
          p.name === editingProject
            ? { name: projectData.name, description: projectData.description }
            : p
        );
        setProjects(updatedProjects);
        setMessage("Projet modifié avec succès !");
      } else {
        const response = await axiosInstance.post(
          `${PROJECT_SERVICE_URL}/api/create-project`,
          projectData,
          {
            headers: {
              Authorization: `Bearer ${accessToken}`,
            },
          }
        );
        console.log("Success:", response.data);
        setProjects([
          ...projects,
          { name: projectData.name, description: projectData.description },
        ]);
        setMessage("Projet créé avec succès !");
      }

      setProjectData({ name: "", description: "" });
      setEditingProject(null);
      setIsProjectModalOpen(false);
    } catch (error) {
      setMessage(
        "Erreur lors de la " +
          (editingProject ? "modification" : "création") +
          " du projet : " +
          (error as Error).message
      );
    }
  };

  const handleEditProject = (project: Project) => {
    setEditingProject(project.name);
    setProjectData({ name: project.name, description: project.description });
    setShowProjectsList(false);
  };

  const handleDeleteProject = async (projectName: string) => {
    if (
      !confirm(`Voulez-vous vraiment supprimer le projet "${projectName}" ?`)
    ) {
      return;
    }

    try {
      await axiosInstance.delete(`${PROJECT_SERVICE_URL}/api/projects`, {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        data: { name: projectName },
      });

      const updatedProjects = projects.filter((p) => p.name !== projectName);
      setProjects(updatedProjects);
      setMessage("Projet supprimé avec succès !");
    } catch (error) {
      setMessage(
        "Erreur lors de la suppression du projet : " + (error as Error).message
      );
    }
  };

  const handleProjectChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { id, value } = e.target;
    setProjectData((prev) => ({ ...prev, [id]: value }));
  };

  const handleOverlayClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) {
      setIsModalOpen(false);
      setIsProjectModalOpen(false);
      setShowProjectsList(false);
      setEditingProject(null);
    }
  };

  useEffect(() => {
    const fetchUserInfo = async () => {
      if (accessToken) {
        try {
          const response = await axios.get("http://localhost:8083/api/me", {
            headers: {
              Authorization: `Bearer ${accessToken}`,
            },
          });

          if (response.status === 200) {
            setUser(response.data);
          } else {
            console.error(
              "Erreur lors de la récupération des infos utilisateur"
            );
          }
        } catch (error) {
          console.error(
            "Erreur lors de la récupération des infos utilisateur",
            error
          );
        }
      }
    };

    if (!isLoading && accessToken) {
      fetchUserInfo();
    }
  }, [accessToken, isLoading]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      const addMenu = document.querySelector(".add-menu");
      const dropDownMenu = document.querySelector(".dropdown-menu");
      const iconContainer = document.querySelector(".icon-container");
      const userIcon = document.querySelector(".user-icon");

      if (
        addMenu &&
        iconContainer &&
        !addMenu.contains(event.target as Node) &&
        !iconContainer.contains(event.target as Node)
      ) {
        setIsAddMenuOpen(false);
      }

      if (
        dropDownMenu &&
        userIcon &&
        !dropDownMenu.contains(event.target as Node) &&
        !userIcon.contains(event.target as Node)
      ) {
        setIsDropdownOpen(false);
      }
    };

    if (isAddMenuOpen || isDropdownOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isAddMenuOpen, isDropdownOpen]);

  const handleLogout = async () => {
    try {
      await axios.post(
        `${process.env.NEXT_PUBLIC_API_AUTHENTIFICATON_SERVICE_URL}/api/logout`,
        {},
        { withCredentials: true }
      );
      router.push("/authentification/signin");
      console.log("Déconnexion réussie");
    } catch (error) {
      console.error("Erreur lors de la déconnexion :", error);
    }
  };

  const toggleDropdown = () => {
    console.log("Clic détecté, isDropdownOpen avant :", isDropdownOpen);
    setIsDropdownOpen(!isDropdownOpen);
    console.log("isDropdownOpen après :", !isDropdownOpen);
  };

  if (isLoading) return <div>Chargement...</div>;

  return (
    <ProtectedRoute>
      <div className="container-manager">
        {/* Topbar */}
        <div className="topbar">
          <div className="topbar-icons">
            <i className="fa fa-list text-icon-list"></i>
            <span
              className="icon-container"
              onClick={() => {
                console.log("Clic sur l'icône + détecté");
                setIsAddMenuOpen((prev) => !prev);
              }}
            >
              <i className="fa fa-plus icon"></i>
              <span className="text-icon-add">add</span>
            </span>
            {/* Menu déroulant pour les options "add" */}
            {isAddMenuOpen && (
              <div className="add-menu">
                {(() => {
                  console.log(
                    "Menu déroulant rendu, isAddMenuOpen :",
                    isAddMenuOpen
                  );
                  return null;
                })()}
                <div className="add-menu-item">
                  <i className="fa fa-check-circle"></i>
                  <span>Task</span>
                </div>
                <div
                  className="add-menu-item"
                  onClick={() => {
                    setIsAddMenuOpen(false);
                    setIsProjectModalOpen(true);
                  }}
                >
                  <i className="fa fa-clipboard"></i>
                  <span>Project</span>
                </div>
                <div className="add-menu-item">
                  <i className="fa fa-comment"></i>
                  <span>Message</span>
                </div>
                <div className="add-menu-item">
                  <i className="fa fa-folder"></i>
                  <span>Portfolio</span>
                </div>
                <div className="add-menu-item">
                  <i className="fa fa-bullseye"></i>
                  <span>Goal</span>
                </div>
                <div
                  className="add-menu-item"
                  onClick={() => {
                    setIsAddMenuOpen(false);
                    setIsModalOpen(true);
                  }}
                >
                  <i className="fa fa-user-plus"></i>
                  <span>Inviter</span>
                </div>
              </div>
            )}
          </div>

          <div className="search-container">
            <i className="fa fa-search search-icon"></i>
            <input type="text" placeholder="Search" className="search-bar" />
          </div>

          <div className="user-container">
            <span className="user-name" style={{ color: "white" }}>
              {user.firstName} {user.lastName}
            </span>
            <div
              className="user-icon"
              onClick={toggleDropdown}
              onKeyDown={(e) => {
                if (e.key === "Enter" || e.key === " ") {
                  toggleDropdown();
                }
              }}
              role="button"
              tabIndex={0}
            >
              <i className="fa fa-user"></i>
            </div>

            {isDropdownOpen && (
              <div className="dropdown-menu">
                <div className="dropdown-header">
                  <div className="user-initials">
                    {user.firstName.charAt(0)}
                    {user.lastName.charAt(0)}
                  </div>
                  <div className="user-info">
                    <span className="user-fullname">
                      {user.firstName} {user.lastName}
                    </span>
                    <span className="user-email">aflousfatima@gmail.com</span>
                  </div>
                </div>
                <div className="dropdown-divider"></div>
                <div className="dropdown-item">
                  <i className="fa fa-tachometer-alt"></i>
                  <span>Administration Console</span>
                </div>
                <div className="dropdown-item">
                  <i className="fa fa-plus-circle"></i>
                  <span>New Workspace</span>
                </div>
                <div className="dropdown-item">
                  <i className="fa fa-user-plus"></i>
                  <span>Invite to Join AGILIA</span>
                </div>
                <div className="dropdown-item">
                  <i className="fa fa-user-circle"></i>
                  <span>Profil</span>
                </div>
                <div className="dropdown-item">
                  <i className="fa fa-cog"></i>
                  <span>Settings</span>
                </div>
                <div className="dropdown-divider"></div>
                <div className="dropdown-item" onClick={handleLogout}>
                  <i className="fa fa-sign-out-alt logout-icon"></i>
                  <span>Log Out</span>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Conteneur pour la sidebar et le contenu principal */}
        <div className="main-wrapper">
          {/* Sidebar */}
          <div className="sidebar">
            <nav>
              <ul>
                <li
                  className={
                    currentPath === "/user/dashboard/home" ? "active" : ""
                  }
                >
                  <Link href="/user/dashboard/home">
                    <i className="fa fa-house-user icon-home"></i>
                    <span>Home</span>
                  </Link>
                </li>
                <li
                  className={
                    currentPath === "/user/dashboard/my-tasks" ? "active" : ""
                  }
                >
                  <Link href="/user/dashboard/my-tasks">
                    <i className="fa fa-check-circle"></i>
                    <span>My Tasks</span>
                  </Link>
                </li>
                <li
                  className={
                    currentPath === "/user/dashboard/inbox" ? "active" : ""
                  }
                >
                  <Link href="/user/dashboard/inbox">
                    <i className="fa fa-bell icon-inbox"></i>
                    <span>Inbox</span>
                    <span className="notification-dot"></span>
                  </Link>
                </li>

                <li className="section-title">
                  <span className="section-title-style">Indicators</span>
                  <button className="add-btn">+</button>
                </li>
                <li
                  className={
                    currentPath === "/user/dashboard/reports" ? "active" : ""
                  }
                >
                  <Link href="/user/dashboard/reports">
                    <i className="fa fa-chart-line"></i>
                    <span>Reports</span>
                  </Link>
                </li>
                <li
                  className={
                    currentPath === "/user/dashboard/portfolio" ? "active" : ""
                  }
                >
                  <Link href="/user/dashboard/portfolio">
                    <i className="fa fa-folder"></i>
                    <span>Portfolios</span>
                  </Link>
                </li>
                <li
                  className={
                    currentPath === "/user/dashboard/goals" ? "active" : ""
                  }
                >
                  <Link href="/user/dashboard/goals">
                    <i className="fa fa-bullseye"></i>
                    <span>Goals</span>
                  </Link>
                </li>

                <li className="section-title">
                  <span className="section-title-style">Projects</span>
                  <button className="add-btn">+</button>
                </li>

                {projects.length > 0 ? (
                  <div className="projects1-list">
                    {projects.map((project, index) => (
                      <li key={index} className="project1-name">
                        <img
                          src="/project.png"
                          alt="logo"
                          className="project-image"
                        />
                        <Link href="/user/dashboard/project">
                          {project.name}
                        </Link>
                      </li>
                    ))}
                  </div>
                ) : (
                  <p>Aucun projet disponible.</p>
                )}

                <span className="section-title"></span>

                <li
                  className={
                    currentPath === "/user/dashboard/teams" ? "active" : ""
                  }
                >
                  <Link href="/user/dashboard/teams">
                    <i className="fa fa-users"></i>
                    <span>Teams</span>
                  </Link>
                </li>

                <div className="section-invite">
                  <ul>
                    <li>
                      <span
                        className="invite-style"
                        onClick={() => setIsModalOpen(true)}
                      >
                        <i className="fa fa-envelope"></i> Invite colleagues
                      </span>
                    </li>
                  </ul>

                  {isModalOpen && (
                    <div
                      className="teams-modalOverlay"
                      onClick={handleOverlayClick}
                    >
                      <div className="teams-modal">
                        <h2 className="teams-modalTitle">
                          Inviter un collègue
                        </h2>
                        <form onSubmit={handleInvite} className="teams-form">
                          <div className="teams-formGroup">
                            <label htmlFor="email">Email</label>
                            <input
                              type="email"
                              id="email"
                              value={email}
                              onChange={(e) => setEmail(e.target.value)}
                              placeholder="Entrez l'email du collègue"
                              required
                            />
                          </div>
                          <div className="teams-formGroup">
                            <label htmlFor="role">Rôle</label>
                            <select
                              id="role"
                              value={role}
                              onChange={(e) => setRole(e.target.value)}
                            >
                              <option value="DEVELOPER">Développeur</option>
                              <option value="TESTER">Testeur</option>
                              <option value="DEVOPS">DevOps</option>
                              <option value="DESIGNER">Designer</option>
                            </select>
                          </div>
                          {message && (
                            <p className="teams-message">{message}</p>
                          )}
                          <div className="teams-modalActions">
                            <button
                              type="submit"
                              className="teams-submitButton"
                            >
                              Envoyer invitation
                            </button>
                            <button
                              type="button"
                              className="teams-cancelButton"
                              onClick={() => setIsModalOpen(false)}
                            >
                              Annuler
                            </button>
                          </div>
                        </form>
                      </div>
                    </div>
                  )}

                  {isProjectModalOpen && (
                    <div
                      className="project-modalOverlay"
                      onClick={handleOverlayClick}
                    >
                      <div className="project-modal">
                        {!showProjectsList ? (
                          <form
                            onSubmit={handleProjectSubmit}
                            className="project-form"
                          >
                            <h2 className="project-modalTitle">
                              {editingProject
                                ? "Modifier un projet"
                                : "Créer un projet"}
                            </h2>
                            <button
                              type="button"
                              className="project-listButton"
                              onClick={() => {
                                console.log(
                                  "Clic sur Liste des projets, showProjectsList avant :",
                                  showProjectsList
                                );
                                setShowProjectsList(true);
                                console.log("showProjectsList après :", true);
                              }}
                            >
                              Liste des projets
                            </button>
                            <div className="project-formGroup">
                              <label htmlFor="name">Nom du projet</label>
                              <input
                                type="text"
                                id="name"
                                value={projectData.name}
                                onChange={handleProjectChange}
                                placeholder="Entrez le nom du projet"
                                required
                              />
                            </div>
                            <div className="project-formGroup">
                              <label htmlFor="description">Description</label>
                              <input
                                type="text"
                                id="description"
                                value={projectData.description}
                                onChange={handleProjectChange}
                                placeholder="Entrez la description"
                                required
                              />
                            </div>
                            {message && (
                              <p className="project-message">{message}</p>
                            )}
                            <div className="project-modalActions">
                              <button
                                type="submit"
                                className="project-submitButton"
                              >
                                {editingProject
                                  ? "Modifier le projet"
                                  : "Créer le projet"}
                              </button>
                              <button
                                type="button"
                                className="project-cancelButton"
                                onClick={() => {
                                  setIsProjectModalOpen(false);
                                  setEditingProject(null);
                                  setProjectData({ name: "", description: "" });
                                  setShowProjectsList(false);
                                }}
                              >
                                Annuler
                              </button>
                            </div>
                          </form>
                        ) : (
                          <div className="projects-list-container">
                            <h3 className="project-modalTitle">
                              Liste des projets
                            </h3>
                            {loading ? (
                              <p>Chargement des projets...</p>
                            ) : error ? (
                              <p>Erreur : {error}</p>
                            ) : projects.length > 0 ? (
                              <>
                                <div className="project-modalActions">
                                  <button
                                    type="button"
                                    className="project-createButton"
                                    onClick={() => {
                                      console.log(
                                        "Clic sur Créer un projet, showProjectsList avant :",
                                        showProjectsList
                                      );
                                      setShowProjectsList(false);
                                      console.log(
                                        "showProjectsList après :",
                                        false
                                      );
                                    }}
                                  >
                                    Créer un projet
                                  </button>
                                </div>
                                <table className="projects-table">
                                  <thead>
                                    <tr>
                                      <th>Nom du projet</th>
                                      <th>Description</th>
                                      <th>Actions</th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    {projects.map((project, index) => (
                                      <tr key={index}>
                                        <td>{project.name}</td>
                                        <td>
                                          {project.description ||
                                            "Aucune description disponible"}
                                        </td>
                                        <td>
                                          <div className="project-actions">
                                            <button
                                              className="edit-button"
                                              onClick={() =>
                                                handleEditProject(project)
                                              }
                                              title="Modifier"
                                            >
                                              <i className="fa fa-edit edit-style"></i>
                                            </button>
                                            <button
                                              className="delete-button"
                                              onClick={() =>
                                                handleDeleteProject(
                                                  project.name
                                                )
                                              }
                                              title="Supprimer"
                                            >
                                              <i className="fa fa-trash delete-style"></i>
                                            </button>
                                          </div>
                                        </td>
                                      </tr>
                                    ))}
                                  </tbody>
                                </table>
                                <button
                                  type="button"
                                  className="project-cancelButton1"
                                  onClick={() => {
                                    setIsProjectModalOpen(false);
                                    setEditingProject(null);
                                    setProjectData({
                                      name: "",
                                      description: "",
                                    });
                                    setShowProjectsList(false);
                                  }}
                                >
                                  Annuler
                                </button>
                              </>
                            ) : (
                              <p>Aucun projet disponible.</p>
                            )}
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                </div>
              </ul>
            </nav>
          </div>

          <div className="main-content">{children}</div>
        </div>
      </div>
    </ProtectedRoute>
  );
}
