import React from "react";
import "../../../../styles/Dashboard-Home.css";
const Home = () => {
  return (
    <div>
      <div className="header">
        <div className="welcome-message">
          <h1>Hello, Fatima</h1>
          <p>Wednesday 19 March</p>
          <div className="stats">
            <i className="fa fa-check"></i> 0 completed tasks{" "}
            <i className="fa fa-users"></i> 0 collaborators
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
          <button className="create-project-btn">
            <i className="fa fa-plus"></i> Create project
          </button>
        </div>
      </div>
    </div>
  );
};

export default Home;
