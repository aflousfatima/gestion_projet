/* eslint-disable */
"use client";
import React, { useEffect, useState, useRef } from "react";
import { useParams } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import useAxios from "@/hooks/useAxios";
import { COLLABORATION_SERVICE_URL, AUTH_SERVICE_URL } from "@/config/useApi";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faMicrophone,
  faMicrophoneSlash,
  faVideo,
  faPhoneSlash,
  faShareSquare,
  faComment,
  faVolumeMute,
  faVolumeUp,
  faUserPlus,
} from "@fortawesome/free-solid-svg-icons";
import "../../../../../../../styles/Collaboration-Vocal-Channel.css";

interface Channel {
  id: string;
  name: string;
  type: "VOICE";
  isPrivate: boolean;
  members: string[];
}

interface Call {
  id: string;
  channelId: string;
  channelName: string;
  initiatorId: string;
  status: "INITIATED" | "ACTIVE" | "ENDED" | "FAILED";
  type: "VOICE" | "VIDEO" | "SCREEN";
  startedAt: string;
  endedAt?: string;
  participantIds: string[];
}

interface SignalingMessage {
  userId: string;
  sdp?: { sdp: string; type: string };
  candidate?: any;
  type: "offer" | "answer" | "ice-candidate";
}

interface ChatMessage {
  userId: string;
  username: string;
  content: string;
  timestamp: string;
}

interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  avatar?: string;
}

const VoiceChannelRoute: React.FC = () => {
  const { channelId } = useParams();
  const { accessToken } = useAuth();
  const axiosInstance = useAxios();
  const [channel, setChannel] = useState<Channel | null>(null);
  const [call, setCall] = useState<Call | null>(null);
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [localStream, setLocalStream] = useState<MediaStream | null>(null);
  const [remoteStreams, setRemoteStreams] = useState<Map<string, MediaStream>>(
    new Map()
  );
  const [users, setUsers] = useState<Map<string, User>>(new Map());
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [chatInput, setChatInput] = useState("");
  const [isChatOpen, setIsChatOpen] = useState(false);
  const [muted, setMuted] = useState(false);
  const [deafened, setDeafened] = useState(false);
  const [videoError, setVideoError] = useState<string | null>(null);
  const [callDuration, setCallDuration] = useState(0);
  const [previousCallType, setPreviousCallType] = useState<
    "VOICE" | "VIDEO" | "SCREEN" | null
  >(null);
  const localVideoRef = useRef<HTMLVideoElement>(null);
  const screenShareRef = useRef<HTMLVideoElement>(null);
  const remoteVideoRefs = useRef<Map<string, HTMLVideoElement>>(new Map());
  const peerConnections = useRef<Map<string, RTCPeerConnection>>(new Map());
  const stompClientRef = useRef<Client | null>(null);
  const chatContainerRef = useRef<HTMLDivElement>(null);
  const chatPanelRef = useRef<HTMLDivElement>(null);

  const configuration = {
    iceServers: [{ urls: "stun:stun.l.google.com:19302" }],
  };

  // Chronomètre
  useEffect(() => {
    let timer: NodeJS.Timeout;
    if (call) {
      timer = setInterval(() => {
        setCallDuration((prev) => prev + 1);
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [call]);

  const formatDuration = (seconds: number) => {
    const h = Math.floor(seconds / 3600)
      .toString()
      .padStart(2, "0");
    const m = Math.floor((seconds % 3600) / 60)
      .toString()
      .padStart(2, "0");
    const s = (seconds % 60).toString().padStart(2, "0");
    return `${h}:${m}:${s}`;
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
            avatar: response.data.avatar,
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

  // Fetch channel and participants
  useEffect(() => {
    const fetchChannel = async () => {
      if (!accessToken || !channelId) {
        console.warn(
          "Skipping fetchChannel: accessToken or channelId is missing",
          {
            accessToken: !!accessToken,
            channelId,
          }
        );
        setError("Identifiant du canal ou jeton d'accès manquant");
        return;
      }

      try {
        console.log("Fetching channel with ID:", channelId);
        const channelResponse = await axiosInstance.get(
          `${COLLABORATION_SERVICE_URL}/api/channels/${channelId}`,
          { headers: { Authorization: `Bearer ${accessToken}` } }
        );
        console.log("Channel response:", channelResponse.data);
        setChannel({
          id: channelResponse.data.id.toString(),
          name: channelResponse.data.name,
          type: channelResponse.data.type,
          isPrivate: channelResponse.data.isPrivate,
          members: channelResponse.data.participantIds || [],
        });

        console.log("Fetching participants for channel:", channelId);
        const participantsResponse = await axiosInstance.get(
          `${COLLABORATION_SERVICE_URL}/api/channels/${channelId}/participants`,
          { headers: { Authorization: `Bearer ${accessToken}` } }
        );
        console.log("Participants response:", participantsResponse.data);

        const userMap = new Map<string, User>();
        participantsResponse.data.forEach((user: any) => {
          userMap.set(user.id, {
            id: user.id.toString(),
            firstName: user.firstName,
            lastName: user.lastName,
            email: user.email,
            avatar: user.avatar,
          });
        });
        setUsers(userMap);
      } catch (err: any) {
        console.error("Error fetching channel or participants:", err);
        const errorMessage =
          err.response?.data?.message ||
          "Erreur lors de la récupération du canal ou des participants";
        setError(
          `${errorMessage} (Code: ${err.response?.status || "inconnu"})`
        );
      }
    };
    fetchChannel();
  }, [accessToken, axiosInstance, channelId]);
  // WebSocket setup
  useEffect(() => {
    if (!accessToken || !channelId || !currentUser) return;

    const socket = new SockJS(`${COLLABORATION_SERVICE_URL}/ws`);
    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      debug: (str) => console.log(str),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.onConnect = () => {
      console.log("Connecté au WebSocket");
      client.subscribe(`/topic/calls.${channelId}`, (message) => {
        const callData: Call = JSON.parse(message.body);
        console.log("Call data received:", callData);
        setCall(callData);
      });
      client.subscribe(`/topic/signaling.${call?.id}`, (message) => {
        const signaling: SignalingMessage = JSON.parse(message.body);
        handleSignalingMessage(signaling);
      });
      client.subscribe(`/topic/chat.${channelId}`, (message) => {
        const chatMessage: ChatMessage = JSON.parse(message.body);
        setChatMessages((prev) => [...prev, chatMessage]);
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
      }
    };
  }, [accessToken, channelId, currentUser, call?.id]);

  // Sync local video stream with retry
  useEffect(() => {
    if (localVideoRef.current && localStream && call?.type === "VIDEO") {
      console.log("Assigning localStream to video element:", localStream);
      localVideoRef.current.srcObject = localStream;
      console.log(
        "localVideoRef srcObject set:",
        localVideoRef.current.srcObject
      );
      const attemptPlay = async (retries = 3, delay = 500) => {
        for (let i = 0; i < retries; i++) {
          try {
            await localVideoRef.current!.play();
            console.log("Local video playing successfully");
            setVideoError(null);
            return;
          } catch (err) {
            console.error(`Attempt ${i + 1} failed to play local video:`, err);
            if (i < retries - 1) {
              await new Promise((resolve) => setTimeout(resolve, delay));
            }
          }
        }
        setVideoError(
          "Impossible de lire la vidéo locale. Vérifiez votre caméra et les permissions."
        );
      };
      attemptPlay();
    }
  }, [localStream, call]);

  // Sync screen share stream
  useEffect(() => {
    if (screenShareRef.current && localStream && call?.type === "SCREEN") {
      screenShareRef.current.srcObject = localStream;
      const attemptPlay = async (retries = 3, delay = 500) => {
        for (let i = 0; i < retries; i++) {
          try {
            await screenShareRef.current!.play();
            console.log("Screen share playing successfully");
            setVideoError(null);
            return;
          } catch (err) {
            console.error(`Attempt ${i + 1} failed to play screen share:`, err);
            if (i < retries - 1) {
              await new Promise((resolve) => setTimeout(resolve, delay));
            }
          }
        }
        setVideoError("Impossible de lire le partage d'écran.");
      };
      attemptPlay();
    }
  }, [localStream, call]);

  // Auto-scroll chat
  useEffect(() => {
    if (chatContainerRef.current) {
      chatContainerRef.current.scrollTop =
        chatContainerRef.current.scrollHeight;
    }
  }, [chatMessages]);

  // Fermer le panneau de chat en cliquant dehors
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        chatPanelRef.current &&
        !chatPanelRef.current.contains(event.target as Node) &&
        isChatOpen
      ) {
        setIsChatOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isChatOpen]);

  const initiateCall = async (callType: "VOICE" | "VIDEO" | "SCREEN") => {
    try {
      console.log("Initiating call with type:", callType);
      let stream: MediaStream;
      if (callType === "SCREEN") {
        stream = await navigator.mediaDevices.getDisplayMedia({
          video: true,
          audio: true,
        });
      } else {
        stream = await navigator.mediaDevices.getUserMedia({
          audio: true,
          video: callType === "VIDEO" ? { facingMode: "user" } : false,
        });
      }
      console.log(
        "Local stream acquired:",
        stream,
        "Video tracks:",
        stream.getVideoTracks()
      );
      if (localStream) {
        localStream.getTracks().forEach((track) => track.stop());
      }
      setLocalStream(stream);
      setMuted(false);
      setDeafened(false);
      setPreviousCallType(callType);
      const response = await axiosInstance.post(
        `${COLLABORATION_SERVICE_URL}/api/channels/${channelId}/calls/initiate`,
        callType,
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
            "Content-Type": "application/json",
          },
        }
      );
      console.log("Call initiated, type:", response.data.type);
      setCall(response.data);

      channel?.members.forEach((participantId) => {
        if (participantId !== currentUser?.id) {
          createPeerConnection(participantId);
        }
      });
    } catch (err: any) {
      console.error("Error initiating call:", err);
      setError(
        err.response?.data?.message ||
          "Erreur lors de l'initiation de l'appel. Vérifiez les permissions de la caméra."
      );
    }
  };

  const endCall = async (callId: string) => {
    try {
      await axiosInstance.post(
        `${COLLABORATION_SERVICE_URL}/api/channels/${channelId}/calls/${callId}/end`,
        {},
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );
      setCall(null);
      setCallDuration(0);
      if (localStream) {
        localStream.getTracks().forEach((track) => track.stop());
        setLocalStream(null);
      }
      peerConnections.current.forEach((pc) => pc.close());
      peerConnections.current.clear();
      setRemoteStreams(new Map());
      setMuted(false);
      setDeafened(false);
      setVideoError(null);
      // Reinitialize camera if previous call was VIDEO
      if (previousCallType === "VIDEO") {
        const stream = await navigator.mediaDevices.getUserMedia({
          audio: true,
          video: { facingMode: "user" },
        });
        setLocalStream(stream);
      }
    } catch (err: any) {
      setError(
        err.response?.data?.message || "Erreur lors de la fin de l'appel"
      );
    }
  };

  const inviteFriend = async () => {
    console.log("Invitation d'un ami");
    setError("Fonctionnalité d'invitation non implémentée.");
  };

  const createPeerConnection = (participantId: string) => {
    const peerConnection = new RTCPeerConnection(configuration);
    peerConnections.current.set(participantId, peerConnection);

    peerConnection.oniceconnectionstatechange = () => {
      console.log(
        `ICE connection state for ${participantId}:`,
        peerConnection.iceConnectionState
      );
    };
    peerConnection.onconnectionstatechange = () => {
      console.log(
        `Connection state for ${participantId}:`,
        peerConnection.connectionState
      );
    };

    localStream?.getTracks().forEach((track) => {
      peerConnection.addTrack(track, localStream);
    });

    peerConnection.ontrack = (event) => {
      const stream = event.streams[0];
      console.log("Remote stream received for", participantId, ":", stream);
      setRemoteStreams((prev) => new Map(prev).set(participantId, stream));
    };

    peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        stompClientRef.current?.publish({
          destination: `/app/signaling.${call?.id}`,
          body: JSON.stringify({
            userId: currentUser?.id,
            candidate: event.candidate,
            type: "ice-candidate",
          }),
          headers: { Authorization: `Bearer ${accessToken}` },
        });
      }
    };

    peerConnection
      .createOffer()
      .then((offer) => {
        peerConnection.setLocalDescription(offer);
        stompClientRef.current?.publish({
          destination: `/app/signaling.${call?.id}`,
          body: JSON.stringify({
            userId: currentUser?.id,
            sdp: { sdp: offer.sdp, type: offer.type },
            type: "offer",
          }),
          headers: { Authorization: `Bearer ${accessToken}` },
        });
      })
      .catch((err) => {
        console.error("Error creating offer:", err);
        setError("Erreur lors de la création de l'offre WebRTC");
      });
  };

  const handleSignalingMessage = async (message: SignalingMessage) => {
    if (message.userId === currentUser?.id) return;

    const peerConnection = peerConnections.current.get(message.userId);
    if (!peerConnection) return;

    try {
      if (message.type === "offer" ) {
        if (message.sdp) {
          const descriptionInit: RTCSessionDescriptionInit = {
            sdp: message.sdp.sdp,
            type: message.sdp.type as RTCSdpType, // 👈 cast ici
          };

          const remoteDescription = new RTCSessionDescription(descriptionInit);
          await peerConnection.setRemoteDescription(remoteDescription);
        }
        
        
        const answer = await peerConnection.createAnswer();
        await peerConnection.setLocalDescription(answer);
        stompClientRef.current?.publish({
          destination: `/app/signaling.${call?.id}`,
          body: JSON.stringify({
            userId: currentUser?.id,
            sdp: { sdp: answer.sdp, type: answer.type },
            type: "answer",
          }),
          headers: { Authorization: `Bearer ${accessToken}` },
        });
      } else if (message.type === "answer") {
        if (message.sdp) {
          const descriptionInit: RTCSessionDescriptionInit = {
            sdp: message.sdp.sdp,
            type: message.sdp.type as RTCSdpType, // 👈 cast ici
          };

          const remoteDescription = new RTCSessionDescription(descriptionInit);
          await peerConnection.setRemoteDescription(remoteDescription);
        }
        
      } else if (message.type === "ice-candidate") {
        await peerConnection.addIceCandidate(
          new RTCIceCandidate(message.candidate)
        );
      }
    } catch (err) {
      console.error("Error handling signaling message:", err);
      setError("Erreur lors du traitement du message de signalisation");
    }
  };

  const toggleMute = () => {
    if (localStream) {
      localStream.getAudioTracks().forEach((track) => {
        track.enabled = !track.enabled;
      });
      setMuted(!muted);
    }
  };

  const toggleDeafen = () => {
    if (localStream) {
      localStream.getAudioTracks().forEach((track) => {
        track.enabled = !deafened;
      });
      setDeafened(!deafened);
    }
  };

  const sendChatMessage = () => {
    if (chatInput.trim() && stompClientRef.current && currentUser) {
      const message: ChatMessage = {
        userId: currentUser.id,
        username: `${currentUser.firstName} ${currentUser.lastName}`,
        content: chatInput,
        timestamp: new Date().toISOString(),
      };
      stompClientRef.current.publish({
        destination: `/app/chat.${channelId}`,
        body: JSON.stringify(message),
        headers: { Authorization: `Bearer ${accessToken}` },
      });
      setChatInput("");
    }
  };

  if (!channel || !currentUser) {
    return (
      <div className="channel-container">
        {error && <div className="error-message">{error}</div>}
        <div className="loading">Chargement...</div>
      </div>
    );
  }

  if (!call) {
    return (
      <div className="channel-container">
        {error && <div className="error-message">{error}</div>}
        <div className="landing-page">
          <div className="landing-content">
            <h2>Let’s Get Things Moving</h2>
            <p>
              Start the conversation, share ideas, and move your project forward
              — together, in real time.
            </p>

            <div className="call-options">
              <button
                onClick={() => initiateCall("VOICE")}
                className="call-option-btn voice"
              >
                <FontAwesomeIcon icon={faMicrophone} />
                voice call
              </button>
              <button
                onClick={() => initiateCall("VIDEO")}
                className="call-option-btn video"
              >
                <FontAwesomeIcon icon={faVideo} /> Video call
              </button>
            </div>
          </div>
          <div className="landing-image-container">
            <img
              src="/vocal-img-3.png"
              alt="Voice Channel"
              className="landing-image"
            />
          </div>
        </div>
      </div>
    );
  }

  const participantCount = call.participantIds.length;

  return (
    <div className="channel-container">
      {error && <div className="error-message">{error}</div>}
      {videoError && <div className="error-message">{videoError}</div>}
      <div className="call-interface">
        <div className="header">
          <h4>
            #{channel.name} - {formatDuration(callDuration)}
          </h4>
          <button
            onClick={() => setIsChatOpen(!isChatOpen)}
            className={`chat-toggle ${isChatOpen ? "active" : ""}`}
          >
            <FontAwesomeIcon icon={faComment} />
          </button>
        </div>
        <div className={`participant-grid grid-${participantCount}`}>
          <div className="participant-card" key={currentUser.id}>
            <div className="participant-video">
              {call.type === "VIDEO" && (
                <>
                  {!localStream && (
                    <div className="video-placeholder">
                      Chargement de la caméra...
                    </div>
                  )}
                  {videoError && (
                    <div className="video-placeholder">{videoError}</div>
                  )}
                  <video
                    ref={localVideoRef}
                    autoPlay
                    muted
                    playsInline
                    className="participant-video"
                    style={{
                      display: "block",
                    }}
                  />
                </>
              )}
              {call.type === "VOICE" && (
                <img
                  src={`https://ui-avatars.com/api/?name=${encodeURIComponent(
                    currentUser.firstName && currentUser.lastName
                      ? `${currentUser.firstName}+${currentUser.lastName}`
                      : currentUser.id
                  )}&background=808080&color=fff&size=80`}
                  alt={`${currentUser.firstName} ${currentUser.lastName}`}
                  className="participant-avatar"
                />
              )}
            </div>
            <div className="participant-info">
              <span>
                {currentUser.firstName} {currentUser.lastName}
              </span>
              <div className="participant-controls">
                <button onClick={toggleMute} className={muted ? "muted" : ""}>
                  <FontAwesomeIcon
                    icon={muted ? faMicrophoneSlash : faMicrophone}
                  />
                </button>
                <button
                  onClick={toggleDeafen}
                  className={deafened ? "deafened" : ""}
                >
                  <FontAwesomeIcon
                    icon={deafened ? faVolumeMute : faVolumeUp}
                  />
                </button>
              </div>
            </div>
          </div>
          {call.type === "SCREEN" && (
            <div
              className="participant-card screen-share-card"
              key="screen-share"
            >
              <div className="participant-video">
                <video
                  ref={screenShareRef}
                  autoPlay
                  playsInline
                  className="participant-video"
                />
              </div>
              <div className="participant-info">
                <span>
                  Share Screen - {currentUser.firstName} {currentUser.lastName}
                </span>
              </div>
            </div>
          )}
          {call.participantIds
            .filter((id) => id !== currentUser.id && users.has(id.toString()))
            .map((participantId) => (
              <div key={participantId} className="participant-card">
                <div className="participant-video">
                  <video
                    ref={(el) => {
                      if (el) {
                        remoteVideoRefs.current.set(participantId, el);
                        const stream = remoteStreams.get(participantId);
                        if (stream) {
                          el.srcObject = stream;
                          el.play().catch((err) => {
                            console.error("Error playing remote video:", err);
                            setError(
                              "Erreur lors de la lecture de la vidéo distante"
                            );
                          });
                        }
                      }
                    }}
                    autoPlay
                    playsInline
                    className={call.type === "VOICE" ? "hidden" : ""}
                  />
                  {call.type === "VOICE" && (
                    <img
                      src={`https://ui-avatars.com/api/?name=${encodeURIComponent(
                        users.get(participantId)?.firstName &&
                          users.get(participantId)?.lastName
                          ? `${users.get(participantId)?.firstName}+${
                              users.get(participantId)?.lastName
                            }`
                          : participantId
                      )}&background=808080&color=fff&size=80`}
                      alt={`${users.get(participantId)?.firstName} ${
                        users.get(participantId)?.lastName
                      }`}
                      className="participant-avatar"
                    />
                  )}
                </div>
                <div className="participant-info">
                  <span>
                    {users.get(participantId)?.firstName}{" "}
                    {users.get(participantId)?.lastName ||
                      "Utilisateur inconnu"}
                  </span>
                  <div className="participant-controls"></div>
                </div>
              </div>
            ))}
        </div>
        <div className="control-bar">
          <button
            onClick={() => endCall(call.id)}
            className="control-btn leave-call"
          >
            <FontAwesomeIcon icon={faPhoneSlash} /> Leave
          </button>
          <button
            onClick={() => initiateCall("SCREEN")}
            className="control-btn screen-share"
          >
            <FontAwesomeIcon icon={faShareSquare} /> Share Screen
          </button>
          <button onClick={inviteFriend} className="control-btn invite-friend">
            <FontAwesomeIcon icon={faUserPlus} /> Invite friend
          </button>
        </div>
        <div
          className={`chat-panel ${isChatOpen ? "open" : ""}`}
          ref={chatPanelRef}
        >
          <div className="chat-header">
            <h4>Chat Channel</h4>
            <p>
              {channel.name} - {channel.isPrivate ? "Privé" : "Public"}
            </p>
          </div>
          <div className="chat-messages" ref={chatContainerRef}>
            {chatMessages.map((msg, index) => (
              <div key={index} className="chat-message">
                <div className="chat-message-header">
                  <span className="chat-username">{msg.username}</span>
                  <span className="chat-timestamp">
                    {new Date(msg.timestamp).toLocaleTimeString()}
                  </span>
                </div>
                <p>{msg.content}</p>
              </div>
            ))}
          </div>
          <div className="chat-input">
            <input
              type="text"
              value={chatInput}
              onChange={(e) => setChatInput(e.target.value)}
              onKeyPress={(e) => e.key === "Enter" && sendChatMessage()}
              placeholder="Send a Message..."
            />
            <button onClick={sendChatMessage}>Envoyer</button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default VoiceChannelRoute;
