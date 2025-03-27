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

  return accessToken ? <>{children}</> : null;
};

export default ProtectedRoute;
