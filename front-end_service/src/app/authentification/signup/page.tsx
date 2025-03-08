"use client";
import "../../../styles/Signup.css";
import { useState, ChangeEvent, FormEvent } from "react";
import axios from "axios";
import Link from "next/link";
interface FormData {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  password: string;
  marketingConsent: boolean;
  termsAccepted: boolean;
}

export default function SignupPage() {
  const [formData, setFormData] = useState<FormData>({
    firstName: "",
    lastName: "",
    username: "",
    email: "",
    password: "",
    marketingConsent: false,
    termsAccepted: false,
  });

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { name, value, type, checked } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  // Assurez-vous que la fonction handleSubmit est marquée comme async
  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    try {
      // Log des données envoyées pour l'inscription
      console.log("Données envoyées pour l'inscription:", formData);

      // Utilisation d'Axios pour envoyer une requête POST
      const response = await axios.post("/api/auth/register", formData);

      // Log de la réponse du serveur
      console.log("Réponse du serveur:", response);

      if (response.status === 200) {
        alert("Inscription réussie! Vérifiez votre email pour confirmer.");
      } else {
        alert(`Erreur : ${response.data.message}`);
      }
    } catch (error: unknown) {
      console.error("Erreur lors de l'inscription :", error);

      // Log détaillé de l'erreur Axios si c'est une erreur Axios
      if (axios.isAxiosError(error)) {
        console.error("Détails de l'erreur Axios:", error.response?.data);
        console.error("Statut de l'erreur Axios:", error.response?.status);
        console.error("En-têtes de l'erreur Axios:", error.response?.headers);
      } else if (error instanceof Error) {
        console.error("Erreur inconnue:", error.message);
      } else {
        console.error("Erreur non identifiable:", error);
      }

      alert("Erreur dans l'inscription. Veuillez réessayer.");
    }
  };

  return (
    <div className="container1">
      <div className=" form-box">
        <h3 className="title1">Get started with AGILIA</h3>
        <h2 className="title2">
          It’s free for up to 10 users - no credit card needed.
        </h2>
        {/* Horizontal Signup Sections */}
        <div className="signup-sections">
          {/* Social Sign Up Section (Left) */}
          {/* Social Sign Up Section */}
          <div className="social-signup">
            <a href="#" className="social-icon" title="Continuer avec Google">
              <img src="/google.png" alt="Google" className="social-img" />
            </a>
            <a href="#" className="social-icon" title="Continuer avec Facebook">
              <img src="/facebook.png" alt="Facebook" className="social-img" />
            </a>
            <a
              href="#"
              className="social-icon"
              title="Continuer avec Microsoft"
            >
              <img
                src="/microsoft.png"
                alt="Microsoft"
                className="social-img"
              />
            </a>
            <a href="#" className="social-icon" title="Continuer avec Apple">
              <img src="/apple.png" alt="Apple" className="social-img" />
            </a>
          </div>

          <div className="separator">
            <div className="vertical-line"></div> {/* Ligne verticale */}
            <div className="or-text">OR</div>
            <div className="vertical-line"></div> {/* Ligne verticale */}
          </div>

          {/* Personal Info Sign Up Section (Right) */}
          <div className="personal-signup">
            <form onSubmit={handleSubmit}>
              <div className="input-group">
                <i className="fas fa-user input-icon"></i>
                <input
                  type="text"
                  name="firstName"
                  value={formData.firstName}
                  onChange={handleChange}
                  placeholder="First Name"
                  className="input-field custom-input"
                  required
                />
                <i className="fas fa-user input-icon"></i>
                <input
                  type="text"
                  name="lastName"
                  value={formData.lastName}
                  onChange={handleChange}
                  placeholder="Last Name"
                  className="input-field custom-input"
                  required
                />
              </div>

              <div className="input-group">
                <i className="fas fa-user-circle input-icon"></i>
                <input
                  type="text"
                  name="username"
                  value={formData.username}
                  onChange={handleChange}
                  placeholder="User name"
                  className="input-field"
                  required
                />
              </div>

              <div className="input-group">
                <i className="fas fa-envelope input-icon"></i>
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  placeholder="Professionnel e-mail"
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
                  placeholder="minimum 8 caracteres "
                  className="input-field"
                  required
                />
              </div>

              <div className="checkbox-group">
                <input
                  type="checkbox"
                  name="marketingConsent"
                  checked={formData.marketingConsent}
                  onChange={handleChange}
                />
                <label>I agree to receive marketing emails.</label>
              </div>

              <div className="checkbox-group">
                <input
                  type="checkbox"
                  name="termsAccepted"
                  checked={formData.termsAccepted}
                  onChange={handleChange}
                  required
                />
                <label>
                  I agree the {""}
                  <a href="#" className="law-custom">
                    terms of use
                  </a>
                  {""} and the {""}
                  <a href="#" className="law-custom">
                    privacy policy
                  </a>
                  .
                </label>
              </div>

              <button type="submit" className="submit-btn">
                Sign Up
              </button>
              <p className="quest">
                Already have an account ?{" "}
                <Link
                  className="nav-link active low-custom1"
                  aria-current="page"
                  href="/authentification/signin"
                >
                  Sign In
                </Link>{" "}
              </p>
            </form>
          </div>
        </div>
      </div>
      <div className="signup-image-container">
        <img
          src="/signin.png"
          alt="Signup Illustration"
          loading="eager"
          className="signup-image"
        />
      </div>
    </div>
  );
}
