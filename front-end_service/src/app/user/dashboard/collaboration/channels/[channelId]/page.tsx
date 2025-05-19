"use client";

import React, { useEffect, useState, useRef } from "react";
import { useParams, useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import useAxios from "@/hooks/useAxios";
import {AUTH_SERVICE_URL ,  COLLABORATION_SERVICE_URL } from "@/config/useApi";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faHashtag,
  faVolumeUp,
  faTimes,
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
  faReply
} from "@fortawesome/free-solid-svg-icons";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import EmojiPicker, { EmojiClickData } from "emoji-picker-react";
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
  channelId: string;
  senderId: string;
  senderName: string;
  text: string;
  fileUrl: string | null;
  mimeType: string | null;
  type: "TEXT" | "IMAGE" | "VIDEO" | "FILE" | "SYSTEM" | "AUDIO";
  createdAt: string;
  reactions?: { [emoji: string]: string[] };
  replyToId: string | null;
  replyToText: string | null;
  replyToSenderName: string | null;
  pinned: boolean;
  modified?: boolean; // Ajouté
}

interface MessageGroup {
  date: string;
  messages: Message[];
}

interface User {
  id: string;
  firstName: string;
  lastName: string;
  avatar?: string;
  email: string
}
const ChannelPage: React.FC = () => {
  const { channelId } = useParams();
  const { accessToken } = useAuth(); // Assume user contains userId
  const axiosInstance = useAxios();
  const router = useRouter();
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [showPinnedMessages, setShowPinnedMessages] = useState(false);
  const [channel, setChannel] = useState<Channel | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [newMessage, setNewMessage] = useState("");
  const [editingMessageId, setEditingMessageId] = useState<string | null>(null);
  const [editedMessageText, setEditedMessageText] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [showActionMenu, setShowActionMenu] = useState(false);
  const [showOptionsMenu, setShowOptionsMenu] = useState(false);
  const [showEmojiMenu, setShowEmojiMenu] = useState(false);
  const actionMenuRef = useRef<HTMLDivElement>(null);
  const optionsMenuRef = useRef<HTMLDivElement>(null);
  const emojiMenuRef = useRef<HTMLDivElement>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const stompClientRef = useRef<Client | null>(null);
  const [hoveredMessageId, setHoveredMessageId] = useState<string | null>(null);
  const [showReactionPicker, setShowReactionPicker] = useState<string | null>(null);
  const [replyingTo, setReplyingTo] = useState<Message | null>(null);
  // Initialize WebSocket client
  useEffect(() => {
    if (!accessToken || !channelId) return;

    const socket = new SockJS(`${COLLABORATION_SERVICE_URL}/ws`);
    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`,
      },
      debug: (str) => {
        console.log(str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

client.onConnect = () => {
  console.log("Connecté au WebSocket");
  client.subscribe(`/topic/messages.${channelId}`, (message) => {
    try {
      const receivedMessage: Message = {
        ...JSON.parse(message.body),
        id: JSON.parse(message.body).id.toString(), // Assurer que l'ID est une chaîne
        replyToId: JSON.parse(message.body).replyToId ? JSON.parse(message.body).replyToId.toString() : null,
        replyToText: JSON.parse(message.body).replyToText || null,
        replyToSenderName: JSON.parse(message.body).replyToSenderName || null,
        pinned: JSON.parse(message.body).pinned || false,
        reactions: JSON.parse(message.body).reactions || {},
        modified: JSON.parse(message.body).modified || false,
      };
      console.log("Message reçu via WebSocket, ID:", receivedMessage.id); // Log pour déboguer

      setMessages((prev) => {
        // Vérifier si le message existe déjà
        const existingIndex = prev.findIndex((msg) => msg.id === receivedMessage.id);
        if (receivedMessage.type === "SYSTEM" && receivedMessage.text === "Message supprimé") {
          console.log("Suppression du message ID:", receivedMessage.id);
          return prev.filter((msg) => msg.id !== receivedMessage.id);
        } else if (existingIndex >= 0) {
          // Mettre à jour le message existant
          console.log("Mise à jour du message ID:", receivedMessage.id);
          const updatedMessages = [...prev];
          updatedMessages[existingIndex] = {
            ...updatedMessages[existingIndex],
            ...receivedMessage,
            reactions: { ...receivedMessage.reactions }, // Fusionner les réactions
          };
          return updatedMessages;
        } else {
          // Ajouter un nouveau message
          console.log("Ajout d'un nouveau message ID:", receivedMessage.id);
          return [...prev, receivedMessage];
        }
      });
    } catch (error) {
      console.error("Erreur lors du traitement du message WebSocket:", error);
    }
  });
};
    client.onStompError = (frame) => {
      console.error("Erreur STOMP:", frame);
      setError("Erreur de connexion WebSocket");
    };

    client.activate();
    stompClientRef.current = client;

    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
        console.log("Déconnecté du WebSocket");
      }
    };
  }, [accessToken, channelId]);

   // Épingler un message (placeholder)
const handlePinMessage = async (messageId: string) => {
  try {
    const response = await axiosInstance.post(
      `${COLLABORATION_SERVICE_URL}/api/channels/${channelId}/messages/${messageId}/pin`,
      {},
      { headers: { Authorization: `Bearer ${accessToken}` } }
    );
    const updatedMessage = response.data;
    console.log("Réponse API /pin:", updatedMessage); // Log pour déboguer
    setMessages((prev) =>
      prev.map((msg) =>
        msg.id === messageId.toString()
          ? { ...msg, pinned: updatedMessage.pinned }
          : msg
      )
    );
    setHoveredMessageId(null);
    setSuccessMessage(updatedMessage.pinned ? "Message épinglé !" : "Message désépinglé !");
    setTimeout(() => setSuccessMessage(null), 3000);
  } catch (err: any) {
    setError(err.response?.data?.message || "Erreur lors de l'épinglage du message");
    console.error("Erreur épinglage:", err);
  }
};
  // Close menus on click outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (actionMenuRef.current && !actionMenuRef.current.contains(event.target as Node)) {
        setShowActionMenu(false);
      }
      if (optionsMenuRef.current && !optionsMenuRef.current.contains(event.target as Node)) {
        setShowOptionsMenu(false);
      }
      if (emojiMenuRef.current && !emojiMenuRef.current.contains(event.target as Node)) {
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
            members: response.data.participantIds || [],
          });
        } catch (err: any) {
          setError(err.response?.data?.message || "Erreur lors de la récupération du canal");
        }
      }
    };
    fetchChannel();
  }, [accessToken, axiosInstance, channelId]);

  // Fetch initial messages
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
              channelId: msg.channelId.toString(),
              senderId: msg.senderId.toString(),
              senderName: msg.senderName || "Unknown",
              text: msg.text,
              fileUrl: msg.fileUrl,
              mimeType: msg.mimeType,
              type: msg.type,
              createdAt: msg.createdAt,
              reactions: msg.reactions || {},
            replyToId: msg.replyToId ? msg.replyToId.toString() : null,
            replyToText: msg.replyToText || null,
            replyToSenderName: msg.replyToSenderName || null,
            pinned: msg.pinned || false,
            }))
          );
        } catch (err: any) {
          setError(err.response?.data?.message || "Erreur lors de la récupération des messages");
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
    if (!newMessage.trim() || !channel || !stompClientRef.current) return;

    const messageDTO: Message = {
        id: "",
        channelId: channelId as string,
        senderId: currentUser?.id || "",
        senderName: currentUser ? `${currentUser.firstName} ${currentUser.lastName}` : "",
        text: newMessage,
        fileUrl: null,
        mimeType: null,
        type: "TEXT",
        createdAt: new Date().toISOString(),
        replyToId: replyingTo ? replyingTo.id : null,
        replyToText: replyingTo ? replyingTo.text : null,
        replyToSenderName: replyingTo ? replyingTo.senderName : null,
        pinned: false, // Ajouté pour correspondre à l'interface
    };

    try {
        stompClientRef.current.publish({
            destination: "/app/send-message",
            body: JSON.stringify(messageDTO),
            headers: { Authorization: `Bearer ${accessToken}` },
        });
        setNewMessage("");
        setReplyingTo(null);
        setSuccessMessage("Message envoyé !");
        setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err: any) {
        setError("Erreur lors de l'envoi du message");
    }
};
    // Fonction pour générer les initiales
  const getInitials = (senderName: string) => {
    const [firstName, lastName] = senderName.split(" ");
    return `${firstName?.[0] || ""}${lastName?.[0] || ""}`.toUpperCase();
  };


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
  

  // Fonction pour générer une couleur d'avatar basée sur senderId
  const getAvatarColor = (senderId: string) => {
  const colors = [
    "#64748b", // Slate
    "#475569", // Dark Slate
    "#6b7280", // Gray
    "#7c3aed", // Soft Violet
    "#4b5563", // Cool Gray
    "#5b21b6", // Deep Purple
    "#0f766e", // Muted Teal
    "#1e293b", // Charcoal
    "#334155", // Cool Dark Blue
    "#3f3f46", // Neutral Gray
  ];
    const index = Math.abs(senderId.split("").reduce((a, c) => a + c.charCodeAt(0), 0)) % colors.length;
    return colors[index];
  };

  // Regrouper les messages par date
  const groupMessagesByDate = (messages: Message[]): MessageGroup[] => {
    const groups: { [key: string]: Message[] } = {};
    messages.forEach((msg) => {
      const date = new Date(msg.createdAt).toLocaleDateString("en-US", {
        day: "numeric",
        month: "long",
        year: "numeric",
      });
      if (!groups[date]) {
        groups[date] = [];
      }
      groups[date].push(msg);
    });
    return Object.entries(groups)
      .map(([date, messages]) => ({ date, messages }))
      .sort((a, b) => new Date(b.messages[0].createdAt).getTime() - new Date(a.messages[0].createdAt).getTime());
  };
    // Ajouter une réaction
const handleAddReaction = async (messageId: string, emoji: string) => {
  if (!stompClientRef.current || !currentUser) return;

  const reactionDTO = {
    messageId,
    participantId: currentUser.id,
    emoji,
  };

  try {
    const message = messages.find((msg) => msg.id === messageId);
    const userHasReacted = message?.reactions?.[emoji]?.includes(currentUser.id);

    if (userHasReacted) {
      await axiosInstance.delete(
        `${COLLABORATION_SERVICE_URL}/api/channels/${channelId}/messages/${messageId}/reactions`,
        {
          headers: { Authorization: `Bearer ${accessToken}` },
          data: reactionDTO,
        }
      );
    } else {
      await axiosInstance.post(
        `${COLLABORATION_SERVICE_URL}/api/channels/${channelId}/messages/${messageId}/reactions`,
        reactionDTO,
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );
    }
    setShowReactionPicker(null);
  } catch (err: any) {
    setError(err.response?.data?.message || "Erreur lors de la gestion de la réaction");
    console.error("Erreur réaction:", err);
  }
};
  // Start editing a message
const startEditingMessage = (message: Message) => {
  setEditingMessageId(message.id);
  setEditedMessageText(message.text);
};

  // Cancel editing
  const cancelEditing = () => {
    setEditingMessageId(null);
    setEditedMessageText("");
  };

  // Update a message
const handleUpdateMessage = async (messageId: string) => {
  if (!editedMessageText.trim() || !stompClientRef.current) return;

  const messageDTO: Message = {
    id: messageId,
    channelId: channelId as string,
    senderId: "",
    senderName: "",
    text: editedMessageText,
    fileUrl: null,
    mimeType: null,
    type: "TEXT",
    createdAt: new Date().toISOString(),
    replyToId: null,
    replyToText: null,
    replyToSenderName: null,
    pinned: false,
    modified: true, // Indiquer que le message est modifié
  };

  try {
    await axiosInstance.patch(
      `${COLLABORATION_SERVICE_URL}/api/channels/${channelId}/messages/${messageId}`,
      messageDTO,
      {
        headers: { Authorization: `Bearer ${accessToken}` },
      }
    );
    setMessages((prev) =>
      prev.map((msg) =>
        msg.id === messageId ? { ...msg, text: editedMessageText, modified: true } : msg
      )
    );
    setEditingMessageId(null);
    setEditedMessageText("");
    setSuccessMessage("Message modifié !");
    setTimeout(() => setSuccessMessage(null), 3000);
  } catch (err: any) {
    setError(err.response?.data?.message || "Erreur lors de la modification du message");
  }
};

  const messageGroups = groupMessagesByDate(messages);
  // Delete a message
  const handleDeleteMessage = async (messageId: string) => {
    if (!confirm("Voulez-vous vraiment supprimer ce message ?")) return;

    try {
      await axiosInstance.delete(
        `${COLLABORATION_SERVICE_URL}/api/channels/${channelId}/messages/${messageId}`,
        {
          headers: { Authorization: `Bearer ${accessToken}` },
        }
      );
      setSuccessMessage("Message supprimé !");
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || "Erreur lors de la suppression du message");
    }
  };

  // Upload file (to be implemented)
  const handleUploadFile = () => {
    alert("Fonctionnalité d'upload de fichier à implémenter");
    setShowOptionsMenu(false);
  };

  // Add emoji to message
  const handleEmojiSelect = (emojiData: EmojiClickData) => {
    setNewMessage((prev) => prev + emojiData.emoji);
    setShowEmojiMenu(false);
  };

  // Edit channel
  const handleEditChannel = () => {
    alert("Fonctionnalité de modification du canal à implémenter");
    setShowActionMenu(false);
  };

  // Delete channel
  const handleDeleteChannel = async () => {
    if (!confirm("Voulez-vous vraiment supprimer ce canal ?")) return;

    try {
      await axiosInstance.delete(`${COLLABORATION_SERVICE_URL}/api/channels/${channelId}`, {
        headers: { Authorization: `Bearer ${accessToken}` },
      });
      setSuccessMessage("Canal supprimé avec succès !");
      setTimeout(() => {
        router.push("/user/dashboard/collaboration");
      }, 2000);
    } catch (err: any) {
      setError(err.response?.data?.message || "Erreur lors de la suppression du canal");
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
      {successMessage && <div className="success-message">{successMessage}</div>}

      <div className="channel-header">
        <div className="channel-info">
          <FontAwesomeIcon
            icon={channel.type === "TEXT" ? faHashtag : faVolumeUp}
            className="channel-icon"
          />
          <h2 className="channel-title">{channel.name}</h2>
          {channel.isPrivate && (
            <FontAwesomeIcon icon={faLock} className="private-icon" />
          )}
        </div>
        <div className="channel-actions">
         <button
    className="action-btn"
    title="Voir les messages épinglés"
    onClick={() => setShowPinnedMessages(!showPinnedMessages)}
  >
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
                  <FontAwesomeIcon icon={faEdit} /> Modifier le canal
                </button>
                <button className="menu-item delete" onClick={handleDeleteChannel}>
                  <FontAwesomeIcon icon={faTrash} /> Supprimer le canal
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
{showPinnedMessages && (
  <div className="pinned-messages-panel">
    <div className="pinned-messages-header">
      <h3>Messages Épinglés</h3>
      <button
        className="action-btn"
        onClick={() => setShowPinnedMessages(false)}
      >
        <FontAwesomeIcon icon={faTimes} />
      </button>
    </div>
    {messages.filter((msg) => msg.pinned).length > 0 ? (
      messages
        .filter((msg) => msg.pinned)
        .map((msg) => (
          <div key={msg.id} className="pinned-message">
            <div className="pinned-message-header">
              <span className="sender">{msg.senderName}</span>
              <span className="timestamp">
                {new Date(msg.createdAt).toLocaleTimeString()}
              </span>
            </div>
            <div className="pinned-message-content">{msg.text}</div>
            <button
              className="action-btn"
              title={msg.pinned ? "Désépingler" : "Épingler"}
              onClick={() => handlePinMessage(msg.id)}
            >
              <FontAwesomeIcon icon={faThumbtack} />
            </button>
          </div>
        ))
    ) : (
      <div className="no-pinned-messages">Aucun message épinglé.</div>
    )}
  </div>
)}
      <div className="channel-content">
        {channel.type === "TEXT" ? (
          <div className="chat-section">
          <div className="pinned-messages">
  {messages.filter(msg => msg.pinned).length > 0 && (
    <>
      <h3>Messages Épinglés</h3>
      {messages
        .filter(msg => msg.pinned)
        .map(msg => (
          <div key={msg.id} className="pinned-message">
            <span className="pinned-message-text">
              {msg.senderName}: {msg.text}
            </span>
            <button
              className="action-btn"
              onClick={() => handlePinMessage(msg.id)}
            >
              <FontAwesomeIcon icon={faThumbtack} />
            </button>
          </div>
        ))}
    </>
  )}
</div>
        <div className="messages">
  {messageGroups.length > 0 ? (
    messageGroups.map((group) => (
      <div key={group.date} className="message-group">
        <div className="date-separator">
          <span>{group.date}</span>
        </div>
        {group.messages.map((msg) => (
        <div
    key={msg.id} // Ensure msg.id is unique
    className={`message ${msg.senderId === currentUser?.id ? 'message-own' : 'message-other'}`}
    onMouseEnter={() => setHoveredMessageId(msg.id)}
    onMouseLeave={(e) => {
      if (e.relatedTarget instanceof Node && e.currentTarget.contains(e.relatedTarget)) {
        return;
      }
      setHoveredMessageId(null);
    }}
  >
            {msg.senderId !== currentUser?.id && (
              <div className="avatar" style={{ backgroundColor: getAvatarColor(msg.senderId) }}>
                {getInitials(msg.senderName)}
              </div>
            )}
            <div className="message-body">
              {msg.replyToId && (
            <div className="reply-preview">
                <div className="reply-header">
                    <span>Réponse à {msg.replyToSenderName}</span>
                </div>
                <div className="reply-content">{msg.replyToText}</div>
            </div>
        )}
              <div className="message-header">
                <span className="sender">{msg.senderName}</span>
                <span className="timestamp">
                  {new Date(msg.createdAt).toLocaleTimeString()}
                </span>
                {msg.pinned && (
          <FontAwesomeIcon
            icon={faThumbtack}
            className="pinned-icon"
            title="Message épinglé"
          />
        )}
              </div>
          {editingMessageId === msg.id ? (
    <div className="message-edit">
      <input
        type="text"
        value={editedMessageText}
        onChange={(e) => setEditedMessageText(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter" && editedMessageText.trim()) {
            handleUpdateMessage(msg.id);
          } else if (e.key === "Escape") {
            cancelEditing();
          }
        }}
        className="message-input"
        autoFocus
      />
    </div>
  ) : (
    <div className="message-content">
          {msg.text}
          {msg.modified && (
            <span className="edited-indicator" title="Message modifié">
              (edited)
            </span>
          )}
        </div>
  )}
{msg.reactions && Object.keys(msg.reactions).length > 0 && (
    <div className="reactions">
      {Object.entries(msg.reactions).map(([emoji, userIds]) => (
        <span
          key={emoji}
          className="reaction"
          title={userIds
            .map((userId) =>
              userId === currentUser?.id
                ? "Vous"
                : messages.find((m) => m.senderId === userId)?.senderName || "Inconnu"
            )
            .join(", ")}
          onClick={() => currentUser?.id && handleAddReaction(msg.id, emoji)}
        >
          {emoji} {userIds.length}
        </span>
      ))}
    </div>
  )}
            </div>
          {hoveredMessageId === msg.id && (
      <div className="message-actions">
        <button
          className="action-btn"
          title="Répondre"
          onClick={() => setReplyingTo(msg)}
        >
          <FontAwesomeIcon icon={faReply} />
        </button>
        {msg.senderId === currentUser?.id && (
          <>
            <button
              className="action-btn"
              title="Modifier"
              onClick={() => startEditingMessage(msg)}
            >
              <FontAwesomeIcon icon={faEdit} />
            </button>
            <button
              className="action-btn"
              title="Supprimer"
              onClick={() => handleDeleteMessage(msg.id)}
            >
              <FontAwesomeIcon icon={faTrash} />
            </button>
          </>
        )}
        <button
          className="action-btn"
          title="Réagir"
          onClick={() => setShowReactionPicker(msg.id)}
        >
          <FontAwesomeIcon icon={faSmile} />
        </button>
        <button
          className="action-btn"
          title={msg.pinned ? "Désépingler" : "Épingler"}
          onClick={() => handlePinMessage(msg.id)}
        >
          <FontAwesomeIcon icon={faThumbtack} />
        </button>
      </div>
    )}
            {showReactionPicker === msg.id && (
              <div className="reaction-picker">
                <EmojiPicker
                  onEmojiClick={(emojiData) => handleAddReaction(msg.id, emojiData.emoji)}
                />
              </div>
            )}
          </div>
        ))}
      </div>
    ))
  ) : (
    <div className="no-messages">Aucun message pour le moment.</div>
  )}
  <div ref={messagesEndRef} />
</div>
            <form className="message-form" onSubmit={handleSendMessage}>
               <div className="message-input-container">
        {replyingTo && (
            <div className="reply-preview">
                <div className="reply-header">
                    <span>Réponse à {replyingTo.senderName}</span>
                    <button
                        className="action-btn"
                        onClick={() => setReplyingTo(null)}
                    >
                        <FontAwesomeIcon icon={faTimes} />
                    </button>
                </div>
                <div className="reply-content">{replyingTo.text}</div>
            </div>
        )}
        <input
            type="text"
            value={newMessage}
            onChange={(e) => setNewMessage(e.target.value)}
            placeholder="Send a message ..."
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
                        <button className="menu-item" onClick={handleUploadFile}>
                          Upload File
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
                    className={`send-btn ${newMessage.trim() ? "" : "disabled"}`}
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
            <button className="join-call-btn">Rejoindre lappel</button>
          </div>
        )}
      </div>
    </div>
  );
};

export default ChannelPage;