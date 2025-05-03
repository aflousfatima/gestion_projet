"use client";
import React, { useEffect, useState } from "react";
import { useAuth } from "@/context/AuthContext";
import useAxios from "@/hooks/useAxios";
import Link from "next/link";
import { AUTH_SERVICE_URL } from "../../../../config/useApi";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faUser,
  faEnvelope,
  faEdit,
  faSave,
  faTimes,
  faLock,
  faBell,
  faProjectDiagram,
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

interface ProjectRole {
  projectId: number;
  projectName: string;
  roleInProject: string;
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
  const [projectRoles, setProjectRoles] = useState<ProjectRole[]>([]);
  const [isEditing, setIsEditing] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [passwordData, setPasswordData] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Récupérer les informations de l'utilisateur
  useEffect(() => {
    const fetchUserInfo = async () => {
      if (!accessToken || isLoading) return;
      try {
        console.log("🔍 Récupération des détails de l'utilisateur...");
        const response = await axiosInstance.get(`${AUTH_SERVICE_URL}/api/me`, {
          headers: { Authorization: `Bearer ${accessToken}` },
        });
        console.log("✅ Réponse de /api/me:", response.data);
        setUser({
          ...response.data,
          avatar: response.data.avatar || `https://ui-avatars.com/api/?name=${response.data.firstName?.charAt(0) || 'U'}+${response.data.lastName?.charAt(0) || 'U'}`,
          notificationPreferences: response.data.notificationPreferences || {
            emailNotifications: true,
            taskUpdates: true,
            deadlineReminders: true,
          },
        });
      } catch (err: any) {
        console.error("Erreur lors de la récupération des infos utilisateur:", err);
        setError(err.response?.data?.message || "Erreur lors de la récupération des informations utilisateur");
      }
    };

    fetchUserInfo();
  }, [accessToken, isLoading, axiosInstance]);

  // Récupérer les rôles dans les projets
  useEffect(() => {
    const fetchProjectRoles = async () => {
      if (!accessToken || isLoading || !user.id) return;
      try {
        console.log("🔍 Récupération des projets pour authId:", user.id);
        const response = await axiosInstance.get(
          "http://localhost:8085/api/projects/by-user",
          {
            headers: { Authorization: `Bearer ${accessToken}` },
            params: { authId: user.id },
          }
        );
        console.log("✅ Réponse de /projects/by-user:", response.data);
        const projectRoles = response.data.projects.map((project: any) => ({
          projectId: project.id,
          projectName: project.name,
          roleInProject: project.roleInProject,
        }));
        console.log("✅ Rôles de projet finaux:", projectRoles);
        setProjectRoles(projectRoles);
      } catch (err: any) {
        console.error("Erreur lors de la récupération des rôles de projet:", err);
        setError(err.response?.data?.message || "Erreur lors de la récupération des rôles de projet");
      }
    };

    fetchProjectRoles();
  }, [accessToken, isLoading, axiosInstance, user.id]);

  // Gérer les changements dans le formulaire utilisateur
  const handleInputChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = e.target;
    setUser((prev) => ({ ...prev, [name]: value }));
  };

  // Gérer les changements dans les préférences de notification
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

  // Gérer les changements dans le formulaire de mot de passe
  const handlePasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setPasswordData((prev) => ({ ...prev, [name]: value }));
  };

  // Soumettre les modifications du profil
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
      setSuccessMessage("Profile updated successfully!");
      setIsEditing(false);
      setError(null);
    } catch (err) {
      setError("Error updating profile");
      setSuccessMessage(null);
      console.error(err);
    }
  };

  // Soumettre le changement de mot de passe
  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (passwordData.newPassword !== passwordData.confirmPassword) {
      setError("New passwords do not match");
      return;
    }
    try {
      await axiosInstance.post(
        "http://localhost:8083/api/user/change-password",
        {
          currentPassword: passwordData.currentPassword,
          newPassword: passwordData.newPassword,
        },
        {
          headers: { Authorization: `Bearer ${accessToken}` },
        }
      );
      setSuccessMessage("Password changed successfully!");
      setIsChangingPassword(false);
      setPasswordData({ currentPassword: "", newPassword: "", confirmPassword: "" });
      setError(null);
    } catch (err) {
      setError("Error changing password");
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
        setSuccessMessage("Avatar updated successfully!");
      } catch (err) {
        setError("Error uploading avatar");
        console.error(err);
      }
    }
  };

  // Générer les initiales pour le placeholder
  const getInitials = () => {
    const firstInitial = user.firstName && user.firstName !== "Inconnu" ? user.firstName.charAt(0) : "U";
    const lastInitial = user.lastName && user.lastName !== "Inconnu" ? user.lastName.charAt(0) : "U";
    return `${firstInitial}${lastInitial}`.toUpperCase();
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
      {error && <div className="error-message">{error}</div>}
      {successMessage && <div className="success-message">{successMessage}</div>}

      <div className="profile-content">
        {/* Section Informations personnelles */}
        <div className="profile-card full-width">
          <div className="card-header">
            <h2>
              <FontAwesomeIcon icon={faUser} /> Personal Informations
            </h2>
            {!isEditing ? (
              <button className="edit-btn" onClick={() => setIsEditing(true)}>
                <FontAwesomeIcon icon={faEdit} /> Edit
              </button>
            ) : (
              <div className="form-actions">
                <button className="save-btn" onClick={handleSubmit}>
                  <FontAwesomeIcon icon={faSave} /> Save
                </button>
                <button className="cancel-btn" onClick={() => setIsEditing(false)}>
                  <FontAwesomeIcon icon={faTimes} /> Cancel
                </button>
              </div>
            )}
          </div>
          <div className="card-body">
            <div className="avatar-section">
              {user.avatar && user.avatar !== `https://ui-avatars.com/api/?name=U+U` ? (
                <img
                  src={user.avatar}
                  alt={`${user.firstName} ${user.lastName}`}
                  className="avatar"
                />
              ) : (
                <div className="user-avatar-placeholder">{getInitials()}</div>
              )}
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
                <div className="form-columns">
                  <div className="form-column">
                    <div className="form-group">
                      <label>First Name</label>
                      <input
                        type="text"
                        name="firstName"
                        value={user.firstName}
                        onChange={handleInputChange}
                        required
                      />
                    </div>
                    <div className="form-group">
                      <label>Last Name</label>
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
                  </div>
                </div>
              </form>
            ) : (
              <div className="info-columns">
                <div className="info-column">
                  <p><strong>First Name:</strong> {user.firstName || "Not defined"}</p>
                  <p><strong>Last Name:</strong> {user.lastName || "Not defined"}</p>
                  <p><strong>Email:</strong> {user.email || "Not defined"}</p>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Section Sécurité */}
        <div className="profile-card">
          <div className="card-header">
            <h2>
              <FontAwesomeIcon icon={faLock} /> Security
            </h2>
            <button
              className="edit-btn"
              onClick={() => setIsChangingPassword(true)}
            >
              <FontAwesomeIcon icon={faEdit} /> Change Password
            </button>
          </div>
          <div className="card-body">
            {isChangingPassword ? (
              <form className="password-form" onSubmit={handlePasswordSubmit}>
                <div className="form-group">
                  <label>Current Password</label>
                  <input
                    type="password"
                    name="currentPassword"
                    value={passwordData.currentPassword}
                    onChange={handlePasswordChange}
                    required
                  />
                </div>
                <div className="form-group">
                  <label>New Password</label>
                  <input
                    type="password"
                    name="newPassword"
                    value={passwordData.newPassword}
                    onChange={handlePasswordChange}
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Confirm New Password</label>
                  <input
                    type="password"
                    name="confirmPassword"
                    value={passwordData.confirmPassword}
                    onChange={handlePasswordChange}
                    required
                  />
                </div>
                <div className="form-actions">
                  <button type="submit" className="save-btn">
                    <FontAwesomeIcon icon={faSave} /> Save
                  </button>
                  <button
                    type="button"
                    className="cancel-btn"
                    onClick={() => setIsChangingPassword(false)}
                  >
                    <FontAwesomeIcon icon={faTimes} /> Cancel
                  </button>
                </div>
              </form>
            ) : (
              <p>Manage your account security by changing your password.</p>
            )}
          </div>
        </div>

        {/* Section Rôles dans les projets */}
        <div className="profile-card">
          <div className="card-header">
            <h2>
              <FontAwesomeIcon icon={faProjectDiagram} /> Roles in Projects
            </h2>
          </div>
          <div className="card-body">
            {projectRoles.length > 0 ? (
              <table className="project-roles-table">
                <thead>
                  <tr>
                    <th>Project</th>
                    <th>Role</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {projectRoles.map((role) => (
                    <tr key={role.projectId}>
                      <td>{role.projectName}</td>
                      <td>{role.roleInProject}</td>
                      <td>
                        <Link
                          href={`/user/dashboard/projects/${role.projectId}`}
                          className="view-project-btn"
                        >
                          View Project
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p>No roles in projects.</p>
            )}
          </div>
        </div>

        {/* Section Préférences de notification */}
        <div className="profile-card">
          <div className="card-header">
            <h2>
              <FontAwesomeIcon icon={faBell} /> Notification Preferences
            </h2>
          </div>
          <div className="card-body">
            <form className="notification-form">
              <div className="form-group">
                <label>
                  <input
                    type="checkbox"
                    name="emailNotifications"
                    checked={user.notificationPreferences?.emailNotifications}
                    onChange={handleNotificationChange}
                    disabled={!isEditing}
                  />
                  Email Notifications
                </label>
              </div>
              <div className="form-group">
                <label>
                  <input
                    type="checkbox"
                    name="taskUpdates"
                    checked={user.notificationPreferences?.taskUpdates}
                    onChange={handleNotificationChange}
                    disabled={!isEditing}
                  />
                  Task Updates
                </label>
              </div>
              <div className="form-group">
                <label>
                  <input
                    type="checkbox"
                    name="deadlineReminders"
                    checked={user.notificationPreferences?.deadlineReminders}
                    onChange={handleNotificationChange}
                    disabled={!isEditing}
                  />
                  Deadline Reminders
                </label>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProfilePage;