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

// Interface for tasks
interface Task {
  id: number;
  name: string;
  responsible: string | null;
  dueDate: string;
  priority: "Faible" | "Moyenne" | "Élevée" | "";
  status:
    | "À faire"
    | "En cours"
    | "Terminé"
    | "Sprint"
    | "Backlog"
    | "User Story";
}

// Interface for team members
interface TeamMember {
  id: string;
  firstName: string;
  lastName: string;
  role: string;
  project: string;
  avatar: string;
}

// Interface for the manager
interface Manager {
  id: string;
  authId?: string;
  firstName: string;
  lastName: string;
}

// Interface for a new task
interface NewTask {
  name: string;
  responsible: string | null;
  dueDate: string;
  priority: "Faible" | "Moyenne" | "Élevée" | "";
  status:
    | "À faire"
    | "En cours"
    | "Terminé"
    | "Sprint"
    | "Backlog"
    | "User Story";
}

const initialTasks: Task[] = [
  {
    id: 1,
    name: "Rédiger un brief de projet",
    responsible: "Fatima",
    dueDate: "Aujourd'hui - 6 avril",
    priority: "Faible",
    status: "À faire",
  },
  {
    id: 2,
    name: "Planifier la réunion de lancement",
    responsible: "Fatima",
    dueDate: "5 - 7 avril",
    priority: "Moyenne",
    status: "À faire",
  },
  {
    id: 3,
    name: "Partager la chronologie avec mes collègues",
    responsible: null,
    dueDate: "6 - 10 avril",
    priority: "Élevée",
    status: "À faire",
  },
  {
    id: 4,
    name: "Analyser les besoins des utilisateurs",
    responsible: "Ahmed",
    dueDate: "10 - 12 avril",
    priority: "Moyenne",
    status: "Backlog",
  },
  {
    id: 5,
    name: "Créer un wireframe initial",
    responsible: "Sara",
    dueDate: "12 - 15 avril",
    priority: "Élevée",
    status: "User Story",
  },
  {
    id: 6,
    name: "Préparer le sprint planning",
    responsible: "Mohamed",
    dueDate: "8 - 9 avril",
    priority: "Moyenne",
    status: "Sprint",
  },
];

export default function Tasks() {
  const { accessToken, isLoading: authLoading } = useAuth();
  const axiosInstance = useAxios();
  const params = useParams();
  const projectId = params.projectId as string;

  const [tasks, setTasks] = useState<Task[]>(initialTasks);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newTask, setNewTask] = useState<NewTask>({
    name: "",
    responsible: null,
    dueDate: "",
    priority: "",
    status: "À faire",
  });
  const [teamMembers, setTeamMembers] = useState<TeamMember[]>([]);
  const [manager, setManager] = useState<Manager | null>(null);
  const [showAllMembers, setShowAllMembers] = useState(false);
  const [projectName, setProjectName] = useState<string>("Projet 1");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Fetch project details (including the manager) and team members
  useEffect(() => {
    const fetchProjectDetailsAndTeamMembers = async () => {
      if (authLoading || !accessToken || !projectId) return;

      try {
        setLoading(true);
        setError(null);

        // Fetch project details (including the manager's authId)
        const projectResponse = await axiosInstance.get(
          `${PROJECT_SERVICE_URL}/api/projects/${projectId}`
        );
        console.log("Project Response:", projectResponse);

        // Set project name
        setProjectName(projectResponse.data.name || "Projet inconnu");

        // Fetch manager details if authId is available
        if (
          projectResponse.data.manager &&
          projectResponse.data.manager.authId
        ) {
          try {
            const managerResponse = await axiosInstance.get(
              `${AUTH_SERVICE_URL}/api/auth/users/${projectResponse.data.manager.authId}`
            );
            setManager({
              id: projectResponse.data.manager.id,
              authId: projectResponse.data.manager.authId,
              firstName: managerResponse.data.firstName,
              lastName: managerResponse.data.lastName,
            });
          } catch (managerErr) {
            console.error(
              "❌ Erreur lors de la récupération des détails du manager :",
              managerErr
            );
            setManager(null);
          }
        } else {
          console.warn("No manager or authId found for this project");
          setManager(null);
        }

        // Fetch team members for the project
        const teamResponse = await axiosInstance.get(
          `${AUTH_SERVICE_URL}/api/team-members/${projectId}`
        );
        setTeamMembers(teamResponse.data);
      } catch (err) {
        console.error("❌ Erreur lors de la récupération des données :", err);
        setError("Impossible de charger les données du projet.");
        setManager(null);
        setProjectName("Projet inconnu");
      } finally {
        setLoading(false);
      }
    };

    fetchProjectDetailsAndTeamMembers();
  }, [accessToken, authLoading, axiosInstance, projectId]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as HTMLElement;
      if (!target.closest(".team-members-display")) {
        setShowAllMembers(false);
      }
    };

    if (showAllMembers) {
      document.addEventListener("click", handleClickOutside);
    }

    return () => {
      document.removeEventListener("click", handleClickOutside);
    };
  }, [showAllMembers]);

  const handleAddTask = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const task: Task = {
      id: tasks.length + 1,
      ...newTask,
    };
    setTasks([...tasks, task]);
    setIsModalOpen(false);
    setNewTask({
      name: "",
      responsible: null,
      dueDate: "",
      priority: "",
      status: "À faire",
    });
  };

  const handleOverlayClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) {
      setIsModalOpen(false);
    }
  };

  const getTasksByStatus = (
    status:
      | "À faire"
      | "En cours"
      | "Terminé"
      | "Sprint"
      | "Backlog"
      | "User Story"
  ) => {
    return tasks.filter((task) => task.status === status);
  };

  if (loading) {
    return <p>Chargement...</p>;
  }

  if (error) {
    return <p>{error}</p>;
  }

  return (
    <div className="project-container">
      {/* En-tête */}
      <div className="tasks-title-container">
        <span className="icon-projet">
          <i className="fa fa-list project-icon-card custom-project-icon"></i>{" "}
        </span>{" "}
        <h1 className="tasks-title">{projectName}</h1>
        <div className="team-list">
          {teamMembers.length > 0 ? (
            <div className="team-members-display">
              {/* Display the first member */}
              <div className="team-member-initial">
                {teamMembers[0].firstName.charAt(0) +
                  teamMembers[0].lastName.charAt(0)}
              </div>
              {/* If there are more members, show the "..." button */}
              {teamMembers.length > 1 && (
                <div
                  className="team-member-more"
                  onClick={() => setShowAllMembers(!showAllMembers)}
                >
                  +{teamMembers.length - 1}..
                </div>
              )}
              {/* Show all members if "..." is clicked */}
              {showAllMembers && teamMembers.length > 1 && (
                <div className="team-members-expanded">
                  <h5 className="team-members-expanded-firsttitle">
                    Membres du projet
                  </h5>

                  {/* Search bar */}
                  <div className="team-members-search">
                    <input
                      type="text"
                      placeholder="Rechercher des membres"
                      className="team-members-search-input"
                    />
                  </div>
                  {/* List of members */}
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
                        <p>Aucun manager trouvé.</p>
                      )}
                    </div>
                    <h4 className="team-members-expanded-title">Invités</h4>
                    <div className="team-members-expanded-section">
                      {teamMembers.slice(0).map((member) => (
                        <div
                          key={member.id}
                          className="team-member-expanded-item"
                        >
                          <div className="team-member-expanded-initial">
                            {member.firstName.charAt(0) +
                              member.lastName.charAt(0)}
                          </div>
                          <div className="team-member-expanded-info">
                            {member.firstName} {member.lastName}
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
            <p>Aucun membre déquipe trouvé.</p>
          )}
          <button className="buton-share-style">
            <i className="fa fa-building"></i> Partager
          </button>
        </div>
      </div>
      <div className="tasks-header">
        <div className="tasks-tabs">
          <button className="tasks-tab active">
            <i className="fa fa-list"></i> Liste
          </button>
          <button className="tasks-tab">
            <i className="fa fa-table"></i> Tableau
          </button>
          <button className="tasks-tab">
            <i className="fa fa-clock"></i> Chronologie
          </button>
          <button className="tasks-tab">
            <i className="fa fa-chart-line"></i> Tableau de bord
          </button>
          <button className="tasks-tab">
            <i className="fa fa-calendar"></i> Calendrier
          </button>
          <button className="tasks-tab">
            <i className="fa fa-comment"></i> Messages
          </button>
          <button className="tasks-tab">
            <i className="fa fa-file"></i> Fichiers
          </button>
          <button className="tasks-tab">
            <i className="fa fa-plus"></i>
          </button>{" "}
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
            {" "}
            <i className="fa fa-filter"></i>
            Filtrer
          </button>
          <button className="tasks-option">
            <i className="fa fa-sort"></i> Trier
          </button>

          <button className="tasks-option">
            <i className="fa fa-gear"></i> Options
          </button>
        </div>
      </div>

      {/* En-tête de la liste */}
      <div className="tasks-list-header">
        <span className="tasks-list-header-item-principal">
          Nom de la tâche
        </span>
        <span className="tasks-list-header-item">Responsable</span>
        <span className="tasks-list-header-item">Échéance</span>
        <span className="tasks-list-header-item">Priorité</span>
        <span className="tasks-list-header-item">Statut</span>
      </div>

      {/* Liste des tâches */}
      <div className="tasks-list">
        {/* Section À faire */}
        <div className="dashboard-task-section">
          <h2 className="dashboard-task-section-title">
            <i className="fa fa-caret-down"></i> À faire
          </h2>
          {getTasksByStatus("À faire").map((task) => (
            <div key={task.id} className="task-row">
              <div className="task-checkbox">
                <input type="checkbox" />
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
          ))}
          <button
            className="tasks-add-subtask"
            onClick={() => setIsModalOpen(true)}
          >
            Ajouter une tâche...
          </button>
        </div>
        {/* Section En cours */}
        <div className="dashboard-task-section">
          <h2 className="dashboard-task-section-title section-encours">
            <i className="fa fa-caret-down"></i> En cours
          </h2>
          {getTasksByStatus("En cours").map((task) => (
            <div key={task.id} className="task-row">
              <div className="task-checkbox">
                <input type="checkbox" />
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
                <span className="task-status">Dans les délais</span>
              </div>
            </div>
          ))}
          <button
            className="tasks-add-subtask"
            onClick={() => setIsModalOpen(true)}
          >
            Ajouter une tâche...
          </button>
        </div>
        {/* Section Terminé */}
        <div className="dashboard-task-section">
          <h2 className="dashboard-task-section-title section-encours">
            <i className="fa fa-caret-down"></i> Terminé
          </h2>
          {getTasksByStatus("Terminé").map((task) => (
            <div key={task.id} className="task-row">
              <div className="task-checkbox">
                <input type="checkbox" checked />
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
                <div className="task-priority">
                  <span className={`priority-${task.priority}`}>
                    {task.priority}
                  </span>
                </div>
                <span className="task-status">Dans les délais</span>
              </div>
            </div>
          ))}
          <button
            className="tasks-add-subtask"
            onClick={() => setIsModalOpen(true)}
          >
            Ajouter une tâche...
          </button>
        </div>
      </div>

      {/* Modal pour ajouter une tâche */}
      {isModalOpen && (
        <div className="tasks-modal-overlay" onClick={handleOverlayClick}>
          <div className="tasks-modal">
            <h2 className="tasks-modal-title">Ajouter une tâche</h2>
            <form onSubmit={handleAddTask} className="tasks-form">
              <div className="tasks-form-group">
                <label htmlFor="name">Nom de la tâche</label>
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
                <label htmlFor="dueDate">Échéance</label>
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
                <label htmlFor="priority">Priorité</label>
                <select
                  id="priority"
                  value={newTask.priority}
                  onChange={(e) =>
                    setNewTask({
                      ...newTask,
                      priority: e.target.value as
                        | "Faible"
                        | "Moyenne"
                        | "Élevée"
                        | "",
                    })
                  }
                >
                  <option value="">Sélectionnez une priorité</option>
                  <option value="Faible">Faible</option>
                  <option value="Moyenne">Moyenne</option>
                  <option value="Élevée">Élevée</option>
                </select>
              </div>
              <div className="tasks-form-group">
                <label htmlFor="status">Statut</label>
                <select
                  id="status"
                  value={newTask.status}
                  onChange={(e) =>
                    setNewTask({
                      ...newTask,
                      status: e.target.value as
                        | "À faire"
                        | "En cours"
                        | "Terminé"
                        | "Sprint"
                        | "Backlog"
                        | "User Story",
                    })
                  }
                >
                  <option value="À faire">À faire</option>
                  <option value="En cours">En cours</option>
                  <option value="Terminé">Terminé</option>
                  <option value="Sprint">Sprint</option>
                  <option value="Backlog">Backlog</option>
                  <option value="User Story">User Story</option>
                </select>
              </div>
              <div className="tasks-modal-actions">
                <button type="submit" className="tasks-submit-button">
                  Ajouter
                </button>
                <button
                  type="button"
                  className="tasks-cancel-button"
                  onClick={() => setIsModalOpen(false)}
                >
                  Annuler
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
