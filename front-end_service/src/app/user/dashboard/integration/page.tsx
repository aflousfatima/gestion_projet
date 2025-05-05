"use client";
import React, { useEffect, useState } from "react";
import { useAuth } from "../../../../context/AuthContext";
import useAxios from "../../../../hooks/useAxios";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import "../../../../styles/Integrations.css";

const Integrations: React.FC = () => {
  const { accessToken, isLoading } = useAuth();
  const axiosInstance = useAxios();
  const [githubConnected, setGithubConnected] = useState(false);
  const [githubLoading, setGithubLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Vérifier l'état de la connexion GitHub
  useEffect(() => {
    const checkGithubConnection = async () => {
      if (!accessToken || isLoading) return;
      try {
        console.log("🔍 Vérification de la connexion GitHub...");
        const response = await axiosInstance.get(
          "http://localhost:8087/api/github-integration/check-token",
          {
            headers: { Authorization: `Bearer ${accessToken}` },
          }
        );
        console.log("✅ Réponse de /api/github/check-token:", response.data);
        setGithubConnected(response.data.hasToken);
      } catch (err: any) {
        console.error("Erreur lors de la vérification de la connexion GitHub:", err);
        setGithubConnected(false);
        setError(
          err.response?.data?.message ||
            "Erreur lors de la vérification de la connexion GitHub"
        );
      }
    };

    checkGithubConnection();
  }, [accessToken, isLoading, axiosInstance]);

  // Vérifier les paramètres d'URL pour les messages GitHub
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get("github") === "success") {
      setSuccessMessage("GitHub connecté avec succès !");
      setGithubConnected(true);
    } else if (params.get("github") === "error") {
      setError(params.get("message") || "Erreur lors de la connexion à GitHub");
    }
  }, []);

  // Gérer la connexion à GitHub
  const handleConnectGithub = async () => {
    setGithubLoading(true);
    setError(null);
    setSuccessMessage(null);
    try {
      console.log("🔄 Déclenchement de la connexion GitHub...");
      await axiosInstance.get(
        "http://localhost:8087/api/github-integration/oauth/login",
        {
          headers: { Authorization: `Bearer ${accessToken}` },
        }
      );
      // La redirection est gérée par le backend
    } catch (err: any) {
      console.error("Erreur lors de la connexion à GitHub:", err);
      setError(
        err.response?.data?.message || "Erreur lors de la connexion à GitHub"
      );
    } finally {
      setGithubLoading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="loading-container">
        <img src="/loading.svg" alt="Loading" className="loading-img" />
      </div>
    );
  }

  return (
    <div className="integrations-page">
      <div className="integrations-container">
        {error && <div className="error-message">{error}</div>}
        {successMessage && <div className="success-message">{successMessage}</div>}

        <div className="integration-card">
          <div className="card-header">
            <i className="fab fa-github github-icon"></i>
            <h2>GitHub Integration</h2>
          </div>
          <div className="card-body">
            {githubConnected ? (
              <p className="connected-message">
                Votre compte GitHub est connecté. Suivez vos commits et pull requests en temps réel.
              </p>
            ) : (
              <>
                <p className="description">
                  Connectez votre compte GitHub pour synchroniser vos projets, suivre les commits et gérer les pull requests directement depuis notre plateforme.
                </p>
                <button
                  className="connect-button"
                  onClick={handleConnectGithub}
                  disabled={githubLoading}
                >
   
                  <i className="fab fa-github button-icon"></i>
                  {githubLoading ? "Connexion..." : "Connecter GitHub"}
                </button>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Integrations;