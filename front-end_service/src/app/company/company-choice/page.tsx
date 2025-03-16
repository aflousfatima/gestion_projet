"use client";
import { useRouter } from "next/navigation";
import "../../../styles/choose-company.css";

export default function ChooseCompanyPage() {
  const router = useRouter();

  return (
    <div className="container-choice">
      <h1 className="title">Une nouvelle opportunité vous attend.</h1>
      <p className="subtitle">
      Construisez votre propre entreprise ou rejoignez une équipe ambitieuse.
      </p>

      <div className="options">
        <div
          className="card create"
          onClick={() => router.push("/create-company")}
        >
          <h3>Créer une entreprise</h3>
          <p>Fondez votre propre entreprise et démarrez votre aventure.</p>
          <button className="btn-choice">Commencer</button>
        </div>

        <div className="card join" onClick={() => router.push("/join-company")}>
          <h3>Rejoindre une entreprise</h3>
          <p>
            Rejoignez une entreprise existante et collaborez avec votre équipe.
          </p>
          <button className="btn-choice">Rejoindre</button>
        </div>
      </div>
    </div>
  );
}
