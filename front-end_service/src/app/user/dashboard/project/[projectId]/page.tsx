"use client";
import React, { useState, useEffect } from "react";
import "../../../../../styles/Dashboard-Project.css";
import { useAuth } from "../../../../../context/AuthContext";
import useAxios from "../../../../../hooks/useAxios";
import {
  AUTH_SERVICE_URL,
  PROJECT_SERVICE_URL,
} from "../../../../../config/useApi";
import { useParams } from "next/navigation";

// Interfaces
interface Task {
  id: number;
  name: string;
  responsible: string | null;
  dueDate: string;
  priority: "LOW" | "MEDIUM" | "HIGH" | "";
  status: "TO DO" | "IN PROGRESS" | "DONE";
  sprintId?: number;
  userStoryId?: number;
}

interface TeamMember {
  id: string;
  firstName: string;
  lastName: string;
  role: string;
  project: string;
  avatar: string;
}

interface Manager {
  id: string;
  authId?: string;
  firstName: string;
  lastName: string;
}

interface UserStory {
  id: number;
  name: string;
  description: string;
  status:
    | "BACKLOG"
    | "SELECTED_FOR_SPRINT"
    | "IN_PROGRESS"
    | "DONE"
    | "BLOCKED" // Nouveaux statuts
    | "ON_HOLD"
    | "CANCELED"
    | "ARCHIVED";
  priority: "LOW" | "MEDIUM" | "HIGH" | "";
  effortPoints: number;
  sprintId?: number;
  dependsOn?: number[]; // Ajout pour les dépendances (comme discuté précédemment)
}

interface Sprint {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  capacity: number;
  goal: string;
  userStories: UserStory[];
  status: "PLANNED" | "ACTIVE" | "COMPLETED" | "ARCHIVED" | "CANCELED"; // Nouveau champ
}

const initialTasks: Task[] = [
  {
    id: 1,
    name: "Rédiger le brief",
    responsible: "Fatima",
    dueDate: "6 avril",
    priority: "LOW",
    status: "TO DO",
    sprintId: 1,
    userStoryId: 1,
  },
  {
    id: 2,
    name: "Planifier réunion",
    responsible: "Fatima",
    dueDate: "7 avril",
    priority: "MEDIUM",
    status: "TO DO",
    sprintId: 1,
    userStoryId: 1,
  },
];

export default function Tasks() {
  const { accessToken, isLoading: authLoading } = useAuth();
  const axiosInstance = useAxios();
  const [backlog, setBacklog] = useState<UserStory[]>([]);
  const [expandedDependsOn, setExpandedDependsOn] = useState<number | null>(
    null
  );
  const [searchQuery, setSearchQuery] = useState(""); // Nouvel état pour la recherche
  // Calculer les éléments à afficher avec recherche et pagination
  const filteredBacklog = backlog
    .filter((us) => !us.sprintId) // Stories sans sprint
    .filter((us) => {
      if (searchQuery === "") return true; // Si pas de recherche, tout afficher
      const nameMatch = us.name
        ?.toLowerCase()
        .includes(searchQuery.toLowerCase());
      const descMatch = us.description
        ?.toLowerCase()
        .includes(searchQuery.toLowerCase());
      return nameMatch || descMatch; // Retourner vrai si match dans name ou description
    });
  // Reset page to 1 when search query changes (optionnel, pour éviter confusion)
  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchQuery(e.target.value);
    setCurrentPage(1); // Revenir à la première page lors d'une nouvelle recherche
  };
  const params = useParams();
  const projectId = params.projectId as string;
  const [currentPage, setCurrentPage] = useState(1); // État pour la page actuelle
  const itemsPerPage = 3; // Nombre d'éléments par page

  // Calculer les éléments à afficher pour la page actuelle
  const totalItems = filteredBacklog.length;
  const totalPages = Math.ceil(totalItems / itemsPerPage); // Nombre total de pages
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const currentStories = filteredBacklog.slice(startIndex, endIndex); // Stories pour la page actuelle

  // Handlers pour la pagination
  const goToNextPage = () => {
    if (currentPage < totalPages) {
      setCurrentPage(currentPage + 1);
    }
  };

  const goToPreviousPage = () => {
    if (currentPage > 1) {
      setCurrentPage(currentPage - 1);
    }
  };

  // Ajout des nouvelles fonctions
  const handleCancelSprint = async (sprintId: number) => {
    try {
      const response = await axiosInstance.post(
        `${PROJECT_SERVICE_URL}/api/projects/${projectId}/sprints/${sprintId}/cancel`,
        {},
        {
          headers: { Authorization: `Bearer ${accessToken}` },
        }
      );
      const updatedSprint: Sprint = response.data; // SprintDTO retourné par le backend
      setSprints(
        sprints.map((s: Sprint) =>
          s.id === sprintId
            ? { ...updatedSprint, userStories: [] } // Utilisation des données de l'API
            : s
        )
      );
      const canceledSprint = sprints.find((s: Sprint) => s.id === sprintId);
      if (canceledSprint) {
        const updatedBacklogStories: UserStory[] =
          canceledSprint.userStories.map((us: UserStory) => ({
            ...us,
            status: "BACKLOG",
            sprintId: undefined,
          }));
        setBacklog([...backlog, ...updatedBacklogStories]);
      }
      if (activeSprint?.id === sprintId) {
        setActiveSprint(null); // Désactiver le sprint actif si c'est celui annulé
      }
    } catch (error) {
      console.error("Erreur lors de l'annulation du sprint :", error);
      alert("Erreur lors de l'annulation du sprint.");
    }
  };

  const handleArchiveSprint = async (sprintId: number) => {
    try {
      const response = await axiosInstance.post(
        `${PROJECT_SERVICE_URL}/api/projects/${projectId}/sprints/${sprintId}/archive`,
        {},
        {
          headers: { Authorization: `Bearer ${accessToken}` },
        }
      );
      const updatedSprint: Sprint = response.data; // SprintDTO retourné par le backend
      setSprints(
        sprints.map(
          (s: Sprint) => (s.id === sprintId ? { ...updatedSprint } : s) // Utilisation des données de l'API
        )
      );
    } catch (error) {
      console.error("Erreur lors de l'archivage du sprint :", error);
      alert("Erreur lors de l'archivage du sprint.");
    }
  };
  const [sprints, setSprints] = useState<Sprint[]>([]);
  const [currentSprintPage, setCurrentSprintPage] = useState(1); // État pour la page actuelle des sprints
  const [sprintSearchQuery, setSprintSearchQuery] = useState(""); // Nouvel état pour la recherche des sprints
  const filteredSprints = sprints.filter((sprint) => {
    if (sprintSearchQuery === "") return true;
    const nameMatch = sprint.name
      ?.toLowerCase()
      .includes(sprintSearchQuery.toLowerCase());
    const goalMatch = sprint.goal
      ?.toLowerCase()
      .includes(sprintSearchQuery.toLowerCase());
    return nameMatch || goalMatch;
  });
  const handleSprintSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSprintSearchQuery(e.target.value);
    setCurrentSprintPage(1); // Revenir à la première page lors d'une nouvelle recherche
  };
  // Calculer les sprints à afficher pour la page actuelle
  const totalSprintItems = filteredSprints.length;
  const totalSprintPages = Math.ceil(totalSprintItems / itemsPerPage); // Nombre total de pages
  const sprintStartIndex = (currentSprintPage - 1) * itemsPerPage;
  const sprintEndIndex = sprintStartIndex + itemsPerPage;
  const currentSprints = filteredSprints.slice(
    sprintStartIndex,
    sprintEndIndex
  );
  // Handlers pour la pagination des sprints
  const goToNextSprintPage = () => {
    if (currentSprintPage < totalSprintPages) {
      setCurrentSprintPage(currentSprintPage + 1);
    }
  };

  const goToPreviousSprintPage = () => {
    if (currentSprintPage > 1) {
      setCurrentSprintPage(currentSprintPage - 1);
    }
  };

  // Ajout d'un état pour gérer l'édition
  const [editingUserStory, setEditingUserStory] = useState<UserStory | null>(
    null
  );
  const [tasks, setTasks] = useState<Task[]>(initialTasks);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [expandedSection, setExpandedSection] = useState<string | null>(
    "backlog"
  );
  const [newTask, setNewTask] = useState<Task>({
    id: 0,
    name: "",
    responsible: null,
    dueDate: "",
    priority: "",
    status: "TO DO",
  });
  const [teamMembers, setTeamMembers] = useState<TeamMember[]>([]);
  const [manager, setManager] = useState<Manager | null>(null);
  const [showAllMembers, setShowAllMembers] = useState(false);
  const [projectName, setProjectName] = useState<string>("Projet 1");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filterMyTasks, setFilterMyTasks] = useState(false);
  const [expandedSprintStories, setExpandedSprintStories] = useState<
    number | null
  >(null);
  // États pour Agile
  const [isAgilePanelOpen, setIsAgilePanelOpen] = useState(false);

  const [newUserStory, setNewUserStory] = useState<UserStory>({
    id: 0,
    name: "",
    description: "",
    status: "BACKLOG",
    priority: "",
    effortPoints: 0,
    dependsOn: [],
  });
  const [editingSprint, setEditingSprint] = useState<Sprint | null>(null);
  const [newSprint, setNewSprint] = useState<Sprint>({
    id: 0,
    name: "",
    startDate: "",
    endDate: "",
    goal: "",
    capacity: 0,
    userStories: [],
    status: "PLANNED",
  });
  const [activeSprint] = useState<Sprint | null>(null);
  const [selectedUserStoryId, setSelectedUserStoryId] = useState<number | null>(
    null
  ); // Pour le mini-modal
  const setActiveSprint = async (sprint) => {
    try {
      const response = await axiosInstance.post(
        `${PROJECT_SERVICE_URL}/api/projects/${projectId}/sprints/${sprint.id}/activate`,
        {},
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );
      // Mettre à jour l'état local avec le sprint activé
      setActiveSprint(response.data);
    } catch (error) {
      console.error("Erreur lors de l'activation du sprint :", error);
      alert("Erreur lors de l'activation du sprint.");
    }
  };

  // Fonction pour éditer une User Story
  const handleEditUserStory = (story: UserStory) => {
    setEditingUserStory(story); // Pré-remplit le formulaire avec les données de la User Story
  };

  // Fonction pour éditer un Sprint
  const handleEditSprint = (sprint: Sprint) => {
    setEditingSprint(sprint); // Pré-remplit le formulaire avec les données du Sprint
  };

  // Fonction pour supprimer un Sprint
  const handleDeleteSprint = async (id: number) => {
    if (!confirm("Voulez-vous vraiment supprimer ce Sprint ?")) return;

    try {
      await axiosInstance.delete(
        `${PROJECT_SERVICE_URL}/api/projects/${projectId}/sprints/${id}`,
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );

      if (activeSprint && activeSprint.id === id) {
        setActiveSprint(null);
      }
      // Si vous avez une liste de sprints, mettez-la à jour ici
    } catch (error) {
      console.error("Erreur lors de la suppression du Sprint :", error);
      alert("Erreur lors de la suppression du Sprint.");
    }
  };

  // Fetch project details
  useEffect(() => {
    const fetchProjectDetailsAndTeamMembers = async () => {
      if (authLoading || !accessToken || !projectId) return;
      try {
        setLoading(true);
        setError(null);
        const projectResponse = await axiosInstance.get(
          `${PROJECT_SERVICE_URL}/api/projects/${projectId}`
        );
        setProjectName(projectResponse.data.name || "Projet inconnu");

        if (
          projectResponse.data.manager &&
          projectResponse.data.manager.authId
        ) {
          const managerResponse = await axiosInstance.get(
            `${AUTH_SERVICE_URL}/api/auth/users/${projectResponse.data.manager.authId}`
          );
          setManager({
            id: projectResponse.data.manager.id,
            authId: projectResponse.data.manager.authId,
            firstName: managerResponse.data.firstName,
            lastName: managerResponse.data.lastName,
          });
        } else {
          setManager(null);
        }

        const teamResponse = await axiosInstance.get(
          `${AUTH_SERVICE_URL}/api/team-members/${projectId}`
        );
        setTeamMembers(teamResponse.data);
      } catch (err) {
        console.error("❌ Erreur :", err);
        setError("Impossible de charger les données du projet.");
        setManager(null);
        setProjectName("Projet inconnu");
      } finally {
        setLoading(false);
      }
    };
    fetchProjectDetailsAndTeamMembers();
  }, [accessToken, authLoading, axiosInstance, projectId]);

  // Gestion du clic extérieur
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as HTMLElement;
      if (!target.closest(".team-members-display")) setShowAllMembers(false);
      if (!target.closest(".agile-sidebar") && !target.closest(".agile-toggle"))
        setIsAgilePanelOpen(false);
    };
    if (showAllMembers || isAgilePanelOpen)
      document.addEventListener("click", handleClickOutside);
    return () => document.removeEventListener("click", handleClickOutside);
  }, [showAllMembers, isAgilePanelOpen]);

  const handleAddTask = (
    e: React.FormEvent<HTMLFormElement>,
    userStoryId?: number
  ) => {
    e.preventDefault();
    const task: Task = {
      ...newTask,
      id: tasks.length + 1,
      sprintId: activeSprint?.id,
      userStoryId,
    };
    setTasks([...tasks, task]);
    setNewTask({
      id: 0,
      name: "",
      responsible: null,
      dueDate: "",
      priority: "",
      status: "TO DO",
    });
    setSelectedUserStoryId(null); // Ferme le mini-modal
  };

  const handleAddUserStory = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    try {
      const response = await axiosInstance.post(
        `${PROJECT_SERVICE_URL}/api/projects/${projectId}/user-stories`,
        {
          title: newUserStory.name,
          description: newUserStory.description,
          priority: newUserStory.priority,
          status: "BACKLOG", // Toujours "BACKLOG" pour une nouvelle user story
          effortPoints: newUserStory.effortPoints,
          dependsOn: newUserStory.dependsOn || [], // Ajout de dependsOn (vide par défaut)
        },
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );

      const createdUserStory = response.data;
      setBacklog([
        ...backlog,
        {
          id: createdUserStory.id,
          name: createdUserStory.title,
          description: createdUserStory.description,
          status: createdUserStory.status, // Récupérer depuis la réponse
          priority: createdUserStory.priority,
          effortPoints: createdUserStory.effortPoints,
          dependsOn: createdUserStory.dependsOn || [], // Récupérer depuis la réponse
        },
      ]);
      setNewUserStory({
        id: 0,
        name: "",
        description: "",
        status: "BACKLOG",
        priority: "",
        effortPoints: 0,
        dependsOn: [], // Réinitialiser avec un tableau vide
      });
    } catch (error) {
      console.error("Erreur lors de la création de la User Story :", error);
      alert("Erreur lors de l'ajout de la User Story.");
    }
  };

  useEffect(() => {
    const fetchUserStories = async () => {
      if (authLoading || !accessToken || !projectId) {
        console.log("Conditions non remplies : ", {
          authLoading,
          accessToken,
          projectId,
        });
        return;
      }
      const url = `${PROJECT_SERVICE_URL}/api/projects/${projectId}/user-stories`;
      console.log("Appel de l’API User Stories avec URL :", url);
      try {
        const response = await axiosInstance.get(url, {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        });
        console.log("Réponse de l’API :", response.data);
        const userStories = response.data.map((us: any) => ({
          id: us.id,
          name: us.title,
          description: us.description,
          priority: us.priority,
          effortPoints: us.effortPoints,
          sprintId: us.sprintId,
          dependsOn: us.dependsOn || [], // Ajout de dependsOn
        }));
        setBacklog(userStories);
      } catch (error) {
        console.error(
          "Erreur lors de la récupération des User Stories :",
          error
        );
        alert("Erreur lors du chargement des User Stories.");
      }
    };
    fetchUserStories();
  }, [accessToken, authLoading, axiosInstance, projectId]);
  // Update user story
  const handleUpdateUserStory = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!editingUserStory) return;

    try {
      const response = await axiosInstance.put(
        `${PROJECT_SERVICE_URL}/api/projects/${projectId}/user-stories/${editingUserStory.id}`,
        {
          title: editingUserStory.name,
          description: editingUserStory.description,
          priority: editingUserStory.priority,
          effortPoints: editingUserStory.effortPoints,
          dependsOn: editingUserStory.dependsOn || [], // Ajout de dependsOn
          // Ne pas inclure status ici, il reste inchangé sauf via assign/remove
        },
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );

      const updatedStory = response.data;
      setBacklog(
        backlog.map((us) =>
          us.id === updatedStory.id
            ? {
                ...us,
                name: updatedStory.title,
                description: updatedStory.description,
                priority: updatedStory.priority,
                effortPoints: updatedStory.effortPoints,
                status: updatedStory.status, // Récupérer depuis la réponse
                dependsOn: updatedStory.dependsOn || [], // Récupérer depuis la réponse
              }
            : us
        )
      );

      if (
        activeSprint &&
        activeSprint.userStories.some((us) => us.id === updatedStory.id)
      ) {
        setActiveSprint({
          ...activeSprint,
          userStories: activeSprint.userStories.map((us) =>
            us.id === updatedStory.id
              ? {
                  ...us,
                  name: updatedStory.title,
                  description: updatedStory.description,
                  priority: updatedStory.priority,
                  effortPoints: updatedStory.effortPoints,
                  status: updatedStory.status,
                  dependsOn: updatedStory.dependsOn || [], // Récupérer depuis la réponse
                }
              : us
          ),
        });
      }

      setEditingUserStory(null);
    } catch (error) {
      console.error("Erreur lors de la mise à jour de la User Story :", error);
      alert("Erreur lors de la mise à jour de la User Story.");
    }
  };

  // Delete user story
  const handleDeleteUserStory = async (id: number) => {
    if (!confirm("Voulez-vous vraiment supprimer cette User Story ?")) return;

    try {
      await axiosInstance.delete(
        `${PROJECT_SERVICE_URL}/api/projects/${projectId}/user-stories/${id}`,
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );
      setBacklog(backlog.filter((us) => us.id !== id));
      if (activeSprint) {
        setActiveSprint({
          ...activeSprint,
          userStories: activeSprint.userStories.filter((us) => us.id !== id),
        });
      }
    } catch (error) {
      console.error("Erreur lors de la suppression de la User Story :", error);
      alert("Erreur lors de la suppression de la User Story.");
    }
  };

  const handleOverlayClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) setIsModalOpen(false);
  };

  useEffect(() => {
    const fetchSprints = async () => {
      if (authLoading || !accessToken || !projectId) return;
      try {
        const response = await axiosInstance.get(
          `${PROJECT_SERVICE_URL}/api/projects/${projectId}/sprints`,
          { headers: { Authorization: `Bearer ${accessToken}` } }
        );
        const fetchedSprints = response.data.map((s: any) => ({
          id: s.id,
          name: s.name,
          startDate: s.startDate,
          endDate: s.endDate,
          goal: s.goal,
          capacity: s.capacity,
          status: s.status || "PLANNED", // Ajout du statut, avec "PLANNED" par défaut si absent
          userStories: [], // À remplir si l'API renvoie les user stories
        }));
        setSprints(fetchedSprints);
      } catch (error) {
        console.error("Erreur lors de la récupération des Sprints :", error);
        alert("Erreur lors du chargement des Sprints.");
      }
    };
    fetchSprints();
  }, [accessToken, authLoading, axiosInstance, projectId]);

  // Gestionnaire pour ajouter un sprint
  const handleAddSprint = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    try {
      const response = await axiosInstance.post(
        `${PROJECT_SERVICE_URL}/api/projects/${projectId}/sprints`,
        {
          name: newSprint.name,
          startDate: newSprint.startDate,
          endDate: newSprint.endDate,
          goal: newSprint.goal,
          status: "PLANNED", // Toujours "BACKLOG" pour une nouvelle user story
          capacity: newSprint.capacity,
        },
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );
      const createdSprint = { ...response.data, userStories: [] };
      setSprints([...sprints, createdSprint]);
      setNewSprint({
        id: 0,
        name: "",
        startDate: "",
        endDate: "",
        goal: "",
        capacity: 0,
        status: "PLANNED",
        userStories: [],
      }); // Réinitialiser après création
    } catch (error) {
      console.error("Erreur lors de la création du Sprint :", error);
      alert("Erreur lors de la création du Sprint.");
    }
  };

  // Gestionnaire pour mettre à jour un sprint
  const handleUpdateSprint = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!editingSprint) return;
    try {
      const response = await axiosInstance.put(
        `${PROJECT_SERVICE_URL}/api/projects/${projectId}/sprints/${editingSprint.id}`,
        {
          name: editingSprint.name,
          startDate: editingSprint.startDate,
          endDate: editingSprint.endDate,
          goal: editingSprint.goal,
          capacity: editingSprint.capacity,
        },
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );
      const updatedSprint = {
        ...response.data,
        userStories: editingSprint.userStories,
      };
      setSprints(
        sprints.map((s) => (s.id === updatedSprint.id ? updatedSprint : s))
      );
      if (activeSprint?.id === updatedSprint.id) setActiveSprint(updatedSprint);
      setEditingSprint(null); // Fermer le formulaire après mise à jour
    } catch (error) {
      console.error("Erreur lors de la mise à jour du Sprint :", error);
      alert("Erreur lors de la mise à jour du Sprint.");
    }
  };

  const getTasksByStatus = (status: "TO DO" | "IN PROGRESS" | "DONE") => {
    let filteredTasks = activeSprint
      ? tasks.filter(
          (task) => task.sprintId === activeSprint.id && task.status === status
        )
      : tasks.filter((task) => task.status === status);
    if (filterMyTasks) {
      // Since 'user' is removed, we'll skip filtering by user for now
      // You can reintroduce this logic if you add user data back to AuthContext
    }
    return filteredTasks;
  };

  if (loading) return <p>Loading...</p>;
  if (error) return <p>{error}</p>;

  return (
    <div className="project-container">
      {/* En-tête */}
      <div className="tasks-title-container">
        <span className="icon-projet">
          <i className="fa fa-list project-icon-card custom-project-icon"></i>
        </span>
        <h1 className="tasks-title">
          {projectName} {activeSprint && <span>- {activeSprint.name}</span>}
        </h1>
        <div className="team-list">
          {teamMembers.length > 0 ? (
            <div className="team-members-display">
              <div className="team-member-initial">
                {teamMembers[0].firstName.charAt(0) +
                  teamMembers[0].lastName.charAt(0)}
              </div>
              {teamMembers.length > 1 && (
                <div
                  className="team-member-more"
                  onClick={() => setShowAllMembers(!showAllMembers)}
                >
                  +{teamMembers.length - 1}..
                </div>
              )}
              {showAllMembers && teamMembers.length > 1 && (
                <div className="team-members-expanded">
                  <h5 className="team-members-expanded-firsttitle">
                    Project Members
                  </h5>
                  <div className="team-members-search">
                    <input
                      type="text"
                      placeholder="Rechercher des membres"
                      className="team-members-search-input"
                    />
                  </div>
                  <div className="team-members-expanded-list">
                    <h5 className="team-members-expanded-title">Manager</h5>
                    <div className="team-members-expanded-section">
                      {manager ? (
                        <div className="team-member-expanded-item">
                          <div className="team-member-expanded-initial-manager">
                            {manager.firstName.charAt(0) +
                              manager.lastName.charAt(0)}
                          </div>
                          <div className="team-member-expanded-info">
                            {manager.firstName} {manager.lastName}
                          </div>
                        </div>
                      ) : (
                        <p>No Manager found.</p>
                      )}
                    </div>
                    <h4 className="team-members-expanded-title">
                      Invited Memberes
                    </h4>
                    <div className="team-members-expanded-section">
                      {teamMembers.map((member) => (
                        <div
                          key={member.id}
                          className="team-member-expanded-item"
                        >
                          <div className="team-member-expanded-initial">
                            {member.firstName.charAt(0) +
                              member.lastName.charAt(0)}
                          </div>
                          <div className="team-member-expanded-info">
                            {member.firstName} {member.lastName}{" "}
                            <span className="team-member-expanded-role">
                              ({member.role})
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <p>No Team member found.</p>
          )}
          <button className="buton-share-style">
            <i className="fa fa-building"></i> Share
          </button>
          <button
            className="buton-share-style agile-toggle"
            onClick={() => setIsAgilePanelOpen(true)}
          >
            <i className="fa fa-rocket"></i> Agile
          </button>
        </div>
      </div>

      <div className="tasks-header">
        <div className="tasks-tabs">
          <button className="tasks-tab active">
            <i className="fa fa-list"></i> List
          </button>
          <button className="tasks-tab">
            <i className="fa fa-table"></i> Table
          </button>
          <button className="tasks-tab">
            <i className="fa fa-clock"></i> Chronology
          </button>
          <button className="tasks-tab">
            <i className="fa fa-chart-line"></i> Dashboard
          </button>
          <button className="tasks-tab">
            <i className="fa fa-calendar"></i> Calendar
          </button>
          <button className="tasks-tab">
            <i className="fa fa-comment"></i> Messages
          </button>
          <button className="tasks-tab">
            <i className="fa fa-file"></i> Files
          </button>
          <button className="tasks-tab">
            <i className="fa fa-plus"></i>
          </button>
        </div>
      </div>

      {/* Actions */}
      <div className="tasks-actions">
        <button
          className="tasks-add-button"
          onClick={() => setIsModalOpen(true)}
        >
          <i className="fa fa-plus"></i> Add Task{" "}
          <span className="second-button">
            {"       "}
            <i className="fa fa-caret-down"></i>
          </span>
        </button>
        <div className="tasks-options">
          <button className="tasks-option">
            <i className="fa fa-filter"></i> Filter
          </button>
          <button className="tasks-option">
            <i className="fa fa-sort"></i> Sort
          </button>
          <button
            className="tasks-option"
            onClick={() => setFilterMyTasks(!filterMyTasks)}
          >
            <i className="fa fa-user"></i> My Tasks
          </button>
          <button className="tasks-option">
            <i className="fa fa-gear"></i> Options
          </button>
        </div>
      </div>

      {/* En-tête de la liste */}
      <div className="tasks-list-header">
        <span className="tasks-list-header-item-principal">
          Task{"'"}s Name
        </span>
        <span className="tasks-list-header-item">Responsable</span>
        <span className="tasks-list-header-item">Due Date</span>
        <span className="tasks-list-header-item">Priority</span>
        <span className="tasks-list-header-item">Status</span>
      </div>

      {/* Liste des tâches */}
      <div className="tasks-list">
        {["À faire", "En cours", "Terminé"].map((status) => (
          <div key={status} className="dashboard-task-section">
            <h2
              className={`dashboard-task-section-title ${
                status === "En cours" || status === "Terminé"
                  ? "section-encours"
                  : ""
              }`}
            >
              <i className="fa fa-caret-down"></i> {status}
            </h2>
            {getTasksByStatus(status as "TO DO" | "IN PROGRESS" | "DONE").map(
              (task) => (
                <div key={task.id} className="task-row">
                  <div className="task-checkbox">
                    <input type="checkbox" checked={task.status === "DONE"} />
                    <span>{task.name}</span>
                  </div>
                  <div className="task-details">
                    <span className="task-responsible">
                      {task.responsible ? (
                        <span className="responsible-circle">
                          {task.responsible.charAt(0)}
                        </span>
                      ) : (
                        <span className="responsible-circle empty">?</span>
                      )}
                      {task.responsible || "Non assigné"}
                    </span>
                    <span className="task-due-date">{task.dueDate}</span>
                    <span className={`task-priority priority-${task.priority}`}>
                      {task.priority}
                    </span>
                    <span className="task-status">
                      {task.dueDate.includes("Aujourd'hui")
                        ? "Dans les délais"
                        : "En retard"}
                    </span>
                  </div>
                </div>
              )
            )}
            <button
              className="tasks-add-subtask"
              onClick={() => setIsModalOpen(true)}
            >
              Add Task...
            </button>
          </div>
        ))}
      </div>

      {/* Modal pour ajouter une tâche (page principale) */}
      {isModalOpen && (
        <div className="tasks-modal-overlay" onClick={handleOverlayClick}>
          <div className="tasks-modal">
            <h2 className="tasks-modal-title">Add Task</h2>
            <form onSubmit={(e) => handleAddTask(e)} className="tasks-form">
              <div className="tasks-form-group">
                <label htmlFor="name">Task{"'"}s Name</label>
                <input
                  type="text"
                  id="name"
                  value={newTask.name}
                  onChange={(e) =>
                    setNewTask({ ...newTask, name: e.target.value })
                  }
                  placeholder="Entrez le nom de la tâche"
                  required
                />
              </div>
              <div className="tasks-form-group">
                <label htmlFor="responsible">Responsable</label>
                <input
                  type="text"
                  id="responsible"
                  value={newTask.responsible || ""}
                  onChange={(e) =>
                    setNewTask({ ...newTask, responsible: e.target.value })
                  }
                  placeholder="Entrez le nom du responsable"
                />
              </div>
              <div className="tasks-form-group">
                <label htmlFor="dueDate">Due date</label>
                <input
                  type="text"
                  id="dueDate"
                  value={newTask.dueDate}
                  onChange={(e) =>
                    setNewTask({ ...newTask, dueDate: e.target.value })
                  }
                  placeholder="Ex: 5 - 7 fév"
                />
              </div>
              <div className="tasks-form-group">
                <label htmlFor="priority">Priority</label>
                <select
                  id="priority"
                  value={newTask.priority}
                  onChange={(e) =>
                    setNewTask({
                      ...newTask,
                      priority: e.target.value as
                        | "LOW"
                        | "MEDIUM"
                        | "HIGH"
                        | "",
                    })
                  }
                >
                  <option value="">Selecy a priority</option>
                  <option value="LOW">LOW</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HIGH">HIGH</option>
                </select>
              </div>
              <div className="tasks-form-group">
                <label htmlFor="status">Status</label>
                <select
                  id="status"
                  value={newTask.status}
                  onChange={(e) =>
                    setNewTask({
                      ...newTask,
                      status: e.target.value as
                        | "TO DO"
                        | "IN PROGRESS"
                        | "DONE",
                    })
                  }
                >
                  <option value="À faire">TO DO</option>
                  <option value="En cours">IN PROGRESS</option>
                  <option value="Terminé">DONE</option>
                </select>
              </div>
              <div className="tasks-modal-actions">
                <button type="submit" className="tasks-submit-button">
                  Add
                </button>
                <button
                  type="button"
                  className="tasks-cancel-button"
                  onClick={() => setIsModalOpen(false)}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {isAgilePanelOpen && (
        <div className="agile-sidebar" onClick={(e) => e.stopPropagation()}>
          <div className="sidebar-header">
            <h2>Agile Flow</h2>
            <button
              className="sidebar-close"
              onClick={() => setIsAgilePanelOpen(false)}
            >
              <i className="fa fa-times"></i>
            </button>
          </div>

          {/* Mini-Dashboard en haut */}
          <div className="sidebar-dashboard">
            <div className="dashboard-card-sprint">
              <i className="fa fa-calendar"></i>
              <span>Sprints: {sprints.length}</span>
            </div>
            <div className="dashboard-card-backlog">
              <i className="fa fa-tasks"></i>
              <span>
                Backlog: {backlog.filter((us) => !us.sprintId).length}
              </span>
            </div>
            <div className="dashboard-card-task">
              <i className="fa fa-clock"></i>
              <span>Tâches: {tasks.length}</span>
            </div>
          </div>

          <div className="sidebar-content">
            {/* Section 1 : Backlog & Stories */}
            <div className="sidebar-section">
              <div
                className="section-card"
                onClick={() =>
                  setExpandedSection(
                    expandedSection === "backlog" ? null : "backlog"
                  )
                }
              >
                <div className="section-icon">
                  <i className="fa fa-book"></i>
                </div>
                <div className="section-info">
                  <h3>Backlog & Stories</h3>
                  <p>{backlog.filter((us) => !us.sprintId).length} items</p>
                </div>
                <span
                  className={`chevron ${
                    expandedSection === "backlog" ? "open" : ""
                  }`}
                >
                  ▼
                </span>
              </div>
              {expandedSection === "backlog" && (
                <div className="section-body">
                  {/* Formulaire */}
                  <div className="story-form">
                    <h4>{editingUserStory ? "Edit Story" : "New Story"}</h4>
                    <form
                      onSubmit={
                        editingUserStory
                          ? handleUpdateUserStory
                          : handleAddUserStory
                      }
                      className="modern-form"
                    >
                      <input
                        type="text"
                        placeholder="Story Title"
                        value={
                          editingUserStory
                            ? editingUserStory.name
                            : newUserStory.name
                        }
                        onChange={(e) =>
                          editingUserStory
                            ? setEditingUserStory({
                                ...editingUserStory,
                                name: e.target.value,
                              })
                            : setNewUserStory({
                                ...newUserStory,
                                name: e.target.value,
                              })
                        }
                        required
                      />
                      <textarea
                        placeholder="Description (optional)"
                        value={
                          editingUserStory
                            ? editingUserStory.description
                            : newUserStory.description
                        }
                        onChange={(e) =>
                          editingUserStory
                            ? setEditingUserStory({
                                ...editingUserStory,
                                description: e.target.value,
                              })
                            : setNewUserStory({
                                ...newUserStory,
                                description: e.target.value,
                              })
                        }
                      />
                      <div className="form-row">
                        <select
                          value={
                            editingUserStory
                              ? editingUserStory.priority
                              : newUserStory.priority
                          }
                          onChange={(e) =>
                            editingUserStory
                              ? setEditingUserStory({
                                  ...editingUserStory,
                                  priority: e.target.value as
                                    | "LOW"
                                    | "MEDIUM"
                                    | "HIGH"
                                    | "",
                                })
                              : setNewUserStory({
                                  ...newUserStory,
                                  priority: e.target.value as
                                    | "LOW"
                                    | "MEDIUM"
                                    | "HIGH"
                                    | "",
                                })
                          }
                        >
                          <option value="">Priority</option>
                          <option value="LOW">Low</option>
                          <option value="MEDIUM">Medium</option>
                          <option value="HIGH">High</option>
                        </select>
                        <input
                          type="number"
                          placeholder="Effort (pts)"
                          value={
                            editingUserStory
                              ? editingUserStory.effortPoints
                              : newUserStory.effortPoints || ""
                          }
                          onChange={(e) =>
                            editingUserStory
                              ? setEditingUserStory({
                                  ...editingUserStory,
                                  effortPoints: parseInt(e.target.value) || 0,
                                })
                              : setNewUserStory({
                                  ...newUserStory,
                                  effortPoints: parseInt(e.target.value) || 0,
                                })
                          }
                          min="0"
                        />
                      </div>
                      <div className="twobuttons">
                        <button type="submit" className="action-btn primary">
                          {editingUserStory ? "Update" : "Add"}
                        </button>
                        {editingUserStory && (
                          <button
                            type="button"
                            className="action-btn secondary-cancel"
                            onClick={() => setEditingUserStory(null)}
                          >
                            Cancel
                          </button>
                        )}
                      </div>
                    </form>
                  </div>

                  {/* Séparateur et Liste */}
                  <div className="section-divider">
                    <span>Stories List</span>
                  </div>
                  <div className="story-list">
                    {backlog.length > 0 ? ( // Vérifier si backlog a des éléments au départ
                      <>
                        <div className="search-container">
                          <div className="search-input-wrapper">
                            <i className="fa fa-search search-icon"></i>
                            <input
                              type="text"
                              placeholder="Search by title"
                              value={searchQuery}
                              onChange={handleSearchChange}
                              className="search-input"
                            />
                          </div>
                        </div>

                        {filteredBacklog.length > 0 ? (
                          <>
                            <table className="story-table">
                              <thead>
                                <tr>
                                  <th>Title</th>
                                  <th>Priority</th>
                                  <th>Effort</th>
                                  <th>Status</th>
                                  <th>Dep</th>
                                  <th>Actions</th>
                                </tr>
                              </thead>
                              <tbody>
                                {currentStories.map((story) => (
                                  <React.Fragment key={story.id}>
                                    <tr>
                                      <td>{story.name}</td>
                                      <td
                                        className={`priority-${story.priority.toLowerCase()}`}
                                      >
                                        {story.priority}
                                      </td>
                                      <td>{story.effortPoints} pts</td>
                                      <td>{story.status || "BACKLOG"}</td>
                                      <td>
                                        <button
                                          className="story-count-btn"
                                          onClick={() =>
                                            setExpandedDependsOn(
                                              expandedDependsOn === story.id
                                                ? null
                                                : story.id
                                            )
                                          }
                                        >
                                          {story.dependsOn?.length || 0}{" "}
                                          <i
                                            className={`fa ${
                                              expandedDependsOn === story.id
                                                ? "fa-chevron-up"
                                                : "fa-chevron-down"
                                            }`}
                                          ></i>
                                        </button>
                                      </td>
                                      <td className="action-cell">
                                        <button
                                          onClick={() =>
                                            handleEditUserStory(story)
                                          }
                                          className="icon-btn edit"
                                        >
                                          <i className="fa fa-edit"></i>
                                        </button>
                                        <button
                                          onClick={() =>
                                            handleDeleteUserStory(story.id)
                                          }
                                          className="icon-btn delete"
                                        >
                                          <i className="fa fa-trash"></i>
                                        </button>
                                        <select
                                          onChange={async (e) => {
                                            const sprintId = parseInt(
                                              e.target.value
                                            );
                                            if (sprintId) {
                                              const sprint = sprints.find(
                                                (s) => s.id === sprintId
                                              );
                                              if (sprint) {
                                                const currentEffort =
                                                  sprint.userStories.reduce(
                                                    (sum, us) =>
                                                      sum + us.effortPoints,
                                                    0
                                                  );
                                                if (
                                                  currentEffort +
                                                    story.effortPoints <=
                                                  sprint.capacity
                                                ) {
                                                  try {
                                                    const response =
                                                      await axiosInstance.put(
                                                        `${PROJECT_SERVICE_URL}/api/projects/${projectId}/user-stories/${story.id}/assign-sprint/${sprintId}`,
                                                        {},
                                                        {
                                                          headers: {
                                                            Authorization: `Bearer ${accessToken}`,
                                                          },
                                                        }
                                                      );
                                                    const updatedStory: UserStory =
                                                      {
                                                        ...story,
                                                        sprintId,
                                                        status:
                                                          sprint.status ===
                                                          "ACTIVE"
                                                            ? "IN_PROGRESS"
                                                            : "SELECTED_FOR_SPRINT",
                                                      };
                                                    setBacklog(
                                                      backlog.map((us) =>
                                                        us.id === story.id
                                                          ? updatedStory
                                                          : us
                                                      )
                                                    );
                                                    setSprints(
                                                      sprints.map((s) =>
                                                        s.id === sprintId
                                                          ? {
                                                              ...s,
                                                              userStories: [
                                                                ...s.userStories,
                                                                updatedStory,
                                                              ],
                                                            }
                                                          : s
                                                      )
                                                    );
                                                  } catch (error) {
                                                    console.error(
                                                      "Erreur lors de l'assignation au Sprint :",
                                                      error
                                                    );
                                                    alert(
                                                      "Erreur lors de l'assignation de la User Story au Sprint : " +
                                                        (error.response?.data
                                                          ?.message ||
                                                          error.message)
                                                    );
                                                  }
                                                } else {
                                                  alert(
                                                    "Sprint capacity insufficient!"
                                                  );
                                                }
                                              }
                                            }
                                          }}
                                          value=""
                                          className="sprint-select"
                                        >
                                          <option value="">To Sprint</option>
                                          {sprints.map((sprint) => (
                                            <option
                                              key={sprint.id}
                                              value={sprint.id}
                                            >
                                              {sprint.name}
                                            </option>
                                          ))}
                                        </select>
                                      </td>
                                    </tr>
                                    {expandedDependsOn === story.id && (
                                      <tr>
                                        <td colSpan={6}>
                                          <div className="sprint-stories-list">
                                            <table className="inner-story-table">
                                              <thead>
                                                <tr>
                                                  <th>Nom</th>
                                                  <th>Statut</th>
                                                  <th>Action</th>
                                                </tr>
                                              </thead>
                                              <tbody>
                                                {story.dependsOn &&
                                                story.dependsOn.length > 0 ? (
                                                  story.dependsOn.map(
                                                    (depId) => {
                                                      const depStory =
                                                        backlog.find(
                                                          (us) =>
                                                            us.id === depId
                                                        );
                                                      return (
                                                        <tr key={depId}>
                                                          <td>
                                                            {depStory
                                                              ? depStory.name
                                                              : `US${depId}`}
                                                          </td>
                                                          <td>
                                                            {depStory
                                                              ? depStory.status
                                                              : "Inconnu"}
                                                            {depStory &&
                                                              depStory.status !==
                                                                "DONE" && (
                                                                <span className="blocked-indicator">
                                                                  {" "}
                                                                  (Bloque)
                                                                </span>
                                                              )}
                                                          </td>
                                                          <td>
                                                            <div className="action-cell-inner">
                                                              <button
                                                                className="icon-btn delete"
                                                                onClick={async () => {
                                                                  const newDependsOn =
                                                                    (
                                                                      story.dependsOn ||
                                                                      []
                                                                    ).filter(
                                                                      (id) =>
                                                                        id !==
                                                                        depId
                                                                    );
                                                                  try {
                                                                    const response =
                                                                      await axiosInstance.put(
                                                                        `${PROJECT_SERVICE_URL}/api/projects/${projectId}/user-stories/${story.id}/dependencies`,
                                                                        {
                                                                          dependsOn:
                                                                            newDependsOn,
                                                                        },
                                                                        {
                                                                          headers:
                                                                            {
                                                                              Authorization: `Bearer ${accessToken}`,
                                                                            },
                                                                        }
                                                                      );
                                                                    const updatedStory =
                                                                      response.data; // Retourne UserStoryDTO avec statut recalculé
                                                                    setBacklog(
                                                                      backlog.map(
                                                                        (us) =>
                                                                          us.id ===
                                                                          story.id
                                                                            ? updatedStory
                                                                            : us
                                                                      )
                                                                    );
                                                                  } catch (error) {
                                                                    console.error(
                                                                      "Erreur lors de la suppression de la dépendance :",
                                                                      error
                                                                    );
                                                                    alert(
                                                                      "Erreur lors de la suppression de la dépendance."
                                                                    );
                                                                  }
                                                                }}
                                                              >
                                                                <i className="fa fa-times"></i>
                                                              </button>
                                                              <select
                                                                onChange={async (
                                                                  e
                                                                ) => {
                                                                  const newDepId =
                                                                    parseInt(
                                                                      e.target
                                                                        .value
                                                                    );
                                                                  if (
                                                                    newDepId
                                                                  ) {
                                                                    const newDependsOn =
                                                                      [
                                                                        ...(story.dependsOn ||
                                                                          []),
                                                                        newDepId,
                                                                      ];
                                                                    try {
                                                                      const response =
                                                                        await axiosInstance.put(
                                                                          `${PROJECT_SERVICE_URL}/api/projects/${projectId}/user-stories/${story.id}/dependencies`,
                                                                          {
                                                                            dependsOn:
                                                                              newDependsOn,
                                                                          },
                                                                          {
                                                                            headers:
                                                                              {
                                                                                Authorization: `Bearer ${accessToken}`,
                                                                              },
                                                                          }
                                                                        );
                                                                      const updatedStory =
                                                                        response.data;
                                                                      setBacklog(
                                                                        backlog.map(
                                                                          (
                                                                            us
                                                                          ) =>
                                                                            us.id ===
                                                                            story.id
                                                                              ? updatedStory
                                                                              : us
                                                                        )
                                                                      );
                                                                      e.target.value =
                                                                        ""; // Réinitialiser le select
                                                                    } catch (error) {
                                                                      console.error(
                                                                        "Erreur lors de l’ajout de la dépendance :",
                                                                        error
                                                                      );
                                                                      alert(
                                                                        "Erreur lors de l’ajout de la dépendance."
                                                                      );
                                                                    }
                                                                  }
                                                                }}
                                                                value=""
                                                              >
                                                                <option value="">
                                                                  Add dependency
                                                                </option>
                                                                {backlog
                                                                  .filter(
                                                                    (us) =>
                                                                      us.id !==
                                                                        story.id &&
                                                                      !story.dependsOn?.includes(
                                                                        us.id
                                                                      )
                                                                  )
                                                                  .map((us) => (
                                                                    <option
                                                                      key={
                                                                        us.id
                                                                      }
                                                                      value={
                                                                        us.id
                                                                      }
                                                                    >
                                                                      {us.name}
                                                                    </option>
                                                                  ))}
                                                              </select>
                                                            </div>
                                                          </td>
                                                        </tr>
                                                      );
                                                    }
                                                  )
                                                ) : (
                                                  <tr>
                                                    <td colSpan={2}>
                                                      Aucune dépendance
                                                    </td>
                                                    <td>
                                                      <select
                                                        onChange={async (e) => {
                                                          const newDepId =
                                                            parseInt(
                                                              e.target.value
                                                            );
                                                          if (newDepId) {
                                                            const newDependsOn =
                                                              [
                                                                ...(story.dependsOn ||
                                                                  []),
                                                                newDepId,
                                                              ];
                                                            try {
                                                              const response =
                                                                await axiosInstance.put(
                                                                  `${PROJECT_SERVICE_URL}/api/projects/${projectId}/user-stories/${story.id}/dependencies`,
                                                                  {
                                                                    dependsOn:
                                                                      newDependsOn,
                                                                  },
                                                                  {
                                                                    headers: {
                                                                      Authorization: `Bearer ${accessToken}`,
                                                                    },
                                                                  }
                                                                );
                                                              const updatedStory =
                                                                response.data;
                                                              setBacklog(
                                                                backlog.map(
                                                                  (us) =>
                                                                    us.id ===
                                                                    story.id
                                                                      ? updatedStory
                                                                      : us
                                                                )
                                                              );
                                                              e.target.value =
                                                                "";
                                                            } catch (error) {
                                                              console.error(
                                                                "Erreur lors de l’ajout de la dépendance :",
                                                                error
                                                              );
                                                              alert(
                                                                "Erreur lors de l’ajout de la dépendance."
                                                              );
                                                            }
                                                          }
                                                        }}
                                                        value=""
                                                      >
                                                        <option value="">
                                                          Add dependency
                                                        </option>
                                                        {backlog
                                                          .filter(
                                                            (us) =>
                                                              us.id !==
                                                                story.id &&
                                                              !story.dependsOn?.includes(
                                                                us.id
                                                              )
                                                          )
                                                          .map((us) => (
                                                            <option
                                                              key={us.id}
                                                              value={us.id}
                                                            >
                                                              {us.name}
                                                            </option>
                                                          ))}
                                                      </select>
                                                    </td>
                                                  </tr>
                                                )}
                                              </tbody>
                                            </table>
                                          </div>
                                        </td>
                                      </tr>
                                    )}
                                  </React.Fragment>
                                ))}
                              </tbody>
                            </table>

                            {totalItems > itemsPerPage && (
                              <div className="pagination-container">
                                <button
                                  onClick={goToPreviousPage}
                                  disabled={currentPage === 1}
                                  className="pagination-btn"
                                >
                                  <i className="fa fa-arrow-left"></i> Previous
                                </button>
                                <span className="pagination-info">
                                  Page {currentPage} of {totalPages}
                                </span>
                                <button
                                  onClick={goToNextPage}
                                  disabled={currentPage === totalPages}
                                  className="pagination-btn"
                                >
                                  Next <i className="fa fa-arrow-right"></i>
                                </button>
                              </div>
                            )}
                          </>
                        ) : (
                          <p className="empty-text">
                            No matching stories found.
                          </p>
                        )}
                      </>
                    ) : (
                      <p className="empty-text">
                        No stories yet. Add one to get started!
                      </p>
                    )}
                  </div>
                </div>
              )}
            </div>

            {/* Section 2 : Sprints & Tasks */}
            <div className="sidebar-section">
              <div
                className="section-card"
                onClick={() =>
                  setExpandedSection(
                    expandedSection === "sprints" ? null : "sprints"
                  )
                }
              >
                <div className="section-icon">
                  <i className="fa fa-rocket"></i>
                </div>
                <div className="section-info">
                  <h3>Sprints & Tasks</h3>
                  <p>{sprints.length} sprints</p>
                </div>
                <span
                  className={`chevron ${
                    expandedSection === "sprints" ? "open" : ""
                  }`}
                >
                  ▼
                </span>
              </div>
              {expandedSection === "sprints" && (
                <div className="section-body">
                  <div className="sprint-form">
                    <h4>{editingSprint ? "Edit Sprint" : "New Sprint"}</h4>
                    <form
                      onSubmit={
                        editingSprint ? handleUpdateSprint : handleAddSprint
                      }
                      className="modern-form"
                    >
                      <input
                        type="text"
                        name="name"
                        placeholder="Sprint Name"
                        value={
                          editingSprint ? editingSprint.name : newSprint.name
                        }
                        onChange={(e) =>
                          editingSprint
                            ? setEditingSprint({
                                ...editingSprint,
                                name: e.target.value,
                              })
                            : setNewSprint({
                                ...newSprint,
                                name: e.target.value,
                              })
                        }
                        required
                      />
                      <input
                        type="date"
                        name="startDate"
                        value={
                          editingSprint
                            ? editingSprint.startDate
                            : newSprint.startDate
                        }
                        onChange={(e) =>
                          editingSprint
                            ? setEditingSprint({
                                ...editingSprint,
                                startDate: e.target.value,
                              })
                            : setNewSprint({
                                ...newSprint,
                                startDate: e.target.value,
                              })
                        }
                        required
                      />
                      <input
                        type="date"
                        name="endDate"
                        value={
                          editingSprint
                            ? editingSprint.endDate
                            : newSprint.endDate
                        }
                        onChange={(e) =>
                          editingSprint
                            ? setEditingSprint({
                                ...editingSprint,
                                endDate: e.target.value,
                              })
                            : setNewSprint({
                                ...newSprint,
                                endDate: e.target.value,
                              })
                        }
                        required
                      />
                      <textarea
                        name="goal"
                        placeholder="Sprint| Sprint Goal"
                        value={
                          editingSprint ? editingSprint.goal : newSprint.goal
                        }
                        onChange={(e) =>
                          editingSprint
                            ? setEditingSprint({
                                ...editingSprint,
                                goal: e.target.value,
                              })
                            : setNewSprint({
                                ...newSprint,
                                goal: e.target.value,
                              })
                        }
                      />
                      <input
                        type="number"
                        name="capacity"
                        placeholder="Capacity (pts)"
                        value={
                          editingSprint
                            ? editingSprint.capacity
                            : newSprint.capacity
                        }
                        onChange={(e) =>
                          editingSprint
                            ? setEditingSprint({
                                ...editingSprint,
                                capacity: parseInt(e.target.value) || 0,
                              })
                            : setNewSprint({
                                ...newSprint,
                                capacity: parseInt(e.target.value) || 0,
                              })
                        }
                        min="0"
                        required
                      />
                      <div className="twobuttons">
                        <button type="submit" className="action-btn primary">
                          {editingSprint ? "Update" : "Create"}
                        </button>
                        {editingSprint && (
                          <button
                            type="button"
                            className="action-btn secondary"
                            onClick={() => setEditingSprint(null)}
                          >
                            Cancel
                          </button>
                        )}
                      </div>
                    </form>
                  </div>

                  <div className="section-divider">
                    <span>Sprints List</span>
                  </div>
                  <div className="sprint-list">
                    {sprints.length > 0 ? (
                      <>
                        {/* Barre de recherche pour les sprints */}
                        <div className="search-container">
                          <div className="search-input-wrapper">
                            <i className="fa fa-search search-icon"></i>
                            <input
                              type="text"
                              placeholder="Search by name"
                              value={sprintSearchQuery}
                              onChange={handleSprintSearchChange}
                              className="search-input"
                            />
                          </div>
                        </div>

                        <table className="sprint-table">
                          <thead>
                            <tr>
                              <th>Name</th>
                              <th>Dates</th>
                              <th>Goal</th>
                              <th>Capacity</th>
                              <th>Status</th>
                              <th>Stories</th>
                              <th>Actions</th>
                            </tr>
                          </thead>
                          <tbody>
                            {currentSprints.map((sprint) => (
                              <React.Fragment key={sprint.id}>
                                <tr>
                                  <td>{sprint.name}</td>
                                  <td>
                                    {new Date(
                                      sprint.startDate
                                    ).toLocaleDateString("en-US", {
                                      month: "short",
                                      day: "2-digit",
                                    })}{" "}
                                    -{" "}
                                    {new Date(
                                      sprint.endDate
                                    ).toLocaleDateString("en-US", {
                                      month: "short",
                                      day: "2-digit",
                                    })}
                                  </td>
                                  <td>{sprint.goal || "No goal set"}</td>
                                  <td>{sprint.capacity} pts</td>
                                  <td>{sprint.status}</td>
                                  <td>
                                    <button
                                      className="story-count-btn"
                                      onClick={() =>
                                        setExpandedSprintStories(
                                          expandedSprintStories === sprint.id
                                            ? null
                                            : sprint.id
                                        )
                                      }
                                    >
                                      {sprint.userStories.length}{" "}
                                      <i
                                        className={`fa ${
                                          expandedSprintStories === sprint.id
                                            ? "fa-chevron-up"
                                            : "fa-chevron-down"
                                        }`}
                                      ></i>
                                    </button>
                                  </td>
                                  <td className="action-cell">
                                    <button
                                      onClick={() => handleEditSprint(sprint)}
                                      className="icon-btn edit"
                                    >
                                      <i className="fa fa-edit"></i>
                                    </button>
                                    <button
                                      onClick={() =>
                                        handleDeleteSprint(sprint.id)
                                      }
                                      className="icon-btn delete"
                                    >
                                      <i className="fa fa-trash"></i>
                                    </button>
                                    <button
                                      onClick={() =>
                                        handleCancelSprint(sprint.id)
                                      }
                                      className="icon-btn cancel"
                                      disabled={
                                        sprint.status === "COMPLETED" ||
                                        sprint.status === "CANCELED" ||
                                        sprint.status === "ARCHIVED"
                                      }
                                    >
                                      <i className="fa fa-stop"></i>
                                    </button>
                                    <button
                                      onClick={() =>
                                        handleArchiveSprint(sprint.id)
                                      }
                                      className="icon-btn archive"
                                      disabled={
                                        sprint.status !== "COMPLETED" &&
                                        sprint.status !== "CANCELED"
                                      }
                                    >
                                      <i className="fa fa-archive"></i>
                                    </button>
                                    <div className="tooltip-container">
                                      <button
                                        onClick={() => setActiveSprint(sprint)}
                                        className={`icon-btn activate ${
                                          activeSprint?.id === sprint.id
                                            ? "active"
                                            : ""
                                        }`}
                                      >
                                        <i className="fa fa-play"></i>
                                      </button>
                                      <span className="tooltip-text">
                                        Activer ce sprint
                                      </span>
                                    </div>
                                  </td>
                                </tr>
                                {expandedSprintStories === sprint.id && (
                                  <tr>
                                    <td colSpan={6}>
                                      <div className="sprint-stories-list">
                                        {sprint.userStories.length > 0 ? (
                                          <table className="inner-story-table">
                                            <thead>
                                              <tr>
                                                <th>Name</th>
                                                <th>Effort (pts)</th>
                                                <th>Status</th>
                                                <th>Action</th>
                                              </tr>
                                            </thead>
                                            <tbody>
                                              {sprint.userStories.map(
                                                (story) => (
                                                  <tr key={story.id}>
                                                    <td>{story.name}</td>
                                                    <td>
                                                      {story.effortPoints}
                                                    </td>
                                                    <td>
                                                      {story.status ||
                                                        "IN_SPRINT"}
                                                    </td>
                                                    <td>
                                                      <button
                                                        onClick={async () => {
                                                          try {
                                                            await axiosInstance.put(
                                                              `${PROJECT_SERVICE_URL}/api/projects/${projectId}/user-stories/${story.id}/remove-sprint`,
                                                              {},
                                                              {
                                                                headers: {
                                                                  Authorization: `Bearer ${accessToken}`,
                                                                },
                                                              }
                                                            );
                                                            const updatedStory: UserStory =
                                                              {
                                                                ...story,
                                                                sprintId:
                                                                  undefined,
                                                                status:
                                                                  "BACKLOG",
                                                              };
                                                            setBacklog([
                                                              ...backlog,
                                                              updatedStory,
                                                            ]);
                                                            setSprints(
                                                              sprints.map((s) =>
                                                                s.id ===
                                                                sprint.id
                                                                  ? {
                                                                      ...s,
                                                                      userStories:
                                                                        s.userStories.filter(
                                                                          (
                                                                            us
                                                                          ) =>
                                                                            us.id !==
                                                                            story.id
                                                                        ),
                                                                    }
                                                                  : s
                                                              )
                                                            );
                                                            if (
                                                              activeSprint?.id ===
                                                              sprint.id
                                                            ) {
                                                              setActiveSprint({
                                                                ...activeSprint,
                                                                userStories:
                                                                  activeSprint.userStories.filter(
                                                                    (us) =>
                                                                      us.id !==
                                                                      story.id
                                                                  ),
                                                              });
                                                            }
                                                          } catch (error) {
                                                            console.error(
                                                              "Erreur lors du retrait du Sprint :",
                                                              error
                                                            );
                                                            alert(
                                                              "Erreur lors du retrait de la User Story du Sprint."
                                                            );
                                                          }
                                                        }}
                                                        className="icon-btn delete"
                                                      >
                                                        <i className="fa fa-times"></i>
                                                      </button>
                                                    </td>
                                                  </tr>
                                                )
                                              )}
                                            </tbody>
                                          </table>
                                        ) : (
                                          <p>
                                            No user stories assigned to this
                                            sprint.
                                          </p>
                                        )}
                                      </div>
                                    </td>
                                  </tr>
                                )}
                              </React.Fragment>
                            ))}
                          </tbody>
                        </table>

                        {/* Pagination pour les sprints */}
                        {totalSprintItems > itemsPerPage && (
                          <div className="pagination-container">
                            <button
                              onClick={goToPreviousSprintPage}
                              disabled={currentSprintPage === 1}
                              className="pagination-btn"
                            >
                              <i className="fa fa-arrow-left"></i> Prev
                            </button>
                            <span className="pagination-info">
                              Page {currentSprintPage} of {totalSprintPages}
                            </span>
                            <button
                              onClick={goToNextSprintPage}
                              disabled={currentSprintPage === totalSprintPages}
                              className="pagination-btn"
                            >
                              Next <i className="fa fa-arrow-right"></i>
                            </button>
                          </div>
                        )}
                      </>
                    ) : (
                      <p className="empty-text">
                        No sprints yet. Create one to start planning!
                      </p>
                    )}
                  </div>

                  {activeSprint && (
                    <div className="active-sprint">
                      <h4>
                        <i className="fa fa-rocket"></i> Sprint actif :{" "}
                        {activeSprint.name}
                      </h4>
                      <p className="sprint-goal">
                        Objectif :{" "}
                        <strong>
                          {activeSprint.goal || "Aucun objectif défini"}
                        </strong>
                      </p>

                      <div className="sprint-progress">
                        <div className="progress-bar">
                          <div
                            className="progress"
                            style={{
                              width: `${
                                (activeSprint.userStories.reduce(
                                  (sum, us) => sum + us.effortPoints,
                                  0
                                ) /
                                  activeSprint.capacity) *
                                100
                              }%`,
                            }}
                          ></div>
                        </div>
                        <p className="capacity-info">
                          <i className="fas fa-sync-alt"></i> Used Capacity :{" "}
                          <strong>
                            {activeSprint.userStories.reduce(
                              (sum, us) => sum + us.effortPoints,
                              0
                            )}
                          </strong>{" "}
                          / {activeSprint.capacity} pts
                        </p>
                      </div>

                      <table className="story-table">
                        <thead>
                          <tr>
                            <th>User Story</th>
                            <th>Effort (pts)</th>
                            <th>Action</th>
                          </tr>
                        </thead>
                        <tbody>
                          {activeSprint.userStories.map((story) => (
                            <tr key={story.id}>
                              <td>{story.name}</td>
                              <td>{story.effortPoints}</td>
                              <td>
                                <button
                                  onClick={() =>
                                    setSelectedUserStoryId(story.id)
                                  }
                                  className="btn-add-task"
                                >
                                  + Task
                                </button>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Section 3 : Quick Actions */}
            <div className="sidebar-section quick-actions">
              <h3>Quick Actions</h3>
              <button
                className="action-btn primary-sprint"
                onClick={() => setExpandedSection("sprints")}
              >
                New Sprint
              </button>
              <button
                className="action-btn primary-backlog"
                onClick={() => setExpandedSection("backlog")}
              >
                Add Story
              </button>

              <button className="action-btn secondary">Export Data</button>
            </div>
          </div>

          {/* Mini-modal pour ajouter une tâche */}
          {selectedUserStoryId && (
            <div
              className="modal-overlay"
              onClick={() => setSelectedUserStoryId(null)}
            >
              <div
                className="modal-content"
                onClick={(e) => e.stopPropagation()}
              >
                <h4>Add Task</h4>
                <form
                  onSubmit={(e) => handleAddTask(e, selectedUserStoryId)}
                  className="modern-form"
                >
                  <input
                    type="text"
                    placeholder="Task Name"
                    value={newTask.name}
                    onChange={(e) =>
                      setNewTask({ ...newTask, name: e.target.value })
                    }
                    required
                  />
                  <input
                    type="text"
                    placeholder="Responsible"
                    value={newTask.responsible || ""}
                    onChange={(e) =>
                      setNewTask({ ...newTask, responsible: e.target.value })
                    }
                  />
                  <input
                    type="date"
                    id="dueDate"
                    value={newTask.dueDate} // garder la date brute ici
                    onChange={(e) =>
                      setNewTask({ ...newTask, dueDate: e.target.value })
                    }
                  />

                  <select
                    value={newTask.priority}
                    onChange={(e) =>
                      setNewTask({
                        ...newTask,
                        priority: e.target.value as
                          | "LOW"
                          | "MEDIUM"
                          | "HIGH"
                          | "",
                      })
                    }
                  >
                    <option value="">Priority</option>
                    <option value="LOW">Low</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High</option>
                  </select>
                  <select
                    value={newTask.status}
                    onChange={(e) =>
                      setNewTask({
                        ...newTask,
                        status: e.target.value as
                          | "TO DO"
                          | "IN PROGRESS"
                          | "DONE",
                      })
                    }
                  >
                    <option value="TO DO">To Do</option>
                    <option value="IN PROGRESS">In Progress</option>
                    <option value="DONE">Done</option>
                  </select>
                  <div className="modal-actions">
                    <button type="submit" className="action-btn primary">
                      Add
                    </button>
                    <button
                      type="button"
                      className="action-btn secondary"
                      onClick={() => setSelectedUserStoryId(null)}
                    >
                      Cancel
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
