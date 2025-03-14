"use client";
import { ReactNode, useEffect } from "react";
import { useAuth } from "../context/AuthContext";
import { useRouter } from "next/navigation";
import "../styles/Loading-style.css";
interface ProtectedRouteProps {
  children: ReactNode;
}

const ProtectedRoute = ({ children }: ProtectedRouteProps) => {
  const { accessToken, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && !accessToken) {
      router.push("/authentification/signin");
    }
  }, [accessToken, isLoading, router]);

  if (isLoading) {
    return (
      <div className="loading-container">
        {" "}
        {/* ✅ Applique la classe CSS */}
        <img
          src="/logo.png" // Remplace par ton image
          alt="Chargement..."
          className="loading-icon" // ✅ Animation CSS
        ></img>
      </div>
    );
  }
  return accessToken ? <>{children}</> : null;
};

export default ProtectedRoute;
