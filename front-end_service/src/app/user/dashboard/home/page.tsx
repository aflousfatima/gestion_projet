"use client";
import React, { useState, useEffect } from "react";
import useAxios from "../../../../hooks/useAxios";
import { useAuth } from "../../../../context/AuthContext";
import {
  AUTH_SERVICE_URL,
  PROJECT_SERVICE_URL,
} from "../../../../config/useApi";
import "../../../../styles/Dashboard-Home.css";
import ProtectedRoute from "../../../../components/ProtectedRoute";

const Home = () => {
  const { accessToken, isLoading: authLoading } = useAuth();
  const axiosInstance = useAxios();

  const [userName, setUserName] = useState("User"); // Nom par défaut
  const [currentDate, setCurrentDate] = useState("");
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);

  // Formater la date actuelle (ex: "Wednesday 19 March")
  useEffect(() => {
    const today = new Date();
    const options = {
      weekday: "long",
      day: "numeric",
      month: "long",
      year: "numeric",
    } as const;
    const formattedDate = today.toLocaleDateString("en-US", options); // Ex: "Wednesday 19 March"
    setCurrentDate(formattedDate);
  }, []);

  // Récupérer les détails de l'utilisateur et les projets
  useEffect(() => {
    const fetchUserDetailsAndProjects = async () => {
      if (authLoading) return; // Attendre que le token soit prêt

      if (!accessToken) {
        setLoading(false);
        return;
      }

      try {
        // Étape 1 : Récupérer l'authId via GET /api/user-id
        console.log("🔍 Récupération de l'authId...");
        const authIdResponse = await axiosInstance.get(
          `${AUTH_SERVICE_URL}/api/user-id`
        );
        const authId = authIdResponse.data;
        console.log("✅ authId récupéré:", authId);

        // Étape 2 : Récupérer les détails de l'utilisateur via GET /api/user-details (optionnel)
        console.log("🔍 Récupération des détails de l'utilisateur...");
        try {
          console.log("Access Token envoyé :", accessToken);
          const userDetailsResponse = await axiosInstance.get(
            `${AUTH_SERVICE_URL}/api/me`,
            {
              headers: {
                Authorization: `Bearer ${accessToken}`,
              },
            }
          );

          // Traite la réponse ici, par exemple userDetailsResponse.data
          const userDetails = userDetailsResponse.data;
          console.log(userDetails); // Exemple d'affichage des informations utilisateur
          console.log("✅ Détails de l'utilisateur récupérés:", userDetails);
          setUserName(userDetails.firstName || "User"); // Utiliser le nom de l'utilisateur ou "User" par défaut
        } catch (error) {
          console.error(
            "Erreur lors de la récupération des détails utilisateur:",
            error
          );
          // Gérer l'erreur si nécessaire
        }

        // Étape 3 : Récupérer les projets via GET /api/projects/by-manager
        console.log("🔍 Récupération des projets pour authId:", authId);
        const projectsResponse = await axiosInstance.get(
          `${PROJECT_SERVICE_URL}/api/projects/by-manager?authId=${authId}`
        );
        const { projects } = projectsResponse.data;
        console.log("✅ Projets récupérés:", projects);
        setProjects(projects);
      } catch (err) {
        console.error("❌ Erreur lors de la récupération des données:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchUserDetailsAndProjects();
  }, [accessToken, authLoading, axiosInstance]);

  if (loading) {
    return <div>Chargement...</div>;
  }

  return (
    <ProtectedRoute>
      <div>
        <div className="header">
          <div className="welcome-message">
            <h1>Hello, {userName}</h1>
            <p>{currentDate}</p>
            <div className="stats">
              <i className="fa fa-check"></i> 0 completed tasks{" "}
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
              <span className="tab active">Upcoming</span>
              <span className="tab">Late</span>
              <span className="tab">Completed</span>
            </div>
            <button className="create-task-btn">
              <i className="fa fa-plus"></i> Create Task
            </button>
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
                    <h3>
                      <i className="fa fa-list project-card"></i> {project}
                    </h3>
                  </div>
                ))}
              </div>
            ) : (
              <p>Aucun projet disponible.</p>
            )}
          </div>
        </div>
      </div>
    </ProtectedRoute>
  );
};

export default Home;
