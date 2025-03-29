// pages/index.tsx
"use client";
import React, { useState } from "react";
import "../../../../styles/Dashboard-Project.css";

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
    dueDate: "Aujourd'hui - 6 fév",
    priority: "Faible",
    status: "À faire",
  },
  {
    id: 2,
    name: "Planifier la réunion de lancement",
    responsible: "Fatima",
    dueDate: "5 - 7 fév",
    priority: "Moyenne",
    status: "À faire",
  },
  {
    id: 3,
    name: "Partager la chronologie avec mes collègues",
    responsible: null,
    dueDate: "6 - 10 fév",
    priority: "Élevée",
    status: "À faire",
  },
  {
    id: 4,
    name: "Analyser les besoins des utilisateurs",
    responsible: "Ahmed",
    dueDate: "10 - 12 fév",
    priority: "Moyenne",
    status: "Backlog",
  },
  {
    id: 5,
    name: "Créer un wireframe initial",
    responsible: "Sara",
    dueDate: "12 - 15 fév",
    priority: "Élevée",
    status: "User Story",
  },
  {
    id: 6,
    name: "Préparer le sprint planning",
    responsible: "Mohamed",
    dueDate: "8 - 9 fév",
    priority: "Moyenne",
    status: "Sprint",
  },
];

export default function Tasks() {
  const [tasks, setTasks] = useState<Task[]>(initialTasks);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newTask, setNewTask] = useState<NewTask>({
    name: "",
    responsible: "",
    dueDate: "",
    priority: "",
    status: "À faire",
  });
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
      responsible: "",
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

  return (
    <div className="project-container">
      {/* En-tête */}
      <div className="tasks-title-container">
        <span className="icon-projet">
          <i className="fa fa-list project-icon-card custom-project-icon"></i>{" "}
        </span>{" "}
        <h1 className="tasks-title">Projet 1</h1>
        <div className="team-list">jhhh</div>
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
            À faire <i className="fa fa-caret-down"></i>
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
          <h2 className="dashboard-task-section-title">
            En cours <i className="fa fa-caret-down"></i>
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
          <h2 className="dashboard-task-section-title">
            Terminé <i className="fa fa-caret-down"></i>
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
        {/* Section Sprint */}
        <div className="dashboard-task-section">
          <h2 className="dashboard-task-section-title">Sprint</h2>
          {getTasksByStatus("Sprint").map((task) => (
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
        {/* Section Backlog */}
        <div className="dashboard-task-section">
          <h2 className="dashboard-task-section-title">Backlog</h2>
          {getTasksByStatus("Backlog").map((task) => (
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
        {/* Section User Story */}
        <div className="dashboard-task-section">
          <h2 className="dashboard-task-section-title">User Story</h2>
          {getTasksByStatus("User Story").map((task) => (
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
