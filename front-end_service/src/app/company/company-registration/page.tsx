"use client";
import React, { useState } from "react";
import "../../styles/Company-registration.css";
import ProtectedRoute from "../../../components/ProtectedRoute";

const Page = () => {
  const [formData, setFormData] = useState({
    companyName: "",
    industry: "",
    teamName: "",
    numEmployees: "",
    department: "",
    role: "",
    projectName: "",
    projectDescription: "",
  });

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    const { id, value } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [id]: value,
    }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    console.log(formData);
  };

  return (
    <ProtectedRoute>
      <div className="form-container">
        {/* Section 1: Tell us about your company */}
        <section className="company-section">
          <img src="/batiment.png" alt="Step Icon" />
          <p className="titres">Tell us about your company</p>
          <div className="roadmap-container">
            <div className="step step-1">
              <h3>Company Name</h3>
              <div className="step-content">
                <input
                  type="text"
                  id="companyName"
                  value={formData.companyName}
                  onChange={handleChange}
                  placeholder="Company Name"
                  required
                />
              </div>
            </div>

            <div className="step step-2">
              <h3>Industry</h3>
              <div className="step-content">
                <select
                  id="industry"
                  value={formData.industry}
                  onChange={handleChange}
                  required
                >
                  <option value="" disabled>
                    Select an industry
                  </option>
                  <option value="tech">Technology</option>
                  <option value="finance">Finance</option>
                  <option value="education">Education</option>
                  <option value="healthcare">Healthcare</option>
                  <option value="retail">Retail</option>
                </select>
              </div>
            </div>
          </div>
        </section>

        {/* Section 2: Tell us about your team */}
        <section className="team-section">
          <img src="/equipe.png" alt="Step Icon" />

          <p className="titres">Tell us about your team</p>

          <div className="roadmap-container">
            <div className="step step-3">
              <h3>Team Name</h3>
              <div className="step-content">
                <input
                  type="text"
                  id="teamName"
                  value={formData.teamName}
                  onChange={handleChange}
                  placeholder="Team Name"
                  required
                />
              </div>
            </div>
            <div className="step step-4">
              <h3>Number of Employees</h3>
              <div className="step-content">
                <select
                  id="numEmployees"
                  value={formData.numEmployees}
                  onChange={handleChange}
                  required
                >
                  <option value="" disabled>
                    Select number of employees
                  </option>
                  <option value="1-25">1 - 25 employees</option>
                  <option value="25-50">25 - 50 employees</option>
                  <option value="50-200">50 - 200 employees</option>
                  <option value="200+">200+ employees</option>
                </select>
              </div>
            </div>
          </div>
        </section>

        {/* Section 3: Tell us more about you */}
        <section className="user-section">
          <img src="/user-interface.png" alt="Step Icon" />

          <p className="titres">Tell us more about you</p>

          <div className="roadmap-container">
            <div className="step step-5">
              <h3>Department</h3>
              <div className="step-content">
                <input
                  type="text"
                  id="department"
                  value={formData.department}
                  onChange={handleChange}
                  placeholder="Your Department"
                  required
                />
              </div>
            </div>

            <div className="step step-6">
              <h3>Your Role</h3>
              <div className="step-content">
                <input
                  type="text"
                  id="role"
                  value={formData.role}
                  onChange={handleChange}
                  placeholder="Your Role"
                  required
                />
              </div>
            </div>
          </div>
        </section>

        {/* Section 4: Project */}
        <section className="project-section">
          <img src="/project-management.png" alt="Step Icon" />

          <p className="titres">Create project</p>
          <div className="roadmap-container">
            <div className="step step-5">
              <h3>Project name</h3>
              <div className="step-content">
                <input
                  type="text"
                  id="ProjectName"
                  value={formData.projectName}
                  onChange={handleChange}
                  placeholder="Project name"
                  required
                />
              </div>
            </div>

            <div className="step step-6">
              <h3>Project Description</h3>
              <div className="step-content">
                <input
                  type="text"
                  id="ProjectDescription"
                  value={formData.projectDescription}
                  onChange={handleChange}
                  placeholder="Project desription"
                  required
                />
              </div>
            </div>
          </div>
        </section>
        {/* Submit button */}
        <div className="submit-container">
          <button onClick={handleSubmit}>Submit</button>
        </div>
      </div>
    </ProtectedRoute>
  );
};

export default Page;
