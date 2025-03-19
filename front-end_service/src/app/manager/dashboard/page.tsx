// pages/index.js
import "../../../styles/ManagerDashboard.css"; // Import du CSS

export default function ManagerDashboard() {
  return (
    <div className="container-manager">
      {/* Nouvelle sidebar horizontale en haut */}
      <div className="topbar">
        <div className="topbar-icons">
          <i className="fa  fa-list text-icon-list"></i>
          <span className="icon-container">
            <i className="fa fa-plus icon"></i>
            <span className="text-icon-add">add</span>
          </span>
        </div>
        <div className="search-container">
          <i className="fa fa-search  search-icon"></i>
          <input type="text" placeholder="Search" className="search-bar" />
        </div>
        <div className="user-container">
          <i className="fa fa-user"></i>
          <span>fatima aflous</span>
        </div>{" "}
      </div>
      {/* Conteneur pour la sidebar et le contenu principal */}
      <div className="main-wrapper">
        {/* Sidebar verticale existante */}
        <div className="sidebar">
          <nav>
            <ul>
              <li>
                <i className="fa fa-house-user"></i>{" "}
                {/* Icône pour "Accueil" */}
                <span>Home</span>
              </li>
              <li>
                <i className="fa fa-check-circle"></i>{" "}
                {/* Icône pour "Mes tâches" */}
                <span>My Tasks</span>
              </li>
              <li>
                <i className="fa fa-bell"></i>{" "}
                {/* Icône pour "Boîte de réception" */}
                <span>Inbox</span>
                <span className="notification-dot"></span>{" "}
                {/* Point de notification */}
              </li>

              {/* Section "INDICATEURS" */}
              <li className="section-title">
                <span className="section-title-style">Indicators</span>
                <button className="add-btn">+</button>
              </li>
              <li>
                <i className="fa fa-chart-line"></i>{" "}
                {/* Icône pour "Rapports" */}
                <span>Reports</span>
              </li>
              <li>
                <i className="fa fa-folder"></i>{" "}
                {/* Icône pour "Portefeuilles" */}
                <span>Portfolios</span>
              </li>
              <li>
                <i className="fa fa-bullseye"></i>{" "}
                {/* Icône pour "Objectifs" */}
                <span>Goals</span>
              </li>

              {/* Section "PROJETS" */}
              <li className="section-title">
                <span className="section-title-style">Projects</span>
                <button className="add-btn">+</button>
              </li>

              {/* Section "ÉQUIPE" */}
              <li className="section-title">
                <span className="section-title-style">Teams</span>
              </li>
              <li>
                <span className="invite-style">
                  <i className="fa fa-envelope"></i>
                  Invite colleagues
                </span>
              </li>
            </ul>
          </nav>
        </div>
        {/*section milieu de page*/}
        <div className="main-content">
          <div className="header">
            <div className="welcome-message">
              <h1>Hello, Fatima</h1>
              <p>Mardi 4 février</p>
              <div className="stats">
                <i className="fa fa-check"></i>0 completed task
                {""}
                <i className="fa fa-users"></i>0 collaborator
              </div>
            </div>
          </div>
          <div className="content">
            <div className="tasks-section">
              <div className="section-header">
                <h2>My Tasks</h2>
                <i className="section-menu">...</i>
              </div>
              <div className="tabs">
                <span className="tab active">Upcoming</span>
                <span className="tab">Late</span>
                <span className="tab">Completed</span>
              </div>
              <button className="create-task-btn">
                <i className="fa fa-plus"></i> Create Task
              </button>
            </div>

            <div className="projects-section">
              <div className="section-header">
                <h2>Projects</h2>
                <i className="section-menu">...</i>
              </div>
              <button className="create-project-btn">Create project </button>
           
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
