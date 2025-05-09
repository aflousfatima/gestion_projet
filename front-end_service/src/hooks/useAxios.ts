import { useEffect } from "react";
import axiosInstance from "../config/axiosInstance";
import { useAuth } from "../context/AuthContext";

const useAxios = () => {
  const { accessToken } = useAuth();

  useEffect(() => {
    
    // Ajouter un intercepteur pour insérer le token dans chaque requête
    const requestInterceptor = axiosInstance.interceptors.request.use(
      (config) => {
        console.log("🔍 Requête interceptée : ", config.url);
        
        if (accessToken) {
          console.log("✅ Token trouvé :", accessToken);
          config.headers.Authorization = `Bearer ${accessToken}`;
        } else {
          console.warn("⚠️ Aucun token trouvé !");
        }

        return config;
      },
      (error) => {
        console.error("❌ Erreur lors de la modification de la requête :", error);
        return Promise.reject(error);
      }
    );

    return () => {
      axiosInstance.interceptors.request.eject(requestInterceptor);
    };
  }, [accessToken]); // ✅ Mise à jour si `accessToken` ou `baseURL` change

  return axiosInstance;
};

export default useAxios;
