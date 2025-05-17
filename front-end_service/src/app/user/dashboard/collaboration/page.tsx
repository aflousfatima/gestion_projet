"use client";
import React, { useEffect, useState } from "react";
import { useAuth } from "@/context/AuthContext";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faMicrophone, faHashtag } from "@fortawesome/free-solid-svg-icons";
import "../../../../styles/Collaboration.css";
import { AUTH_SERVICE_URL , COLLABORATION_SERVICE_URL } from "../../../../config/useApi";
import useAxios from "../../../../hooks/useAxios";
interface Channel {
  id: string;
  name: string;
  type: "TEXT" | "VOICE";
  isPrivate: boolean;
  members: string[];
}

interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
}

interface Role {
  id: string;
  name: string;
}

const CollaborationPage: React.FC = () => {
  const { accessToken, isLoading } = useAuth();
  const axiosInstance = useAxios();
  const [channels, setChannels] = useState<Channel[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [roles, setRoles] = useState<Role[]>([
    { id: "role1", name: "Admin" },
    { id: "role2", name: "Member" },
  ]);
  const [showCreateModal, setShowCreateModal] = useState(true);
  const [newChannel, setNewChannel] = useState({
    name: "",
    type: "TEXT" as "TEXT" | "VOICE",
    isPrivate: false,
    members: [] as string[],
    roles: [] as string[],
    projectId: null as number | null,
  });
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedRoles, setSelectedRoles] = useState<{ [key: string]: string }>({});
  const [filteredUsers, setFilteredUsers] = useState<User[]>([]);
  const [step, setStep] = useState(1);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [currentUser, setCurrentUser] = useState<User | null>(null);

  // Récupérer l'utilisateur courant depuis le microservice d'authentification
  useEffect(() => {
    const fetchCurrentUser = async () => {
      if (accessToken) {
        try {
          const response = await axiosInstance.get(`${AUTH_SERVICE_URL}/api/me`, {
            headers: { Authorization: `Bearer ${accessToken}` },
          });
          setCurrentUser({
            id: response.data.id.toString(),
            firstName: response.data.firstName,
            lastName: response.data.lastName,
            email: response.data.email,
          });
        } catch (err: any) {
          setError(err.response?.data?.message || "Erreur lors de la récupération de l'utilisateur");
        }
      }
    };
    fetchCurrentUser();
  }, [accessToken, axiosInstance]);

  // Récupérer les canaux accessibles
  useEffect(() => {
    const fetchChannels = async () => {
      if (accessToken) {
        try {
          const response = await axiosInstance.get(`${COLLABORATION_SERVICE_URL}/api/channels/public`, {
            headers: { Authorization: `Bearer ${accessToken}` },
          });
          setChannels(
            response.data.map((channel: any) => ({
              id: channel.id.toString(),
              name: channel.name,
              type: channel.type,
              isPrivate: channel.isPrivate,
              members: channel.participants?.map((p: any) => p.id.toString()) || [],
            }))
          );
        } catch (err: any) {
          setError(err.response?.data?.message || "Erreur lors de la récupération des canaux");
        }
      }
    };
    fetchChannels();
  }, [accessToken, axiosInstance]);

  // Récupérer les utilisateurs
  useEffect(() => {
    const fetchUsers = async () => {
      try {
        const response = await axiosInstance.get(`${COLLABORATION_SERVICE_URL}/api/users`, {
          headers: { Authorization: `Bearer ${accessToken}` },
        });
        setUsers(
          response.data.map((user: any) => ({
            id: user.id.toString(),
            firstName: user.firstName,
            lastName: user.lastName,
            email: user.email,
          }))
        );
      } catch (err: any) {
        setError(err.response?.data?.message || "Erreur lors de la récupération des utilisateurs");
      }
    };
    fetchUsers();
  }, [axiosInstance, accessToken]);

  // Filtrer les utilisateurs pour la recherche
  useEffect(() => {
    const filtered = users.filter((user) =>
      `${user.firstName} ${user.lastName}`.toLowerCase().includes(searchQuery.toLowerCase())
    );
    setFilteredUsers(filtered);
  }, [searchQuery, users]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    setNewChannel((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? (e.target as HTMLInputElement).checked : value,
    }));
  };

  const handleRoleSelection = (userId: string, role: string) => {
    setSelectedRoles((prev) => ({ ...prev, [userId]: role }));
  };

  const handleAddMember = (userId: string) => {
    if (!newChannel.members.includes(userId)) {
      setNewChannel((prev) => ({
        ...prev,
        members: [...prev.members, userId],
      }));
    }
    setSearchQuery("");
  }

  const nextStep = () => {
    if (newChannel.name && newChannel.type) {
      setStep(2);
      setError(null);
    } else {
      setError("Veuillez remplir le nom et le type du canal");
    }
  };

  const prevStep = () => {
    setStep(1);
    setError(null);
  };

  if (isLoading || !currentUser) {
    return (
      <div className="loading-container">
        <img src="/loading.svg" alt="Loading" className="loading-img" />
      </div>
    );
  }

const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMessage(null);

    // Préparer la charge utile pour le backend
    const payload = {
      name: newChannel.name,
      type: newChannel.type,
      isPrivate: newChannel.isPrivate,
      projectId: newChannel.projectId, // null si aucun projet sélectionné
      participantIds: newChannel.members.map((id) => parseInt(id)), // Convertir les IDs en nombres
    };

    try {
      const response = await axiosInstance.post(`${COLLABORATION_SERVICE_URL}/api/channels/createChanel`, payload, {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      });

      const createdChannel = response.data;
      // Mettre à jour l'état local des channels
      setChannels((prev) => [
        ...prev,
        {
          id: createdChannel.id.toString(),
          name: createdChannel.name,
          type: createdChannel.type,
          isPrivate: createdChannel.isPrivate,
          members: createdChannel.participants?.map((p: any) => p.id.toString()) || [],
        },
      ]);
      setSuccessMessage("Canal créé avec succès !");
      setShowCreateModal(false);
      setNewChannel({ name: "", type: "TEXT", isPrivate: false, members: [], roles: [], projectId: null });
      setStep(1);
    } catch (err: any) {
      setError(err.response?.data?.message || "Erreur lors de la création du canal");
    }
  };


return (
    <div className="collaboration-container">
      {error && <div className="error-message">{error}</div>}
      {successMessage && <div className="success-message">{successMessage}</div>}

      {showCreateModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h2>Create New Channel</h2>
            <button className="close-btn" onClick={() => setShowCreateModal(false)}>
              ×
            </button>
            <div className="step-indicator">
              <div className={`step-circle ${step >= 1 ? "active" : ""}`}>
                <span>1</span>
                <p>Channel Details</p>
              </div>
              <div className="step-line"></div>
              <div className={`step-circle ${step >= 2 ? "active" : ""}`}>
                <span>2</span>
                <p>Members & Roles</p>
              </div>
            </div>

            {step === 1 ? (
              <form className="create-channel-form">
                <div className="form-group">
                  <label>Channel Name</label>
                  <div className="input-wrapper">
                    <input
                      type="text"
                      name="name"
                      value={newChannel.name}
                      onChange={handleInputChange}
                      placeholder="Enter channel name"
                      required
                    />
                    {newChannel.name.length > 0 && (
                      <span className="validation-icon">✔</span>
                    )}
                  </div>
                </div>
                <div className="form-group channel-type-group">
                  <label>Channel Type</label>
                  <div className="channel-type-options">
                    <label className={`type-option ${newChannel.type === "TEXT" ? "active" : ""}`}>
                      <input
                        type="radio"
                        name="type"
                        value="TEXT"
                        checked={newChannel.type === "TEXT"}
                        onChange={handleInputChange}
                        className="hidden-radio"
                      />
                      <span className="type-icon">
                        <FontAwesomeIcon icon={faHashtag} />
                      </span>
                      <span>Text</span>
                    </label>
                    <label className={`type-option ${newChannel.type === "VOICE" ? "active" : ""}`}>
                      <input
                        type="radio"
                        name="type"
                        value="VOICE"
                        checked={newChannel.type === "VOICE"}
                        onChange={handleInputChange}
                        className="hidden-radio"
                      />
                      <span className="type-icon">
                        <FontAwesomeIcon icon={faMicrophone} />
                      </span>
                      <span>Voice</span>
                    </label>
                  </div>
                </div>
                <div className="form-group toggle-group">
                  <label>Private Channel</label>
                  <label className="toggle-switch">
                    <input
                      type="checkbox"
                      name="isPrivate"
                      checked={newChannel.isPrivate}
                      onChange={handleInputChange}
                    />
                    <span className="slider"></span>
                  </label>
                  <span className="toggle-desc">
                    Restrict access to selected members and roles.
                  </span>
                </div>
                <div className="form-actions-collab">
                  <button
                    type="button"
                    className="cancel-btn"
                    onClick={() => setShowCreateModal(false)}
                  >
                    Cancel
                  </button>
                  <button type="button" className="next-btn" onClick={nextStep}>
                    Next
                  </button>
                </div>
              </form>
            ) : (
              <form className="create-channel-form" onSubmit={handleSubmit}>
                <div className="form-group">
                  <label>Associated Project</label>
                  <select
                    name="projectId"
                    value={newChannel.projectId ?? ""}
                    onChange={(e) =>
                      setNewChannel((prev) => ({
                        ...prev,
                        projectId: e.target.value ? parseInt(e.target.value) : null,
                      }))
                    }
                  >
                    <option value="">No project</option>
                    <option value="1">Project Alpha</option>
                    <option value="2">Project Beta</option>
                    <option value="3">Project Gamma</option>
                  </select>
                </div>
                {newChannel.isPrivate && (
                  <>
                    <div className="form-group">
                      <label>Members</label>
                      <div className="search-members">
                        <input
                          type="text"
                          placeholder="Search members..."
                          value={searchQuery}
                          onChange={(e) => setSearchQuery(e.target.value)}
                        />
                        {filteredUsers.length > 0 && (
                          <div className="suggestions">
                            {filteredUsers.map((user) => (
                              <div key={user.id} className="suggestion-item">
                                <span>
                                  {user.firstName} {user.lastName}
                                </span>
                                <select
                                  value={selectedRoles[user.id] || "member"}
                                  onChange={(e) => handleRoleSelection(user.id, e.target.value)}
                                >
                                  <option value="member">Member</option>
                                  <option value="admin">Admin</option>
                                </select>
                                <button
                                  className="add-btn"
                                  onClick={() => handleAddMember(user.id)}
                                >
                                  Add
                                </button>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>
                  </>
                )}
                <div className="form-actions-collab-2">
                  <button type="button" className="cancel-btn" onClick={prevStep}>
                    Back
                  </button>
                  <button type="submit" className="create-btn">
                    Create Channel
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default CollaborationPage;