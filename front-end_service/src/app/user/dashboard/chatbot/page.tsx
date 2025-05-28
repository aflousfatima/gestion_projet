"use client";
import React, { useState, useEffect, useRef } from "react";
import { useAuth } from "../../../../context/AuthContext";
import useAxios from "../../../../hooks/useAxios";
import "../../../../styles/Chatbot.css";
import { AUTH_SERVICE_URL } from "@/config/useApi";

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

// Define the expected shape of WebSocket messages
interface WebSocketMessage {
  error?: string;
  status?: string;
  response?: string;
  buttons?: { label: string; action: string }[];
  intent?: string;
  parameters?: Record<string, any>;
  confidence?: number;
}

const Chatbot: React.FC = () => {
  const { accessToken, isLoading } = useAuth();
  const axiosInstance = useAxios();
  const [user, setUser] = useState<User | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [isOpen, setIsOpen] = useState(false);
  const [isTyping, setIsTyping] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const reconnectAttempts = useRef(0);
  const maxReconnectAttempts = 5;

  // Load Lottie script
  useEffect(() => {
    console.debug("[Chatbot] Loading Lottie script");
    const script = document.createElement("script");
    script.src =
      "https://unpkg.com/@dotlottie/player-component@2.7.12/dist/dotlottie-player.mjs";
    script.type = "module";
    script.async = true;
    script.onload = () => console.info("[Chatbot] Lottie script loaded successfully");
    script.onerror = () => console.error("[Chatbot] Failed to load Lottie script");
    document.body.appendChild(script);
    return () => {
      if (document.body.contains(script)) {
        document.body.removeChild(script);
        console.debug("[Chatbot] Lottie script removed");
      }
    };
  }, []);

  // Fetch user info
  useEffect(() => {
    const fetchUserInfo = async () => {
      if (!accessToken || isLoading) {
        console.debug("[Chatbot] Skipping user info fetch: no token or loading", {
          accessToken,
          isLoading,
        });
        return;
      }
      try {
        console.info("[Chatbot] Fetching user info from", `${AUTH_SERVICE_URL}/api/me`);
        const response = await axiosInstance.get(`${AUTH_SERVICE_URL}/api/me`, {
          headers: { Authorization: `Bearer ${accessToken}` },
        });
        console.debug("[Chatbot] User info response:", response.data);
        const userData: User = {
          ...response.data,
          avatar:
            response.data.avatar ||
            `https://ui-avatars.com/api/?name=${
              response.data.firstName?.charAt(0) || "U"
            }+${response.data.lastName?.charAt(0) || "U"}`,
          notificationPreferences:
            response.data.notificationPreferences || {
              emailNotifications: true,
              taskUpdates: true,
              deadlineReminders: true,
            },
        };
        setUser(userData);
        console.info("[Chatbot] User info set:", {
          id: userData.id,
          firstName: userData.firstName,
        });
      } catch (err: any) {
        console.error("[Chatbot] Error fetching user info:", err.message, err.response?.data);
        setUser(null);
      }
    };

    fetchUserInfo();
  }, [accessToken, isLoading, axiosInstance]);

  // Establish WebSocket connection
  useEffect(() => {
    if (!accessToken || isLoading || !user) {
      console.debug("[Chatbot] WebSocket not initialized", {
        accessToken: !!accessToken,
        isLoading,
        user: !!user,
      });
      return;
    }

    const connectWebSocket = () => {
      console.info("[Chatbot] Initializing WebSocket connection to ws://localhost:8000/api/v1/ws/chat");
      const ws = new WebSocket(`ws://localhost:8000/api/v1/ws/chat`);
      wsRef.current = ws;

      ws.onopen = () => {
        console.info("[Chatbot] WebSocket connected");
        reconnectAttempts.current = 0; // Reset reconnect attempts
        if (accessToken) {
          const tokenMessage = { token: `Bearer ${accessToken}` };
          console.debug("[Chatbot] Sending token message:", tokenMessage);
          ws.send(JSON.stringify(tokenMessage));
        } else {
          console.error("[Chatbot] No accessToken available, closing WebSocket");
          ws.close(1008); // Policy violation
        }
      };

      ws.onmessage = (event) => {
        console.debug("[Chatbot] Received WebSocket message:", event.data);
        let data: WebSocketMessage;
        try {
          data = JSON.parse(event.data);
        } catch (err) {
          console.error("[Chatbot] Failed to parse WebSocket message:", err);
          return;
        }
        if (data.error) {
          console.error("[Chatbot] Server error:", data.error);
          setMessages((prev) => [
            ...prev,
            {
              id: Date.now().toString(),
              text: data.error || "Une erreur inconnue est survenue", // Fallback if error is undefined
              sender: "bot" as const,
            },
          ]);
          return;
        }
        if (data.status === "typing") {
          console.debug("[Chatbot] Server is typing");
          setIsTyping(true);
          return;
        }
        setIsTyping(false);
        if (data.response) {
          const newMessage: Message = {
            id: Date.now().toString(),
            text: data.response,
            sender: "bot" as const,
            buttons: Array.isArray(data.buttons)
              ? data.buttons.map((btn) => ({
                  label: btn.label,
                  action: btn.action,
                }))
              : [],
          };
          console.info("[Chatbot] Adding bot message:", newMessage.text);
          setMessages((prev) => [...prev, newMessage]);
        }
      };

      ws.onclose = (event) => {
        console.warn("[Chatbot] WebSocket disconnected", { code: event.code, reason: event.reason });
        if (reconnectAttempts.current < maxReconnectAttempts) {
          const delay = Math.min(1000 * 2 ** reconnectAttempts.current, 10000); // Exponential backoff
          console.info(
            `[Chatbot] Attempting to reconnect WebSocket in ${delay}ms (attempt ${reconnectAttempts.current + 1})`
          );
          setTimeout(() => {
            if (!isLoading && user && accessToken) {
              reconnectAttempts.current += 1;
              connectWebSocket();
            } else {
              console.debug("[Chatbot] Reconnection skipped", {
                isLoading,
                user: !!user,
                accessToken: !!accessToken,
              });
            }
          }, delay);
        } else {
          console.error("[Chatbot] Max reconnect attempts reached, giving up");
          setMessages((prev) => [
            ...prev,
            {
              id: Date.now().toString(),
              text: "Impossible de se connecter au serveur",
              sender: "bot" as const,
            },
          ]);
        }
      };

      ws.onerror = (error) => {
        console.error("[Chatbot] WebSocket error:", error);
        setMessages((prev) => [
          ...prev,
          {
            id: Date.now().toString(),
            text: "Erreur de connexion",
            sender: "bot" as const,
          },
        ]);
      };
    };

    connectWebSocket();

    return () => {
      console.debug("[Chatbot] Cleaning up WebSocket");
      if (wsRef.current) {
        wsRef.current.close(1000); // Normal closure
        console.info("[Chatbot] WebSocket closed");
      }
    };
  }, [accessToken, isLoading, user]);

  // Scroll to latest message
  useEffect(() => {
    console.debug("[Chatbot] Scrolling to latest message");
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // Send message
  const sendMessage = async (query: string = input) => {
    if (!query.trim() || !wsRef.current || !user) {
      console.warn("[Chatbot] Cannot send message", {
        query: !!query.trim(),
        ws: !!wsRef.current,
        user: !!user,
      });
      return;
    }

    if (wsRef.current.readyState !== WebSocket.OPEN) {
      console.error("[Chatbot] WebSocket is not open, state:", wsRef.current.readyState);
      setMessages((prev) => [
        ...prev,
        {
          id: Date.now().toString(),
          text: "Erreur : Connexion WebSocket fermée",
          sender: "bot" as const,
        },
      ]);
      return;
    }

    const userMessage: Message = {
      id: Date.now().toString(),
      text: query,
      sender: "user" as const,
    };
    console.info("[Chatbot] Adding user message:", userMessage.text);
    setMessages((prev) => [...prev, userMessage]);

    const message = { query, userId: user.id };
    console.debug("[Chatbot] Sending message:", message);
    wsRef.current.send(JSON.stringify(message));
    setInput("");
    console.debug("[Chatbot] Input cleared");
  };

  // Handle button click
  const handleButtonClick = (action: string) => {
    console.debug("[Chatbot] Button clicked, action:", action);
    sendMessage(action);
  };

  return (
    <div className="chatbot-container">
      <div
        className={`chatbot-toggle ${isOpen ? "chatbot-toggle-open" : ""}`}
        onClick={() => {
          console.debug("[Chatbot] Toggling chatbot window", { isOpen: !isOpen });
          setIsOpen(!isOpen);
        }}
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
            <img className="navbar-logo" src="/logo.png" alt="logo" />
            <button
              className="close-button"
              onClick={() => {
                console.debug("[Chatbot] Closing chatbot window");
                setIsOpen(false);
              }}
            >
              ×
            </button>
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
              onChange={(e) => {
                console.debug("[Chatbot] Input changed:", e.target.value);
                setInput(e.target.value);
              }}
              onKeyPress={(e) => {
                if (e.key === "Enter") {
                  console.debug("[Chatbot] Enter key pressed");
                  sendMessage();
                }
              }}
              placeholder="Compose your message..."
              className="chat-input"
            />
            <button
              className="send-button"
              onClick={() => {
                console.debug("[Chatbot] Send button clicked");
                sendMessage();
              }}
            >
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