"use client";
import "../../../styles/Signin.css";
import { useState, ChangeEvent, FormEvent, useEffect } from "react";
import axios from "axios";
import Link from "next/link";
import { useRouter } from "next/navigation"; // Utilisez `next/navigation` au lieu de `next/router`
import { useAuth } from "../../../context/AuthContext"; // Importer le contexte d'authentification

interface FormData {
  email: string;
  password: string;
}

export default function SigninPage() {
  const [formData, setFormData] = useState<FormData>({
    email: "",
    password: "",
  });
  const router = useRouter(); // Initialisation de useRouter pour la redirection
  const { login } = useAuth(); // Utilisation du contexte Auth pour accéder à la fonction login

  useEffect(() => {
    router.prefetch("/company-registration"); // Précharge la page en cache
  }, []);
  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value, type, checked } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    try {
      // Log des données envoyées pour l'inscription
      console.log("Données envoyées pour l'inscription:", formData);
      console.log(process.env.NEXT_PUBLIC_API_AUTHENTIFICATON_SERVICE_URL);

      // Envoi de la requête POST au backend Spring Boot
      const response = await axios.post(
        `${process.env.NEXT_PUBLIC_API_AUTHENTIFICATON_SERVICE_URL}/api/login`,
        formData,
        { withCredentials: true }
      );

      // Log de la réponse du serveur
      console.log("Réponse du serveur:", response);

      if (response.status === 200) {
        alert("Login réussie!");
        // Récupération du access_token dans la réponse et stockage dans le contexte
        const accessToken = response.data.access_token;
        console.log("Access Token récupéré:", accessToken);
        login(accessToken); // Appeler la fonction `login` du contexte pour stocker le token

        // Redirection vers la page de création d'entreprise après une connexion réussie
        router.replace("/company-registration"); // Remplacez "/entreprise/creation" par l'URL de votre page entreprise
      } else {
        alert(`Erreur : ${response.data.message}`);
      }
    } catch (error) {
      console.error("Erreur lors de l'inscription :", error);
      alert("Erreur dans l'inscription. Veuillez réessayer.");
    }
  };

  return (
    <div className="container2">
      <div className="form-box">
        <h3 className="title1">Welcome Back to AGILIA</h3>
        <h2 className="title2">Sign in to access your workspace</h2>
        {/* Horizontal Signup Sections */}
        <div className="signin-sections">
          {/* Social Sign Up Section (Left) */}
          {/* Social Sign Up Section */}

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
