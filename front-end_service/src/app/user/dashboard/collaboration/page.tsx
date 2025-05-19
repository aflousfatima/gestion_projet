"use client";
import React, { useEffect, useState, useRef } from "react";
import { useAuth } from "@/context/AuthContext";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faMicrophone, faHashtag } from "@fortawesome/free-solid-svg-icons";
import Link from "next/link";
import "../../../../styles/Collaboration.css";
import { AUTH_SERVICE_URL, PROJECT_SERVICE_URL ,COLLABORATION_SERVICE_URL } from "../../../../config/useApi";
import useAxios from "../../../../hooks/useAxios";
import { useSearchParams, useRouter } from "next/navigation";

interface Channel {
  id: string;
  name: string;
  type: "TEXT" | "VOCAL";
  isPrivate: boolean;
  members: string[];
}

interface User {
  id: string;
  firstName: string;
  lastName: string;
  avatar?: string;
  email: string
}

interface Project {
  id: string;
  name: string;
  description?: string;
  creationDate?: string;
}

interface Role {
  id: string;
  name: string;
}

const CollaborationPage: React.FC = () => {
  const { accessToken } = useAuth();
  const axiosInstance = useAxios();
  const searchParams = useSearchParams();
  const router = useRouter();
  const [users, setUsers] = useState<User[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [roles] = useState<Role[]>([
    { id: "member", name: "Member" },
    { id: "admin", name: "Admin" },
  ]);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newChannel, setNewChannel] = useState({
    name: "",
    type: "TEXT" as "TEXT" | "VOCAL",
    isPrivate: false,
    members: [] as string[],
    roles: [] as string[],
    projectId: null as number | null,
  });
  const [searchQuery, setSearchQuery] = useState("");
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [selectedRoles, setSelectedRoles] = useState<{ [key: string]: string }>({});
  const [filteredUsers, setFilteredUsers] = useState<User[]>([]);
  const [step, setStep] = useState(1);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const searchRef = useRef<HTMLDivElement>(null);

  // Handle modal visibility
  useEffect(() => {
    const shouldShowModal = searchParams.get("create") === "true";
    setShowCreateModal(shouldShowModal);
  }, [searchParams]);

  // Fetch current user
  useEffect(() => {
    const fetchCurrentUser = async () => {
      if (accessToken && !currentUser) {
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
          console.error("Failed to fetch current user:", err);
          setError(err.response?.data?.message || "Erreur lors de la récupération de l'utilisateur");
        }
      }
    };
    fetchCurrentUser();
  }, [accessToken, axiosInstance, currentUser]);

  // Fetch projects
  useEffect(() => {
    const fetchProjects = async () => {
      try {
        const response = await axiosInstance.get(`${PROJECT_SERVICE_URL}/api/projects/AllProjects`, {
          headers: { Authorization: `Bearer ${accessToken}` },
        });
        setProjects(
          response.data.map((project: any) => ({
            id: project.id.toString(),
            name: project.name,
            description: project.description,
            creationDate: project.creationDate,
          }))
        );
      } catch (err: any) {
        setError(err.response?.data?.message || "Erreur lors de la récupération des projets");
      }
    };
    fetchProjects();
  }, [axiosInstance, accessToken]);

  // Fetch team members
  useEffect(() => {
    const fetchTeamMembers = async () => {
      try {
        const response = await axiosInstance.get(`${AUTH_SERVICE_URL}/api/team-members`, {
          headers: { Authorization: `Bearer ${accessToken}` },
        });
        setUsers(
          response.data.map((user: any) => ({
            id: user.id.toString(),
            firstName: user.firstName,
            lastName: user.lastName,
            avatar: user.avatar || `https://ui-avatars.com/api/?name=${user.firstName.charAt(0)}+${user.lastName.charAt(0)}`,
          }))
        );
      } catch (err: any) {
        setError(err.response?.data?.message || "Erreur lors de la récupération des membres de l'équipe");
      }
    };
    fetchTeamMembers();
  }, [axiosInstance, accessToken]);

  // Filter users for search
  useEffect(() => {
    const filtered = users.filter((user) =>
      `${user.firstName} ${user.lastName}`.toLowerCase().includes(searchQuery.toLowerCase())
    );
    setFilteredUsers(filtered);
    setShowSuggestions(searchQuery.length > 0 && filtered.length > 0);
  }, [searchQuery, users]);

  // Close suggestions on click outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

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
        roles: [...prev.roles, selectedRoles[userId] || "member"],
      }));
    }
    setSearchQuery("");
    setShowSuggestions(false);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMessage(null);

    const payload = {
      name: newChannel.name,
      type: newChannel.type,
      isPrivate: newChannel.isPrivate,
      projectId: newChannel.projectId,
      participantIds: newChannel.members, // Send as strings
      roles: newChannel.roles, // Liste des rôles
      createdBy: currentUser?.id || "",
    };

    try {
      await axiosInstance.post(`${COLLABORATION_SERVICE_URL}/api/channels/createChanel`, payload, {
        headers: { Authorization: `Bearer ${accessToken}` },
      });
      setSuccessMessage("Canal créé avec succès !");
      setShowCreateModal(false);
      router.replace("/user/dashboard/collaboration", { scroll: false });
      setNewChannel({ name: "", type: "TEXT", isPrivate: false, members: [], roles: [], projectId: null });
      setStep(1);
    } catch (err: any) {
      console.error("Channel creation error:", err.response?.data);
      setError(err.response?.data?.message || "Erreur lors de la création du canal");
    }
  };

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

  const closeModal = () => {
    setShowCreateModal(false);
    router.replace("/user/dashboard/collaboration", { scroll: false });
  };

  if (!currentUser) {
    return null;
  }

  return (
    <div className="collaboration-container">
      {error && <div className="error-message">{error}</div>}
      {successMessage && <div className="success-message">{successMessage}</div>}

      {!showCreateModal && (
        <div className="welcome-section">
          <div className="wave-bg"></div>
          <div className="content-wrapper">
            <div className="content-left">
              <h2 className="dashboard-title">Unite Your Team</h2>
              <p className="dashboard-subtitle">Create channels to collaborate, communicate, and achieve greatness.</p>
              <ul className="feature-list">
                <li className="feature-item3">
                  <svg className="feature-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="#ffffff" strokeWidth="2" />
                    <path d="M2 17L12 22L22 17" stroke="#ffffff" strokeWidth="2" />
                    <path d="M2 12L12 17L22 12" stroke="#ffffff" strokeWidth="2" />
                  </svg>
                  <div>
                    <h3 className="feature-title">Team Projects</h3>
                    <p className="feature-desc">Streamline tasks and shared objectives.</p>
                  </div>
                </li>
                <li className="feature-item1">
                  <FontAwesomeIcon icon={faHashtag} className="feature-icon" />
                  <div>
                    <h3 className="feature-title">Text Channels</h3>
                    <p className="feature-desc">Instant messaging for seamless team chats.</p>
                  </div>
                </li>
                <li className="feature-item2">
                  <FontAwesomeIcon icon={faMicrophone} className="feature-icon" />
                  <div>
                    <h3 className="feature-title">Voice Channels</h3>
                    <p className="feature-desc">Clear voice calls for lively discussions.</p>
                  </div>
                </li>
              </ul>
              <Link href="/user/dashboard/collaboration?create=true" aria-label="Create a new collaboration channel">
                <button className="dashboard-cta">Launch a Channel</button>
              </Link>
            </div>
            <div className="content-right">
              <img src="/collab-pic.png" alt="Team collaboration illustration" className="collab-illustration" />
            </div>
          </div>
        </div>
      )}

      {showCreateModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h2>Create New Channel</h2>
            <button className="close-btn" onClick={closeModal}>
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
                    <label className={`type-option ${newChannel.type === "VOCAL" ? "active" : ""}`}>
                      <input
                        type="radio"
                        name="type"
                        value="VOCAL"
                        checked={newChannel.type === "VOCAL"}
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
                  <button type="button" className="cancel-btn" onClick={closeModal}>
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
                    {projects.map((project) => (
                      <option key={project.id} value={project.id}>
                        {project.name}
                      </option>
                    ))}
                  </select>
                </div>
                {newChannel.isPrivate && (
                  <div className="form-group">
                    <label>Members</label>
                    <div className="search-members" ref={searchRef}>
                      <input
                        type="text"
                        placeholder="Search members..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        onFocus={() => setShowSuggestions(searchQuery.length > 0 && filteredUsers.length > 0)}
                      />
                      {showSuggestions && filteredUsers.length > 0 && (
                        <div className="suggestions">
                          {filteredUsers.map((user) => (
                            <div key={user.id} className="suggestion-item">
                              <img
                                src={user.avatar}
                                alt={`${user.firstName} ${user.lastName}`}
                                className="user-avatar"
                                style={{ width: "32px", height: "32px", borderRadius: "50%", marginRight: "8px" }}
                              />
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
                    <div className="selected-members">
                      {newChannel.members.map((userId, index) => {
                        const user = users.find((u) => u.id === userId);
                        return user ? (
                          <div key={userId} className="selected-member">
                            <img
                              src={user.avatar}
                              alt={`${user.firstName} ${user.lastName}`}
                              className="user-avatar"
                              style={{ width: "24px", height: "24px", borderRadius: "50%", marginRight: "8px" }}
                            />
                            <span>
                              {user.firstName} {user.lastName} ({newChannel.roles[index] || "member"})
                            </span>
                            <button
                              className="remove-btn"
                              onClick={() =>
                                setNewChannel((prev) => ({
                                  ...prev,
                                  members: prev.members.filter((id) => id !== userId),
                                  roles: prev.roles.filter((_, i) => i !== index),
                                }))
                              }
                            >
                              ×
                            </button>
                          </div>
                        ) : null;
                      })}
                    </div>
                  </div>
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