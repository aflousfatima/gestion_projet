"use client";
import { useState } from "react";
import axios from "axios";
import "../../../../styles/Teams.css";

// Simuler une liste de membres (à remplacer par une API plus tard)
const teamMembers = [
  {
    id: 1,
    firstName: "Fatima",
    lastName: "Aflous",
    role: "DevOps",
    project: "Projet C",
    avatar: "https://ui-avatars.com/api/?name=Fat+Af", // Placeholder pour la photo
  },
  {
    id: 2,
    firstName: "Narjiss",
    lastName: "Elmekadem",
    role: "Développeur",
    project: "Projet A",
    avatar: "https://ui-avatars.com/api/?name=Narji+El", // Placeholder pour la photo
  },
  {
    id: 3,
    firstName: "Khadija Chakkour",
    lastName: "Smith",
    role: "Testeur",
    project: "Projet B",
    avatar: "https://ui-avatars.com/api/?name=Khad+Chak", // Placeholder pour la photo
  },
];

export default function Teams() {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [email, setEmail] = useState("");
  const [role, setRole] = useState("DEVELOPER");
  const [project, setProject] = useState("Project A");
  const [message, setMessage] = useState("");

  const handleInvite = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    try {
      await axios.post("http://localhost:8083/api/invitations", {
        email,
        role,
        entrepriseId: "123",
      });
      setMessage("Invitation envoyée avec succès !");
      setEmail("");
      setRole("DEVELOPER");
      setIsModalOpen(false);
    } catch (error) {
      setMessage(
        "Erreur lors de l'envoi de l'invitation : " + (error as Error).message
      );
    }
  };

  const handleOverlayClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) {
      setIsModalOpen(false);
    }
  };

  return (
    <div className="teams-container">
      <div className="teams-header">
        <h1 className="teams-title">Your Team at a Glance</h1>
        <p className="teams-subtitle">
          See all project members in one place. Add new teammates and build your
          dream team!.
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
        {teamMembers.map((member) => (
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
              <p className="teams-memberProject">Projet : {member.project}</p>
            </div>
          </div>
        ))}
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
                <label htmlFor="project">Project</label>
                <select
                  id="project"
                  value={project}
                  onChange={(e) => setProject(e.target.value)}
                >
                  <option value="Project A">Project A</option>
                  <option value="TESTER">Testeur</option>
                  <option value="DEVOPS">DevOps</option>
                  <option value="DESIGNER">Designer</option>
                </select>
              </div>
              <div className="teams-formGroup">
                <label htmlFor="role">Rôle</label>
                <select
                  id="role"
                  value={role}
                  onChange={(e) => setRole(e.target.value)}
                >
                  <option value="DEVELOPER">Développeur</option>
                  <option value="TESTER">Testeur</option>
                  <option value="DEVOPS">DevOps</option>
                  <option value="DESIGNER">Designer</option>
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
  );
}
