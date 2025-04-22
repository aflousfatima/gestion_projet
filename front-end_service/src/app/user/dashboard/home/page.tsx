"use client";
import React, { useState, useEffect } from "react";
import { useAuth } from "../../../../context/AuthContext";
import { AUTH_SERVICE_URL } from "../../../../config/useApi";
import "../../../../styles/Dashboard-Home.css";
import ProtectedRoute from "../../../../components/ProtectedRoute";
import { useProjects } from "../../../../hooks/useProjects"; // Importer useProjects

const Home = () => {
  const { accessToken, isLoading: authLoading } = useAuth();
  const { projects, loading: projectsLoading, error: projectsError } = useProjects(); // Utiliser useProjects

  const [userName, setUserName] = useState("User"); // Nom par défaut
  const [currentDate, setCurrentDate] = useState("");
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
    const formattedDate = today.toLocaleDateString("en-US", options);
    setCurrentDate(formattedDate);
  }, []);

  // Récupérer les détails de l'utilisateur
  useEffect(() => {
    const fetchUserDetails = async () => {
      if (authLoading) return; // Attendre que le token soit prêt
      if (!accessToken) {
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

  if (loading || projectsLoading) {
    return <div><img src="/loading.svg" alt="Loading" className="loading-img" />.</div>;
  }

  if (projectsError) {
    return <div>Erreur : {projectsError}</div>;
  }

  return (
    <ProtectedRoute>
      <div className="style-page-home-dash">
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