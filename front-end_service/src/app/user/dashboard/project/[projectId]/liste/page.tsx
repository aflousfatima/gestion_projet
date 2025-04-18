// components/AddTaskModal.jsx
import React from "react";
import "./AddTaskModal.css"; // Si tu as des styles spécifiques


interface Task {
    id: number;
    name: string;
    responsible: string | null;
    dueDate: string;
    priority: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL" | "";
    status: "TO DO" | "IN PROGRESS" | "DONE";
    sprintId?: number;
    userStoryId?: number;
  }

  const initialTasks: Task[] = [
    {
      id: 1,
      name: "Rédiger le brief",
      responsible: "Fatima",
      dueDate: "6 avril",
      priority: "LOW",
      status: "TO DO",
      sprintId: 1,
      userStoryId: 1,
    },
    {
      id: 2,
      name: "Planifier réunion",
      responsible: "Fatima",
      dueDate: "7 avril",
      priority: "MEDIUM",
      status: "TO DO",
      sprintId: 1,
      userStoryId: 1,
    },
  ];


  export default function Tasks() {


      const [tasks, setTasks] = useState<Task[]>(initialTasks);
        const [isModalOpen, setIsModalOpen] = useState(false);
        const [filterMyTasks, setFilterMyTasks] = useState(false);
      
    const [newTask, setNewTask] = useState<Task>({
        id: 0,
        name: "",
        responsible: null,
        dueDate: "",
        priority: "",
        status: "TO DO",
      });
      const handleOverlayClick = (e: React.MouseEvent<HTMLDivElement>) => {
        if (e.target === e.currentTarget) setIsModalOpen(false);
      };
       const handleAddTask = (
         e: React.FormEvent<HTMLFormElement>,
         userStoryId?: number
       ) => {
         e.preventDefault();
         const task: Task = {
           ...newTask,
           id: tasks.length + 1,
           sprintId: activeSprint?.id,
           userStoryId,
         };
         setTasks([...tasks, task]);
         setNewTask({
           id: 0,
           name: "",
           responsible: null,
           dueDate: "",
           priority: "",
           status: "TO DO",
         });
         setSelectedUserStoryId(null); // Ferme le mini-modal
       };
    // Déclenche à chaque changement de sprints ou activeSprint
    const getTasksByStatus = (status: "TO DO" | "IN PROGRESS" | "DONE") => {
      let filteredTasks = activeSprint
        ? tasks.filter(
            (task) => task.sprintId === activeSprint.id && task.status === status
          )
        : tasks.filter((task) => task.status === status);
      if (filterMyTasks) {
        // Since 'user' is removed, we'll skip filtering by user for now
        // You can reintroduce this logic if you add user data back to AuthContext
      }
      return filteredTasks;
    };    


    
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <h4>Add Task</h4>
        <form
          onSubmit={(e) => handleAddTask(e, userStoryId)}
          className="modern-form"
        >
          <input
            type="text"
            placeholder="Task Name"
            value={newTask.name}
            onChange={(e) => setNewTask({ ...newTask, name: e.target.value })}
            required
          />
          <input
            type="text"
            placeholder="Responsible"
            value={newTask.responsible || ""}
            onChange={(e) =>
              setNewTask({ ...newTask, responsible: e.target.value })
            }
          />
          <input
            type="date"
            id="dueDate"
            value={newTask.dueDate}
            onChange={(e) =>
              setNewTask({ ...newTask, dueDate: e.target.value })
            }
          />
          <select
            value={newTask.priority}
            onChange={(e) =>
              setNewTask({
                ...newTask,
                priority: e.target.value,
              })
            }
          >
            <option value="">Priority</option>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
          </select>
          <select
            value={newTask.status}
            onChange={(e) =>
              setNewTask({
                ...newTask,
                status: e.target.value,
              })
            }
          >
            <option value="TO_DO">To Do</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="DONE">Done</option>
          </select>
          <div className="modal-actions">
            <button type="submit" className="action-btn primary">
              Add
            </button>
            <button
              type="button"
              className="action-btn secondary"
              onClick={onClose}
            >
              Cancel
            </button>
          </div>
        </form>
      </div>

    {/* Actions */}
    <div className="tasks-actions">
        <button
          className="tasks-add-button"
          onClick={() => setIsModalOpen(true)}
        >
          <i className="fa fa-plus"></i> Add Task{" "}
          <span className="second-button">
            {"       "}
            <i className="fa fa-caret-down"></i>
          </span>
        </button>
        <div className="tasks-options">
          <button className="tasks-option">
            <i className="fa fa-filter"></i> Filter
          </button>
          <button className="tasks-option">
            <i className="fa fa-sort"></i> Sort
          </button>
          <button
            className="tasks-option"
            onClick={() => setFilterMyTasks(!filterMyTasks)}
          >
            <i className="fa fa-user"></i> My Tasks
          </button>
          <button className="tasks-option">
            <i className="fa fa-gear"></i> Options
          </button>
        </div>
      </div>
       {/* Modal pour ajouter une tâche (page principale) */}
       {isModalOpen && (
        <div className="tasks-modal-overlay" onClick={handleOverlayClick}>
          <div className="tasks-modal">
            <h2 className="tasks-modal-title">Add Task</h2>
            <form onSubmit={(e) => handleAddTask(e)} className="tasks-form">
              <div className="tasks-form-group">
                <label htmlFor="name">Task{"'"}s Name</label>
                <input
                  type="text"
                  id="name"
                  value={newTask.name}
                  onChange={(e) =>
                    setNewTask({ ...newTask, name: e.target.value })
                  }
                  placeholder="Entrez le nom de la tâche"
                  required
                />
              </div>
              <div className="tasks-form-group">
                <label htmlFor="responsible">Responsable</label>
                <input
                  type="text"
                  id="responsible"
                  value={newTask.responsible || ""}
                  onChange={(e) =>
                    setNewTask({ ...newTask, responsible: e.target.value })
                  }
                  placeholder="Entrez le nom du responsable"
                />
              </div>
              <div className="tasks-form-group">
                <label htmlFor="dueDate">Due date</label>
                <input
                  type="text"
                  id="dueDate"
                  value={newTask.dueDate}
                  onChange={(e) =>
                    setNewTask({ ...newTask, dueDate: e.target.value })
                  }
                  placeholder="Ex: 5 - 7 fév"
                />
              </div>
              <div className="tasks-form-group">
                <label htmlFor="priority">Priority</label>
                <select
                  id="priority"
                  value={newTask.priority}
                  onChange={(e) =>
                    setNewTask({
                      ...newTask,
                      priority: e.target.value as
                        | "LOW"
                        | "MEDIUM"
                        | "HIGH"
                        | "",
                    })
                  }
                >
                  <option value="">Selecy a priority</option>
                  <option value="LOW">LOW</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HIGH">HIGH</option>
                </select>
              </div>
              <div className="tasks-form-group">
                <label htmlFor="status">Status</label>
                <select
                  id="status"
                  value={newTask.status}
                  onChange={(e) =>
                    setNewTask({
                      ...newTask,
                      status: e.target.value as
                        | "TO DO"
                        | "IN PROGRESS"
                        | "DONE",
                    })
                  }
                >
                  <option value="À faire">TO DO</option>
                  <option value="En cours">IN PROGRESS</option>
                  <option value="Terminé">DONE</option>
                </select>
              </div>
              <div className="tasks-modal-actions">
                <button type="submit" className="tasks-submit-button">
                  Add
                </button>
                <button
                  type="button"
                  className="tasks-cancel-button"
                  onClick={() => setIsModalOpen(false)}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}


      {/* Liste des tâches */}
      <div className="tasks-list">
        {["À faire", "En cours", "Terminé"].map((status) => (
          <div key={status} className="dashboard-task-section">
            <h2
              className={`dashboard-task-section-title ${
                status === "En cours" || status === "Terminé"
                  ? "section-encours"
                  : ""
              }`}
            >
              <i className="fa fa-caret-down"></i> {status}
            </h2>
            {getTasksByStatus(status as "TO DO" | "IN PROGRESS" | "DONE").map(
              (task) => (
                <div key={task.id} className="task-row">
                  <div className="task-checkbox">
                    <input type="checkbox" checked={task.status === "DONE"} />
                    <span>{task.name}</span>
                  </div>
                  <div className="task-details">
                    <span className="task-responsible">
                      {task.responsible ? (
                        <span className="responsible-circle">
                          {task.responsible.charAt(0)}
                        </span>
                      ) : (
                        <span className="responsible-circle empty">?</span>
                      )}
                      {task.responsible || "Non assigné"}
                    </span>
                    <span className="task-due-date">{task.dueDate}</span>
                    <span className={task-priority priority-${task.priority}}>
                      {task.priority}
                    </span>
                    <span className="task-status">
                      {task.dueDate.includes("Aujourd'hui")
                        ? "Dans les délais"
                        : "En retard"}
                    </span>
                  </div>
                </div>
              )
            )}
            <button
              className="tasks-add-subtask"
              onClick={() => setIsModalOpen(true)}
            >
              Add Task...
            </button>
          </div>
        ))}
      </div>

    </div>
  );
};