// src/app/user/dashboard/layout.tsx
"use client";
import { useEffect, useState } from "react";
import { usePathname } from "next/navigation";
import Link from "next/link";
import "../../../styles/UserDashboard.css";
import { useAuth } from "@/context/AuthContext";
import { useRouter } from "next/navigation";
import axios from "axios";
import ProtectedRoute from "../../../components/ProtectedRoute";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
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
  const currentPath = usePathname();
  const [user, setUser] = useState({ firstName: "", lastName: "" });
  const [isDropdownOpen, setIsDropdownOpen] = useState(false); // État pour le menu déroulant
  const { accessToken, isLoading } = useAuth();
  const router = useRouter();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [email, setEmail] = useState("");
  const [role, setRole] = useState("DEVELOPER");
  const [message, setMessage] = useState("");

  const handleOverlayClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) {
      setIsModalOpen(false);
    }
  };

  useEffect(() => {
    const fetchUserInfo = async () => {
      if (accessToken) {
        try {
          const response = await axios.get("http://localhost:8083/api/me", {
            headers: {
              Authorization: `Bearer ${accessToken}`,
            },
          });

          if (response.status === 200) {
            setUser(response.data);
          } else {
            console.error(
              "Erreur lors de la récupération des infos utilisateur"
            );
          }
        } catch (error) {
          console.error(
            "Erreur lors de la récupération des infos utilisateur",
            error
          );
        }
      }
    };

    if (!isLoading && accessToken) {
      fetchUserInfo();
    }
  }, [accessToken, isLoading]);

  const handleLogout = async () => {
    try {
      await axios.post(
        `${process.env.NEXT_PUBLIC_API_AUTHENTIFICATON_SERVICE_URL}/api/logout`,
        {},
        { withCredentials: true }
      );
      router.push("/authentification/signin");
      console.log("Déconnexion réussie");
    } catch (error) {
      console.error("Erreur lors de la déconnexion :", error);
    }
  };

  const toggleDropdown = () => {
    console.log("Clic détecté, isDropdownOpen avant :", isDropdownOpen);
    setIsDropdownOpen(!isDropdownOpen);
    console.log("isDropdownOpen après :", !isDropdownOpen);
  };

  if (isLoading) return <div>Chargement...</div>;

  return (
    <ProtectedRoute>
    <div className="container-manager">
      {/* Topbar */}
      <div className="topbar">
        <div className="topbar-icons">
          <i className="fa fa-list text-icon-list"></i>
          <span className="icon-container">
            <i className="fa fa-plus icon"></i>
            <span className="text-icon-add">add</span>
          </span>
        </div>
        <div className="search-container">
          <i className="fa fa-search search-icon"></i>
          <input type="text" placeholder="Search" className="search-bar" />
        </div>

        <div className="user-container">
          <span className="user-name" style={{ color: "white" }}>
            {user.firstName} {user.lastName}
            {""}
          </span>
          <div
            className="user-icon"
            onClick={toggleDropdown}
            onKeyDown={(e) => {
              if (e.key === "Enter" || e.key === " ") {
                toggleDropdown();
              }
            }}
            role="button"
            tabIndex={0}
          >
            {""}
            <i className="fa fa-user"></i>
          </div>

          {isDropdownOpen && (
            <div className="dropdown-menu">
              {/* En-tête avec les initiales et l'email */}
              <div className="dropdown-header">
                <div className="user-initials">
                  {user.firstName.charAt(0)}
                  {user.lastName.charAt(0)}
                </div>
                <div className="user-info">
                  <span className="user-fullname">
                    {user.firstName} {user.lastName}
                  </span>
                  <span className="user-email">aflousfatima@gmail.com</span>
                </div>
              </div>
              <div className="dropdown-divider"></div>
              {/* Options du menu */}
              <div className="dropdown-item">
                <i className="fa fa-tachometer-alt"></i>
                <span>Administration Console</span>
              </div>
              <div className="dropdown-item">
                <i className="fa fa-plus-circle"></i>
                <span>New Workspace</span>
              </div>
              <div className="dropdown-item">
                <i className="fa fa-user-plus"></i>
                <span>Invite to Join AGILIA</span>
              </div>
              <div className="dropdown-item">
                <i className="fa fa-user-circle"></i>
                <span>Profil</span>
              </div>
              <div className="dropdown-item">
                <i className="fa fa-cog"></i>
                <span>Settings</span>
              </div>
              <div className="dropdown-divider"></div>
              <div className="dropdown-item" onClick={handleLogout}>
                <i className="fa fa-sign-out-alt logout-icon"></i>
                <span>Log Out</span>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Conteneur pour la sidebar et le contenu principal */}
      <div className="main-wrapper">
        {/* Sidebar */}
        <div className="sidebar">
          <nav>
            <ul>
              <li
                className={
                  currentPath === "/user/dashboard/home" ? "active" : ""
                }
              >
                <Link href="/user/dashboard/home">
                  <i className="fa fa-house-user"></i>
                  <span>Home</span>
                </Link>
              </li>
              <li
                className={
                  currentPath === "/user/dashboard/my-tasks" ? "active" : ""
                }
              >
                <Link href="/user/dashboard/my-tasks">
                  <i className="fa fa-check-circle"></i>
                  <span>My Tasks</span>
                </Link>
              </li>
              <li
                className={
                  currentPath === "/user/dashboard/inbox" ? "active" : ""
                }
              >
                <Link href="/user/dashboard/inbox">
                  <i className="fa fa-bell"></i>
                  <span>Inbox</span>
                  <span className="notification-dot"></span>
                </Link>
              </li>

              {/* Section "INDICATEURS" */}
              <li className="section-title">
                <span className="section-title-style">Indicators</span>
                <button className="add-btn">+</button>
              </li>
              <li
                className={
                  currentPath === "/user/dashboard/reports" ? "active" : ""
                }
              >
                <Link href="/user/dashboard/reports">
                  <i className="fa fa-chart-line"></i>
                  <span>Reports</span>
                </Link>
              </li>
              <li
                className={
                  currentPath === "/user/dashboard/portfolio" ? "active" : ""
                }
              >
                <Link href="/user/dashboard/portfolio">
                  <i className="fa fa-folder"></i>
                  <span>Portfolios</span>
                </Link>
              </li>
              <li
                className={
                  currentPath === "/user/dashboard/goals" ? "active" : ""
                }
              >
                <Link href="/user/dashboard/goals">
                  <i className="fa fa-bullseye"></i>
                  <span>Goals</span>
                </Link>
              </li>

              {/* Section "PROJETS" */}
              <li className="section-title">
                <span className="section-title-style">Projects</span>
                <button className="add-btn">+</button>
              </li>

              {/* Section "ÉQUIPE" */}

              <li
                className={
                  currentPath === "/user/dashboard/teams" ? "active" : ""
                }
              >
                <Link href="/user/dashboard/teams">
                  <i className="fa fa-users"></i>{" "}
                  {/* Ajout d'une icône pour la cohérence */}
                  <span>Teams</span>
                </Link>
              </li>

              <div className="section-invite">
                <ul>
                  <li>
                    <span
                      className="invite-style"
                      onClick={() => setIsModalOpen(true)}
                    >
                      <i className="fa fa-envelope"></i> Invite colleagues
                    </span>
                  </li>
                </ul>

                {isModalOpen && (
                  <div
                    className="teams-modalOverlay"
                    onClick={handleOverlayClick}
                  >
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
                            Envoyer invitation
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
            </ul>
          </nav>
        </div>

        {/* Contenu principal */}
        <div className="main-content">{children}</div>
      </div>
    </div>
    </ProtectedRoute>
  );
}
