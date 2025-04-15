"use client";
import { useState, useEffect } from "react";
import "../../../../styles/Teams.css";
import { useAuth } from "../../../../context/AuthContext";
import useAxios from "../../../../hooks/useAxios";
import {
  AUTH_SERVICE_URL,
  PROJECT_SERVICE_URL,
} from "../../../../config/useApi";
import ProtectedRoute from "../../../../components/ProtectedRoute";
import { useProjects } from "../../../../hooks/useProjects";
import { AxiosError } from "axios";

// Interface pour les membres de l'équipe
interface TeamMember {
  id: string;
  firstName: string;
  lastName: string;
  role: string;
  project: string;
  avatar: string;
}

export default function Teams() {
  const { accessToken, isLoading: authLoading } = useAuth();
  const axiosInstance = useAxios();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [email, setEmail] = useState("");
  const [role, setRole] = useState("DEVELOPER");
  const [selectedProjectId, setSelectedProjectId] = useState<string>("");

  const [message, setMessage] = useState("");
  const [companyName, setCompanyName] = useState("");

  // État pour les membres de l'équipe
  const [teamMembers, setTeamMembers] = useState<TeamMember[]>([]);
  const [teamLoading, setTeamLoading] = useState(true);
  const [teamError, setTeamError] = useState<string | null>(null);

  // Utiliser le hook useProjects
  const {
    projects,
    loading: projectsLoading,
    error: projectsError,
  } = useProjects();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Mettre à jour les états de chargement et d'erreur pour les projets
  useEffect(() => {
    setLoading(projectsLoading);
    setError(projectsError);

    console.log("📋 Projets récupérés :", projects);

    if (projects.length > 0) {
      setSelectedProjectId(projects[0].id.toString());
      console.log("✅ Projet sélectionné par défaut (ID) :", projects[0].id);
    } else {
      console.log("⚠️ Aucun projet disponible, selectedProjectId reste vide.");
    }
  }, [projectsLoading, projectsError, projects]);

  // Récupérer le nom de l'entreprise
  useEffect(() => {
    const fetchCompanyName = async () => {
      if (authLoading || !accessToken) return;

      try {
        const authIdResponse = await axiosInstance.get(
          `${AUTH_SERVICE_URL}/api/user-id`
        );
        const authId = authIdResponse.data;
        console.log("🔑 Auth ID récupéré :", authId);

        const companyResponse = await axiosInstance.get(
          `${PROJECT_SERVICE_URL}/api/projects/by-manager?authId=${authId}`
        );

        const { companyName } = companyResponse.data;
        console.log("🏢 Nom de l'entreprise récupéré :", companyName);
        setCompanyName(companyName);
      } catch (err) {
        console.error(
          "❌ Erreur lors de la récupération du nom de l'entreprise:",
          err
        );
      }
    };

    fetchCompanyName();
  }, [accessToken, authLoading, axiosInstance]);

  // Récupérer les membres de l'équipe
  useEffect(() => {
    const fetchTeamMembers = async () => {
      if (authLoading || !accessToken) return;

      setTeamLoading(true);
      setTeamError(null);

      try {
        const response = await axiosInstance.get(
          `${AUTH_SERVICE_URL}/api/team-members`
        );
        console.log("👥 Membres de l'équipe récupérés :", response.data);
        setTeamMembers(response.data);
      } catch (err: unknown) {
        if (err instanceof AxiosError) {
          console.error(
            "❌ Erreur lors de la récupération des membres de l'équipe :",
            err
          );
          setTeamError(
            err.response?.data ||
              "Erreur lors de la récupération des membres de l'équipe."
          );
        } else {
          console.error("❌ Erreur inattendue :", err);
          setTeamError("Une erreur inattendue est survenue.");
        }
      } finally {
        setTeamLoading(false);
      }
    };

    fetchTeamMembers();
  }, [accessToken, authLoading, axiosInstance]);

  const handleInvite = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    console.log("📤 Envoi de l'invitation avec les données suivantes :");
    console.log("  Email :", email);
    console.log("  Rôle :", role);
    console.log("  Project ID :", selectedProjectId);
    console.log("  Entreprise :", companyName);

    try {
      await axiosInstance.post(`${AUTH_SERVICE_URL}/api/invitations`, {
        email,
        role,
        projectId: selectedProjectId,
        entreprise: companyName,
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      });

      setMessage("Invitation envoyée avec succès !");
      setEmail("");
      setRole("DEVELOPER");
      setIsModalOpen(false);
      console.log("✅ Invitation envoyée avec succès !");

      // Rafraîchir la liste des membres après une invitation réussie
      const response = await axiosInstance.get(
        `${AUTH_SERVICE_URL}/api/team-members`
      );
      setTeamMembers(response.data);
    } catch (err: unknown) {
      if (err instanceof AxiosError) {
        setMessage(
          "Erreur lors de l'envoi de l'invitation : " +
            (err.response?.data || err.message)
        );
        console.error("❌ Erreur lors de l'envoi de l'invitation :", err);
      } else {
        setMessage(
          "Une erreur inattendue est survenue lors de l'envoi de l'invitation."
        );
        console.error("❌ Erreur inattendue :", err);
      }
    }
  };

  const handleOverlayClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) {
      setIsModalOpen(false);
    }
  };

  if (loading) {
    return <div><img src="/loading.svg" alt="Loading" className="loading-img" /></div>;
  }

  if (error) {
    return (
      <div className="teams-container">
        <div className="teams-header">
          <h1 className="teams-title">Erreur</h1>
          <p>{error}</p>
        </div>
      </div>
    );
  }

  return (
    <ProtectedRoute>
      <div className="teams-container">
        <div className="teams-header">
          <h1 className="teams-title">
            Your Team at {companyName || "a Glance"}
          </h1>
          <p className="teams-subtitle">
            See all project members in one place for{" "}
            {companyName || "your company"}. Add new teammates and build your
            dream team!
          </p>
        </div>

        <div className="teams-actions">
          <button
            className="teams-inviteButton"
            onClick={() => setIsModalOpen(true)}
          >
            <i className="fa fa-envelope"></i> Inviter des collègues
          </button>
        </div>

        <div className="teams-teamList">
          {teamLoading ? (
            <p><img src="/loading.svg" alt="Loading" className="loading-img" /></p>
          ) : teamError ? (
            <p style={{ color: "red" }}>{teamError}</p>
          ) : teamMembers.length > 0 ? (
            teamMembers.map((member) => (
              <div key={member.id} className="teams-teamMemberCard">
                <div className="teams-memberAvatar">
                  <img
                    src={member.avatar}
                    alt={`${member.firstName} ${member.lastName}`}
                  />
                </div>
                <div className="teams-memberInfo">
                  <h3 className="teams-memberName">
                    {member.firstName} {member.lastName}
                  </h3>
                  <p className="teams-memberRole">Rôle : {member.role}</p>
                  <p className="teams-memberProject">
                    Projet : {member.project}
                  </p>
                </div>
              </div>
            ))
          ) : (
            <p>Aucun membre d équipe trouvé.</p>
          )}
        </div>

        {isModalOpen && (
          <div className="teams-modalOverlay" onClick={handleOverlayClick}>
            <div className="teams-modal">
              <h2 className="teams-modalTitle">Inviter un collègue</h2>
              <form onSubmit={handleInvite} className="teams-form">
                <div className="teams-formGroup">
                  <label htmlFor="email">Email</label>
                  <input
                    type="email"
                    id="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="Entrez l'email du collègue"
                    required
                  />
                </div>
                <div className="teams-formGroup">
                  <label htmlFor="project">Projet</label>
                  <select
                    id="project"
                    value={selectedProjectId}
                    onChange={(e) => {
                      setSelectedProjectId(e.target.value);
                      console.log(
                        "🔄 Projet sélectionné (ID) :",
                        e.target.value
                      );
                    }}
                  >
                    {projects.length > 0 ? (
                      projects.map((project) => (
                        <option key={project.id} value={project.id}>
                          {project.name}
                        </option>
                      ))
                    ) : (
                      <option value="">Aucun projet disponible</option>
                    )}
                  </select>
                </div>
                <div className="teams-formGroup">
                  <label htmlFor="role">Rôle</label>
                  <select
                    id="role"
                    value={role}
                    onChange={(e) => {
                      setRole(e.target.value);
                      console.log("🔄 Rôle sélectionné :", e.target.value);
                    }}
                  >
                    <option value="DEVELOPER">Développeur</option>
                    <option value="TESTER">Testeur</option>
                    <option value="DEVOPS">DevOps</option>
                    <option value="DESIGNER">Designer</option>
                    <option value="CLOUD_ENGINEER">Ingenieur Cloud</option>
                    <option value="FRONTEND_DEVELOPPER">Develppeur Front End </option>
                    <option value="SCRUM_MASTER">Scrum Master</option>
                    <option value="PRODUCT_OWNER">Product Owner</option>
                  </select>
                </div>
                {message && <p className="teams-message">{message}</p>}
                <div className="teams-modalActions">
                  <button type="submit" className="teams-submitButton">
                    Envoyer linvitation
                  </button>
                  <button
                    type="button"
                    className="teams-cancelButton"
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
    </ProtectedRoute>
  );
}
