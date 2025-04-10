"use client";
import React, { useState, useEffect } from "react";
import "../../../../../styles/Dashboard-Project.css";
import { useAuth } from "../../../../../context/AuthContext";
import useAxios from "../../../../../hooks/useAxios";
import { AxiosError } from "axios"; // Ajoutez cet import en haut du fichier
import {
  AUTH_SERVICE_URL,
  PROJECT_SERVICE_URL,
} from "../../../../../config/useApi";
import { useParams } from "next/navigation";
import { useCallback } from "react"; // Ajoutez cet import si ce n'est pas déjà fait
// Interfaces
interface Task {
  id: number;
  name: string;
  responsible: string | null;
  dueDate: string;
  priority: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL" | "";
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
  title: string;
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
  priority: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL" | "";
  effortPoints: number;
  sprintId?: number;
  dependsOn?: number[]; // Ajout pour les dépendances (comme discuté précédemment)
  tags?: string[];
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

interface History {
  id: number;
  action: string; // ex: "CREATE", "UPDATE", "DELETE"
  date: string; // LocalDateTime sera converti en string par l'API (ex: "2025-04-08T10:00:00")
  authorFullName: string;
  description: string;
}

interface UserStoryHistory extends History {
  userStoryId: number;
}

interface SprintHistory extends History {
  sprintId: number;
}
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
      const nameMatch = us.title
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
    title: "",
    description: "",
    status: "BACKLOG",
    priority: "",
    effortPoints: 0,
    dependsOn: [],
    tags: [],
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
  const [activeSprint, setActiveSprint] = useState<Sprint | null>(null);
  const [selectedUserStoryId, setSelectedUserStoryId] = useState<number | null>(
    null
  ); // Pour le mini-modal

  const [isHistoryModalOpen, setIsHistoryModalOpen] = useState(false);
  const [selectedItem, setSelectedItem] = useState<{
    id: number;
    type: "userStory" | "sprint";
    title: string
  } | null>(null);
  const [history, setHistory] = useState<(UserStoryHistory | SprintHistory)[]>(
    []
  );

  const fetchHistory = async (id: number, type: "userStory" | "sprint") => {
    try {
      const endpoint =
        type === "userStory"
          ? `${PROJECT_SERVICE_URL}/api/projects/user-story/${id}/history`
          : `${PROJECT_SERVICE_URL}/api/projects/sprint/${id}/history`;
      const response = await axiosInstance.get(endpoint, {
        headers: { Authorization: `Bearer ${accessToken}` },
      });
      setHistory(response.data); // Stocke l'historique
      setIsHistoryModalOpen(true); // Ouvre la modale
    } catch (error) {
      console.error(
        `Erreur lors de la récupération de l'historique ${type} :`,
        error
      );
      alert("Erreur lors du chargement de l'historique.");
    }
  };

  // Fonction pour éditer une User Story
  const handleEditUserStory = (story: UserStory) => {
    console.log("Édition de la User Story sélectionnée :", story);
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

    console.log("newUserStory avant envoi :", newUserStory);

    try {
      const response = await axiosInstance.post(
        `${PROJECT_SERVICE_URL}/api/projects/${projectId}/user-stories`,
        {
          title: newUserStory.title,
          description: newUserStory.description,
          priority: newUserStory.priority,
          status: "BACKLOG",
          effortPoints: newUserStory.effortPoints,
          dependsOn: newUserStory.dependsOn || [],
          tags: newUserStory.tags || [],
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
          title: createdUserStory.title,
          description: createdUserStory.description,
          status: createdUserStory.status,
          priority: createdUserStory.priority,
          effortPoints: createdUserStory.effortPoints,
          dependsOn: createdUserStory.dependsOn || [],
          tags: createdUserStory.tags || [],
        },
      ]);
      setNewUserStory({
        id: 0,
        title: "",
        description: "",
        status: "BACKLOG",
        priority: "",
        effortPoints: 0,
        dependsOn: [],
        tags: [],
      });
    } catch (error) {
      console.error("Erreur lors de la création de la User Story :", error);
      alert("Erreur lors de l'ajout de la User Story.");
    }
  };

  // Update user story
  const handleUpdateUserStory = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!editingUserStory) return;
    console.log("Mise à jour de la User Story :", {
      projectId,
      userStoryId: editingUserStory.id,
      data: {
        title: editingUserStory.title,
        description: editingUserStory.description,
        priority: editingUserStory.priority,
        effortPoints: editingUserStory.effortPoints,
        dependsOn: editingUserStory.dependsOn || [],
        tags: editingUserStory.tags || [],
      },
    });
    try {
      const response = await axiosInstance.put(
        `${PROJECT_SERVICE_URL}/api/projects/${projectId}/user-stories/${editingUserStory.id}`,
        {
          title: editingUserStory.title,
          description: editingUserStory.description,
          priority: editingUserStory.priority,
          effortPoints: editingUserStory.effortPoints,
          dependsOn: editingUserStory.dependsOn || [],
          tags: editingUserStory.tags || [],
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
                title: updatedStory.title,
                description: updatedStory.description,
                priority: updatedStory.priority,
                effortPoints: updatedStory.effortPoints,
                status: updatedStory.status,
                dependsOn: updatedStory.dependsOn || [],
                tags: updatedStory.tags || [],
              }
            : us
        )
      );
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
        const fetchedSprints = response.data.map((s: Sprint) => ({
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

  // Définir fetchData avec useCallback
  const [isDataLoaded, setIsDataLoaded] = useState(false);

  const fetchData = useCallback(async () => {
    if (isDataLoaded) return; // Évite les appels multiples inutiles
    try {
      const sprintsResponse = await axiosInstance.get(
        `${PROJECT_SERVICE_URL}/api/projects/${projectId}/sprints`,
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );
      const fetchedSprints = sprintsResponse.data.map((sprint: Sprint) => ({
        id: sprint.id,
        name: sprint.name,
        startDate: sprint.startDate,
        endDate: sprint.endDate,
        capacity: sprint.capacity,
        goal: sprint.goal || "",
        status: sprint.status,
        userStories: sprint.userStories || [],
      }));
      setSprints(fetchedSprints);

      const active = fetchedSprints.find((s: Sprint) => s.status === "ACTIVE");
      if (active) {
        setActiveSprint(active);
      }

      const backlogResponse = await axiosInstance.get(
        `${PROJECT_SERVICE_URL}/api/projects/${projectId}/user-stories`,
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );
      setBacklog(backlogResponse.data);
      setIsDataLoaded(true); // Marque les données comme chargées
    } catch (error) {
      console.error("Erreur lors du rechargement des données :", error);
    }
  }, [axiosInstance, projectId, accessToken, isDataLoaded]);

  useEffect(() => {
    if (!authLoading && accessToken && projectId) {
      fetchData();
    }
  }, [fetchData, authLoading, accessToken, projectId]);

  useEffect(() => {
    if (!authLoading && accessToken && projectId) {
      fetchData();
    }
  }, [fetchData, authLoading, accessToken, projectId]);
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

  const displayedSprint =
    activeSprint || sprints.find((s) => s.status === "ACTIVE");

  useEffect(() => {
    const syncActiveSprint = async () => {
      await fetchData(); // Charge les données initiales
      const active = sprints.find((s) => s.status === "ACTIVE");
      if (active && !activeSprint) {
        setActiveSprint(active); // Définit activeSprint si un sprint est actif
      }
    };
    syncActiveSprint();
  }, [sprints, activeSprint, fetchData, projectId, accessToken]); // Ajoutez projectId et accessToken comme dépendances

  // Déclenche à chaque changement de sprints ou activeSprint
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

  const [editingTagsUserStory, setEditingTagsUserStory] =
    useState<UserStory | null>(null);

  const handleEditTags = (story: UserStory) => {
    setEditingTagsUserStory(story); // Ouvre un formulaire ou une modale pour éditer les tags
  };

  // Nouvelle fonction pour mettre à jour les tags
  const handleUpdateTags = async (userStoryId: number, newTags: string[]) => {
    try {
      const response = await axiosInstance.put(
        `${PROJECT_SERVICE_URL}/api/projects/${projectId}/user-stories/${userStoryId}/tags`,
        { tags: newTags },
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
            ? { ...us, tags: updatedStory.tags || [] }
            : us
        )
      );
      setEditingTagsUserStory(null); // Ferme le formulaire après mise à jour
    } catch (error) {
      console.error("Erreur lors de la mise à jour des tags :", error);
      alert("Erreur lors de la mise à jour des tags.");
    }
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
          <button className="tasks-tab active">
            <i className="fa fa-chart-simple"></i> Kanban
          </button>
          <button className="tasks-tab">
            <i className="fa fa-table"></i> Table
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
                  {/* Formulaire de création */}
                  {!editingUserStory && (
                    <div className="story-form">
                      <h4>New Story</h4>
                      <form
                        onSubmit={handleAddUserStory}
                        className="modern-form"
                      >
                        <input
                          type="text"
                          placeholder="Story Title"
                          value={newUserStory.title}
                          onChange={(e) =>
                            setNewUserStory({
                              ...newUserStory,
                              title: e.target.value,
                            })
                          }
                          required
                        />
                        <textarea
                          placeholder="Description (optional)"
                          value={newUserStory.description}
                          onChange={(e) =>
                            setNewUserStory({
                              ...newUserStory,
                              description: e.target.value,
                            })
                          }
                        />
                        <div className="form-row">
                          <select
                            value={newUserStory.priority}
                            onChange={(e) =>
                              setNewUserStory({
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
                            value={newUserStory.effortPoints || ""}
                            onChange={(e) =>
                              setNewUserStory({
                                ...newUserStory,
                                effortPoints: parseInt(e.target.value) || 0,
                              })
                            }
                            min="0"
                          />
                        </div>
                        <div className="form-row">
                          <input
                            type="text"
                            placeholder="Tags (press Enter to add)"
                            onKeyDown={(e) => {
                              if (
                                e.key === "Enter" &&
                                e.currentTarget.value.trim()
                              ) {
                                e.preventDefault();
                                const newTag = e.currentTarget.value.trim();
                                const currentTags = newUserStory.tags || [];
                                if (!currentTags.includes(newTag)) {
                                  setNewUserStory({
                                    ...newUserStory,
                                    tags: [...currentTags, newTag],
                                  });
                                }
                                e.currentTarget.value = "";
                              }
                            }}
                          />
                          <div className="tag-list">
                            {newUserStory.tags &&
                            newUserStory.tags.length > 0 ? (
                              newUserStory.tags.map((tag, index) => (
                                <span key={index} className="tag">
                                  {tag}
                                  <button
                                    type="button"
                                    onClick={() => {
                                      const updatedTags =
                                        newUserStory.tags!.filter(
                                          (t) => t !== tag
                                        );
                                      setNewUserStory({
                                        ...newUserStory,
                                        tags: updatedTags,
                                      });
                                    }}
                                  >
                                    x
                                  </button>
                                </span>
                              ))
                            ) : (
                              <span>No tags</span>
                            )}
                          </div>
                        </div>
                        <button type="submit" className="action-btn primary">
                          Add
                        </button>
                      </form>
                    </div>
                  )}

                  {/* Formulaire d'édition */}
                  {editingUserStory && (
                    <div className="story-form">
                      <h4>Edit Story</h4>
                      <form
                        onSubmit={handleUpdateUserStory}
                        className="modern-form"
                      >
                        <input
                          type="text"
                          placeholder="Story Title"
                          value={editingUserStory.title}
                          onChange={(e) =>
                            setEditingUserStory({
                              ...editingUserStory,
                              title: e.target.value,
                            })
                          }
                          required
                        />
                        <textarea
                          placeholder="Description (optional)"
                          value={editingUserStory.description}
                          onChange={(e) =>
                            setEditingUserStory({
                              ...editingUserStory,
                              description: e.target.value,
                            })
                          }
                        />
                        <div className="form-row">
                          <select
                            value={editingUserStory.priority}
                            onChange={(e) =>
                              setEditingUserStory({
                                ...editingUserStory,
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
                            value={editingUserStory.effortPoints || ""}
                            onChange={(e) =>
                              setEditingUserStory({
                                ...editingUserStory,
                                effortPoints: parseInt(e.target.value) || 0,
                              })
                            }
                            min="0"
                          />
                        </div>
                        <div className="form-row">
                          <input
                            type="text"
                            placeholder="Tags (press Enter to add)"
                            onKeyDown={(e) => {
                              if (
                                e.key === "Enter" &&
                                e.currentTarget.value.trim()
                              ) {
                                e.preventDefault();
                                const newTag = e.currentTarget.value.trim();
                                const currentTags = editingUserStory.tags || [];
                                if (!currentTags.includes(newTag)) {
                                  setEditingUserStory({
                                    ...editingUserStory,
                                    tags: [...currentTags, newTag],
                                  });
                                }
                                e.currentTarget.value = "";
                              }
                            }}
                          />
                          <div className="tag-list">
                            {editingUserStory.tags &&
                            editingUserStory.tags.length > 0 ? (
                              editingUserStory.tags.map((tag, index) => (
                                <span key={index} className="tag">
                                  {tag}
                                  <button
                                    type="button"
                                    onClick={() => {
                                      const updatedTags =
                                        editingUserStory.tags!.filter(
                                          (t) => t !== tag
                                        );
                                      setEditingUserStory({
                                        ...editingUserStory,
                                        tags: updatedTags,
                                      });
                                    }}
                                  >
                                    x
                                  </button>
                                </span>
                              ))
                            ) : (
                              <span>No tags</span>
                            )}
                          </div>
                        </div>
                        <div className="twobuttons">
                          <button type="submit" className="action-btn primary">
                            Update
                          </button>
                          <button
                            type="button"
                            className="action-btn secondary-cancel"
                            onClick={() => setEditingUserStory(null)}
                          >
                            Cancel
                          </button>
                        </div>
                      </form>
                    </div>
                  )}

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
                                  <th>Tags</th>
                                  <th>Dep</th>
                                  <th>Actions</th>
                                </tr>
                              </thead>
                              <tbody>
                                {currentStories.map((story) => (
                                  <React.Fragment key={story.id}>
                                    <tr
                                      key={story.id}
                                      onClick={() => {
                                        setSelectedItem({
                                          id: story.id,
                                          type: "userStory",
                                          title:story.title
                                        });
                                        fetchHistory(story.id, "userStory");
                                      }}
                                      style={{ cursor: "pointer" }}
                                    >
                                      <td>{story.title}</td>
                                      <td
                                        className={`priority-${story.priority.toLowerCase()}`}
                                      >
                                        {story.priority}
                                      </td>
                                      <td>{story.effortPoints} pts</td>
                                      <td>{story.status || "BACKLOG"}</td>
                                      <td>
                                        {story.tags && story.tags.length > 0
                                          ? story.tags.join(", ")
                                          : "No tags"}
                                      </td>
                                      <td onClick={(e) => e.stopPropagation()}>
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
                                          onClick={(e) => {
                                            e.stopPropagation();
                                            handleEditUserStory(story);
                                          }}
                                          className="icon-btn edit"
                                        >
                                          <i className="fa fa-edit"></i>
                                        </button>
                                        <button
                                          onClick={(e) => {
                                            e.stopPropagation();
                                            handleDeleteUserStory(story.id);
                                          }}
                                          className="icon-btn delete"
                                        >
                                          <i className="fa fa-trash"></i>
                                        </button>
                                        <button
                                          onClick={(e) => {
                                            e.stopPropagation();
                                            handleEditTags(story);
                                          }}
                                          className="icon-btn tags"
                                        >
                                          <i className="fa fa-tags"></i>
                                        </button>
                                        {editingTagsUserStory && (
                                          <div className="modal">
                                            <div className="modal-content">
                                              <h4>
                                                Edit Tags for{" "}
                                                {editingTagsUserStory.title}
                                              </h4>
                                              <input
                                                type="text"
                                                placeholder="Add tag (press Enter)"
                                                onKeyDown={(e) => {
                                                  if (
                                                    e.key === "Enter" &&
                                                    e.currentTarget.value.trim()
                                                  ) {
                                                    e.preventDefault();
                                                    const newTag =
                                                      e.currentTarget.value.trim();
                                                    const currentTags =
                                                      editingTagsUserStory.tags ||
                                                      [];
                                                    if (
                                                      !currentTags.includes(
                                                        newTag
                                                      )
                                                    ) {
                                                      setEditingTagsUserStory({
                                                        ...editingTagsUserStory,
                                                        tags: [
                                                          ...currentTags,
                                                          newTag,
                                                        ],
                                                      });
                                                    }
                                                    e.currentTarget.value = "";
                                                  }
                                                }}
                                              />
                                              <div className="tag-list">
                                                {editingTagsUserStory.tags?.map(
                                                  (tag, index) => (
                                                    <span
                                                      key={index}
                                                      className="tag"
                                                    >
                                                      {tag}
                                                      <button
                                                        onClick={() =>
                                                          setEditingTagsUserStory(
                                                            {
                                                              ...editingTagsUserStory,
                                                              tags: editingTagsUserStory.tags?.filter(
                                                                (t) => t !== tag
                                                              ),
                                                            }
                                                          )
                                                        }
                                                      >
                                                        x
                                                      </button>
                                                    </span>
                                                  )
                                                )}
                                              </div>
                                              <div className="twobuttons">
                                                <button
                                                  onClick={() =>
                                                    handleUpdateTags(
                                                      editingTagsUserStory.id,
                                                      editingTagsUserStory.tags ||
                                                        []
                                                    )
                                                  }
                                                  className="action-btn primary"
                                                >
                                                  Save
                                                </button>
                                                <button
                                                  onClick={() =>
                                                    setEditingTagsUserStory(
                                                      null
                                                    )
                                                  }
                                                  className="action-btn secondary-cancel"
                                                >
                                                  Cancel
                                                </button>
                                              </div>
                                            </div>
                                          </div>
                                        )}

                                        <select
                                          onClick={(e) => e.stopPropagation()}
                                          onChange={async (e) => {
                                            const sprintId = parseInt(
                                              e.target.value
                                            );
                                            if (sprintId) {
                                              try {
                                                await axiosInstance.put(
                                                  `${PROJECT_SERVICE_URL}/api/projects/${projectId}/user-stories/${story.id}/assign-sprint/${sprintId}`,
                                                  {},
                                                  {
                                                    headers: {
                                                      Authorization: `Bearer ${accessToken}`,
                                                    },
                                                  }
                                                );

                                                await fetchData(); // Recharger toutes les données
                                              } catch (error) {
                                                const axiosError =
                                                  error as AxiosError<{
                                                    message?: string;
                                                  }>;
                                                console.error(
                                                  "Erreur lors de l'assignation au Sprint :",
                                                  axiosError
                                                );
                                                alert(
                                                  "Erreur lors de l'assignation : " +
                                                    (axiosError.response?.data
                                                      ?.message ||
                                                      axiosError.message)
                                                );
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
                                                              ? depStory.title
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
                                                                    setBacklog(
                                                                      (prev) =>
                                                                        prev.map(
                                                                          (
                                                                            us
                                                                          ) =>
                                                                            us.id ===
                                                                            story.id
                                                                              ? {
                                                                                  ...us,
                                                                                  dependsOn:
                                                                                    newDependsOn,
                                                                                }
                                                                              : us
                                                                        )
                                                                    ); // Mise à jour locale
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
                                                                      setBacklog(
                                                                        (
                                                                          prev
                                                                        ) =>
                                                                          prev.map(
                                                                            (
                                                                              us
                                                                            ) =>
                                                                              us.id ===
                                                                              story.id
                                                                                ? {
                                                                                    ...us,
                                                                                    dependsOn:
                                                                                      newDependsOn,
                                                                                  }
                                                                                : us
                                                                          )
                                                                      ); // Mise à jour locale
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
                                                                      key={
                                                                        us.id
                                                                      }
                                                                      value={
                                                                        us.id
                                                                      }
                                                                    >
                                                                      {us.title}
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
                                                              setBacklog(
                                                                (prev) =>
                                                                  prev.map(
                                                                    (us) =>
                                                                      us.id ===
                                                                      story.id
                                                                        ? {
                                                                            ...us,
                                                                            dependsOn:
                                                                              newDependsOn,
                                                                          }
                                                                        : us
                                                                  )
                                                              ); // Mise à jour locale
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
                                                              {us.title}
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
                                <tr
                                  key={sprint.id}
                                  onClick={() => {
                                    setSelectedItem({
                                      id: sprint.id,
                                      type: "sprint",
                                      title:sprint.name
                                    });
                                    fetchHistory(sprint.id, "sprint");
                                  }}
                                  style={{ cursor: "pointer" }}
                                >
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
                                    <div className="tooltip-container">
                                      <button
                                        onClick={async () => {
                                          try {
                                            const response =
                                              await axiosInstance.post(
                                                `${PROJECT_SERVICE_URL}/api/projects/${projectId}/sprints/${sprint.id}/activate`,
                                                {},
                                                {
                                                  headers: {
                                                    Authorization: `Bearer ${accessToken}`,
                                                  },
                                                }
                                              );
                                            setActiveSprint(response.data); // Utilise le setter de useState
                                            await fetchData(); // Recharge les données
                                          } catch (error) {
                                            console.error(
                                              "Erreur lors de l'activation du sprint :",
                                              error
                                            );
                                            alert(
                                              "Erreur lors de l'activation du sprint."
                                            );
                                          }
                                        }}
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
                                                    <td>{story.title}</td>
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
                                                            await fetchData(); // Recharger après suppression
                                                          } catch (error) {
                                                            console.error(
                                                              "Erreur lors du retrait du Sprint :",
                                                              error
                                                            );
                                                            alert(
                                                              "Erreur lors du retrait de la User Story."
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

                  {displayedSprint && (
                    <div className="active-sprint">
                      <h4>
                        <i className="fa fa-rocket"></i> Sprint actif :{" "}
                        {displayedSprint.name}
                      </h4>
                      <p className="sprint-goal">
                        Objectif :{" "}
                        <strong>
                          {displayedSprint.goal || "Aucun objectif défini"}
                        </strong>
                      </p>

                      <div className="sprint-progress">
                        <div className="progress-bar">
                          <div
                            className="progress"
                            style={{
                              width: `${
                                ((displayedSprint.userStories || []).reduce(
                                  (sum, us) => sum + us.effortPoints,
                                  0
                                ) /
                                  displayedSprint.capacity) *
                                  100 || 0
                              }%`,
                            }}
                          ></div>
                        </div>
                        <p className="capacity-info">
                          <i className="fas fa-sync-alt"></i> Used Capacity :{" "}
                          <strong>
                            {(displayedSprint.userStories || []).reduce(
                              (sum, us) => sum + us.effortPoints,
                              0
                            )}
                          </strong>{" "}
                          / {displayedSprint.capacity} pts
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
                          {(displayedSprint.userStories || []).map((story) => (
                            <tr key={story.id}>
                              <td>{story.title}</td>{" "}
                              {/* Remplacez "title" par "name" si nécessaire */}
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

          {/* Mini-modal pour afficher l'historique */}
          {isHistoryModalOpen && selectedItem && (
  <div
    className="modal-overlay-history"
    onClick={() => setIsHistoryModalOpen(false)}
  >
    <div
      className="modal-content-history"
      onClick={(e) => e.stopPropagation()}
    >
      <h4>
        Historique{" "}
        {selectedItem.type === "userStory" ? "User Story" : "Sprint"}{" "}
        {selectedItem.title}
      </h4>
      {history.length > 0 ? (
        <div className="history-timeline">
          {history.map((entry, index) => (
            <div
              className={`history-event ${entry.action.toLowerCase()}`}
              key={index}
            >
              <div className="event-date">
                {new Date(entry.date).toLocaleString()}
              </div>
              <div className="event-details-container">
                <div className="event-details">
                  <div className="top-row">
                    <div className="author">
                      <i className="fa fa-user"></i>
                      <strong>Auteur:</strong> <span>{entry.authorFullName}</span>
                    </div>
                    <div className="action">
                      <i className="fa fa-bolt"></i>
                      <strong>Action:</strong> <span className={entry.action.toLowerCase()}>{entry.action}</span>
                    </div>
                  </div>
                  <div className="description-row">
                    <div className="description">
                      <i className="fa fa-comment"></i>
                      <strong>Description:</strong> <span>{entry.description}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <p>Aucun historique disponible.</p>
      )}
      <button
        className="action-btn secondary"
        onClick={() => setIsHistoryModalOpen(false)}
      >
        Fermer
      </button>
    </div>
  </div>
)}
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
