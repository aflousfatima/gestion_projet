"use client";
import React, { useEffect, useState, useRef } from "react";
import { useParams, useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import useAxios from "@/hooks/useAxios";
import { COLLABORATION_SERVICE_URL } from "@/config/useApi";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import EmojiPicker, { EmojiClickData } from "emoji-picker-react";
import {
  faHashtag,
  faVolumeUp,
  faLock,
  faPaperPlane,
  faThumbtack,
  faBell,
  faUsers,
  faEllipsisV,
  faPlus,
  faSmile,
  faMicrophone,
  faEdit,
  faTrash,
  faUpload
} from "@fortawesome/free-solid-svg-icons";
import "../../../../../../styles/Collaboration-Channel.css";

interface Channel {
  id: string;
  name: string;
  type: "TEXT" | "VOICE";
  isPrivate: boolean;
  members: string[];
}

interface Message {
  id: string;
  content: string;
  senderId: string;
  senderName: string;
  timestamp: string;
}

const ChannelPage: React.FC = () => {
  const { channelId } = useParams();
  const { accessToken } = useAuth();
  const axiosInstance = useAxios();
  const router = useRouter();
  const [channel, setChannel] = useState<Channel | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [newMessage, setNewMessage] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [showActionMenu, setShowActionMenu] = useState(false);
  const [showOptionsMenu, setShowOptionsMenu] = useState(false);
  const [showEmojiMenu, setShowEmojiMenu] = useState(false);
  const actionMenuRef = useRef<HTMLDivElement>(null);
  const optionsMenuRef = useRef<HTMLDivElement>(null);
  const emojiMenuRef = useRef<HTMLDivElement>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Fermer les menus si clic en dehors
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        actionMenuRef.current &&
        !actionMenuRef.current.contains(event.target as Node)
      ) {
        setShowActionMenu(false);
      }
      if (
        optionsMenuRef.current &&
        !optionsMenuRef.current.contains(event.target as Node)
      ) {
        setShowOptionsMenu(false);
      }
      if (
        emojiMenuRef.current &&
        !emojiMenuRef.current.contains(event.target as Node)
      ) {
        setShowEmojiMenu(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Fetch channel details
  useEffect(() => {
    const fetchChannel = async () => {
      if (accessToken && channelId) {
        try {
          const response = await axiosInstance.get(
            `${COLLABORATION_SERVICE_URL}/api/channels/${channelId}`,
            {
              headers: { Authorization: `Bearer ${accessToken}` },
            }
          );
          setChannel({
            id: response.data.id.toString(),
            name: response.data.name,
            type: response.data.type,
            isPrivate: response.data.isPrivate,
            members:
              response.data.participants?.map((p: any) =>
                p.userId.toString()
              ) || [],
          });
        } catch (err: any) {
          setError(
            err.response?.data?.message ||
              "Erreur lors de la récupération du canal"
          );
        }
      }
    };
    fetchChannel();
  }, [accessToken, axiosInstance, channelId]);

  // Fetch messages for Text Channels
  useEffect(() => {
    const fetchMessages = async () => {
      if (accessToken && channelId && channel?.type === "TEXT") {
        try {
          const response = await axiosInstance.get(
            `${COLLABORATION_SERVICE_URL}/api/channels/${channelId}/messages`,
            {
              headers: { Authorization: `Bearer ${accessToken}` },
            }
          );
          setMessages(
            response.data.map((msg: any) => ({
              id: msg.id.toString(),
              content: msg.content,
              senderId: msg.senderId.toString(),
              senderName: msg.senderName || "Unknown",
              timestamp: msg.timestamp,
            }))
          );
        } catch (err: any) {
          setError(err.response?.data?.message);
        }
      }
    };
    fetchMessages();
  }, [accessToken, axiosInstance, channelId, channel?.type]);

  // Scroll to bottom when messages change
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages]);

  // Send a new message
  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMessage.trim() || !channel) return;

    try {
      const response = await axiosInstance.post(
        `${COLLABORATION_SERVICE_URL}/api/channels/${channelId}/messages`,
        { content: newMessage },
        {
          headers: { Authorization: `Bearer ${accessToken}` },
        }
      );
      setMessages((prev) => [
        ...prev,
        {
          id: response.data.id.toString(),
          content: newMessage,
          senderId: response.data.senderId.toString(),
          senderName: response.data.senderName || "You",
          timestamp: new Date().toISOString(),
        },
      ]);
      setNewMessage("");
      setSuccessMessage("Message envoyé !");
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err: any) {
      setError(
        err.response?.data?.message || "Erreur lors de l'envoi du message"
      );
    }
  };

  // Upload file (à implémenter)
  const handleUploadFile = () => {
    // Placeholder pour l'upload de fichier
    alert("Fonctionnalité d'upload de fichier à implémenter");
    setShowOptionsMenu(false);
  };

  // Ajouter un émoji au message
  const handleEmojiSelect = (emojiData: EmojiClickData) => {
    setNewMessage((prev) => prev + emojiData.emoji);
    setShowEmojiMenu(false);
  };

  // Modifier le canal
  const handleEditChannel = () => {
    // Rediriger vers une page de modification ou ouvrir un modal (à implémenter)
    alert("Fonctionnalité de modification du canal à implémenter");
    setShowActionMenu(false);
  };

  // Supprimer le canal
  const handleDeleteChannel = async () => {
    if (!confirm("Voulez-vous vraiment supprimer ce canal ?")) return;

    try {
      await axiosInstance.delete(
        `${COLLABORATION_SERVICE_URL}/api/channels/${channelId}`,
        {
          headers: { Authorization: `Bearer ${accessToken}` },
        }
      );
      setSuccessMessage("Canal supprimé avec succès !");
      setTimeout(() => {
        router.push("/user/dashboard/collaboration");
      }, 2000);
    } catch (err: any) {
      setError(
        err.response?.data?.message || "Erreur lors de la suppression du canal"
      );
    }
    setShowActionMenu(false);
  };

  if (!channel) {
    return (
      <div className="channel-container">
        {error && <div className="error-message">{error}</div>}
        <div className="loading">Chargement...</div>
      </div>
    );
  }

  return (
    <div className="channel-container">
      {error && <div className="error-message">{error}</div>}
      {successMessage && (
        <div className="success-message">{successMessage}</div>
      )}

      <div className="channel-header">
        <div className="channel-info">
          <FontAwesomeIcon
            icon={channel.type === "TEXT" ? faHashtag : faVolumeUp}
            className="chnnel-icon"
          />
          <h2 className="channel-title">{channel.name}</h2>
          {channel.isPrivate && (
            <FontAwesomeIcon icon={faLock} className="private-icon" />
          )}
        </div>
        <div className="channel-actions">
          <button className="action-btn" title="Épingler">
            <FontAwesomeIcon icon={faThumbtack} />
          </button>
          <button className="action-btn" title="Notifications">
            <FontAwesomeIcon icon={faBell} />
          </button>
          <button className="action-btn" title="Participants">
            <FontAwesomeIcon icon={faUsers} />
          </button>
          <div className="action-menu-container" ref={actionMenuRef}>
            <button
              className="action-btn"
              title="Plus d'actions"
              onClick={() => setShowActionMenu(!showActionMenu)}
            >
              <FontAwesomeIcon icon={faEllipsisV} />
            </button>
            {showActionMenu && (
              <div className="action-menu">
                <button className="menu-item" onClick={handleEditChannel}>
                  <FontAwesomeIcon icon={faEdit} /> Edit the Channel
                </button>
                <button
                  className="menu-item delete"
                  onClick={handleDeleteChannel}
                >
                  <FontAwesomeIcon icon={faTrash} /> Delete the Channel
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="channel-content">
        {channel.type === "TEXT" ? (
          <div className="chat-section">
            <div className="messages">
              {messages.length > 0 ? (
                messages.map((msg) => (
                  <div key={msg.id} className="message">
                    <div className="message-header">
                      <span className="sender">{msg.senderName}</span>
                      <span className="timestamp">
                        {new Date(msg.timestamp).toLocaleString()}
                      </span>
                    </div>
                    <div className="message-content">{msg.content}</div>
                  </div>
                ))
              ) : (
                <div className="no-messages">No Message for the moment.</div>
              )}
              <div ref={messagesEndRef} />
            </div>
            <form className="message-form" onSubmit={handleSendMessage}>
              <div className="message-input-container">
                <input
                  type="text"
                  value={newMessage}
                  onChange={(e) => setNewMessage(e.target.value)}
                  placeholder="Send a message..."
                  className="message-input"
                />
                <div className="input-actions">
                  <div className="options-menu-container" ref={optionsMenuRef}>
                    <button
                      type="button"
                      className="input-action-btn"
                      title="Plus d'options"
                      onClick={() => setShowOptionsMenu(!showOptionsMenu)}
                    >
                      <FontAwesomeIcon icon={faPlus} />
                    </button>
                    {showOptionsMenu && (
                      <div className="options-menu">
                        <button
                          className="menu-item"
                          onClick={handleUploadFile}
                        >
                           <FontAwesomeIcon icon={faUpload} className="icon" /> Upload File

                        </button>
                      </div>
                    )}
                  </div>
                  <div className="emoji-menu-container" ref={emojiMenuRef}>
                    <button
                      type="button"
                      className="input-action-btn"
                      title="Émoticônes"
                      onClick={() => setShowEmojiMenu(!showEmojiMenu)}
                    >
                      <FontAwesomeIcon icon={faSmile} />
                    </button>
                    {showEmojiMenu && (
                      <div className="emoji-menu">
                        <EmojiPicker onEmojiClick={handleEmojiSelect} />
                      </div>
                    )}
                  </div>
                  <button
                    type="button"
                    className="input-action-btn disabled"
                    title="Audio (à venir)"
                  >
                    <FontAwesomeIcon icon={faMicrophone} />
                  </button>
                  <button
                    type="submit"
                    className={`send-btn ${
                      newMessage.trim() ? "" : "disabled"
                    }`}
                    disabled={!newMessage.trim()}
                  >
                    <FontAwesomeIcon icon={faPaperPlane} />
                  </button>
                </div>
              </div>
            </form>
          </div>
        ) : (
          <div className="voice-section">
            <h3>Salon Vocal : {channel.name}</h3>
            <p>Rejoindre le salon vocal (fonctionnalité à venir).</p>
            <button className="join-call-btn">Rejoindre l appel</button>
          </div>
        )}
      </div>
    </div>
  );
};

export default ChannelPage;
