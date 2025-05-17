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
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faLock,
  faHashtag,
  faVolumeUp,
  faCaretDown,
  faCaretUp
} from "@fortawesome/free-solid-svg-icons";


interface Channel {
  id: string;
  name: string;
  type: "TEXT" | "VOICE";
  isPrivate: boolean;
  members: string[]; // Liste des IDs des membres autorisés
}

interface User {
  id: string; // Ajoute le champ id
  firstName: string;
  lastName: string;
  email: string;
}
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
  const [user, setUser] = useState({id: "", firstName: "", lastName: "" , email:""});
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const { accessToken, isLoading } = useAuth();
  const router = useRouter();
  const [isAddMenuOpen, setIsAddMenuOpen] = useState(false);
  const [isProjectModalOpen, setIsProjectModalOpen] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [email, setEmail] = useState("");
  const [role, setRole] = useState("DEVELOPER");
  const [message, setMessage] = useState("");
  const [showProjectsList, setShowProjectsList] = useState(false);
  const [editingProject, setEditingProject] = useState<string | null>(null);
  const [isTextChannelOpen, setIsTextChannelOpen] = useState(true);
  const [isVocalChannelOpen, setIsVocalChannelOpen] = useState(true);
  const [channels, setChannels] = useState<Channel[]>([
    // Données simulées pour l'instant
    { id: "1", name: "annoncements", type: "TEXT", isPrivate: false, members: [] },
    { id: "2", name: "tech global", type: "TEXT", isPrivate: false, members: [] },
    { id: "3", name: "vocal general", type: "VOICE", isPrivate: false, members: [] },
    { id: "4", name: "private meeting", type: "VOICE", isPrivate: true, members: [""] },
  ]);
  const [projectData, setProjectData] = useState<Project>({
    id: 0, // Champ requis par l'interface, mais sera généralement défini par l'API
    name: "",
    description: "",
    creationDate: "",
    startDate: "",
    deadline: "",
    status: "",
    phase: "",
    priority: "",
  });

  const handleProjectSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    try {
      if (editingProject) {
        const requestBody = {
          oldName: editingProject,
          newName: projectData.name,
          description: projectData.description,
          startDate: projectData.startDate,
          deadline: projectData.deadline,
          status: projectData.status,
          phase: projectData.phase,
          priority: projectData.priority,
        };

        const response = await axiosInstance.put(
          `${PROJECT_SERVICE_URL}/api/modify-project`,
          requestBody,
          {
            headers: {
              Authorization: `Bearer ${accessToken}`,
            },
          }
        );
        console.log("Réponse de PUT /modify-project:", response.data);

        const updatedProjects: Project[] = projects.map((p) =>
          p.name === editingProject ? { ...p, ...projectData } : p
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
        const newProject: Project = {
          ...projectData,
          id: response.data.id, // L'ID est généralement renvoyé par l'API
        };
        setProjects([...projects, newProject]);
        setMessage("Projet créé avec succès !");
      }

      // Réinitialisation avec tous les champs requis par l'interface Project
      setProjectData({
        id: 0,
        name: "",
        description: "",
        creationDate: "",
        startDate: "",
        deadline: "",
        status: "START",
        phase: "PLANIFICATION",
        priority: "LOW",
      });
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
    setProjectData(project); // Utilise toutes les propriétés du projet
    setShowProjectsList(false);
  };

  const handleDeleteProject = async (projectName: string) => {
    if (
      !confirm(`Voulez-vous vraiment supprimer le projet "${projectName}" ?`)
    ) {
      return;
    }

    try {
      console.log("Envoi de la requête DELETE avec projectName:", projectName);
      await axiosInstance.delete(`${PROJECT_SERVICE_URL}/api/delete-project`, {
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

  const handleProjectChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
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
      if (isLoading) return <div><img src="/loading.svg" alt="Loading" className="loading-img" /></div>;
      if (!accessToken) router.push("/authentification/signin");
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


    // Récupérer les channels du projet
  // Simuler la récupération des channels (à remplacer par API plus tard)
  /* Exemple : const response = await axiosInstance.get("/api/channels", { headers: { Authorization: `Bearer ${accessToken}` } });
 useEffect(() => {
  // Filtrer les salons privés pour n'afficher que ceux où l'utilisateur est membre
  setChannels((prev) =>
    prev.filter((channel) => channel.isPrivate))
  );
}, [user.id]);
*/
  // États supplémentaires
  const [searchQuery, setSearchQuery] = useState("");
  const [filterStatus, setFilterStatus] = useState("");
  const [filterPriority, setFilterPriority] = useState("");
  const [filterPhase, setFilterPhase] = useState("");
  const [showFilters, setShowFilters] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const projectsPerPage = 4;

  // Filtrage et recherche
  const filteredProjects = projects.filter((project) => {
    const matchesSearch =
      project.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      project.description?.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = filterStatus ? project.status === filterStatus : true;
    const matchesPriority = filterPriority
      ? project.priority === filterPriority
      : true;
    const matchesPhase = filterPhase ? project.phase === filterPhase : true;
    return matchesSearch && matchesStatus && matchesPriority && matchesPhase;
  });

  // Pagination
  const totalPages = Math.ceil(filteredProjects.length / projectsPerPage);
  const startIndex = (currentPage - 1) * projectsPerPage;
  const currentProjects = filteredProjects.slice(
    startIndex,
    startIndex + projectsPerPage
  );

  // Fonction d'export (exemple)
  const handleExport = () => {
    const csvContent = [
      [
        "Nom",
        "Description",
        "Date de création",
        "Date de début",
        "Date limite",
        "Statut",
        "Phase",
        "Priorité",
      ],
      ...projects.map((project) => [
        project.name,
        project.description || "Sans description",
        project.creationDate
          ? new Date(project.creationDate).toLocaleDateString()
          : "N/A",
        new Date(project.startDate).toLocaleDateString(),
        new Date(project.deadline).toLocaleDateString(),
        project.status,
        project.phase,
        project.priority,
      ]),
    ]
      .map((row) => row.join(","))
      .join("\n");

    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = "projets.csv";
    link.click();
  };

  if (isLoading) return <div>              <img src="/loading.svg" alt="Loading" className="loading-img" />
</div>;

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
                    <span className="user-email">{user.email}</span>
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
                  <Link
                    href="/user/dashboard/profile"
                    className="nav-link btn style-profile"
                  >
                    <span>Profil</span>
                 </Link>
                </div>

                <div className="dropdown-item">
                  <i className="fa fa-cog"></i>
                  <span>Settings</span>
                </div>

                <div className="dropdown-item">
                  <i className="fab fa-github"></i>
                  <Link
                    href="/user/dashboard/integration"
                    className="nav-link btn style-profile"
                  >
                    <span>Github Integration</span>
                 </Link>
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
                        <Link
                          href={`/user/dashboard/project/${project.id}/liste`}
                        >
                          {" "}
                          {project.name ? project.name : "Nom inconnu"}
                        </Link>
                      </li>
                    ))}
                  </div>
                ) : (
                  <p>No project available.</p>
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
                              onClick={() => setShowProjectsList(true)}
                            >
                              Liste des projets
                            </button>

                            <div className="project-formContainer">
                              {/* Partie gauche : Nom et Description */}
                              <div className="project-formLeft">
                                <div className="project-formGroup">
                                  <input
                                    type="text"
                                    id="name"
                                    value={projectData.name}
                                    onChange={handleProjectChange}
                                    placeholder="Nom du projet"
                                    required
                                  />
                                </div>
                                <div className="project-formGroup">
                                  <input
                                    type="text"
                                    id="description"
                                    value={projectData.description}
                                    onChange={handleProjectChange}
                                    placeholder="Description du projet"
                                    required
                                  />
                                </div>
                              </div>

                              {/* Partie droite : Dates */}
                              <div className="project-formRight">
                                <div className="project-formGroup">
                                  <label htmlFor="startDate">
                                    Date de début
                                  </label>
                                  <input
                                    type="date"
                                    id="startDate"
                                    value={projectData.startDate}
                                    onChange={handleProjectChange}
                                    required
                                  />
                                </div>
                                <div className="project-formGroup">
                                  <label htmlFor="Deadline">Date de fin</label>
                                  <input
                                    type="date"
                                    id="deadline"
                                    value={projectData.deadline}
                                    onChange={handleProjectChange}
                                    required
                                  />
                                </div>
                              </div>

                              {/* Ligne horizontale en dessous */}
                              <div className="project-formHorizontal">
                                <div className="project-formGroup project-formGroup-small">
                                  <select
                                    id="priority"
                                    value={projectData.priority}
                                    onChange={handleProjectChange}
                                    required
                                  >
                                    <option value="" disabled hidden>
                                      Priorité
                                    </option>
                                    <option value="LOW">Basse</option>
                                    <option value="MEDIUM">Moyenne</option>
                                    <option value="HIGH">Haute</option>
                                    <option value="CRITICAL">Critique</option>
                                  </select>
                                </div>
                                <div className="project-formGroup project-formGroup-small">
                                  <select
                                    id="phase"
                                    value={projectData.phase}
                                    onChange={handleProjectChange}
                                    required
                                  >
                                    <option value="" disabled hidden>
                                      Phase
                                    </option>
                                    <option value="PLANIFICATION">
                                      Planification
                                    </option>
                                    <option value="DESIGN">Design</option>
                                    <option value="DEVELOPPEMENT">
                                      Développement
                                    </option>
                                    <option value="TEST">Test</option>
                                    <option value="DEPLOY">Déploiement</option>
                                    <option value="MAINTENANCE">
                                      Maintenance
                                    </option>
                                    <option value="CLOSE">Clôture</option>
                                  </select>
                                </div>
                                <div className="project-formGroup project-formGroup-small">
                                  <select
                                    id="status"
                                    value={projectData.status}
                                    onChange={handleProjectChange}
                                    required
                                  >
                                    <option value="" disabled hidden>
                                      Statut
                                    </option>
                                    <option value="START">Non commencé</option>
                                    <option value="IN_PROGRESS">
                                      En cours
                                    </option>
                                    <option value="IN_PAUSE">En pause</option>
                                    <option value="DONE">Terminé</option>
                                    <option value="CANCEL">Annulé</option>
                                    <option value="ARCHIVE">Archivé</option>
                                  </select>
                                </div>
                              </div>
                            </div>
                            {message && (
                              <p className="project-message">{message}</p>
                            )}

                            <div className="project-actions">
                              <button
                                type="button"
                                className="project-cancelButton"
                                onClick={() => {
                                  setIsProjectModalOpen(false);
                                  setEditingProject(null);
                                  setProjectData({
                                    id: 0,
                                    name: "",
                                    description: "",
                                    creationDate: "",
                                    startDate: "",
                                    deadline: "",
                                    status: "START",
                                    phase: "PLANIFICATION",
                                    priority: "LOW",
                                  });
                                }}
                              >
                                Annuler
                              </button>
                              <button
                                type="submit"
                                className="project-submitButton"
                              >
                                {editingProject ? "Modifier" : "Créer"}
                              </button>
                            </div>
                          </form>
                        ) : (
                          <div className="projects-list-container">
                            <h3 className="project-modalTitle">
                              Liste des projets
                            </h3>

                            {loading ? (
                              <img src="/loading.svg" alt="Loading" className="loading-img" />                            ) : error ? (
                              <p>Erreur : {error}</p>
                            ) : filteredProjects.length > 0 ? (
                              <>
                                {/* Boutons d'action (Créer, Exporter, Filtrer) */}

                                <div className="projects-actions-bar">
                                  {/* Barre de recherche */}
                                  <div className="projects-search-bar">
                                    <i className="fa fa-search search-icon-project"></i>
                                    <input
                                      type="text"
                                      placeholder="Search"
                                      value={searchQuery}
                                      onChange={(e) =>
                                        setSearchQuery(e.target.value)
                                      }
                                      className="search-input-project"
                                    />
                                  </div>
                                  <button
                                    type="button"
                                    className="project-exportButton"
                                    onClick={handleExport}
                                  >
                                    <i className="fa fa-download"></i> Exporter
                                  </button>
                                  <button
                                    type="button"
                                    className="project-filterButton"
                                    onClick={() => setShowFilters(!showFilters)}
                                  >
                                    <i className="fa fa-filter"></i> Filtrer
                                  </button>

                                  <button
                                    type="button"
                                    className="project-createButton"
                                    onClick={() => setShowProjectsList(false)}
                                  >
                                    <i className="fa fa-plus"></i> Créer un
                                    projet
                                  </button>
                                </div>

                                {/* Filtres (optionnels, affichés si showFilters est true) */}
                                {showFilters && (
                                  <div className="projects-filters">
                                    <select
                                      value={filterStatus}
                                      onChange={(e) =>
                                        setFilterStatus(e.target.value)
                                      }
                                    >
                                      <option value="">Tous les statuts</option>
                                      <option value="START">
                                        Non commencé
                                      </option>
                                      <option value="IN_PROGRESS">
                                        En cours
                                      </option>
                                      <option value="IN_PAUSE">En pause</option>
                                      <option value="DONE">Terminé</option>
                                      <option value="CANCEL">Annulé</option>
                                      <option value="ARCHIVE">Archivé</option>
                                    </select>
                                    <select
                                      value={filterPriority}
                                      onChange={(e) =>
                                        setFilterPriority(e.target.value)
                                      }
                                    >
                                      <option value="">
                                        Toutes les priorités
                                      </option>
                                      <option value="LOW">Basse</option>
                                      <option value="MEDIUM">Moyenne</option>
                                      <option value="HIGH">Haute</option>
                                      <option value="CRITICAL">Critique</option>
                                    </select>
                                    <select
                                      value={filterPhase}
                                      onChange={(e) =>
                                        setFilterPhase(e.target.value)
                                      }
                                    >
                                      <option value="">
                                        Toutes les phases
                                      </option>
                                      <option value="PLANIFICATION">
                                        Planification
                                      </option>
                                      <option value="DESIGN">Design</option>
                                      <option value="DEVELOPPEMENT">
                                        Développement
                                      </option>
                                      <option value="TEST">Test</option>
                                      <option value="DEPLOY">
                                        Déploiement
                                      </option>
                                      <option value="MAINTENANCE">
                                        Maintenance
                                      </option>
                                      <option value="CLOSE">Clôture</option>
                                    </select>
                                  </div>
                                )}

                                {/* Tableau des projets */}
                                <table className="projects-table">
                                  <thead>
                                    <tr>
                                      <th>Nom</th>
                                      <th>Description</th>
                                      <th>Date de création</th>
                                      <th>Date de début</th>
                                      <th>Date limite</th>
                                      <th>Statut</th>
                                      <th>Phase</th>
                                      <th>Priorité</th>
                                      <th>Actions</th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    {currentProjects.map((project, index) => (
                                      <tr key={index}>
                                        <td data-label="Nom">{project.name}</td>
                                        <td data-label="Description">
                                          {project.description ||
                                            "Sans description"}
                                        </td>
                                        <td data-label="Date de création">
                                          {project.creationDate
                                            ? new Date(
                                                project.creationDate
                                              ).toLocaleDateString()
                                            : "N/A"}
                                        </td>
                                        <td data-label="Date de début">
                                          {new Date(
                                            project.startDate
                                          ).toLocaleDateString()}
                                        </td>
                                        <td data-label="Date limite">
                                          {new Date(
                                            project.deadline
                                          ).toLocaleDateString()}
                                        </td>
                                        <td data-label="Statut">
                                          <span
                                            className={`status-badge status-${project.status.toLowerCase()}`}
                                          >
                                            {project.status}
                                          </span>
                                        </td>
                                        <td data-label="Phase">
                                          {project.phase}
                                        </td>
                                        <td data-label="Priorité">
                                          <span
                                            className={`priority-badge priority-${project.priority.toLowerCase()}`}
                                          >
                                            {project.priority}
                                          </span>
                                        </td>
                                        <td data-label="Actions">
                                          <div className="project-actions-cell">
                                            <button
                                              className="edit-button"
                                              onClick={() =>
                                                handleEditProject(project)
                                              }
                                            >
                                              <i className="fa fa-edit"></i>
                                            </button>
                                            <button
                                              className="delete-button"
                                              onClick={() =>
                                                handleDeleteProject(
                                                  project.name
                                                )
                                              }
                                            >
                                              <i className="fa fa-trash"></i>
                                            </button>
                                          </div>
                                        </td>
                                      </tr>
                                    ))}
                                  </tbody>
                                </table>

                                {/* Pagination */}
                                <div className="projects-pagination">
                                  <button
                                    className="pagination-button"
                                    onClick={() =>
                                      setCurrentPage((prev) =>
                                        Math.max(prev - 1, 1)
                                      )
                                    }
                                    disabled={currentPage === 1}
                                  >
                                    <i className="fa fa-chevron-left"></i>{" "}
                                    Précédent
                                  </button>
                                  <span className="pagination-info">
                                    Page {currentPage} / {totalPages}
                                  </span>
                                  <button
                                    className="pagination-button"
                                    onClick={() =>
                                      setCurrentPage((prev) =>
                                        Math.min(prev + 1, totalPages)
                                      )
                                    }
                                    disabled={currentPage === totalPages}
                                  >
                                    Suivant{" "}
                                    <i className="fa fa-chevron-right"></i>
                                  </button>
                                </div>

                                {/* Bouton Fermer */}
                                <div className="projects-list-footer">
                                  <button
                                    type="button"
                                    className="project-cancelButton"
                                    onClick={() => setIsProjectModalOpen(false)}
                                  >
                                    Fermer
                                  </button>
                                  <div className="teams-actions">
                                  <button
                                  className="teams-inviteButton"           
                                  >
                                 <i className="fa fa-envelope"></i> Invite
                                  </button>
                                 </div>
                                </div>
                              </>
                            ) : (
                              <div className="no-projects">
                                <p>No project available.</p>
                                <button
                                  type="button"
                                  className="project-createButton"
                                  onClick={() => setShowProjectsList(false)}
                                >
                                  <i className="fa fa-plus"></i> Créer un projet
                                </button>
                              </div>
                            )}
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                </div>
              
             <li className="section-collab-title">
  <span className="section-title-style">Collaboration</span>
  <Link href="/user/dashboard/collaboration">
    <button className="add-btn">+</button>
  </Link>
</li>
<li className="subcategory-title" onClick={() => setIsTextChannelOpen(!isTextChannelOpen)}>
  <span>
    Text Channel
    <FontAwesomeIcon
      icon={isTextChannelOpen ? faCaretDown : faCaretUp}
      className="caret-icon"
    />
  </span>
</li>
{isTextChannelOpen && channels.filter((channel) => channel.type === "TEXT").length > 0 ? (
  <div className="channels-list">
    {channels
      .filter((channel) => channel.type === "TEXT")
      .map((channel) => (
        <li key={channel.id} className="channel-name">
          <FontAwesomeIcon icon={faHashtag} className="channel-icon" />
          <Link href={`/user/dashboard/collaboration/channels/${channel.id}`}>
               {channel.name}
              {channel.isPrivate && (
              <FontAwesomeIcon icon={faLock} className="private-icon" />
            )}
          </Link>
        </li>
      ))}
  </div>
) : isTextChannelOpen ? (
  <p className="no-channels">Aucun salon textuel disponible.</p>
) : null}
<li className="subcategory-title" onClick={() => setIsVocalChannelOpen(!isVocalChannelOpen)}>
  <span>
    Vocal Channel
    <FontAwesomeIcon
      icon={isVocalChannelOpen ? faCaretDown : faCaretUp}
      className="caret-icon"
    />
  </span>
</li>
{isVocalChannelOpen && channels.filter((channel) => channel.type === "VOICE").length > 0 ? (
  <div className="channels-list">
    {channels
      .filter((channel) => channel.type === "VOICE")
      .map((channel) => (
        <li key={channel.id} className="channel-name">
          <FontAwesomeIcon icon={faVolumeUp} className="channel-icon" />
          <Link href={`/user/dashboard/collaboration/channels/${channel.id}`}>
              {channel.name}
              {channel.isPrivate && (
              <FontAwesomeIcon icon={faLock} className="private-icon" />
            )}
          </Link>
        </li>
      ))}
  </div>
) : isVocalChannelOpen ? (
  <p className="no-channels">Aucun salon vocal disponible.</p>
) : null}
              </ul>
            </nav>
          </div>

          <div className="main-content">{children}</div>
        </div>
      </div>
    </ProtectedRoute>
  );
}
