"use client";

import React, { useEffect, useState, useRef } from "react";
import { useParams, useRouter } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import useAxios from "@/hooks/useAxios";
import { AUTH_SERVICE_URL, COLLABORATION_SERVICE_URL } from "@/config/useApi";
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
  faReply,
  faTrashAlt,
  faPlay,
  faPause,
  faStop,
  faFile
} from "@fortawesome/free-solid-svg-icons";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import EmojiPicker, { EmojiClickData } from "emoji-picker-react";
import "../../../../../../../styles/Collaboration-Channel.css";

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
  modified?: boolean;
  duration?: number;
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
  email: string;
}

const ChannelPage: React.FC = () => {
  const { channelId } = useParams();
  const { accessToken } = useAuth();
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
  const [isRecording, setIsRecording] = useState(false);
  const [audioBlob, setAudioBlob] = useState<Blob | null>(null);
  const [recordingTime, setRecordingTime] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const timerRef = useRef<NodeJS.Timeout | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const AudioPlayer: React.FC<{ src: string; duration?: number }> = ({
    src,
    duration,
  }) => {
    const [computedDuration, setComputedDuration] = useState<number | null>(
      duration || null
    );
    const audioRef = useRef<HTMLAudioElement>(null);

    useEffect(() => {
      const audio = audioRef.current;
      if (audio && !duration) {
        const handleLoadedMetadata = () => {
          console.log("Durée brute de l'audio:", audio.duration);
          if (isNaN(audio.duration) || !isFinite(audio.duration)) {
            console.warn("Durée invalide détectée:", audio.duration);
            setComputedDuration(null);
          } else {
            setComputedDuration(audio.duration);
          }
        };

        const handleError = () => {
          console.error("Erreur lors du chargement de l'audio:", audio.error);
          setComputedDuration(duration || null);
        };

        audio.addEventListener("loadedmetadata", handleLoadedMetadata);
        audio.addEventListener("error", handleError);
        audio.load();

        return () => {
          audio.removeEventListener("loadedmetadata", handleLoadedMetadata);
          audio.removeEventListener("error", handleError);
        };
      }
    }, [src, duration]);

    const formatDuration = (seconds: number | null) => {
      if (seconds === null || isNaN(seconds) || !isFinite(seconds)) {
        return "Durée inconnue";
      }
      const mins = Math.floor(seconds / 60);
      const secs = Math.floor(seconds % 60)
        .toString()
        .padStart(2, "0");
      return `${mins}:${secs}`;
    };

    return (
      <div className="audio-player-container">
        <audio ref={audioRef} controls src={src} className="audio-player">
          Votre navigateur ne prend pas en charge l'élément audio.
        </audio>
        <span className="audio-duration">{formatDuration(computedDuration)}</span>
      </div>
    );
  };

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
            id: JSON.parse(message.body).id.toString(),
            replyToId: JSON.parse(message.body).replyToId
              ? JSON.parse(message.body).replyToId.toString()
              : null,
            replyToText: JSON.parse(message.body).replyToText || null,
            replyToSenderName: JSON.parse(message.body).replyToSenderName || null,
            pinned: JSON.parse(message.body).pinned || false,
            reactions: JSON.parse(message.body).reactions || {},
            modified: JSON.parse(message.body).modified || false,
          };
          console.log("Message reçu via WebSocket, ID:", receivedMessage.id);

          setMessages((prev) => {
            const existingIndex = prev.findIndex(
              (msg) => msg.id === receivedMessage.id
            );
            if (
              receivedMessage.type === "SYSTEM" &&
              receivedMessage.text === "Message supprimé"
            ) {
              console.log("Suppression du message ID:", receivedMessage.id);
              return prev.filter((msg) => msg.id !== receivedMessage.id);
            } else if (existingIndex >= 0) {
              console.log("Mise à jour du message ID:", receivedMessage.id);
              const updatedMessages = [...prev];
              updatedMessages[existingIndex] = {
                ...updatedMessages[existingIndex],
                ...receivedMessage,
                reactions: { ...receivedMessage.reactions },
              };
              return updatedMessages;
            } else {
              console.log("Ajout d'un nouveau message ID:", receivedMessage.id);
              return [...prev, receivedMessage];
            }
          });
        } catch (error) {
          console.error(
            "Erreur lors du traitement du message WebSocket:",
            error
          );
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

  // Épingler un message
  const handlePinMessage = async (messageId: string) => {
    try {
      const response = await axiosInstance.post(
        `${COLLABORATION_SERVICE_URL}/api/channels/${channelId}/messages/${messageId}/pin`,
        {},
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );
      const updatedMessage = response.data;
      console.log("Réponse API /pin:", updatedMessage);
      setMessages((prev) =>
        prev.map((msg) =>
          msg.id === messageId.toString()
            ? { ...msg, pinned: updatedMessage.pinned }
            : msg
        )
      );
      setHoveredMessageId(null);
      setSuccessMessage(
        updatedMessage.pinned ? "Message épinglé !" : "Message désépinglé !"
      );
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err: any) {
      setError(
        err.response?.data?.message || "Erreur lors de l'épinglage du message"
      );
      console.error("Erreur épinglage:", err);
    }
  };

  // Close menus on click outside
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
            members: response.data.participantIds || [],
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
              duration: msg.duration || null,
            }))
          );
        } catch (err: any) {
          setError(
            err.response?.data?.message ||
              "Erreur lors de la récupération des messages"
          );
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
      senderName: currentUser
        ? `${currentUser.firstName} ${currentUser.lastName}`
        : "",
      text: newMessage,
      fileUrl: null,
      mimeType: null,
      type: "TEXT",
      createdAt: new Date().toISOString(),
      replyToId: replyingTo ? replyingTo.id : null,
      replyToText: replyingTo ? replyingTo.text : null,
      replyToSenderName: replyingTo ? replyingTo.senderName : null,
      pinned: false,
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

  const pauseRecording = () => {
    if (mediaRecorderRef.current && isRecording && !isPaused) {
      console.log(
        "Pause de l'enregistrement, recordingTime actuel:",
        recordingTime
      );
      mediaRecorderRef.current.pause();
      setIsPaused(true);
      clearInterval(timerRef.current!);
      console.log("Timer mis en pause");
    } else {
      console.warn(
        "Impossible de mettre en pause: enregistrement non actif ou déjà en pause"
      );
    }
  };

  const resumeRecording = () => {
    if (mediaRecorderRef.current && isRecording && isPaused) {
      console.log(
        "Reprise de l'enregistrement, recordingTime actuel:",
        recordingTime
      );
      mediaRecorderRef.current.resume();
      setIsPaused(false);
      timerRef.current = setInterval(() => {
        setRecordingTime((prev) => {
          const newTime = prev + 1;
          console.log("Mise à jour recordingTime:", newTime);
          return newTime;
        });
      }, 1000);
      console.log("Timer repris, Timer ID:", timerRef.current);
    } else {
      console.warn(
        "Impossible de reprendre: enregistrement non actif ou non en pause"
      );
    }
  };

  const getInitials = (senderName: string) => {
    const [firstName, lastName] = senderName.split(" ");
    return `${firstName?.[0] || ""}${lastName?.[0] || ""}`.toUpperCase();
  };

  // Fetch current user
  useEffect(() => {
    const fetchCurrentUser = async () => {
      if (accessToken && !currentUser) {
        try {
          const response = await axiosInstance.get(
            `${AUTH_SERVICE_URL}/api/me`,
            {
              headers: { Authorization: `Bearer ${accessToken}` },
            }
          );
          setCurrentUser({
            id: response.data.id.toString(),
            firstName: response.data.firstName,
            lastName: response.data.lastName,
            email: response.data.email,
          });
        } catch (err: any) {
          console.error("Failed to fetch current user:", err);
          setError(
            err.response?.data?.message ||
              "Erreur lors de la récupération de l'utilisateur"
          );
        }
      }
    };
    fetchCurrentUser();
  }, [accessToken, axiosInstance, currentUser]);

  const getAvatarColor = (senderId: string) => {
    const colors = [
      "#64748b",
      "#475569",
      "#6b7280",
      "#7c3aed",
      "#4b5563",
      "#5b21b6",
      "#0f766e",
      "#1e293b",
      "#334155",
      "#3f3f46",
    ];
    const index =
      Math.abs(senderId.split("").reduce((a, c) => a + c.charCodeAt(0), 0)) %
      colors.length;
    return colors[index];
  };

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
      .sort(
        (a, b) =>
          new Date(b.messages[0].createdAt).getTime() -
          new Date(a.messages[0].createdAt).getTime()
      );
  };

  const handleAddReaction = async (messageId: string, emoji: string) => {
    if (!stompClientRef.current || !currentUser) return;

    const reactionDTO = {
      messageId,
      participantId: currentUser.id,
      emoji,
    };

    try {
      const message = messages.find((msg) => msg.id === messageId);
      const userHasReacted = message?.reactions?.[emoji]?.includes(
        currentUser.id
      );

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
      setError(
        err.response?.data?.message ||
          "Erreur lors de la gestion de la réaction"
      );
      console.error("Erreur réaction:", err);
    }
  };

  const startEditingMessage = (message: Message) => {
    setEditingMessageId(message.id);
    setEditedMessageText(message.text);
  };

  const cancelEditing = () => {
    setEditingMessageId(null);
    setEditedMessageText("");
  };

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
      modified: true,
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
          msg.id === messageId
            ? { ...msg, text: editedMessageText, modified: true }
            : msg
        )
      );
      setEditingMessageId(null);
      setEditedMessageText("");
      setSuccessMessage("Message modifié !");
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err: any) {
      setError(
        err.response?.data?.message ||
          "Erreur lors de la modification du message"
      );
    }
  };

  const messageGroups = groupMessagesByDate(messages);

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
      setError(
        err.response?.data?.message ||
          "Erreur lors de la suppression du message"
      );
    }
  };

const handleUploadFile = async (type: "IMAGE" | "FILE") => {
  if (!fileInputRef.current || !channel || !currentUser || !stompClientRef.current) {
    setError("Conditions non remplies pour l'envoi du fichier.");
    return;
  }

  fileInputRef.current.accept = type === "IMAGE" ? "image/*" : "*/*";
  fileInputRef.current.click();

  fileInputRef.current.onchange = async () => {
    if (fileInputRef.current?.files && fileInputRef.current.files.length > 0) {
      const file = fileInputRef.current.files[0];
      const mimeType = file.type || (file.name.toLowerCase().endsWith(".pdf") ? "application/pdf" : "application/octet-stream");
      const formData = new FormData();
      formData.append("file", file);
      if (replyingTo) {
        formData.append("replyToId", replyingTo.id);
      }

      try {
        const tempMessage: Message = {
          id: `temp-${Date.now()}`,
          channelId: channelId as string,
          senderId: currentUser.id,
          senderName: `${currentUser.firstName} ${currentUser.lastName}`,
          text: file.name,
          fileUrl: null,
          mimeType: mimeType,
          type,
          createdAt: new Date().toISOString(),
          replyToId: replyingTo ? replyingTo.id : null,
          replyToText: replyingTo ? replyingTo.text : null,
          replyToSenderName: replyingTo ? replyingTo.senderName : null,
          pinned: false,
          modified: false,
        };

        setMessages((prev) => [...prev, tempMessage]);

        const response = await axiosInstance.post(
          `${COLLABORATION_SERVICE_URL}/api/channels/${channelId}/messages/${type.toLowerCase()}`,
          formData,
          {
            headers: {
              Authorization: `Bearer ${accessToken}`,
              "Content-Type": "multipart/form-data",
            },
          }
        );

        setMessages((prev) => prev.filter((msg) => msg.id !== tempMessage.id));
        setReplyingTo(null);
        setSuccessMessage(`${type === "IMAGE" ? "Image" : "Fichier"} envoyé !`);
        setTimeout(() => setSuccessMessage(null), 3000);
      } catch (err: any) {
        setError(
          err.response?.data?.message || `Erreur lors de l'envoi du ${type.toLowerCase()}`
        );
        setMessages((prev) => prev.filter((msg) => msg.id !== tempMessage.id));
        console.error(`Erreur envoi ${type.toLowerCase()}:`, err);
      } finally {
        fileInputRef.current.value = "";
      }
    }
  };
  setShowOptionsMenu(false);
};

  const handleEmojiSelect = (emojiData: EmojiClickData) => {
    setNewMessage((prev) => prev + emojiData.emoji);
    setShowEmojiMenu(false);
  };

  const handleEditChannel = () => {
    alert("Fonctionnalité de modification du canal à implémenter");
    setShowActionMenu(false);
  };

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

  const startRecording = async () => {
    try {
      console.log(
        "Début de l'enregistrement, initialisation de recordingTime à 0"
      );
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      console.log("Stream tracks:", stream.getAudioTracks());
      const mimeType = MediaRecorder.isTypeSupported("audio/webm;codecs=opus")
        ? "audio/webm;codecs=opus"
        : "audio/webm";
      console.log("MimeType utilisé:", mimeType);
      mediaRecorderRef.current = new MediaRecorder(stream, { mimeType });

      audioChunksRef.current = [];

      mediaRecorderRef.current.ondataavailable = (event) => {
        console.log("Chunk reçu, taille:", event.data.size);
        if (event.data.size > 0) {
          audioChunksRef.current.push(event.data);
        } else {
          console.warn("Chunk vide reçu");
        }
      };

      mediaRecorderRef.current.onstop = () => {
        console.log(
          "audioChunks:",
          audioChunksRef.current.length,
          "tailles:",
          audioChunksRef.current.map((chunk) => chunk.size)
        );
        const audioBlob = new Blob(audioChunksRef.current, { type: mimeType });
        console.log(
          "Audio Blob créé, taille:",
          audioBlob.size,
          "recordingTime:",
          recordingTime
        );
        setAudioBlob(audioBlob);
        stream.getTracks().forEach((track) => track.stop());
        clearInterval(timerRef.current!);
        console.log(
          "Enregistrement arrêté, recordingTime final:",
          recordingTime
        );
      };

      mediaRecorderRef.current.onstart = () => {
        console.log("MediaRecorder démarré");
      };

      mediaRecorderRef.current.onerror = (event) => {
        console.error("Erreur MediaRecorder:", event);
      };

      mediaRecorderRef.current.start(100);
      setIsRecording(true);
      setIsPaused(false);
      setRecordingTime(0);
      console.log("Timer démarré pour recordingTime");
      if (timerRef.current) {
        console.warn("Timer précédent non nettoyé, nettoyage");
        clearInterval(timerRef.current);
      }
      timerRef.current = setInterval(() => {
        setRecordingTime((prev) => {
          const newTime = prev + 1;
          console.log("Mise à jour recordingTime:", newTime);
          return newTime;
        });
      }, 1000);
      console.log("Timer ID:", timerRef.current);
    } catch (err) {
      console.error("Erreur lors de l'accès au microphone:", err);
      setError(
        "Impossible d'accéder au microphone. Vérifiez les autorisations."
      );
    }
  };

  const stopRecording = () => {
    if (mediaRecorderRef.current && isRecording) {
      console.log(
        "Arrêt de l'enregistrement, recordingTime actuel:",
        recordingTime
      );
      mediaRecorderRef.current.stop();
      setIsRecording(false);
      setIsPaused(false);
      clearInterval(timerRef.current!);
      console.log("Timer arrêté");
    }
  };

  const cancelRecording = () => {
    if (mediaRecorderRef.current && isRecording) {
      mediaRecorderRef.current.stop();
      setIsRecording(false);
      setIsPaused(false);
      setAudioBlob(null);
      setRecordingTime(0);
      clearInterval(timerRef.current!);
    }
  };

  const handleSendAudioMessage = async () => {
    if (!audioBlob || !channel || !currentUser || !stompClientRef.current) {
      console.error(
        "Conditions non remplies pour l'envoi: audioBlob, channel, currentUser ou stompClient manquant"
      );
      return;
    }

    let tempMessage: Message | null = null;

    try {
      console.log("Envoi de l'audio, recordingTime:", recordingTime);
      console.log("Taille de audioBlob:", audioBlob.size);
      let calculatedDuration = recordingTime;
      if (recordingTime <= 0) {
        console.warn(
          "recordingTime invalide, calcul de la durée à partir du Blob"
        );
        if (audioBlob.size === 0) {
          console.error("audioBlob vide, enregistrement corrompu");
          setError("Enregistrement audio vide. Veuillez réessayer.");
          return;
        }
        const audioUrl = URL.createObjectURL(audioBlob);
        console.log("URL de test pour audioBlob:", audioUrl);
        const audio = new Audio(audioUrl);
        await new Promise((resolve) => {
          audio.onloadedmetadata = () => {
            console.log("Durée brute audio.duration:", audio.duration);
            calculatedDuration =
              isFinite(audio.duration) && audio.duration > 0
                ? Math.floor(audio.duration)
                : 0;
            console.log("Durée calculée à partir du Blob:", calculatedDuration);
            resolve(null);
          };
          audio.onerror = () => {
            console.error(
              "Erreur lors du chargement des métadonnées du Blob:",
              audio.error
            );
            calculatedDuration = 0;
            resolve(null);
          };
        });
        console.log("Ouvrir l'URL pour tester l'audio:", audioUrl);
        URL.revokeObjectURL(audioUrl);
      }

      if (calculatedDuration <= 0 || !isFinite(calculatedDuration)) {
        setError("Durée d'enregistrement invalide. Veuillez réessayer.");
        console.error(
          "calculatedDuration invalide:",
          calculatedDuration,
          "envoi annulé"
        );
        return;
      }

      tempMessage = {
        id: `temp-${Date.now()}`,
        channelId: channelId as string,
        senderId: currentUser.id,
        senderName: `${currentUser.firstName} ${currentUser.lastName}`,
        text: "Envoi de l'audio en cours...",
        fileUrl: null,
        mimeType: "audio/webm",
        type: "AUDIO",
        createdAt: new Date().toISOString(),
        replyToId: replyingTo ? replyingTo.id : null,
        replyToText: replyingTo ? replyingTo.text : null,
        replyToSenderName: replyingTo ? replyingTo.senderName : null,
        pinned: false,
        modified: false,
        duration: calculatedDuration,
      };

      if (tempMessage) {
        setMessages((prev) => [...prev, tempMessage as Message]);
      }

      const formData = new FormData();
      formData.append("file", audioBlob, `audio-${Date.now()}.webm`);
      formData.append("duration", calculatedDuration.toString());
      console.log("formData duration:", calculatedDuration.toString());

      const response = await axiosInstance.post(
        `${COLLABORATION_SERVICE_URL}/api/channels/${channelId}/messages/audio`,
        formData,
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
            "Content-Type": "multipart/form-data",
          },
        }
      );
      console.log("Réponse API, duration:", response.data.duration);

      if (tempMessage) {
        setMessages((prev) => prev.filter((msg) => msg.id !== tempMessage!.id));
      }
      setAudioBlob(null);
      setReplyingTo(null);
      setRecordingTime(0);
      setSuccessMessage("Message audio envoyé !");
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err: any) {
      setError(
        err.response?.data?.message || "Erreur lors de l'envoi du message audio"
      );
      console.error("Erreur envoi audio:", err);
      if (tempMessage) {
        setMessages((prev) => prev.filter((msg) => msg.id !== tempMessage!.id));
      }
    }
  };

  const formatRecordingTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60)
      .toString()
      .padStart(2, "0");
    const secs = (seconds % 60).toString().padStart(2, "0");
    return `${mins}:${secs}`;
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
                <button
                  className="menu-item delete"
                  onClick={handleDeleteChannel}
                >
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
  {messages.filter((msg) => msg.pinned).length > 0 && (
    <>
      <h3>Messages Épinglés</h3>
      {messages
        .filter((msg) => msg.pinned)
        .map((msg) => (
          <div key={msg.id} className="pinned-message">
            <span className="pinned-message-text">
              {msg.senderName}:{" "}
              {msg.type === "IMAGE" && msg.fileUrl ? (
                <img
                  src={msg.fileUrl}
                  alt="Pinned image"
                  className="message-image"
                  style={{ maxWidth: "100px", maxHeight: "100px" }}
                />
              ) : msg.type === "FILE" && msg.fileUrl ? (
                <div className="file-preview-container">
                  {msg.mimeType?.toLowerCase().startsWith("application/pdf") || msg.fileUrl.toLowerCase().endsWith(".pdf") ? (
                    <div className="pdf-preview">
                      <object
                        data={msg.fileUrl}
                        type="application/pdf"
                        width="100"
                        height="100"
                        title="PDF Preview"
                        onError={() => console.error("PDF object failed to load:", msg.fileUrl)}
                      >
                        <a
                          href={msg.fileUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="pdf-fallback"
                        >
                          Prévisualisation non disponible
                        </a>
                      </object>
                    </div>
                  ) : (
                    <div className="file-icon">
                      <FontAwesomeIcon icon={faFile} size="1x" />
                    </div>
                  )}
                  <div className="file-info">
                    <span className="file-name">
                      {msg.text || (msg.fileUrl ? decodeURIComponent(msg.fileUrl.split("/").pop() || "Fichier sans nom") : "Fichier sans nom")}
                    </span>
                    <a
                      href={msg.fileUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="file-download-link"
                    >
                      Télécharger
                    </a>
                  </div>
                </div>
              ) : (
                msg.text
              )}
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
                        key={msg.id}
                        className={`message ${
                          msg.senderId === currentUser?.id
                            ? "message-own"
                            : "message-other"
                        }`}
                        onMouseEnter={() => setHoveredMessageId(msg.id)}
                        onMouseLeave={(e) => {
                          if (
                            e.relatedTarget instanceof Node &&
                            e.currentTarget.contains(e.relatedTarget)
                          ) {
                            return;
                          }
                          setHoveredMessageId(null);
                        }}
                      >
                        {msg.senderId !== currentUser?.id && (
                          <div
                            className="avatar"
                            style={{
                              backgroundColor: getAvatarColor(msg.senderId),
                            }}
                          >
                            {getInitials(msg.senderName)}
                          </div>
                        )}
                        <div className="message-body">
                          {msg.replyToId && (
                            <div className="reply-preview">
                              <div className="reply-header">
                                <span>Réponse à {msg.replyToSenderName}</span>
                              </div>
                              <div className="reply-content">
                                {msg.replyToText}
                              </div>
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
                                onChange={(e) =>
                                  setEditedMessageText(e.target.value)
                                }
                                onKeyDown={(e) => {
                                  if (
                                    e.key === "Enter" &&
                                    editedMessageText.trim()
                                  ) {
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
  {(() => {
    console.log("Message data:", {
      id: msg.id,
      type: msg.type,
      fileUrl: msg.fileUrl,
      text: msg.text,
      mimeType: msg.mimeType,
      raw: msg
    });
    if (msg.type === "AUDIO" && msg.fileUrl) {
      return <AudioPlayer src={msg.fileUrl} duration={msg.duration} />;
    } else if (msg.type === "IMAGE" && msg.fileUrl) {
      return (
        <img
          src={msg.fileUrl}
          alt="Uploaded image"
          className="message-image"
          style={{ maxWidth: "300px", maxHeight: "300px" }}
        />
      );
    } else if (msg.type === "FILE" && msg.fileUrl) {
      const fileName = msg.text || (msg.fileUrl ? decodeURIComponent(msg.fileUrl.split("/").pop() || "Fichier sans nom") : "Fichier sans nom");
      const isPdf = msg.mimeType?.toLowerCase().startsWith("application/pdf") || msg.fileUrl.toLowerCase().endsWith(".pdf");
      return (
        <div className="file-preview-container">
          {isPdf ? (
            <div className="pdf-preview">
              <object
                data={msg.fileUrl}
                type="application/pdf"
                width="150"
                height="200"
                title="PDF Preview"
                onError={() => console.error("PDF object failed to load:", msg.fileUrl)}
              >
                <a
                  href={msg.fileUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="pdf-fallback"
                >
                  Prévisualisation non disponible. Cliquez pour ouvrir le PDF.
                </a>
              </object>
            </div>
          ) : (
            <div className="file-icon">
              <FontAwesomeIcon icon={faFile} size="2x" />
            </div>
          )}
          <div className="file-info">
            <span className="file-name">{fileName}</span>
            <a
              href={msg.fileUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="file-download-link"
              onClick={() => console.log("Downloading file:", msg.fileUrl)}
            >
              Télécharger le fichier
            </a>
          </div>
        </div>
      );
    } else {
      return (
        <>
          {msg.text || "Message vide"}
          {msg.modified && (
            <span className="edited-indicator" title="Message modifié">
              (edited)
            </span>
          )}
        </>
      );
    }
  })()}
</div>
                          )}
                          {msg.reactions &&
                            Object.keys(msg.reactions).length > 0 && (
                              <div className="reactions">
                                {Object.entries(msg.reactions).map(
                                  ([emoji, userIds]) => (
                                    <span
                                      key={emoji}
                                      className="reaction"
                                      title={userIds
                                        .map((userId) =>
                                          userId === currentUser?.id
                                            ? "Vous"
                                            : messages.find(
                                                (m) => m.senderId === userId
                                              )?.senderName || "Inconnu"
                                        )
                                        .join(", ")}
                                      onClick={() =>
                                        currentUser?.id &&
                                        handleAddReaction(msg.id, emoji)
                                      }
                                    >
                                      {emoji} {userIds.length}
                                    </span>
                                  )
                                )}
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
                                {msg.type !== "AUDIO" && (
                                  <button
                                    className="action-btn"
                                    title="Modifier"
                                    onClick={() => startEditingMessage(msg)}
                                  >
                                    <FontAwesomeIcon icon={faEdit} />
                                  </button>
                                )}
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
                              onEmojiClick={(emojiData) =>
                                handleAddReaction(msg.id, emojiData.emoji)
                              }
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
            <form
              className="message-form"
              onSubmit={(e) => {
                e.preventDefault();
                if (audioBlob) {
                  handleSendAudioMessage();
                } else {
                  handleSendMessage(e);
                }
              }}
            >
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
                {isRecording ? (
                  <div className="recording-indicator">
                    <span className="recording-timer">
                      {formatRecordingTime(recordingTime)}
                    </span>
                    <div className="recording-wave">
                      <span></span>
                      <span></span>
                      <span></span>
                      <span></span>
                      <span></span>
                    </div>
                    <div className="recording-actions">
                      <button
                        type="button"
                        className="input-action-btn"
                        title="Supprimer l'enregistrement"
                        onClick={cancelRecording}
                      >
                        <FontAwesomeIcon icon={faTrashAlt} />
                      </button>
                      <button
                        type="button"
                        className="input-action-btn"
                        title={isPaused ? "Reprendre" : "Pause"}
                        onClick={isPaused ? resumeRecording : pauseRecording}
                      >
                        <FontAwesomeIcon icon={isPaused ? faPlay : faPause} />
                      </button>
                      <button
                        type="button"
                        className="input-action-btn"
                        title="Arrêter l'enregistrement"
                        onClick={stopRecording}
                      >
                        <FontAwesomeIcon icon={faStop} />
                      </button>
                    </div>
                  </div>
                ) : audioBlob ? (
                  <div className="audio-preview">
                    <audio
                      controls
                      src={URL.createObjectURL(audioBlob)}
                      className="audio-player"
                    >
                      Votre navigateur ne prend pas en charge lélément audio.
                    </audio>
                    <button
                      type="button"
                      className="input-action-btn"
                      title="Annuler l'audio"
                      onClick={() => {
                        setAudioBlob(null);
                        audioChunksRef.current = [];
                      }}
                    >
                      <FontAwesomeIcon icon={faTrashAlt} />
                    </button>
                  </div>
                ) : (
                  <input
                    type="text"
                    value={newMessage}
                    onChange={(e) => setNewMessage(e.target.value)}
                    placeholder="Send a message ..."
                    className="message-input"
                  />
                )}
                <input
                  type="file"
                  ref={fileInputRef}
                  style={{ display: "none" }}
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
                          onClick={() => handleUploadFile("IMAGE")}
                        >
                          Upload Image
                        </button>
                        <button
                          className="menu-item"
                          onClick={() => handleUploadFile("FILE")}
                        >
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
                    className={`input-action-btn ${
                      isRecording ? "recording" : ""
                    }`}
                    title={
                      isRecording
                        ? "Enregistrement en cours"
                        : "Enregistrer un audio"
                    }
                    onClick={isRecording ? stopRecording : startRecording}
                  >
                    <FontAwesomeIcon icon={faMicrophone} />
                  </button>
                  <button
                    type="submit"
                    className={`send-btn ${
                      newMessage.trim() || audioBlob ? "" : "disabled"
                    }`}
                    disabled={!newMessage.trim() && !audioBlob}
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