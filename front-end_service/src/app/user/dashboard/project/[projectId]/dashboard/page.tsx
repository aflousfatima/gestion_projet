"use client";
import "../../../../../../styles/Dashboard-Task-Dashboard.css";
import React, { useEffect, useState } from "react";
import { Container, Row, Col } from "react-bootstrap";
import useAxios from "../../../../../../hooks/useAxios";
import { useAuth } from "../../../../../../context/AuthContext";
import { TASK_SERVICE_URL } from "../../../../../../config/useApi";
import { useParams } from "next/navigation";
import {
  Chart as ChartJS,
  ArcElement,
  BarElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend,
} from "chart.js";
import { Pie, Bar } from "react-chartjs-2";

// Register Chart.js components
ChartJS.register(
  ArcElement,
  BarElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend
);

interface DashboardStats {
  completedTasks: number;
  notCompletedTasks: number;
  overdueTasks: number;
  totalTasks: number;
  tasksByStatus: { [key: string]: number };
  tasksByPriority: { [key: string]: number };
}

export default function Dashboard() {
  const params = useParams();
  const projectId = params.projectId as string;
  const { accessToken } = useAuth();
  const axiosInstance = useAxios();
  const [stats, setStats] = useState<DashboardStats>({
    completedTasks: 0,
    notCompletedTasks: 0,
    overdueTasks: 0,
    totalTasks: 0,
    tasksByStatus: {},
    tasksByPriority: {},
  });

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const response = await axiosInstance.get(
          `${TASK_SERVICE_URL}/api/project/tasks/dashboard/${projectId}`,
          {
            headers: { Authorization: `Bearer ${accessToken}` },
          }
        );
        setStats(response.data);
      } catch (error) {
        console.error("Error fetching dashboard stats:", error);
      }
    };
    fetchStats();
  }, [projectId, accessToken, axiosInstance]);

  // Pie chart data (Tasks by Status)
  const pieChartData = {
    labels: Object.keys(stats.tasksByStatus).map((status) =>
      status.replace("_", " ")
    ),
    datasets: [
      {
        data: Object.values(stats.tasksByStatus),
        backgroundColor: [
          "#F1BD6C", // DONE
          "#CEF3F1", // IN_PROGRESS
          "#FF8989", // TODO
          "#f44336", // Other statuses
        ],
        borderColor: "#fff",
        borderWidth: 2,
        hoverOffset: 20, // Increase hover effect
      },
    ],
  };

  // Pie chart options
  const pieChartOptions = {
    responsive: true,
    maintainAspectRatio: false, // Allow better sizing control
    plugins: {
      legend: {
        position: "right" as const, // Move legend to the right
        labels: {
          font: {
            size: 14, // Larger font for readability
          },
          color: "#333",
          padding: 20, // Increase spacing between legend items
          boxWidth: 20, // Size of the color box
          usePointStyle: true, // Use circular points for legend
        },
      },
      tooltip: {
        backgroundColor: "rgba(0, 0, 0, 0.8)",
        titleFont: { size: 14 },
        bodyFont: { size: 12 },
        padding: 10,
      },
    },
    elements: {
      arc: {
        borderWidth: 2, // Thicker borders for segments
        hoverBorderWidth: 3, // Slightly thicker on hover
      },
    },
  };

  // Bar chart data (Tasks by Priority)
  const barChartData = {
    labels: Object.keys(stats.tasksByPriority).map((priority) =>
      priority.replace("_", " ")
    ),
    datasets: [
      {
        label: "Tasks by Priority",
        data: Object.values(stats.tasksByPriority),
        backgroundColor: [
          "#E07A5F", // HIGH
          "#F2CC8F", // MEDIUM
          "#81B29A", // LOW
        ],
        borderColor: "#fff",
        borderWidth: 1,
      },
    ],
  };

  // Bar chart options
  const barChartOptions = {
    responsive: true,
    plugins: {
      legend: {
        display: false, // Hide legend for bar chart
      },
      tooltip: {
        backgroundColor: "rgba(0, 0, 0, 0.8)",
        titleFont: { size: 14 },
        bodyFont: { size: 12 },
      },
    },
    scales: {
      y: {
        beginAtZero: true,
        title: {
          display: true,
          text: "Number of Tasks",
          font: { size: 14 },
        },
        ticks: {
          stepSize: 1, // Ensure integer steps
        },
      },
      x: {
        title: {
          display: true,
          text: "Priority",
          font: { size: 14 },
        },
      },
    },
  };

  return (
    <Container className="dashboard-container">
      <Row className="g-4">
        <Col xs={12} sm={6} lg={3}>
          <div className="dashboard-card completed-card">
            <h3 className="card-title-view">Completed Tasks</h3>
            <p className="card-number">{stats.completedTasks}</p>
          </div>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <div className="dashboard-card not-completed-card">
            <h3 className="card-title-view">Not Completed</h3>
            <p className="card-number">{stats.notCompletedTasks}</p>
          </div>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <div className="dashboard-card overdue-card">
            <h3 className="card-title-view">Overdue Tasks</h3>
            <p className="card-number">{stats.overdueTasks}</p>
          </div>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <div className="dashboard-card total-card">
            <h3 className="card-title-view">Total Tasks</h3>
            <p className="card-number">{stats.totalTasks}</p>
          </div>
        </Col>
      </Row>
      <Row className="mt-5 g-5">
        <Col xs={12} md={6} className="chart-container-pie">
          <h2 className="chart-title">Tasks by Status Achievement - Total</h2>
          <div className="pie-style">
            <Pie data={pieChartData} options={pieChartOptions} />
          </div>
        </Col>
        <Col xs={12} md={6} className="chart-container-barre">
          <h2 className="chart-title">Tasks by Priority</h2>
          <div className="barre-style">
            <Bar data={barChartData} options={barChartOptions} />
          </div>
        </Col>
      </Row>
    </Container>
  );
}