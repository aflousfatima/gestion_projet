"use client";

import React, { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import useAxios from "@/hooks/useAxios";
import { COLLABORATION_SERVICE_URL } from "@/config/useApi";


interface Channel {
  id: string;
  name: string;
  type: "VOICE";
  isPrivate: boolean;
  members: string[];
}

const VoiceChannelRoute: React.FC = () => {
  const { channelId } = useParams();
  const { accessToken } = useAuth();
  const axiosInstance = useAxios();
  const [channel, setChannel] = useState<Channel | null>(null);
  const [error, setError] = useState<string | null>(null);

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

  if (!channel) {
    return (
      <div className="channel-container">
        {error && <div className="error-message">{error}</div>}
        <div className="loading">Chargement...</div>
      </div>
    );
  }
};

export default VoiceChannelRoute;