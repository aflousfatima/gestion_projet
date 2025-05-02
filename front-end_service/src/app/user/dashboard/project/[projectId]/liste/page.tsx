"use client";
import React, { useState } from "react";
import { useWorkItems } from "../../../../../../hooks/useWorkItems"; // Corrigé l'importation
import WorkItemCard from "../../../../../../hooks/WorkItemCard";
import "../../../../../../styles/Dashboard-Task-List.css";

export default function ListView() {
  const { workItems, loading, error, handleWorkItemUpdate } = useWorkItems();
  const [showDates, setShowDates] = useState(false);

  const sections = [
    { status: "TO_DO", title: "TO DO" },
    { status: "IN_PROGRESS", title: "IN PROGRESS" },
    { status: "DONE", title: "DONE" },
  ];

  return (
    <div className="list-container-work-item">
      {loading && (
        <p>
          <img src="/loading.svg" alt="Loading" className="loading-img" />
        </p>
      )}
      {error && <p style={{ color: "red" }}>Error: {error}</p>}
      {!loading && !error && workItems.length === 0 && <p>No work-items found.</p>}
      {!loading && !error && workItems.length > 0 && (
        <div className="work-items-wrapper">
          {/* En-tête de la liste */}
          <div className="work-items-list-header">
            <span className="work-items-list-header-item-principal">
              Task{"'"}s Name
            </span>
            <span className="work-items-list-header-item">Responsable</span>
            <span className="work-items-list-header-item">Due Date</span>
            <span className="work-items-list-header-item">Priority</span>
            <span className="work-items-list-header-item">Tags</span>
            <span className="work-items-list-header-item">Files</span>
            <span className="work-items-list-header-item">Comments</span>
          </div>

          {sections.map((section) => {
            const sectionTasks = workItems.filter(
              (workItem) => workItem.status === section.status
            );
            return (
              <div key={section.status} className="work-item-section">
                <h3 className="section-work-item-title">{section.title}</h3>
                <div className="work-item-details">
                  {sectionTasks.map((workItem) => (
                    <WorkItemCard
                      key={workItem.id}
                      workItem={workItem}
                      showDates={showDates}
                      toggleDates={() => setShowDates(!showDates)}
                      onWorkItemUpdate={handleWorkItemUpdate}
                      displayMode="row"
                    />
                  ))}
                  <div className="work-items-add-subwork-item">Add Task or Bug...</div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
