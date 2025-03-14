"use client";
import axios from "axios";
import { useAuth } from "../context/AuthContext";

const axiosInstance = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_AUTHENTIFICATON_SERVICE_URL,
  withCredentials: true,
});

axiosInstance.interceptors.request.use(
  async (config) => {
    const { accessToken, isLoading } = useAuth();

    if (isLoading) {
      console.log("⏳ Attente du rafraîchissement du token...");
      await new Promise((resolve) => setTimeout(resolve, 500));
    }

    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default axiosInstance;
