"use client";
import "../../../styles/Signup.css";
import { useState, ChangeEvent, FormEvent, useEffect } from "react";
import axios, { AxiosError } from "axios";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { useRouter } from "next/navigation"; // Utilisez `next/navigation` au lieu de `next/router`

interface FormData {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  password: string;
  marketingConsent: boolean;
  termsAccepted: boolean;
  token?: string; // Ajout du jeton optionnel
}
interface InvitationData {
  email: string;
  role: string;
  entrepriseId: string;
  project: string;
}
export default function SignupPage() {
  const searchParams = useSearchParams();
  const router = useRouter(); // Initialisation de useRouter pour la redirection
  useEffect(() => {
    router.prefetch("/company-registration"); // Précharge la page en cache
  }, []);
  const token = searchParams.get("token"); // Extraire le jeton de l'URL
  const [formData, setFormData] = useState<FormData>({
    firstName: "",
    lastName: "",
    username: "",
    email: "",
    password: "",
    marketingConsent: false,
    termsAccepted: false,
    token: token || undefined, // Stocker le jeton dans formData
  });
  const [invitationData, setInvitationData] = useState<InvitationData | null>(
    null
  );
  const [error, setError] = useState<string | null>(null);
  // Vérifier le jeton au chargement de la page
  useEffect(() => {
    const verifyToken = async () => {
      if (token) {
        try {
          const response = await axios.get(
            `${process.env.NEXT_PUBLIC_API_AUTHENTIFICATON_SERVICE_URL}/api/invitations/verify?token=${token}`
          );
          const invitation = response.data;
          setInvitationData({
            email: invitation.email,
            role: invitation.role,
            entrepriseId: invitation.entrepriseId,
            project: invitation.project,
          });
          // Pré-remplir l'email
          setFormData((prevData) => ({
            ...prevData,
            email: invitation.email,
          }));
        } catch (err) {
          console.error(err); // Log the error for debugging
          setError("Lien d'invitation invalide ou expiré");
        }
      }
    };
    verifyToken();
  }, [token]);

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

      // Envoi de la requête POST au backend Spring Boot
      const response = await axios.post(
        `${process.env.NEXT_PUBLIC_API_AUTHENTIFICATON_SERVICE_URL}/api/signup`,
        formData
      );

      // Log de la réponse du serveur
      console.log("Réponse du serveur:", response);

      if (response.status === 201) {
        alert("Inscription réussie! Vérifiez votre email pour confirmer.");
        router.replace("/authentification/signin"); // Remplacez "/entreprise/creation" par l'URL de votre page entreprise
      } else {
        alert(`Erreur : ${response.data.message}`);
      }
    } catch (err: unknown) {
      // Utiliser "unknown" au lieu de "AxiosError"
      // Vérifier si l'erreur est une instance de AxiosError
      if (err instanceof AxiosError) {
        console.error("❌ Erreur lors de l'inscription :", err);
        if (err.response && err.response.data) {
          setError(err.response.data as string); // Typage explicite du message d'erreur
        } else {
          setError(
            "Une erreur est survenue lors de l'inscription. Veuillez réessayer."
          );
        }
      } else {
        // Gérer les autres types d'erreurs (non-Axios)
        console.error("❌ Erreur inattendue :", err);
        setError("Une erreur inattendue est survenue. Veuillez réessayer.");
      }
    }
  };
  if (error) {
    return (
      <div className="container1">
        <div className="form-box">
          <h3 className="title1">Erreur</h3>
          <p>{error}</p>
          <Link href="/authentification/signin">Retour à la connexion</Link>
        </div>
      </div>
    );
  }
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
                  readOnly={!!invitationData} // Rendre l'email non modifiable si pré-rempli
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

              <div className="checkbox-group1">
                <input
                  type="checkbox"
                  name="marketingConsent"
                  checked={formData.marketingConsent}
                  onChange={handleChange}
                />
                <label>I agree to receive marketing emails.</label>
              </div>

              <div className="checkbox-group1">
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
