"use client";
import React, { useEffect, useState } from "react";
import { useAuth } from "../../../../context/AuthContext";
import useAxios from "../../../../hooks/useAxios";

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
      console.log("🔍 Access Token:", accessToken);
      if (!accessToken || isLoading) {
        console.log("🔍 Pas de token ou chargement en cours, annulation...");
        return;
      }
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
          err.response?.data?.error ||
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
      setError(decodeURIComponent(params.get("message") || "Erreur lors de la connexion à GitHub"));
    }
  }, []);

  // Gérer la connexion à GitHub
  const handleConnectGithub = () => {
    setGithubLoading(true);
    setError(null);
    setSuccessMessage(null);
    try {
      console.log("🔄 Redirection vers l'authentification GitHub...");
      if (!accessToken) {
        throw new Error("Aucun token d'accès disponible. Veuillez vous connecter.");
      }
      // Ajouter le token comme paramètre d'URL (solution temporaire)
      const githubLoginUrl = `http://localhost:8087/api/github-integration/oauth/login?accessToken=${encodeURIComponent(accessToken)}`;
      console.log("🔄 Redirection vers:", githubLoginUrl);
      window.location.href = githubLoginUrl;
    } catch (err: any) {
      console.error("Erreur lors de la redirection GitHub:", err);
      setError(err.message || "Erreur lors de la connexion à GitHub");
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
                Your GitHub account is connected. Track your commits and pull requests in real time.
              </p>
            ) : (
              <>
                <p className="description">
                  Connect your GitHub account to sync your projects, track commits, and manage pull requests directly from our platform.
                </p>
                <button
                  className="connect-button"
                  onClick={handleConnectGithub}
                  disabled={githubLoading || !accessToken}
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