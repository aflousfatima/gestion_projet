/* eslint-disable */
"use client";
import "../../../styles/Signup.css";
import { useState, ChangeEvent, FormEvent, useEffect } from "react";
import axios from "axios";
import Link from "next/link";
import { useRouter } from "next/navigation";

interface FormData {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  password: string;
  marketingConsent: boolean;
  termsAccepted: boolean;
  token?: string;
}

interface InvitationData {
  email: string;
  role: string;
  entrepriseId: string;
  project: string;
}

export default function SignupPage() {
  const router = useRouter();
  const [formData, setFormData] = useState<FormData>({
    firstName: "",
    lastName: "",
    username: "",
    email: "",
    password: "",
    marketingConsent: false,
    termsAccepted: false,
    token: undefined,
  });
  const [invitationData, setInvitationData] = useState<InvitationData | null>(
    null
  );
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    // Get token from query parameters client-side
    const searchParams = new URLSearchParams(window.location.search);
    const token = searchParams.get("token");
    setFormData((prevData) => ({
      ...prevData,
      token: token || undefined,
    }));

    const verifyToken = async () => {
      if (token) {
        try {
          const response = await axios.get(
            `${process.env.NEXT_PUBLIC_API_AUTHENTIFICATION_SERVICE_URL}/api/invitations/verify?token=${token}`
          );
          const invitation = response.data;
          setInvitationData({
            email: invitation.email,
            role: invitation.role,
            entrepriseId: invitation.entrepriseId,
            project: invitation.project,
          });
          setFormData((prevData) => ({
            ...prevData,
            email: invitation.email,
          }));
        } catch (err: any) {
          console.error("Token verification error:", err);
          setError(err.response?.data?.message ?? null);
        }
      }
    };
    verifyToken();
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
    try {
      console.log("Signup data submitted:", formData);
      const response = await axios.post(
        `${process.env.NEXT_PUBLIC_API_AUTHENTIFICATION_SERVICE_URL}/api/signup`,
        formData
      );
      console.log("Server response:", response);
      if (response.status === 201) {
        setSuccessMessage(
          "Registration successful! Please check your email to confirm."
        );
        setTimeout(() => {
          router.replace("/authentification/signin");
        }, 3000);
      } else {
        setError(response.data.message);
      }
    } catch (err: any) {
      console.error("Signup error:", err);
      setError(err.response?.data?.message ?? null);
    }
  };

  if (error) {
    return (
      <div className="feedback-container">
        <div className="feedback-box">
          <h3 className="feedback-title error-title">Error</h3>
          <p className="feedback-message error-message">{error}</p>
          <Link href="/authentification/signup" className="feedback-link">
            Back to Signup
          </Link>
        </div>
      </div>
    );
  }

  if (successMessage) {
    return (
      <div className="feedback-container">
        <div className="feedback-box">
          <h3 className="feedback-title success-title">Success</h3>
          <p className="success-message">{successMessage}</p>
          <p className="feedback-info">Redirecting to sign in...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="container1">
      <div className="form-box1">
        <h3 className="title1up">Get started with AGILIA</h3>
        <h2 className="title2up">
          Free for up to 10 users - no credit card needed.
        </h2>
        <div className="signup-sections">
          <div className="social-signup">
            <a href="#" className="social-icon" title="Continue with Google">
              <img src="/google.png" alt="Google" className="social-img" />
            </a>
            <a href="#" className="social-icon" title="Continue with Facebook">
              <img src="/facebook.png" alt="Facebook" className="social-img" />
            </a>
            <a href="#" className="social-icon" title="Continue with Microsoft">
              <img
                src="/microsoft.png"
                alt="Microsoft"
                className="social-img"
              />
            </a>
            <a href="#" className="social-icon" title="Continue with Apple">
              <img src="/apple.png" alt="Apple" className="social-img" />
            </a>
          </div>
          <div className="separator">
            <div className="vertical-line"></div>
            <div className="or-text">OR</div>
            <div className="vertical-line"></div>
          </div>
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
                  placeholder="Username"
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
                  placeholder="Professional email"
                  className="input-field"
                  required
                  readOnly={!!invitationData}
                />
              </div>
              <div className="input-group">
                <i className="fas fa-lock input-icon"></i>
                <input
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="Minimum 8 characters"
                  className="input-field"
                  required
                />
              </div>
              <div className="checkbox-group1">
                <input
                  type="checkbox"
                  id="marketingConsent"
                  name="marketingConsent"
                  checked={formData.marketingConsent}
                  onChange={handleChange}
                />
                <label htmlFor="marketingConsent">
                  I agree to receive marketing emails.
                </label>
              </div>
              <div className="checkbox-group1">
                <input
                  type="checkbox"
                  id="termsAccepted"
                  name="termsAccepted"
                  checked={formData.termsAccepted}
                  onChange={handleChange}
                  required
                />
                <label htmlFor="termsAccepted">
                  I agree to the{" "}
                  <a href="#" className="law-custom">
                    terms of use
                  </a>{" "}
                  and the{" "}
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
                Already have an account?{" "}
                <Link
                  className="nav-link active low-custom1"
                  aria-current="page"
                  href="/authentification/signin"
                >
                  Sign In
                </Link>
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
