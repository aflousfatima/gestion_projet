"use client";
import { createContext, useContext, useState, useEffect, ReactNode } from "react";
import axios from "axios";

interface AuthContextType {
  accessToken: string | null;
  login: (token: string) => void;
  logout: () => Promise<void>;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider = ({ children }: AuthProviderProps) => {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true); // ✅ Éviter les requêtes avant la récupération du token

  // Fonction pour rafraîchir le token
  const refreshAccessToken = async () => {
    try {
      console.log("🔄 Tentative de rafraîchissement du token...");
      const response = await axios.post(
        `${process.env.NEXT_PUBLIC_API_AUTHENTIFICATON_SERVICE_URL}/api/refresh`,
        {},
        { withCredentials: true }
      );

      if (response.data.access_token) {
        console.log("✅ Nouveau token reçu :", response.data.access_token);
        setAccessToken(response.data.access_token);
      } else {
        console.log("⚠️ Impossible de récupérer un nouveau token.");
        setAccessToken(null);
      }
    } catch (error) {
      console.error("❌ Erreur lors du rafraîchissement du token :", error);
      setAccessToken(null);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    refreshAccessToken();
  }, []);

  const login = (token: string) => {
    console.log("✅ Connexion réussie, token reçu :", token);
    setAccessToken(token);
  };

  const logout = async () => {
    console.log("🚪 Déconnexion en cours...");
    setAccessToken(null);
    await axios.post(
      `${process.env.NEXT_PUBLIC_API_AUTHENTIFICATON_SERVICE_URL}/api/logout`,
      {},
      { withCredentials: true }
    );
    console.log("🔓 Déconnecté, token supprimé.");
  };

  return (
    <AuthContext.Provider value={{ accessToken, login, logout, isLoading }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};
