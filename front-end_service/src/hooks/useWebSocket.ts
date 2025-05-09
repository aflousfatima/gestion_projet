import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { TASK_SERVICE_URL } from "../config/useApi"; // Import TASK_SERVICE_URL

interface Comment {
  id: number;
  content: string;
  author: string;
  workItem:  number;
  createdAt: string;
  updatedAt: string;
}

export const useWebSocket = (taskId: number | undefined, accessToken: string | null) => {
  const [comments, setComments] = useState<Comment[]>([]);
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!taskId || !accessToken) {
      console.warn("Skipping WebSocket: taskId or accessToken missing");
      return;
    }

    const wsUrl = `${TASK_SERVICE_URL}/ws`;
    console.log("Connecting to WebSocket:", wsUrl);

    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.onConnect = () => {
      console.log("WebSocket connected");
      client.subscribe(`/topic/tasks/${taskId}`, (message) => {
        const newComment: Comment = JSON.parse(message.body);
        setComments((prev) => [...prev, newComment]);
      });
    };

    client.onStompError = (frame) => {
      console.error("WebSocket error:", frame);
    };

    client.activate();
    clientRef.current = client;

    return () => {
      console.log("Deactivating WebSocket");
      client.deactivate();
    };
  }, [taskId, accessToken]);

  return { comments, setComments };
};