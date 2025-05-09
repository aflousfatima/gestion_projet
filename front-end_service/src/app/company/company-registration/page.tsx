"use client";
import React, { useState } from "react";
import "../../../styles/Company-registration.css";
import useAxios from "../../../hooks/useAxios";
import { PROJECT_SERVICE_URL } from "../../../config/useApi";

const Page = () => {
  const axiosInstance = useAxios();
  const [step, setStep] = useState(1);
  const totalSteps = 4;

  const [formData, setFormData] = useState({
    companyName: "",
    industry: "",
    teamName: "",
    numEmployees: "",
    department: "",
    role: "",
    projectName: "",
    projectDescription: "",
    creationDate: "",
    startDate: "",
    deadline: "",
    status: "",
    phase: "",
    priority: "",
  });

  const [successMessage, setSuccessMessage] = useState("");

  const handleChange = (e) => {
    const { id, value } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [id]: value,
    }));
  };

  const handleNext = () => {
    if (step < totalSteps) setStep(step + 1);
  };

  const handleBack = () => {
    if (step > 1) setStep(step - 1);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await axiosInstance.post(
        `${PROJECT_SERVICE_URL}/api/create-initial-project`,
        formData
      );
      console.log("Success:", response.data);
      setSuccessMessage("Company created successfully!");
    } catch (error) {
      console.error("Error submitting form:", error);
    }
  };

  const renderStep = () => {
    // Same renderStep function as provided
    switch (step) {
      case 1:
        return (
          <div className="step-content">
            <h2 className="step-title">Your Company</h2>
            <div className="form-group">
              <label htmlFor="companyName">Organization Name</label>
              <input
                type="text"
                id="companyName"
                value={formData.companyName}
                onChange={handleChange}
                placeholder="Enter company name"
                required
              />
            </div>
            <div className="form-group">
              <label htmlFor="industry">Industry</label>
              <input
                type="text"
                id="industry"
                value={formData.industry}
                onChange={handleChange}
                placeholder="Enter industry"
                required
              />
            </div>
          </div>
        );
      case 2:
        return (
          <div className="step-content">
            <h2 className="step-title">Your Team</h2>
            <div className="form-group">
              <label htmlFor="teamName">Team Name</label>
              <input
                type="text"
                id="teamName"
                value={formData.teamName}
                onChange={handleChange}
                placeholder="Enter team name"
                required
              />
            </div>
            <div className="form-group">
              <label htmlFor="numEmployees">Number of Employees</label>
              <input
                type="text"
                id="numEmployees"
                value={formData.numEmployees}
                onChange={handleChange}
                placeholder="Enter number of employees"
                required
              />
            </div>
          </div>
        );
      case 3:
        return (
          <div className="step-content">
            <h2 className="step-title">About You</h2>
            <div className="form-group">
              <label htmlFor="department">Department</label>
              <input
                type="text"
                id="department"
                value={formData.department}
                onChange={handleChange}
                placeholder="Your department"
                required
              />
            </div>
            <div className="form-group">
              <label htmlFor="role">Your Role</label>
              <input
                type="text"
                id="role"
                value={formData.role}
                onChange={handleChange}
                placeholder="Your role"
                required
              />
            </div>
          </div>
        );
      case 4:
        return (
          <div className="step-content">
            <h2 className="step-title">Your Project</h2>
            <div className="project-columns-r">
              <div className="column-left-r">
                <div className="form-group-r">
                  <label htmlFor="projectName">Project Name</label>
                  <input
                    type="text"
                    id="projectName"
                    value={formData.projectName}
                    onChange={handleChange}
                    placeholder="Project name"
                    required
                  />
                </div>
                
                <div className="form-group">
                  <label htmlFor="projectDescription">Description</label>
                  <input
                    type="text"
                    id="projectDescription"
                    value={formData.projectDescription}
                    onChange={handleChange}
                    placeholder="Project description"
                    required
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="startDate">Start Date</label>
                  <input
                    type="date"
                    id="startDate"
                    value={formData.startDate}
                    onChange={handleChange}
                    required
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="deadline">Deadline</label>
                  <input
                    type="date"
                    id="deadline"
                    value={formData.deadline}
                    onChange={handleChange}
                    required
                  />
                </div>
              </div>
              <div className="column-right">
                <div className="form-group">
                  <label htmlFor="status">Status</label>
                  <select
                    id="status"
                    value={formData.status}
                    onChange={handleChange}
                    required
                  >
                    <option value="" disabled>
                      Select status
                    </option>
                    <option value="START">Start</option>
                    <option value="IN_PROGRESS">In Progress</option>
                    <option value="IN_PAUSE">In Pause</option>
                    <option value="DONE">Done</option>
                    <option value="CANCEL">Cancel</option>
                    <option value="ARCHIVE">Archive</option>
                  </select>
                </div>
                <div className="form-group">
                  <label htmlFor="phase">Phase</label>
                  <select
                    id="phase"
                    value={formData.phase}
                    onChange={handleChange}
                    required
                  >
                    <option value="" disabled>
                      Select phase
                    </option>
                    <option value="PLANIFICATION">Planification</option>
                    <option value="DESIGN">Design</option>
                    <option value="DEVELOPPEMENT">Développement</option>
                    <option value="TEST">Test</option>
                    <option value="DEPLOY">Deploy</option>
                    <option value="MAINTENANCE">Maintenance</option>
                    <option value="CLOSE">Close</option>
                  </select>
                </div>
                <div className="form-group-r">
                  <label htmlFor="priority">Priority</label>
                  <select
                    id="priority"
                    value={formData.priority}
                    onChange={handleChange}
                    required
                  >
                    <option value="" disabled>
                      Select priority
                    </option>
                    <option value="LOW">Low</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High</option>
                    <option value="CRITICAL">Critical</option>
                  </select>
                </div>
              </div>
            </div>
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div className="stepper-container-r">
      <header className="header-r">
        <h1>Welcome to Your Setup</h1>
        <p>Complete the {totalSteps} steps to get started</p>
      </header>

      <div className="main-content-r">
        <div className="sidebar-r">
          <ul className="step-list-r">
            <li className={step === 1 ? "active" : ""}>
              <span className="step-icon">
                <i className="fa fa-building"></i>
              </span>
              <span className="step-label">Your Company</span>
            </li>
            <li className={step === 2 ? "active" : ""}>
              <span className="step-icon">
                <i className="fa fa-users"></i>
              </span>
              <span className="step-label">Your Team</span>
            </li>
            <li className={step === 3 ? "active" : ""}>
              <span className="step-icon">
                <i className="fa fa-user"></i>
              </span>
              <span className="step-label">About You</span>
            </li>
            <li className={step === 4 ? "active" : ""}>
              <span className="step-icon">
                <i className="fa fa-list"></i>
              </span>
              <span className="step-label">Your Project</span>
            </li>
          </ul>
        </div>

        <div className="content-r">
          <div className="progress-bar-r">
            <div
              className="progress-r"
              style={{ width: `${(step / totalSteps) * 100}%` }}
            >
              <span className="progress-label-r">{`${Math.round(
                (step / totalSteps) * 100
              )}%`}</span>
              <div className="progress-glow"></div>
            </div>
            <div className="progress-waves"></div>
            <div className="progress-particles">
              <span className="particle particle-1"></span>
              <span className="particle particle-2"></span>
              <span className="particle particle-3"></span>
            </div>
          </div>
          {renderStep()}
          <div className="navigation-r">
            {step > 1 && (
              <button className="nav-btn-r back-btn" onClick={handleBack}>
                <span>← Back</span>
              </button>
            )}
            {step < totalSteps && (
              <button className="nav-btn-r next-btn" onClick={handleNext}>
                <span>Next →</span>
              </button>
            )}
            {step === totalSteps && (
              <button className="submit-btn-r" onClick={handleSubmit}>
                Launch
              </button>
            )}
          </div>
        </div>
      </div>

      {successMessage && (
        <div className="success-message">
          <p>{successMessage}</p>
        </div>
      )}
    </div>
  );
};

export default Page;
