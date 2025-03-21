// src/app/user/dashboard/layout.tsx
"use client";
import { useEffect, useState } from "react";
import { usePathname } from "next/navigation";
import Link from "next/link";
import "../../../styles/UserDashboard.css";
import { useAuth } from "@/context/AuthContext";
import { useRouter } from "next/navigation";

import axios from "axios";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const currentPath = usePathname();
  const [user, setUser] = useState({ firstName: "", lastName: "" });
  const { accessToken, isLoading } = useAuth(); // Utilisation du contexte pour récupérer le token
  const router = useRouter();
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

  if (isLoading) return <div>Chargement...</div>;
  return (
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
          <i className="fa fa-user"></i>
          <span>
            {user.firstName} {user.lastName}
            <button onClick={handleLogout}>Déconnexion</button>
          </span>
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
              <li className="section-title">
                <span className="section-title-style">Teams</span>
              </li>
              <li
                className={
                  currentPath === "/user/dashboard/teams" ? "active" : ""
                }
              >
                <Link href="/user/dashboard/teams">
                  <span className="invite-style">
                    <i className="fa fa-envelope"></i>
                    Invite colleagues
                  </span>
                </Link>
              </li>
            </ul>
          </nav>
        </div>

        {/* Contenu principal */}
        <div className="main-content">{children}</div>
      </div>
    </div>
  );
}
