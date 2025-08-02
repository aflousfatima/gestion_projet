/* eslint-disable */
"use client";
import React, { useState, useEffect } from "react";
import { useAuth } from "../../../../../../context/AuthContext";
import useAxios from "../../../../../../hooks/useAxios";
import { useParams } from "next/navigation";
import {
  AUTH_SERVICE_URL,
  COLLABORATION_SERVICE_URL,
} from "../../../../../../config/useApi";
import "../../../../../../styles/Meetings.css";

// Interfaces pour typer les données
interface User {
  id: string;
  firstName: string;
  lastName: string;
  avatar?: string;
  initial?: string;
}

interface Meeting {
  id: string;
  title: string;
  date: string;
  time: string;
  duration: string;
  startTime: string;
  status: "UPCOMING" | "COMPLETED" | "CANCELLED";
  participantIds: string[];
  creatorId: string; // Remplace creator par creatorId
  project: string;
  meetingType: "ONE_TIME" | "WEEKLY" | "BIWEEKLY" | "MONTHLY";
  meetingPriority: "LOW" | "MEDIUM" | "HIGH";
  participantCount: number;
  timezone: "EUROPE_PARIS" | "UTC" | "AMERICA/NEW_YORK";
  creatorFirstName: string; // Nouveau champ
  creatorLastName: string; // Nouveau champ
  participantFirstNames: string[]; // Nouveaux champs
  participantLastNames: string[]; // Nouveaux champs
}

const Meetings: React.FC = () => {
  const { accessToken, isLoading: authLoading } = useAuth();
  const axiosInstance = useAxios();
  const params = useParams();
  const projectId = params.projectId as string;
  const [meetings, setMeetings] = useState<Meeting[]>([]);
  const [teamMembers, setTeamMembers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [showManualForm, setShowManualForm] = useState(false);
  const [showAIForm, setShowAIForm] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState("UPCOMING");
  const [searchTerm, setSearchTerm] = useState("");
  const [manualFormData, setManualFormData] = useState<{
    title: string;
    date: string;
    time: string;
    duration: string;
    location: string;
    participantIds: string[];
    project: string;
    meetingType: "ONE_TIME" | "WEEKLY" | "BIWEEKLY" | "MONTHLY"; // Union complète
    meetingPriority: "LOW" | "MEDIUM" | "HIGH"; // Union complète
    status: "UPCOMING" | "COMPLETED" | "CANCELLED"; // Union complète
    participantCount: number;
    timezone: "EUROPE_PARIS" | "UTC" | "AMERICA/NEW_YORK"; // Union complète
  }>({
    title: "",
    date: "",
    time: "",
    duration: "30",
    location: "",
    participantIds: [],
    project: projectId,
    meetingType: "ONE_TIME",
    meetingPriority: "MEDIUM",
    status: "UPCOMING",
    participantCount: 0,
    timezone: "EUROPE_PARIS",
  });
  const [aiFormData, setAIFormData] = useState<{
    duration: string;
    participantIds: string[];
    mandatoryParticipants: string;
    priority: string;
    meetingType: "ONE_TIME" | "WEEKLY" | "BIWEEKLY" | "MONTHLY"; // Union complète
    preferredTime: string;
    meetingPriority: "LOW" | "MEDIUM" | "HIGH"; // Union complète
    participantCount: number;
    timezone: "EUROPE_PARIS" | "UTC" | "AMERICA/NEW_YORK"; // Union complète
  }>({
    duration: "30",
    participantIds: [],
    mandatoryParticipants: "1",
    priority: "MEDIUM",
    meetingType: "ONE_TIME",
    preferredTime: "MORNING",
    meetingPriority: "MEDIUM",
    participantCount: 0,
    timezone: "EUROPE_PARIS",
  });

  const monthNames = [
    "January",
    "February",
    "March",
    "April",
    "May",
    "June",
    "July",
    "August",
    "September",
    "October",
    "November",
    "December",
  ];
  const dayNames = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
  const [currentMonth, setCurrentMonth] = useState(new Date().getMonth()); // July = 6
  const [currentYear, setCurrentYear] = useState(new Date().getFullYear()); // 2025
  const [isTeamListOpen, setIsTeamListOpen] = useState(false);

  // Charger les réunions depuis le backend
  useEffect(() => {
    const fetchMeetings = async () => {
      if (authLoading || !accessToken) return;
      try {
        const response = await axiosInstance.get<Meeting[]>(
          `${COLLABORATION_SERVICE_URL}/api/meetings`,
          {
            headers: { Authorization: `Bearer ${accessToken}` },
          }
        );
        setMeetings(response.data);
      } catch (err) {
        setError("Error fetching meetings");
        console.error(err);
      }
    };
    fetchMeetings();
  }, [accessToken, authLoading, axiosInstance]);

  // Charger les membres d'équipe
  useEffect(() => {
    const fetchTeamMembers = async () => {
      if (authLoading || !accessToken || !projectId) {
        console.log("Skipping fetchTeamMembers: missing dependencies", {
          authLoading,
          accessToken,
          projectId,
        });
        return;
      }
      try {
        setLoading(true);
        setError(null);
        const teamResponse = await axiosInstance.get<User[]>(
          `${AUTH_SERVICE_URL}/api/team-members/${projectId}`,
          {
            headers: { Authorization: `Bearer ${accessToken}` },
          }
        );
        setTeamMembers(teamResponse.data);
      } catch (err) {
        setError("Unable to load team members.");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchTeamMembers();
  }, [accessToken, authLoading, axiosInstance, projectId]);

  // Calcul du temps restant
  const getTimeRemaining = (startTime: string) => {
    const now = new Date();
    const start = new Date(startTime);
    const diffMs = start.getTime() - now.getTime();
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffMinutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
    if (diffMs < 0) return "Completed";
    if (diffHours > 0) return `Starts in ${diffHours}h ${diffMinutes}m`;
    return `Starts in ${diffMinutes}m`;
  };
  const [menuStates, setMenuStates] = useState<{ [key: string]: boolean }>({});
  // Filtrer les réunions
  const filteredMeetings = meetings.filter((meeting: Meeting) => {
    const today = new Date().toISOString().split("T")[0];
    const matchesFilter =
      (filter === "today" && meeting.date === today) ||
      (filter === "UPCOMING" && new Date(meeting.startTime) > new Date()) ||
      (filter === "COMPLETED" && new Date(meeting.startTime) < new Date());
    const matchesSearch = meeting.title
      ? meeting.title.toLowerCase().includes(searchTerm.toLowerCase())
      : true;
    return matchesFilter && matchesSearch;
  });

  // Soumission manuelle vers le backend
  const handleManualSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const response = await axiosInstance.post<Meeting>(
        `${COLLABORATION_SERVICE_URL}/api/meetings`,
        {
          ...manualFormData,
          participantCount: manualFormData.participantIds.length,
          status: manualFormData.status || "UPCOMING",
        },
        {
          headers: { Authorization: `Bearer ${accessToken}` },
        }
      );
      setMeetings([...meetings, response.data]);
      setManualFormData({
        title: "",
        date: "",
        time: "",
        duration: "30",
        location: "",
        participantIds: [],
        project: projectId,
        meetingType: "ONE_TIME" as const, // Changé de "one-time" à "ONE_TIME"
        meetingPriority: "MEDIUM" as const, // Changé de "medium" à "MEDIUM"
        participantCount: 0,
        timezone: "EUROPE_PARIS" as const, // Changé de "Europe/Paris" à "EUROPE_PARIS"
        status: "UPCOMING",
      });
      setShowManualForm(false);
    } catch (err) {
      setError("Error creating meeting");
      console.error(err);
    }
  };

  // Soumission IA vers le backend
  const handleAISubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const response = await axiosInstance.post<Meeting>(
        `${COLLABORATION_SERVICE_URL}/api/meetings`,
        {
          title: `AI Meeting #${meetings.length + 1}`,
          date: new Date().toISOString().split("T")[0], // Date actuelle
          time: aiFormData.preferredTime === "MORNING" ? "09:00" : "14:00",
          duration: aiFormData.duration,
          status: "UPCOMING",
          participantIds: aiFormData.participantIds,
          project: projectId,
          meetingType: aiFormData.meetingType,
          meetingPriority: aiFormData.meetingPriority,
          participantCount: aiFormData.participantIds.length,
          timezone: aiFormData.timezone,
        },
        {
          headers: { Authorization: `Bearer ${accessToken}` },
        }
      );
      setMeetings([...meetings, response.data]);
      setAIFormData({
        duration: "30",
        participantIds: [],
        mandatoryParticipants: "1",
        priority: "MEDIUM",
        meetingType: "ONE_TIME",
        preferredTime: "MORNING",
        meetingPriority: "MEDIUM",
        participantCount: 0,
        timezone: "EUROPE_PARIS",
      });
      setShowAIForm(false);
    } catch (err) {
      setError("Error creating AI meeting");
      console.error(err);
    }
  };

  // Gestion de la sélection des participants
  const handleUserSelection = (userId: string) => {
    setManualFormData((prev) => {
      const newParticipantIds = prev.participantIds.includes(userId)
        ? prev.participantIds.filter((id) => id !== userId)
        : [...prev.participantIds, userId];
      return {
        ...prev,
        participantIds: newParticipantIds,
        participantCount: newParticipantIds.length,
      };
    });
    setAIFormData((prev) => {
      const newParticipantIds = prev.participantIds.includes(userId)
        ? prev.participantIds.filter((id) => id !== userId)
        : [...prev.participantIds, userId];
      return {
        ...prev,
        participantIds: newParticipantIds,
        participantCount: newParticipantIds.length,
      };
    });
  };

  return (
    <div className="meetings-page">
      {/* En-tête */}
      <header className="header-meetIA">
        <div className="title-meetIA">Meeting Management</div>
        <div className="header-actions">
          <input
            type="text"
            placeholder="Search for a meeting..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="search-bar-meetIA"
          />
          <div className="filters">
            <button
              className={`filter-btn ${filter === "today" ? "active" : ""}`}
              onClick={() => setFilter("today")}
            >
              <i className="fas fa-calendar-day"></i> Today
            </button>
            <button
              className={`filter-btn ${filter === "UPCOMING" ? "active" : ""}`}
              onClick={() => setFilter("UPCOMING")}
            >
              <i className="fas fa-clock"></i> Upcoming
            </button>
            <button
              className={`filter-btn ${filter === "COMPLETED" ? "active" : ""}`}
              onClick={() => setFilter("COMPLETED")}
            >
              <i className="fas fa-check"></i> Completed
            </button>
          </div>
          <div className="action-icons">
            <button
              className="action-icon manual"
              onClick={() => {
                setShowManualForm(true);
                setShowAIForm(false);
                setError(null);
              }}
            >
              <i className="fas fa-calendar-plus"></i>
            </button>
            <button
              className="action-icon ai"
              onClick={() => {
                setShowAIForm(true);
                setShowManualForm(false);
                setError(null);
              }}
            >
              <i className="fas fa-magic"></i>
            </button>
          </div>
        </div>
      </header>
      {/* Cartes de réunions */}
      <div className="meetings-grid">
        {filteredMeetings.length === 0 ? (
          <p className="no-meetings">No meetings found.</p>
        ) : (
          filteredMeetings.map((meeting: Meeting) => {
            const creatorInitial =
              (meeting.creatorFirstName?.[0] || "U") +
              (meeting.creatorLastName?.[0] || "");
            const creatorName = `${meeting.creatorFirstName || "Unknown"} ${
              meeting.creatorLastName || "Unknown"
            }`;

            const handleDelete = async () => {
              if (
                window.confirm("Are you sure you want to delete this meeting?")
              ) {
                try {
                  await axiosInstance.delete(
                    `${COLLABORATION_SERVICE_URL}/api/meetings/${meeting.id}`,
                    {
                      headers: { Authorization: `Bearer ${accessToken}` },
                    }
                  );
                  setMeetings(meetings.filter((m) => m.id !== meeting.id));
                } catch (err) {
                  setError("Error deleting meeting");
                  console.error(err);
                }
              }
            };

            const handleEdit = () => {
              // Logique pour ouvrir un formulaire de modification (à implémenter)
              alert("Edit functionality to be implemented");
            };

            return (
              <div key={meeting.id} className="meeting-card">
                <div className="card-header-meetIA">
                  <div className="avatar-meetIA">{creatorInitial}</div>
                  <div className="creator-info">
                    <div className="creator-name">{creatorName}</div>
                  </div>
                  <div
                    className="card-menu"
                    onClick={() =>
                      setMenuStates((prev) => ({
                        ...prev,
                        [meeting.id]: !prev[meeting.id],
                      }))
                    }
                  >
                    <span className="menu-dots">⋮</span>
                    {menuStates[meeting.id] && (
                      <div className="menu-options">
                        <button className="menu-option" onClick={handleEdit}>
                          Modifier
                        </button>
                        <button className="menu-option" onClick={handleDelete}>
                          Supprimer
                        </button>
                      </div>
                    )}
                  </div>
                </div>
                <div className="card-body-meetIA">
                  <span className="title-card-meeting">{meeting.title}</span>
                  <div className="time-remaining">
                    {getTimeRemaining(meeting.startTime)}
                  </div>
                  <div className="meeting-time">
                    {meeting.time} -{" "}
                    {new Date(
                      new Date(meeting.startTime).getTime() +
                        parseInt(meeting.duration) * 60 * 1000
                    ).toLocaleTimeString([], {
                      hour: "2-digit",
                      minute: "2-digit",
                    })}{" "}
                    {new Date(meeting.startTime).getHours() >= 12 ? "PM" : "AM"}
                  </div>
                  <div className="meeting-details">
                    <span>Type: {meeting.meetingType}</span>
                    <span>Timezone: {meeting.timezone}</span>
                  </div>
                  <div className="participants">
                    {meeting.participantIds.map((p, index) => (
                      <span key={index} className="participant-icon">
                        {(meeting.participantFirstNames?.[index]?.[0] || p[0]) +
                          (meeting.participantLastNames?.[index]?.[0] || "") ||
                          p[0]}
                      </span>
                    ))}
                  </div>
                </div>
                <button className="show-details-btn">Show Details</button>
              </div>
            );
          })
        )}
      </div>
      {/* Filtres supplémentaires */}
      <div className="additional-filters">
        <div className="filter-item">
          <i className="fas fa-users"></i> Overall Meetings: {meetings.length}
        </div>
        <div className="filter-item">
          <i className="fas fa-redo"></i> Rescheduled Meetings: 0{" "}
          {/* À récupérer depuis le backend */}
        </div>
        <div className="filter-item">
          <i className="fas fa-times"></i> Cancelled Meetings: 0{" "}
          {/* À récupérer depuis le backend */}
        </div>
      </div>
      {/* Idée brillante : Calendrier Mensuel Interactif */}
      <div className="brilliant-idea">
        <h3 className="idea-title">Monthly Calendar</h3>
        <div className="calendar-controls">
          <span className="calendar-month">{`${monthNames[currentMonth]} ${currentYear}`}</span>
          <div className="nav-buttons">
            <button
              className="nav-button"
              onClick={() => {
                setCurrentMonth((prev) => (prev - 1 + 12) % 12);
                if (currentMonth === 0) setCurrentYear((prev) => prev - 1);
              }}
            >
              <i className="fas fa-chevron-left"></i>
            </button>
            <button
              className="nav-button"
              onClick={() => {
                setCurrentMonth((prev) => (prev + 1) % 12);
                if (currentMonth === 11) setCurrentYear((prev) => prev + 1);
              }}
            >
              <i className="fas fa-chevron-right"></i>
            </button>
          </div>
        </div>
        <div className="monthly-calendar">
          <div className="calendar-grid">
            {Array.from({ length: 35 }, (_, weekIndex) =>
              Array.from({ length: 7 }, (_, dayIndex) => {
                const firstDay = new Date(
                  currentYear,
                  currentMonth,
                  1
                ).getDay();
                const dayOfMonth =
                  weekIndex * 7 +
                  dayIndex +
                  1 -
                  (firstDay === 0 ? 7 : firstDay) +
                  1;
                const lastDay = new Date(
                  currentYear,
                  currentMonth + 1,
                  0
                ).getDate();
                if (dayOfMonth > 0 && dayOfMonth <= lastDay) {
                  const dateStr = `${currentYear}-${(currentMonth + 1)
                    .toString()
                    .padStart(2, "0")}-${dayOfMonth
                    .toString()
                    .padStart(2, "0")}`;
                  const dayMeetings = meetings.filter(
                    (m: Meeting) => m.date === dateStr
                  );
                  const dayName =
                    dayNames[(new Date(dateStr).getDay() + 6) % 7];
                  return (
                    <div
                      key={`${weekIndex}-${dayIndex}`}
                      className="calendar-day"
                    >
                      <div className="day-header">
                        <span className="day-name">{dayName}</span>
                        <span className="day-number">{dayOfMonth}</span>
                      </div>
                      <div className="meetings-badges">
                        {dayMeetings.map((meeting: Meeting) => {
                          const badgeColor =
                            meeting.status === "UPCOMING"
                              ? "blue"
                              : meeting.status === "COMPLETED"
                              ? "green"
                              : "red";
                          return (
                            <div
                              key={meeting.id}
                              className={`meeting-badge ${badgeColor}`}
                              onClick={() =>
                                alert(
                                  `Details: ${
                                    meeting.title || "No title"
                                  }\nDate: ${meeting.date} ${
                                    meeting.time
                                  }\nStatus: ${
                                    meeting.status === "UPCOMING"
                                      ? "Upcoming"
                                      : meeting.status === "COMPLETED"
                                      ? "Completed"
                                      : "Cancelled"
                                  }`
                                )
                              }
                            >
                              <i
                                className={`fas ${
                                  meeting.status === "UPCOMING"
                                    ? "fa-clock"
                                    : meeting.status === "COMPLETED"
                                    ? "fa-check"
                                    : "fa-times"
                                }`}
                              ></i>
                              <span>
                                {meeting.time} -{" "}
                                {meeting.title
                                  ? meeting.title.slice(0, 10)
                                  : "No title"}
                              </span>
                              {meeting.status === "UPCOMING" && (
                                <i className="fas fa-bell notification-icon" />
                              )}
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  );
                }
                return (
                  <div
                    key={`${weekIndex}-${dayIndex}`}
                    className="calendar-day empty"
                  ></div>
                );
              })
            )}
          </div>
        </div>
      </div>

      {/* Formulaire manuel */}
      {showManualForm && (
        <div className="modal-meetIA">
          <div className="modal-content-meetIA">
            <h2 className="modal-title-meetIA">Manual Scheduling</h2>
            <form onSubmit={handleManualSubmit}>
              <div className="form-group-meetIA">
                <label>Title</label>
                <input
                  type="text"
                  value={manualFormData.title}
                  onChange={(e) =>
                    setManualFormData({
                      ...manualFormData,
                      title: e.target.value,
                    })
                  }
                  required
                />
              </div>
              <div className="form-group-meetIA">
                <label>Date</label>
                <input
                  type="date"
                  value={manualFormData.date}
                  onChange={(e) =>
                    setManualFormData({
                      ...manualFormData,
                      date: e.target.value,
                    })
                  }
                  required
                />
              </div>
              <div className="form-group-meetIA">
                <label>Time</label>
                <input
                  type="time"
                  value={manualFormData.time}
                  onChange={(e) =>
                    setManualFormData({
                      ...manualFormData,
                      time: e.target.value,
                    })
                  }
                  required
                />
              </div>
              <div className="form-group-meetIA">
                <label>Duration</label>
                <select
                  value={manualFormData.duration}
                  onChange={(e) =>
                    setManualFormData({
                      ...manualFormData,
                      duration: e.target.value,
                    })
                  }
                >
                  <option value="30">30 min</option>
                  <option value="60">1 hour</option>
                  <option value="90">1h30</option>
                </select>
              </div>
              <div className="form-group-meetIA">
                <label>Meeting Type</label>
                <select
                  value={manualFormData.meetingType}
                  onChange={(e) =>
                    setManualFormData({
                      ...manualFormData,
                      meetingType: e.target.value as
                        | "ONE_TIME"
                        | "WEEKLY"
                        | "BIWEEKLY"
                        | "MONTHLY",
                    })
                  }
                >
                  <option value="ONE_TIME">One-time</option>
                  <option value="WEEKLY">WEEKLY</option>
                  <option value="BIWEEKLY">BIWEEKLY</option>
                  <option value="MONTHLY">Monthly</option>
                </select>
              </div>
              <div className="form-group-meetIA">
                <label>Priority</label>
                <select
                  value={manualFormData.meetingPriority}
                  onChange={(e) =>
                    setManualFormData({
                      ...manualFormData,
                      meetingPriority: e.target.value as
                        | "LOW"
                        | "MEDIUM"
                        | "HIGH",
                    })
                  }
                >
                  <option value="LOW">Low</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HIGH">High</option>
                </select>
              </div>
              <div className="form-group-meetIA">
                <label>Timezone</label>
                <select
                  value={manualFormData.timezone}
                  onChange={(e) =>
                    setManualFormData({
                      ...manualFormData,
                      timezone: e.target.value as
                        | "EUROPE_PARIS"
                        | "UTC"
                        | "AMERICA/NEW_YORK",
                    })
                  }
                >
                  <option value="EUROPE_PARIS">EUROPE_PARIS</option>
                  <option value="UTC">UTC</option>
                  <option value="America/New_York">America/New_York</option>
                </select>
              </div>
              <div className="form-group-meetIA participant-selection">
                <label>Participants</label>
                <div
                  className="avatar-stack"
                  onClick={() => setIsTeamListOpen(!isTeamListOpen)}
                >
                  {manualFormData.participantIds.length > 0 ? (
                    <>
                      {manualFormData.participantIds
                        .slice(0, 3)
                        .map((userId) => {
                          const user = teamMembers.find((u) => u.id === userId);
                          return user ? (
                            <img
                              key={user.id}
                              src={user.avatar || "/default-avatar.png"}
                              alt={`${user.firstName} ${user.lastName}`}
                              className="user-avatar-stack"
                            />
                          ) : null;
                        })}
                      {manualFormData.participantIds.length > 3 && (
                        <span className="user-avatar-stack more">
                          +{manualFormData.participantIds.length - 3}
                        </span>
                      )}
                    </>
                  ) : (
                    <span className="placeholder1">Select Members</span>
                  )}
                  <span className="dropdown-arrow">
                    {isTeamListOpen ? "▲" : "▼"}
                  </span>
                </div>
                {isTeamListOpen && (
                  <div className="team-list-task">
                    {loading ? (
                      <span>Loading team members...</span>
                    ) : error ? (
                      <span>{error}</span>
                    ) : teamMembers.length === 0 ? (
                      <span></span>
                    ) : (
                      teamMembers.map((user) => (
                        <label key={user.id} className="team-member">
                          <input
                            type="checkbox"
                            checked={manualFormData.participantIds.includes(
                              user.id
                            )}
                            onChange={() => handleUserSelection(user.id)}
                            style={{ display: "none" }}
                          />
                          <div
                            className={`team-member-inner ${
                              manualFormData.participantIds.includes(user.id)
                                ? "selected"
                                : ""
                            }`}
                          >
                            <img
                              src={user.avatar || "/default-avatar.png"}
                              alt={`${user.firstName} ${user.lastName}`}
                              className="user-avatar"
                            />
                            <span className="user-name">{`${user.firstName} ${user.lastName}`}</span>
                          </div>
                        </label>
                      ))
                    )}
                  </div>
                )}
              </div>
              <div className="form-actions-meetIA">
                <button type="submit" className="btn-meetIA primary">
                  Schedule
                </button>
                <button
                  type="button"
                  className="btn-meetIA secondary"
                  onClick={() => {
                    setShowManualForm(false);
                    setIsTeamListOpen(false);
                  }}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
      {/* Formulaire IA */}
      {showAIForm && (
        <div className="modal">
          <div className="modal-content">
            <h2 className="modal-title">AI Optimization</h2>
            <form onSubmit={handleAISubmit}>
              <div className="form-group-meetIA">
                <label>Duration</label>
                <select
                  value={aiFormData.duration}
                  onChange={(e) =>
                    setAIFormData({ ...aiFormData, duration: e.target.value })
                  }
                >
                  <option value="30">30 min</option>
                  <option value="60">1 hour</option>
                  <option value="90">1h30</option>
                </select>
              </div>
              <div className="form-group-meetIA">
                <label>Meeting Type</label>
                <select
                  value={aiFormData.meetingType}
                  onChange={(e) =>
                    setAIFormData({
                      ...aiFormData,
                      meetingType: e.target.value as
                        | "ONE_TIME"
                        | "WEEKLY"
                        | "BIWEEKLY"
                        | "MONTHLY",
                    })
                  }
                >
                  <option value="ONE_TIME">One-time</option>
                  <option value="WEEKLY">Weekly</option>
                  <option value="BIWEEKLY">Biweekly</option>
                  <option value="MONTHLY">Monthly</option>
                </select>
              </div>
              <div className="form-group-meetIA">
                <label>Priority</label>
                <select
                  value={aiFormData.meetingPriority}
                  onChange={(e) =>
                    setAIFormData({
                      ...aiFormData,
                      meetingPriority: e.target.value as
                        | "LOW"
                        | "MEDIUM"
                        | "HIGH",
                    })
                  }
                >
                  <option value="LOW">Low</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HIGH">High</option>
                </select>
              </div>
              <div className="form-group-meetIA">
                <label>Preferred Time</label>
                <select
                  value={aiFormData.preferredTime}
                  onChange={(e) =>
                    setAIFormData({
                      ...aiFormData,
                      preferredTime: e.target.value,
                    })
                  }
                >
                  <option value="MORNING">Morning</option>
                  <option value="AFTERNOON">Afternoon</option>
                </select>
              </div>
              <div className="form-group-meetIA">
                <label>Timezone</label>
                <select
                  value={aiFormData.timezone}
                  onChange={(e) =>
                    setAIFormData({
                      ...aiFormData,
                      timezone: e.target.value as
                        | "EUROPE_PARIS"
                        | "UTC"
                        | "AMERICA/NEW_YORK",
                    })
                  }
                >
                  <option value="EUROPE_PARIS">EUROPE_PARIS</option>
                  <option value="UTC">UTC</option>
                  <option value="America/New_York">America/New_York</option>
                </select>
              </div>
              <div className="form-group-meetIA participant-selection">
                <label>Participants</label>
                <div
                  className="avatar-stack"
                  onClick={() => setIsTeamListOpen(!isTeamListOpen)}
                >
                  {aiFormData.participantIds.length > 0 ? (
                    <>
                      {aiFormData.participantIds.slice(0, 3).map((userId) => {
                        const user = teamMembers.find((u) => u.id === userId);
                        return user ? (
                          <img
                            key={user.id}
                            src={user.avatar || "/default-avatar.png"}
                            alt={`${user.firstName} ${user.lastName}`}
                            className="user-avatar-stack"
                          />
                        ) : null;
                      })}
                      {aiFormData.participantIds.length > 3 && (
                        <span className="user-avatar-stack more">
                          +{aiFormData.participantIds.length - 3}
                        </span>
                      )}
                    </>
                  ) : (
                    <span className="placeholder1">Select Members</span>
                  )}
                  <span className="dropdown-arrow">
                    {isTeamListOpen ? "▲" : "▼"}
                  </span>
                </div>
                {isTeamListOpen && (
                  <div className="team-list-task">
                    {loading ? (
                      <span>Loading team members...</span>
                    ) : error ? (
                      <span>{error}</span>
                    ) : teamMembers.length === 0 ? (
                      <span></span>
                    ) : (
                      teamMembers.map((user) => (
                        <label key={user.id} className="team-member">
                          <input
                            type="checkbox"
                            checked={aiFormData.participantIds.includes(
                              user.id
                            )}
                            onChange={() => handleUserSelection(user.id)}
                            style={{ display: "none" }}
                          />
                          <div
                            className={`team-member-inner ${
                              aiFormData.participantIds.includes(user.id)
                                ? "selected"
                                : ""
                            }`}
                          >
                            <img
                              src={user.avatar || "/default-avatar.png"}
                              alt={`${user.firstName} ${user.lastName}`}
                              className="user-avatar"
                            />
                            <span className="user-name">{`${user.firstName} ${user.lastName}`}</span>
                          </div>
                        </label>
                      ))
                    )}
                  </div>
                )}
              </div>
              <div className="form-actions-meetIA">
                <button type="submit" className="btn-meetIA primary ai">
                  Optimize
                </button>
                <button
                  type="button"
                  className="btn-meetIA secondary"
                  onClick={() => {
                    setShowAIForm(false);
                    setIsTeamListOpen(false);
                  }}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Meetings;
