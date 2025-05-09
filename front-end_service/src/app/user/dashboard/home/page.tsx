"use client";
import React, { useState, useEffect } from "react";
import { useAuth } from "../../../../context/AuthContext";
import { AUTH_SERVICE_URL } from "../../../../config/useApi";
import "../../../../styles/Dashboard-Home.css";
import ProtectedRoute from "../../../../components/ProtectedRoute";
import { useProjects } from "../../../../hooks/useProjects";

interface Task {
  id: number;
  title: string;
  status: string;
  projectId: number;
  projectName: string;
}

const Home = () => {
  const { accessToken, isLoading: authLoading } = useAuth();
  const { projects, loading: projectsLoading, error: projectsError } = useProjects();
  const [userName, setUserName] = useState("User");
  const [currentDate, setCurrentDate] = useState("");
  const [tasks, setTasks] = useState<Task[]>([]);
  const [activeTab, setActiveTab] = useState<"Upcoming" | "Late" | "Completed">("Upcoming");
  const [loading, setLoading] = useState(true);
  const [tasksError, setTasksError] = useState<string | null>(null);

  // Formater la date actuelle
  useEffect(() => {
    const today = new Date();
    const options = {
      weekday: "long",
      day: "numeric",
      month: "long",
      year: "numeric",
    } as const;
    const formattedDate = today.toLocaleDateString("en-US", options);
    setCurrentDate(formattedDate);
  }, []);

  // Récupérer les détails de l'utilisateur
  useEffect(() => {
    const fetchUserDetails = async () => {
      if (authLoading || !accessToken) {
        console.log("🔍 Auth en chargement ou pas de token, arrêt de fetchUserDetails");
        setLoading(false);
        return;
      }

      try {
        console.log("🔍 Récupération des détails de l'utilisateur...");
        const userDetailsResponse = await fetch(`${AUTH_SERVICE_URL}/api/me`, {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        });

        if (!userDetailsResponse.ok) {
          throw new Error("Échec de la récupération des détails utilisateur");
        }

        const userDetails = await userDetailsResponse.json();
        console.log("✅ Détails de l'utilisateur récupérés:", userDetails);
        setUserName(userDetails.firstName || "User");
      } catch (error) {
        console.error("Erreur lors de la récupération des détails utilisateur:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchUserDetails();
  }, [accessToken, authLoading]);

  // Récupérer les tâches de l'utilisateur
  useEffect(() => {
    const fetchTasks = async () => {
      console.log("🔍 Début de fetchTasks");
      console.log("Conditions: ", {
        authLoading,
        projectsLoading,
        accessToken: !!accessToken,
        projectsLength: projects.length,
      });

      if (authLoading || projectsLoading || !accessToken) {
        console.log("🔍 Conditions non remplies pour fetchTasks, arrêt");
        return;
      }

      try {
        console.log("🔍 Récupération des tâches de l'utilisateur");
        const response = await fetch(`http://localhost:8086/api/project/tasks/user/active-sprints`, {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        });

        if (!response.ok) {
          throw new Error("Échec de la récupération des tâches de l'utilisateur");
        }

        const tasksData = await response.json();
        console.log("🔍 Tâches reçues:", tasksData);

        // Créer une map des projets pour associer projectName
        const projectMap = new Map(projects.map(p => [p.id, p.name]));
        const mappedTasks = tasksData.map((task: any) => ({
          id: task.id,
          title: task.title,
          status: task.status,
          projectId: task.projectId,
          projectName: projectMap.get(task.projectId) || "Projet inconnu",
        }));

        console.log("✅ Tâches mappées:", mappedTasks);
        setTasks(mappedTasks);
      } catch (error) {
        console.error("Erreur lors de la récupération des tâches:", error);
        setTasksError("Impossible de charger les tâches");
      }
    };

    fetchTasks();
  }, [accessToken, authLoading, projects, projectsLoading]);

  // Filtrer les tâches selon l'onglet actif
  const filteredTasks = tasks.filter((task) => {
    if (activeTab === "Upcoming") {
      return task.status === "TO_DO";
    } else if (activeTab === "Completed") {
      return task.status === "DONE";
    } else {
      return task.status !== "TO_DO" && task.status !== "DONE";
    }
  });

  if (loading || projectsLoading || authLoading) {
    return <div><img src="/loading.svg" alt="Loading" className="loading-img" />.</div>;
  }

  if (projectsError || tasksError) {
    return <div>Erreur : {projectsError || tasksError}</div>;
  }

  return (
    <ProtectedRoute>
      <div className="style-page-home-dash">
        <div className="header">
          <div className="welcome-message">
            <h1>Hello, {userName}</h1>
            <p>{currentDate}</p>
            <div className="stats">
              <i className="fa fa-check"></i> {tasks.filter(t => t.status === "DONE").length} completed tasks{" "}
              <i className="fa fa-users"></i> 0 collaborators
            </div>
          </div>
        </div>

        <div className="content">
          <div className="tasks-section">
            <div className="section-header">
              <h2>My Tasks</h2>
              <i className="section-menu">...</i>
            </div>
            <div className="tabs">
              <span
                className={`tab ${activeTab === "Upcoming" ? "active" : ""}`}
                onClick={() => setActiveTab("Upcoming")}
              >
                Upcoming
              </span>
              <span
                className={`tab ${activeTab === "Late" ? "active" : ""}`}
                onClick={() => setActiveTab("Late")}
              >
                Late
              </span>
              <span
                className={`tab ${activeTab === "Completed" ? "active" : ""}`}
                onClick={() => setActiveTab("Completed")}
              >
                Completed
              </span>
            </div>
            <button className="create-task-btn">
              <i className="fa fa-plus"></i> Create Task
            </button>
            <div className="tasks-list">
      {filteredTasks.length > 0 ? (
        filteredTasks.map((task) => (
          <div key={task.id} className="task-item">
            <div className="task-node"></div>
            <i className="fa fa-task task-icon"></i>
            <div className="task-content">
              <p className="task-title">{task.title}</p>
              <span className="project-info">Projet: {task.projectName}</span>
            </div>
          </div>
        ))
      ) : (
        <p>No Task Available in this Category.</p>
      )}
    </div>
          </div>

          <div className="projects-section">
            <div className="section-header">
              <h2>Projects</h2>
              <i className="section-menu">...</i>
            </div>
            <button className="create-project-btn">
              <i className="fa fa-plus"></i> Create project
            </button>
            {projects.length > 0 ? (
              <div className="projects-list">
                {projects.map((project, index) => (
                  <div key={index} className="project-name">
                    <span className="project-name">
                      <i className="fa fa-list project-card custom-icon"></i>{" "}
                      {project.name || "Projet sans nom"}
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <p>No project available.</p>
            )}
          </div>
        </div>
      </div>
    </ProtectedRoute>
  );
};

export default Home;