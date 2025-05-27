"use client";
import React, { useState, useEffect, useRef } from "react";
import { useAuth } from "../../../../context/AuthContext";
import useAxios from "../../../../hooks/useAxios";
import "../../../../styles/Chatbot.css";
import { AUTH_SERVICE_URL} from "@/config/useApi";

interface Message {
  id: string;
  text: string;
  sender: "user" | "bot";
  buttons?: { label: string; action: string }[];
}

interface User {
  id: string;
  firstName?: string;
  lastName?: string;
  avatar?: string;
  notificationPreferences?: {
    emailNotifications: boolean;
    taskUpdates: boolean;
    deadlineReminders: boolean;
  };
}

const Chatbot: React.FC = () => {
  const { accessToken, isLoading } = useAuth(); // Pas de 'user' direct
  const axiosInstance = useAxios();
  const [user, setUser] = useState<User | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [isOpen, setIsOpen] = useState(false);
  const [isTyping, setIsTyping] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Charger le script Lottie
  useEffect(() => {
    const script = document.createElement("script");
    script.src =
      "https://unpkg.com/@dotlottie/player-component@2.7.12/dist/dotlottie-player.mjs";
    script.type = "module";
    script.async = true;
    script.onload = () => console.log("Lottie script loaded");
    script.onerror = () => console.error("Failed to load Lottie script");
    document.body.appendChild(script);
    return () => {
      if (document.body.contains(script)) {
        document.body.removeChild(script);
      }
    };
  }, []);

  // Récupérer les infos de l'utilisateur
  useEffect(() => {
    const fetchUserInfo = async () => {
      if (!accessToken || isLoading) return;
      try {
        console.log("🔍 Récupération des détails de l'utilisateur...");
        const response = await axiosInstance.get(
          `${AUTH_SERVICE_URL}/api/me`,
          {
            headers: { Authorization: `Bearer ${accessToken}` },
          }
        );
        console.log("✅ Réponse de /api/me:", response.data);
        setUser({
          ...response.data,
          avatar:
            response.data.avatar ||
            `https://ui-avatars.com/api/?name=${response.data.firstName?.charAt(0) || "U"}+${response.data.lastName?.charAt(0) || "U"}`,
          notificationPreferences:
            response.data.notificationPreferences || {
              emailNotifications: true,
              taskUpdates: true,
              deadlineReminders: true,
            },
        });
      } catch (err: any) {
        console.error("Erreur lors de la récupération des infos utilisateur:", err);
        setUser(null);
      }
    };

    fetchUserInfo();
  }, [accessToken, isLoading, axiosInstance]);

  // Établir la connexion WebSocket
  useEffect(() => {
    if (!accessToken || isLoading || !user) {
      console.log("WebSocket not initialized", { accessToken, isLoading, user });
      return;
    }

    const ws = new WebSocket(`ws://localhost:8000/api/v1/ws/chat`);
    wsRef.current = ws;

    ws.onopen = () => {
      console.log("WebSocket connected");
      ws.send(JSON.stringify({ token: `Bearer ${accessToken}` }));
    };

    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      if (data.error) {
        setMessages((prev) => [
          ...prev,
          { id: Date.now().toString(), text: data.error, sender: "bot" },
        ]);
        return;
      }
      if (data.status === "typing") {
        setIsTyping(true);
        return;
      }
      setIsTyping(false);
      setMessages((prev) => [
        ...prev,
        {
          id: Date.now().toString(),
          text: data.response,
          sender: "bot",
          buttons: data.buttons || [],
        },
      ]);
    };

    ws.onclose = () => {
      console.log("WebSocket disconnected, attempting to reconnect...");
      setTimeout(() => {
        if (!isLoading && user) {
          wsRef.current = new WebSocket(`ws://localhost:8000/api/v1/ws/chat`);
        }
      }, 3000);
    };

    ws.onerror = (error) => {
      console.error("WebSocket error:", error);
      setMessages((prev) => [
        ...prev,
        { id: Date.now().toString(), text: "Connection error", sender: "bot" },
      ]);
    };

    return () => {
      ws.close();
    };
  }, [accessToken, isLoading, user]);

  // Faire défiler les messages vers le bas
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // Envoyer un message
  const sendMessage = async (query: string = input) => {
    if (!query.trim() || !wsRef.current || !user) return;

    setMessages((prev) => [
      ...prev,
      { id: Date.now().toString(), text: query, sender: "user" },
    ]);
    wsRef.current.send(JSON.stringify({ query, userId: user.id }));
    setInput("");
  };

  // Gérer le clic sur un bouton
  const handleButtonClick = (action: string) => {
    sendMessage(action);
  };

  return (
    <div className="chatbot-container">
      <div
        className={`chatbot-toggle ${isOpen ? "chatbot-toggle-open" : ""}`}
        onClick={() => setIsOpen(!isOpen)}
        dangerouslySetInnerHTML={{
          __html: `
            <dotlottie-player
              src="https://lottie.host/0f8ef4f1-c4d7-49d8-b0ff-34f15a6206f0/7iPqJOgxDg.lottie"
              background="transparent"
              speed="1"
              style="width: ${isOpen ? "90px" : "90px"}; height: ${isOpen ? "90px" : "90px"}; cursor: pointer; filter: drop-shadow(0 4px 8px rgba(0,0,0,0.5));"
              loop
              autoplay
            ></dotlottie-player>
          `,
        }}
      />
      {isOpen && (
        <div className="chatbot-window">
          <div className="chatbot-header">
            <h3>TaskBot</h3>
          </div>
          <div className="messages">
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`message ${msg.sender === "user" ? "user" : "bot"}`}
              >
                <div className="message-content">{msg.text}</div>
                {msg.buttons && (
                  <div className="message-buttons">
                    {msg.buttons.map((btn, idx) => (
                      <button
                        key={idx}
                        className="action-button"
                        onClick={() => handleButtonClick(btn.action)}
                      >
                        {btn.label}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            ))}
            {isTyping && (
              <div className="typing">
                <span className="dot"></span>
                <span className="dot"></span>
                <span className="dot"></span>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>
          <div className="input-container">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyPress={(e) => e.key === "Enter" && sendMessage()}
              placeholder="Ask about tasks or projects..."
              className="chat-input"
            />
            <button className="send-button" onClick={() => sendMessage()}>
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="currentColor"
                width="24"
                height="24"
              >
                <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" />
              </svg>
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default Chatbot;