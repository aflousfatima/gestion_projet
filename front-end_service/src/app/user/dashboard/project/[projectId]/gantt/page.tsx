
"use client";
import React, { useEffect, useRef, useState } from "react";
import { useTasks } from "../../../../../../hooks/useTask";
import { useAuth } from "../../../../../../context/AuthContext";
import useAxios from "../../../../../../hooks/useAxios";
import { TASK_SERVICE_URL } from "../../../../../../config/useApi";
import Gantt from "dhtmlx-gantt";
import "dhtmlx-gantt/codebase/dhtmlxgantt.css";
import "../../../../../../styles/Dashboard-Task-Gantt.css";

// Declare Gantt type to fix TypeScript errors
interface GanttInstance {
  config: {
    date_format?: string;
    grid_width?: number;
    row_height?: number;
    scale_height?: number;
    task_height?: number;
    bar_height?: number;
    drag_links?: boolean;
    fit_tasks?: boolean;
    auto_scheduling?: boolean;
    [key: string]: any;
  };
  init: (element: HTMLElement) => void;
  templates: {
    task_text?: (start: Date, end: Date, task: GanttTask) => string;
    task_class?: (start: Date, end: Date, task: GanttTask) => string;
    [key: string]: any;
  };
  parse: (data: { data: GanttTask[]; links: any[] }) => void;
  attachEvent: (event: string, handler: (id: string, mode?: string, task?: GanttTask) => void) => void;
  clearAll: () => void;
}

const GanttTyped = Gantt as unknown as GanttInstance;

interface GanttTask {
  id: string;
  text: string;
  start_date: string;
  end_date: string;
  progress: number;
  dependencies: string;
  custom_class: string;
  assignedUsers: { id: string; firstName: string; lastName: string; avatar?: string }[];
  status: string;
  hasDependencies?: boolean;
}

const GanttView: React.FC = () => {
  const { tasks, loading, error, handleTaskUpdate } = useTasks();
  const { accessToken } = useAuth();
  const axiosInstance = useAxios();
  const ganttRef = useRef<HTMLDivElement>(null);
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [selectedTaskId, setSelectedTaskId] = useState<string | null>(null);

  const toggleUsersPopup = (taskId: string) => {
    setSelectedTaskId(selectedTaskId === taskId ? null : taskId);
    console.log(`Toggled users popup for task ${taskId}`);
  };

  useEffect(() => {
    // Log raw tasks to inspect assignedUsers
    console.log("Raw tasks from useTasks:", tasks.map((t) => ({
      id: t.id,
      title: t.title,
      hasDependencies: !!t.dependencies?.length,
      assignedUsers: t.assignedUsers,
    })));

    if (!ganttRef.current) {
      console.error("ganttRef.current is null or undefined");
      return;
    }

    try {
      // Initialize DHTMLX Gantt with settings to prevent content truncation
      GanttTyped.config.date_format = "%Y-%m-%d";
      GanttTyped.config.grid_width = isSidebarOpen ? 250 : 0;
      GanttTyped.config.row_height = 50;
      GanttTyped.config.task_height = 50; // Ensure enough height for content
      GanttTyped.config.bar_height = 40; // Ensure task bar height
      GanttTyped.config.scale_height = 80;
      GanttTyped.config.drag_links = true;
      GanttTyped.config.fit_tasks = false; // Prevent auto-resize issues
      GanttTyped.config.auto_scheduling = false; // Disable to test dependency interference
      GanttTyped.config.preserve_scroll = true;
      GanttTyped.config.show_links = true;
      GanttTyped.init(ganttRef.current);
      console.log("DHTMLX Gantt initialized");

      // Simplified task template to test rendering
      GanttTyped.templates.task_text = (start: Date, end: Date, task: GanttTask) => {
        console.log(`Rendering task ${task.id}:`, {
          hasDependencies: task.hasDependencies,
          assignedUsers: task.assignedUsers,
          assignedUsersLength: task.assignedUsers?.length,
          taskText: task.text,
        });

        // Generate avatars HTML
        let avatars = `<div class="avatars-stack" data-task-id="${task.id}" onclick="window.toggleUsersPopup('${task.id}')">`;
        const users = Array.isArray(task.assignedUsers) ? task.assignedUsers : [];
        if (users.length > 0) {
          console.log(`Task ${task.id} has ${users.length} users:`, users);
          users.slice(0, 3).forEach((user, index) => {
            avatars += `
              <div class="avatar-wrapper" style="z-index: ${users.length - index}">
                ${user.avatar
                  ? `<img src="${user.avatar}" alt="${user.firstName} ${user.lastName}" class="user-avatar-stack" />`
                  : `<div class="user-avatar-placeholder">${user.firstName.charAt(0)}${user.lastName.charAt(0)}</div>`
                }
              </div>`;
          });
          if (users.length > 3) {
            avatars += `<div class="avatar-more">+${users.length - 3}</div>`;
          }
        } else {
          console.log(`Task ${task.id} has no assigned users, adding placeholder`);
          avatars += `<div class="user-avatar-placeholder">N/A</div>`;
        }
        avatars += `</div>`;

        // Generate dependency indicator
        const dependencyIndicator = task.hasDependencies ? '<span class="dependency-indicator"></span>' : '';

        // Combine all parts
        const taskText = `<span class="task-text" data-tooltip="Task: ${task.text}\nStatus: ${task.status}\nAssigned: ${users
          .map((u) => `${u.firstName} ${u.lastName}`)
          .join(", ")}\nDependencies: ${task.dependencies || "None"}">${dependencyIndicator}${avatars}${task.text}</span>`;
        console.log(`Task ${task.id} rendered HTML:`, taskText);
        return taskText;
      };

      // Custom task class for styling
      GanttTyped.templates.task_class = (start: Date, end: Date, task: GanttTask) => {
        return `task-${task.status.toLowerCase()} ${task.hasDependencies ? "task-dependent" : ""}`;
      };

      // Map tasks to DHTMLX Gantt format
      const ganttTasks: GanttTask[] = tasks
        .filter((task) => task.startDate && task.dueDate && task.id)
        .map((task) => {
          const mappedTask = {
            id: task.id!.toString(),
            text: task.title,
            start_date: task.startDate!,
            end_date: task.dueDate!,
            progress: task.status === "DONE" ? 1 : task.status === "IN_PROGRESS" ? 0.5 : 0,
            dependencies: task.dependencies?.map((dep) => dep.id.toString()).join(", ") || "",
            custom_class: `task-${task.status.toLowerCase()}`,
            assignedUsers: Array.isArray(task.assignedUsers) ? task.assignedUsers : [],
            status: task.status,
            hasDependencies: !!task.dependencies?.length,
          };
          console.log(`Mapped task ${task.id}:`, {
            assignedUsers: mappedTask.assignedUsers,
            hasDependencies: mappedTask.hasDependencies,
          });
          return mappedTask;
        });

      // Log tasks and links before parsing
      console.log("Gantt tasks to parse:", ganttTasks);
      console.log("Gantt links to parse:", ganttTasks
        .filter((task) => task.dependencies)
        .map((task) => {
          const dependencyIds = task.dependencies.split(", ");
          return dependencyIds.map((sourceId) => ({
            id: `link-${task.id}-${sourceId}`,
            source: sourceId,
            target: task.id,
            type: "0",
          }));
        })
        .flat());

      // Load tasks into Gantt
      GanttTyped.parse({
        data: ganttTasks,
        links: ganttTasks
          .filter((task) => task.dependencies)
          .map((task) => {
            const dependencyIds = task.dependencies.split(", ");
            return dependencyIds.map((sourceId) => ({
              id: `link-${task.id}-${sourceId}`,
              source: sourceId,
              target: task.id,
              type: "0",
            }));
          })
          .flat(),
      });
      console.log("Tasks loaded into Gantt");

      // Handle task updates
      GanttTyped.attachEvent("onAfterTaskDrag", (id: string, mode: string, task: GanttTask) => {
        console.log("Task dragged:", { id, startDate: task.start_date, endDate: task.end_date });
        const updatedTask = {
          ...tasks.find((t) => t.id!.toString() === id),
          startDate: task.start_date,
          dueDate: task.end_date,
        };
        axiosInstance
          .put(
            `${TASK_SERVICE_URL}/api/project/tasks/${id}/updateTask`,
            updatedTask,
            { headers: { Authorization: `Bearer ${accessToken}` } }
          )
          .then(() => {
            console.log("Task updated successfully:", updatedTask);
            handleTaskUpdate(updatedTask);
          })
          .catch((err) => console.error("Erreur lors de la mise à jour de la tâche :", err));
      });

      // Expose toggleUsersPopup to window for onclick
      (window as any).toggleUsersPopup = toggleUsersPopup;

      // Enhanced fallback: Force avatar rendering and log DOM state
      GanttTyped.attachEvent("onGanttRender", () => {
        console.log("Gantt rendered, inspecting DOM for avatars");
        ganttTasks.forEach((task) => {
          const taskElement = document.querySelector(`.gantt_task_line[data-task-id="${task.id}"] .task-text`);
          if (taskElement) {
            let avatarsStack = taskElement.querySelector(".avatars-stack");
            if (!avatarsStack) {
              console.warn(`Task ${task.id} missing avatars-stack, injecting manually`);
              avatarsStack = document.createElement("div");
              avatarsStack.classeName = "avatars-stack";
              avatarsStack.setAttribute("data-task-id", task.id);
              avatarsStack.onclick = () => toggleUsersPopup(task.id);
              const users = task.assignedUsers || [];
              if (users.length > 0) {
                users.slice(0, 3).forEach((user, index) => {
                  const wrapper = document.createElement("div");
                  wrapper.className = "avatar-wrapper";
                  wrapper.style.zIndex = `${users.length - index}`;
                  const avatarContent = user.avatar
                    ? `<img src="${user.avatar}" alt="${user.firstName} ${user.lastName}" className="user-avatar-stack" />`
                    : `<div className="user-avatar-placeholder">${user.firstName.charAt(0)}${user.lastName.charAt(0)}</div>`;
                  wrapper.innerHTML = avatarContent;
                  avatarsStack.appendChild(wrapper);
                });
                if (users.length > 3) {
                  const more = document.createElement("div");
                  more.className = "avatar-more";
                  more.textContent = `+${users.length - 3}`;
                  avatarsStack.appendChild(more);
                }
              } else {
                const placeholder = document.createElement("div");
                placeholder.className = "user-avatar-placeholder";
                placeholder.textContent = "N/A";
                avatarsStack.appendChild(placeholder);
              }
              const dependencyIndicator = taskElement.querySelector(".dependency-indicator");
              taskElement.insertBefore(avatarsStack, dependencyIndicator ? dependencyIndicator.nextSibling : taskElement.firstChild.nextSibling);
            }
            // Log DOM content and force visibility
            console.log(`Task ${task.id} DOM content:`, taskElement.innerHTML);
            avatarsStack.setAttribute("style", "display: inline-flex !important; visibility: visible !important; opacity: 1 !important; margin-left: 4px;");
            taskElement.setAttribute("style", "display: inline-block !important; visibility: visible !important; opacity: 1 !important; white-space: nowrap !important;");
          } else {
            console.warn(`Task ${task.id} task-text element not found in DOM`);
          }
        });
      });

      return () => {
        GanttTyped.clearAll();
        delete (window as any).toggleUsersPopup;
      };
    } catch (err) {
      console.error("Error initializing DHTMLX Gantt:", err);
    }
  }, [tasks, accessToken, axiosInstance, handleTaskUpdate, isSidebarOpen]);

  const toggleSidebar = () => {
    setIsSidebarOpen(!isSidebarOpen);
  };

  if (loading) return <div className="gantt-loading">Chargement...</div>;
  if (error) return <div className="gantt-error">Erreur : {error}</div>;
  if (tasks.length === 0)
    return <div className="gantt-empty">Aucune tâche planifiée trouvée.</div>;

  return (
    <div className="gantt-container" role="region" aria-label="Diagramme de Gantt des tâches">
      <div className="gantt-header">
        <h2 className="gantt-title">Diagramme de Gantt</h2>
        <button
          className="gantt-toggle-btn"
          onClick={toggleSidebar}
          aria-label={isSidebarOpen ? "Masquer la liste des tâches" : "Afficher la liste des tâches"}
        >
          {isSidebarOpen ? "◄" : "►"}
        </button>
      </div>
      <div className="gantt-wrapper">
        <div
          ref={ganttRef}
          className={`gantt-chart ${isSidebarOpen ? "sidebar-open" : "sidebar-closed"}`}
          style={{ minHeight: "600px", width: "100%" }}
        />
      </div>
      <div className="gantt-legend">
        <div className="legend-item">
          <div className="legend-color task-to_do"></div>
          <span>À faire</span>
        </div>
        <div className="legend-item">
          <div className="legend-color task-in_progress"></div>
          <span>En cours</span>
        </div>
        <div className="legend-item">
          <div className="legend-color task-done"></div>
          <span>Terminé</span>
        </div>
        <div className="legend-item">
          <div className="legend-color task-dependent"></div>
          <span>Dépendante</span>
        </div>
      </div>
      {selectedTaskId && (
        <div className="users-popup">
          <p>Users for task {selectedTaskId}</p>
          <button onClick={() => setSelectedTaskId(null)}>Close</button>
        </div>
      )}
    </div>
  );
};

export default GanttView;
