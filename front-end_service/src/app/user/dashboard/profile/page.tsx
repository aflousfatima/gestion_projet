"use client";
import React, { useEffect, useState } from "react";
import { useAuth } from "@/context/AuthContext";
import useAxios from "@/hooks/useAxios";
import Link from "next/link";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faUser,
  faTasks,
  faChartBar,
  faEdit,
  faSave,
  faTimes,
} from "@fortawesome/free-solid-svg-icons";
import "../../../../styles/Dashboard-User-Profil.css";

interface User {
  id?: string;
  firstName: string;
  lastName: string;
  email: string;
  role?: string;
  avatar?: string;
  bio?: string;
  phone?: string;
  notificationPreferences?: {
    emailNotifications: boolean;
    taskUpdates: boolean;
    deadlineReminders: boolean;
  };
}

interface Task {
  id: string;
  title: string;
  status: string;
  dueDate: string;
}

const ProfilePage: React.FC = () => {
  const { accessToken, isLoading } = useAuth();
  const axiosInstance = useAxios();
  const [user, setUser] = useState<User>({
    firstName: "",
    lastName: "",
    email: "",
    role: "",
    avatar: "",
    bio: "",
    phone: "",
    notificationPreferences: {
      emailNotifications: true,
      taskUpdates: true,
      deadlineReminders: true,
    },
  });
  const [isEditing, setIsEditing] = useState(false);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [stats, setStats] = useState({
    tasksCompleted: 0,
    tasksInProgress: 0,
    sprintsContributed: 0,
  });
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Récupérer les informations de l'utilisateur
  useEffect(() => {
    const fetchUserInfo = async () => {
      if (!accessToken) return;
      try {
        const response = await axiosInstance.get("http://localhost:8083/api/me", {
          headers: { Authorization: `Bearer ${accessToken}` },
        });
        setUser({
          ...response.data,
          notificationPreferences: response.data.notificationPreferences || {
            emailNotifications: true,
            taskUpdates: true,
            deadlineReminders: true,
          },
        });
      } catch (err) {
        setError("Erreur lors de la récupération des informations utilisateur");
        console.error(err);
      }
    };

    const fetchUserTasks = async () => {
      try {
        const response = await axiosInstance.get(
          "http://localhost:8082/api/project/tasks/assigned",
          {
            headers: { Authorization: `Bearer ${accessToken}` },
          }
        );
        setTasks(response.data);
      } catch (err) {
        console.error("Erreur lors de la récupération des tâches", err);
      }
    };

    const fetchUserStats = async () => {
      try {
        const response = await axiosInstance.get(
          "http://localhost:8082/api/project/user-stats",
          {
            headers: { Authorization: `Bearer ${accessToken}` },
          }
        );
        setStats(response.data);
      } catch (err) {
        console.error("Erreur lors de la récupération des statistiques", err);
      }
    };

    if (!isLoading && accessToken) {
      fetchUserInfo();
      fetchUserTasks();
      fetchUserStats();
    }
  }, [accessToken, isLoading, axiosInstance]);

  // Gérer les changements dans le formulaire
  const handleInputChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = e.target;
    setUser((prev) => ({ ...prev, [name]: value }));
  };

  const handleNotificationChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, checked } = e.target;
    setUser((prev) => ({
      ...prev,
      notificationPreferences: {
        ...prev.notificationPreferences,
        [name]: checked,
      },
    }));
  };

  // Soumettre les modifications
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await axiosInstance.put(
        "http://localhost:8083/api/user/update",
        user,
        {
          headers: { Authorization: `Bearer ${accessToken}` },
        }
      );
      setSuccessMessage("Profil mis à jour avec succès !");
      setIsEditing(false);
      setError(null);
    } catch (err) {
      setError("Erreur lors de la mise à jour du profil");
      setSuccessMessage(null);
      console.error(err);
    }
  };

  // Gérer le téléchargement d'un avatar
  const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      const formData = new FormData();
      formData.append("avatar", file);
      try {
        const response = await axiosInstance.post(
          "http://localhost:8083/api/user/upload-avatar",
          formData,
          {
            headers: {
              Authorization: `Bearer ${accessToken}`,
              "Content-Type": "multipart/form-data",
            },
          }
        );
        setUser((prev) => ({ ...prev, avatar: response.data.avatarUrl }));
        setSuccessMessage("Avatar mis à jour avec succès !");
      } catch (err) {
        setError("Erreur lors du téléchargement de l'avatar");
        console.error(err);
      }
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
    <div className="profile-container">
      <div className="profile-header">
        <h1 className="profile-title">
          Profil de {user.firstName} {user.lastName}
        </h1>
        <Link href="/user/dashboard/home" className="back-btn">
          <FontAwesomeIcon icon={faTimes} /> Retour
        </Link>
      </div>

      {error && <div className="error-message">{error}</div>}
      {successMessage && <div className="success-message">{successMessage}</div>}

      <div className="profile-content">
        {/* Section Informations personnelles */}
        <div className="profile-card full-width">
          <div className="card-header">
            <h2>
              <FontAwesomeIcon icon={faUser} /> Informations personnelles
            </h2>
            {!isEditing ? (
              <button className="edit-btn" onClick={() => setIsEditing(true)}>
                <FontAwesomeIcon icon={faEdit} /> Modifier
              </button>
            ) : (
              <div>
                <button className="save-btn" onClick={handleSubmit}>
                  <FontAwesomeIcon icon={faSave} /> Enregistrer
                </button>
                <button className="cancel-btn" onClick={() => setIsEditing(false)}>
                  <FontAwesomeIcon icon={faTimes} /> Annuler
                </button>
              </div>
            )}
          </div>

          <div className="card-body">
            <div className="avatar-section">
              <img
                src={user.avatar || "/default-avatar.png"}
                alt="Avatar"
                className="avatar"
              />
              {isEditing && (
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleAvatarChange}
                  className="avatar-upload"
                />
              )}
            </div>

            {isEditing ? (
              <form className="profile-form">
                <div className="form-group">
                  <label>Prénom</label>
                  <input
                    type="text"
                    name="firstName"
                    value={user.firstName}
                    onChange={handleInputChange}
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Nom</label>
                  <input
                    type="text"
                    name="lastName"
                    value={user.lastName}
                    onChange={handleInputChange}
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Email</label>
                  <input
                    type="email"
                    name="email"
                    value={user.email}
                    disabled
                  />
                </div>
                
                
              </form>
            ) : (
              <div className="info-list">
                <p><strong>Prénom :</strong> {user.firstName}</p>
                <p><strong>Nom :</strong> {user.lastName}</p>
                <p><strong>Email :</strong> {user.email}</p>
               
              </div>
            )}
          </div>
        </div>

        {/* Section Tâches assignées */}
        <div className="profile-card">
          <div className="card-header">
            <h2>
              <FontAwesomeIcon icon={faTasks} /> Tâches assignées
            </h2>
          </div>
          <div className="card-body">
            {tasks.length > 0 ? (
              <table className="tasks-table">
                <thead>
                  <tr>
                    <th>Titre</th>
                    <th>Statut</th>
                    <th>Date d'échéance</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {tasks.slice(0, 5).map((task) => (
                    <tr key={task.id}>
                      <td>{task.title}</td>
                      <td>{task.status}</td>
                      <td>{new Date(task.dueDate).toLocaleDateString()}</td>
                      <td>
                        <Link
                          href={`/user/dashboard/tasks/${task.id}`}
                          className="view-task-btn"
                        >
                          Voir
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p>Aucune tâche assignée.</p>
            )}
            {tasks.length > 5 && (
              <Link href="/user/dashboard/my-tasks" className="see-all-btn">
                Voir toutes les tâches
              </Link>
            )}
          </div>
        </div>

        {/* Section Statistiques */}
        <div className="profile-card">
          <div className="card-header">
            <h2>
              <FontAwesomeIcon icon={faChartBar} /> Statistiques de contribution
            </h2>
          </div>
          <div className="card-body">
            <div className="stats-list">
              <p><strong>Tâches terminées :</strong> {stats.tasksCompleted}</p>
              <p><strong>Tâches en cours :</strong> {stats.tasksInProgress}</p>
              <p><strong>Sprints contribués :</strong> {stats.sprintsContributed}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProfilePage;