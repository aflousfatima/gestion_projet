// pages/index.js
import Head from "next/head";
import "../../../styles/ManagerDashboard.css"; // Import du CSS

export default function Home() {
  return (
    <div className="container">
      <Head>
        <title>Tableau de bord des vols</title>
        <meta
          name="description"
          content="Tableau de bord pour la gestion des vols"
        />
        <link rel="icon" href="/favicon.ico" />
      </Head>

      <div className="sidebar">
        <div className="user-info">
          <img src="/user-placeholder.png" alt="User" className="user-img" />
          <h2>Alex Johnson</h2>
          <p>alex.johnson@gmail.com</p>
        </div>
        <nav>
          <ul>
            <li>Dashboard</li>
            <li>Flights</li>
            <li>Wallet</li>
            <li>Reports</li>
            <li>Statistics</li>
            <li>Settings</li>
          </ul>
        </nav>
        <div className="active-users">
          <h3>Active Users</h3>
          <p>+70</p>
        </div>
      </div>

      <div className="main-content">
        <div className="top-cards">
          <div className="card">
            <h3>Boeing 787</h3>
            <p>$548</p>
            <img src="/boeing.png" alt="Boeing" className="plane-img" />
          </div>
          <div className="card">
            <h3>Airbus 811</h3>
            <p>$620</p>
            <img src="/airbus.png" alt="Airbus" className="plane-img" />
          </div>
          <div className="card">
            <h3>Total Flights</h3>
            <p>850</p>
          </div>
        </div>

        <div className="last-trips">
          <h3>Last Trips</h3>
          <div className="trip">
            <p>John Doe</p>
            <p>Qatar</p>
            <p>5</p>
            <p>$556k</p>
          </div>
          <div className="trip">
            <p>Martin Loiness</p>
            <p>Emirates</p>
            <p>2</p>
            <p>$556k</p>
          </div>
        </div>

        <div className="statistics">
          <h3>Statistics</h3>
          <div className="chart">
            {/* Ici, tu peux intégrer une bibliothèque comme Chart.js pour les graphiques */}
            <p>Graphique des prix des billets (Jan - Jun)</p>
          </div>
        </div>

        <div className="flight-share">
          <h3>Flight Share</h3>
          <div className="donut-chart">
            {/* Ici aussi, Chart.js peut être utilisé pour un graphique en donut */}
            <p>Graphique en donut</p>
          </div>
        </div>

        <div className="flight-schedule">
          <h3>Flight Schedule</h3>
          <div className="line-chart">
            {/* Graphique linéaire pour le calendrier des vols */}
            <p>Graphique linéaire (3500 passagers en pic)</p>
          </div>
        </div>
      </div>
    </div>
  );
}
