// /app/user/dashboard/collaboration/channels/[channelId]/page.tsx
"use client";

import React, { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import useAxios from "@/hooks/useAxios";
import { COLLABORATION_SERVICE_URL } from "@/config/useApi";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faHashtag, faVolumeUp, faLock, faPaperPlane } from "@fortawesome/free-solid-svg-icons";
import "../../../../../../styles/ChannelPage.css";

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
  const [channel, setChannel] = useState<Channel | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [newMessage, setNewMessage] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

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
            members: response.data.participants?.map((p: any) => p.id.toString()) || [],
          });
        } catch (err: any) {
          setError(err.response?.data?.message || "Erreur lors de la récupération du canal");
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
          setError(err.response?.data?.message || "Erreur lors de la récupération des messages");
        }
      }
    };
    fetchMessages();
  }, [accessToken, axiosInstance, channelId, channel?.type]);

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
      setError(err.response?.data?.message || "Erreur lors de l'envoi du message");
    }
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
                <div className="no-messages">Aucun message pour le moment.</div>
              )}
            </div>
            <form className="message-form" onSubmit={handleSendMessage}>
              <input
                type="text"
                value={newMessage}
                onChange={(e) => setNewMessage(e.target.value)}
                placeholder="Écrire un message..."
                className="message-input"
              />
              <button type="submit" className="send-btn">
                <FontAwesomeIcon icon={faPaperPlane} />
              </button>
            </form>
          </div>
        ) : (
          <div className="voice-section">
            <h3>Salon Vocal : {channel.name}</h3>
            <p>Rejoindre le salon vocal (fonctionnalité à venir).</p>
            <button className="join-call-btn">Rejoindre l'appel</button>
          </div>
        )}
        <div className="files-section">
          <h3>Fichiers</h3>
          <p>Section pour les fichiers partagés (à implémenter).</p>
        </div>
      </div>
    </div>
  );
};

export default ChannelPage;