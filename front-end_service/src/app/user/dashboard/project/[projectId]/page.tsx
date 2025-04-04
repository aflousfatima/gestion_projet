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
  priority: "LOW" | "MEDUIM" | "HIGH" | "";
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
  priority: "LOW" | "MEDUIM" | "HIGH" | "";
  effortPoints: number;
  sprintId?: number;
}

interface Sprint {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  capacity: number;
  userStories: UserStory[];
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
    priority: "MEDUIM",
    status: "TO DO",
    sprintId: 1,
    userStoryId: 1,
  },
];

const initialUserStories: UserStory[] = [
  {
    id: 1,
    name: "Implémenter auth",
    description: "Connexion sécurisée",
    priority: "MEDUIM",
    effortPoints: 8,
    sprintId: 1,
  },
  {
    id: 2,
    name: "Créer page profil",
    description: "Afficher infos utilisateur",
    priority: "HIGH",
    effortPoints: 5,
  },
];

export default function Tasks() {
  const { accessToken, isLoading: authLoading, user } = useAuth();
  const axiosInstance = useAxios();
  const params = useParams();
  const projectId = params.projectId as string;

  const [tasks, setTasks] = useState<Task[]>(initialTasks);
  const [isModalOpen, setIsModalOpen] = useState(false);
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

  // États pour Agile
  const [isAgilePanelOpen, setIsAgilePanelOpen] = useState(false);
  const [backlog, setBacklog] = useState<UserStory[]>(initialUserStories);
  const [newUserStory, setNewUserStory] = useState<UserStory>({
    id: 0,
    name: "",
    description: "",
    priority: "",
    effortPoints: 0,
  });
  const [activeSprint, setActiveSprint] = useState<Sprint | null>({
    id: 1,
    name: "Sprint 1",
    startDate: "5 avril",
    endDate: "12 avril",
    capacity: 20,
    userStories: initialUserStories.filter((us) => us.sprintId === 1),
  });
  const [selectedUserStoryId, setSelectedUserStoryId] = useState<number | null>(
    null
  ); // Pour le mini-modal

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
      if (!target.closest(".agile-panel") && !target.closest(".agile-toggle"))
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
      id: tasks.length + 1,
      ...newTask,
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
      // Ajout du console.log pour afficher les données envoyées
      console.log("Données envoyées dans la requête POST :", {
        title: newUserStory.name,
        description: newUserStory.description,
        priority: newUserStory.priority,
        effortPoints: newUserStory.effortPoints,
      });

      const response = await axiosInstance.post(
        `${PROJECT_SERVICE_URL}/api/projects/${projectId}/user-stories`,
        {
          title: newUserStory.name,
          description: newUserStory.description,
          priority: newUserStory.priority,
          effortPoints: newUserStory.effortPoints,
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
          priority: createdUserStory.priority,
          effortPoints: createdUserStory.effortPoints,
        },
      ]);
      setNewUserStory({
        id: 0,
        name: "",
        description: "",
        priority: "",
        effortPoints: 0,
      });
    } catch (error) {
      console.error("Erreur lors de la création de la User Story :", error);
      alert("Erreur lors de l'ajout de la User Story.");
    }
  };

  const handleAddToSprint = (story: UserStory) => {
    if (activeSprint && activeSprint.capacity >= story.effortPoints) {
      const updatedStory = { ...story, sprintId: activeSprint.id };
      setBacklog(backlog.filter((us) => us.id !== story.id));
      setActiveSprint({
        ...activeSprint,
        userStories: [...activeSprint.userStories, updatedStory],
      });
    } else {
      alert("Capacité du Sprint insuffisante !");
    }
  };

  const handleOverlayClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) setIsModalOpen(false);
  };

  const getTasksByStatus = (status: "TO DO" | "IN PROGRESS" | "DONE") => {
    let filteredTasks = activeSprint
      ? tasks.filter(
          (task) => task.sprintId === activeSprint.id && task.status === status
        )
      : tasks.filter((task) => task.status === status);
    if (filterMyTasks && user) {
      filteredTasks = filteredTasks.filter(
        (task) => task.responsible === user.firstName
      );
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
                        | "MEDUIM"
                        | "HIGH"
                        | "",
                    })
                  }
                >
                  <option value="">Selecy a priority</option>
                  <option value="LOW">LOW</option>
                  <option value="MEDUIM">MEDUIM</option>
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
        <div className="agile-panel">
          <div className="agile-panel-header">
            <h3>Agile Management</h3>
            <button
              className="agile-panel-close"
              onClick={() => setIsAgilePanelOpen(false)}
            >
              <i className="fa fa-times"></i>
            </button>
          </div>
          <div className="agile-panel-content">
            <div className="agile-sections-container">
              {/* Section 1 : Créer une User Story */}
              <div className="agile-section">
                <h4 className="agile-section-title">New User Story</h4>
                <form onSubmit={handleAddUserStory} className="modern-form">
                  <input
                    type="text"
                    placeholder="Title of the User Story"
                    value={newUserStory.name}
                    onChange={(e) =>
                      setNewUserStory({ ...newUserStory, name: e.target.value })
                    }
                    required
                  />
                  <textarea
                    placeholder="Description"
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
                            | "MEDUIM"
                            | "HIGH"
                            | "",
                        })
                      }
                    >
                      <option value="">Priority</option>
                      <option value="LOW">LOW</option>
                      <option value="MEDUIM">MEDUIM</option>
                      <option value="HIGH">HIGH</option>
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
                  <button type="submit" className="modern-button">
                    Add to Backlog
                  </button>
                </form>
              </div>

              {/* Section 2 : Créer un Sprint */}
              <div className="agile-section">
                <h4 className="agile-section-title">New Sprint</h4>
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    const newSprint: Sprint = {
                      id: activeSprint ? activeSprint.id + 1 : 1,
                      name: `Sprint ${activeSprint ? activeSprint.id + 1 : 1}`,
                      startDate: prompt("Date de début (ex: 5 avril)") || "",
                      endDate: prompt("Date de fin (ex: 12 avril)") || "",
                      capacity:
                        parseInt(prompt("Capacité (pts)") || "20") || 20,
                      userStories: [],
                    };
                    setActiveSprint(newSprint);
                  }}
                  className="modern-form"
                >
                  <input type="text" placeholder="Nom du Sprint" required />
                  <input
                    type="text"
                    placeholder="Date de début (ex: 5 avril)"
                    required
                  />
                  <input
                    type="text"
                    placeholder="Date de fin (ex: 12 avril)"
                    required
                  />
                  <input
                    type="number"
                    placeholder="Capacité (pts)"
                    min="0"
                    required
                  />
                  <button type="submit" className="modern-button">
                    Créer Sprint
                  </button>
                </form>
              </div>

              {/* Section 3 : Backlog */}
              <div className="agile-section">
                <h4 className="agile-section-title">Backlog</h4>
                <div className="section-content">
                  {backlog.filter((us) => !us.sprintId).length > 0 ? (
                    backlog
                      .filter((us) => !us.sprintId)
                      .map((story) => (
                        <div
                          key={story.id}
                          className="user-story-card modern-card"
                        >
                          <div className="user-story-content">
                            <strong>{story.name}</strong>
                            <p>{story.description || "Pas de description"}</p>
                            <div className="user-story-meta">
                              <span
                                className={`task-priority priority-${story.priority}`}
                              >
                                {story.priority}
                              </span>
                              <span>Effort : {story.effortPoints} pts</span>
                            </div>
                          </div>
                          {activeSprint && (
                            <button
                              className="add-to-sprint-btn modern-button"
                              onClick={() => handleAddToSprint(story)}
                            >
                              Add to Sprint
                            </button>
                          )}
                        </div>
                      ))
                  ) : (
                    <p className="empty-message">
                      No User Story in the Backlog.
                    </p>
                  )}
                </div>
              </div>

              {/* Section 4 : Sprint Actif */}
              <div className="agile-section">
                <h4 className="agile-section-title">Actif Sprint</h4>
                <div className="section-content">
                  {activeSprint ? (
                    <>
                      <div className="sprint-info modern-card">
                        <h5>{activeSprint.name}</h5>
                        <p>
                          {activeSprint.startDate} - {activeSprint.endDate}
                        </p>
                        <p>Capacité : {activeSprint.capacity} pts</p>
                      </div>
                      <h5 className="sub-section-title">User Stories</h5>
                      {activeSprint.userStories.map((story) => (
                        <div
                          key={story.id}
                          className="user-story-card modern-card"
                        >
                          <div className="user-story-content">
                            <strong>{story.name}</strong>
                            <p>{story.description || "Pas de description"}</p>
                            <div className="user-story-meta">
                              <span
                                className={`task-priority priority-${story.priority}`}
                              >
                                {story.priority}
                              </span>
                              <span>Effort : {story.effortPoints} pts</span>
                              <span>
                                Tasks :{" "}
                                {
                                  tasks.filter(
                                    (t) => t.userStoryId === story.id
                                  ).length
                                }
                              </span>
                            </div>
                          </div>
                          <button
                            className="add-task-btn modern-button"
                            onClick={() => setSelectedUserStoryId(story.id)}
                          >
                            Add Task
                          </button>
                        </div>
                      ))}
                    </>
                  ) : (
                    <p className="empty-message">No Actif Sprint .</p>
                  )}
                </div>
              </div>
            </div>

            {/* Mini-modal pour ajouter une tâche */}
            {selectedUserStoryId && (
              <div
                className="mini-modal-overlay"
                onClick={() => setSelectedUserStoryId(null)}
              >
                <div
                  className="mini-modal"
                  onClick={(e) => e.stopPropagation()}
                >
                  <form
                    onSubmit={(e) => handleAddTask(e, selectedUserStoryId)}
                    className="mini-form"
                  >
                    <input
                      type="text"
                      placeholder="Nom de la tâche"
                      value={newTask.name}
                      onChange={(e) =>
                        setNewTask({ ...newTask, name: e.target.value })
                      }
                      required
                    />
                    <input
                      type="text"
                      placeholder="Responsable"
                      value={newTask.responsible || ""}
                      onChange={(e) =>
                        setNewTask({ ...newTask, responsible: e.target.value })
                      }
                    />
                    <input
                      type="text"
                      placeholder="Échéance (ex: 5 avril)"
                      value={newTask.dueDate}
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
                            | "MEDUIM"
                            | "HIGH"
                            | "",
                        })
                      }
                    >
                      <option value="">Priority</option>
                      <option value="LOW">LOW</option>
                      <option value="MEDUIM">MEDUIM</option>
                      <option value="HIGH">HIGH</option>
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
                      <option value="TO DO">TO DO</option>
                      <option value="IN PROGRESS">IN PROGRESS</option>
                      <option value="DONE">DONE</option>
                    </select>
                    <div className="mini-modal-actions">
                      <button type="submit" className="mini-button">
                        Add
                      </button>
                      <button
                        type="button"
                        className="mini-button cancel"
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
        </div>
      )}
    </div>
  );
}
