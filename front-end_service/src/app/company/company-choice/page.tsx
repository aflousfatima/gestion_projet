"use client";
import { useRouter } from "next/navigation";
import "../../../styles/choose-company.css";

export default function ChooseCompanyPage() {
  const router = useRouter();

  return (
    <div className="choose-company-wrapper">
      <div className="hero-section">
        <h1 className="hero-title">Embark on Your Agile Journey</h1>
        <p className="hero-subtitle">
          Will you lead your own empire or join a thriving team? The choice is yours.
        </p>
      </div>

      <div className="choice-container">
        <div
          className="choice-card create-card"
          onClick={() => router.push("/create-company")}
        >
          <div className="card-content">
            <h2 className="card-title">Create a Company</h2>
            <p className="card-description">
              Build your vision from the ground up and lead with purpose.
            </p>
            <button className="choice-btn create-btn">Start Now</button>
          </div>
        </div>

        <div
          className="choice-card join-card"
          onClick={() => router.push("/join-company")}
        >
          <div className="card-content">
            <h2 className="card-title">Join a Company</h2>
            <p className="card-description">
              Collaborate with innovators and contribute to success.
            </p>
            <button className="choice-btn join-btn">Join Now</button>
          </div>
        </div>
      </div>

      <div className="footer-note">
        <p>Agilia - Where Agile Dreams Take Flight</p>
      </div>
    </div>
  );
}