"use client";
import "../../../../../../styles/Dashboard-Task-Calendar.css";
import React, { useEffect, useState, useMemo } from "react";
import { Container } from "react-bootstrap";
import useAxios from "../../../../../../hooks/useAxios";
import { useAuth } from "../../../../../../context/AuthContext";
import { TASK_SERVICE_URL } from "../../../../../../config/useApi";
import { useParams } from "next/navigation";
import {
  format,
  parseISO,
  eachDayOfInterval,
  startOfMonth,
  endOfMonth,
  differenceInDays,
  getDay,
  getDate,
} from "date-fns";
import { enUS } from "date-fns/locale";

// FontAwesome for icons
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
  faCircle,
  faChevronLeft,
  faChevronRight,
  faCalendarDay,
} from "@fortawesome/free-solid-svg-icons";

interface TaskCalendar {
  id: number;
  title: string;
  startDate: string;
  dueDate: string;
}

interface CalendarEvent {
  id: number;
  title: string;
  start: Date;
  due: Date;
}

export default function CalendarView() {
  const params = useParams();
  const projectId = params.projectId as string;
  const { accessToken } = useAuth();
  const axiosInstance = useAxios();
  const [events, setEvents] = useState<CalendarEvent[]>([]);
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cellWidth, setCellWidth] = useState(150); // Default width in pixels
  const [cellHeight, setCellHeight] = useState(100); // Default height in pixels

  useEffect(() => {
    const fetchTasks = async () => {
      setLoading(true);
      setError(null);
      try {
        console.log("Fetching tasks for project:", projectId);
        const startTime = performance.now();
        const response = await axiosInstance.get(
          `${TASK_SERVICE_URL}/api/project/tasks/calendar/${projectId}`,
          {
            headers: { Authorization: `Bearer ${accessToken}` },
          }
        );
        console.log("API response time:", performance.now() - startTime, "ms");
        console.log("API response:", response.data);
        const tasks: TaskCalendar[] = response.data;
        if (!Array.isArray(tasks)) {
          throw new Error(
            "Expected an array of tasks, but received: " + JSON.stringify(tasks)
          );
        }
        const formattedEvents: CalendarEvent[] = tasks
          .map((task) => {
            if (!task.startDate || !task.dueDate) {
              console.warn("Invalid task dates:", task);
              return null;
            }
            return {
              id: task.id,
              title: task.title,
              start: parseISO(task.startDate),
              due: parseISO(task.dueDate),
            };
          })
          .filter((event): event is CalendarEvent => event !== null)
          .slice(0, 50);
        setEvents(formattedEvents);
      } catch (error) {
        console.error("Error fetching calendar tasks:", error);
        setError("Failed to load tasks. Please try again later.");
      } finally {
        setLoading(false);
      }
    };
    fetchTasks();
  }, [projectId, accessToken, axiosInstance]);

  const monthStart = startOfMonth(currentMonth);
  const monthEnd = endOfMonth(currentMonth);
  const daysInMonth = useMemo(
    () => eachDayOfInterval({ start: monthStart, end: monthEnd }),
    [monthStart, monthEnd]
  );

  // Calculate weeks in the month
  const firstDayOfMonth = getDay(monthStart); // 0 (Sunday) to 6 (Saturday)
  const daysOffset = firstDayOfMonth === 0 ? 6 : firstDayOfMonth - 1; // Adjust for Monday start
  const totalDays = daysInMonth.length + daysOffset;
  const weeksInMonth = Math.ceil(totalDays / 7);

  // Create an array of weeks, each containing 7 days
  const weeks = useMemo(() => {
    const weeksArray = [];
    let dayIndex = -daysOffset;
    for (let week = 0; week < weeksInMonth; week++) {
      const weekDays = [];
      for (let day = 0; day < 7; day++) {
        if (dayIndex < 0 || dayIndex >= daysInMonth.length) {
          weekDays.push(null); // Empty day before/after the month
        } else {
          weekDays.push(daysInMonth[dayIndex]);
        }
        dayIndex++;
      }
      weeksArray.push(weekDays);
    }
    return weeksArray;
  }, [daysInMonth, weeksInMonth, daysOffset]);

  const getTaskPosition = (task: CalendarEvent) => {
    const startDay = differenceInDays(task.start, monthStart);
    const endDay = differenceInDays(task.due, monthStart);
    if (endDay < 0 || startDay >= daysInMonth.length) return null; // Task is outside the current month

    const adjustedStartDay = Math.max(0, startDay);
    const adjustedEndDay = Math.min(daysInMonth.length - 1, endDay);

    const startWeek = Math.floor((adjustedStartDay + daysOffset) / 7);
    const endWeek = Math.floor((adjustedEndDay + daysOffset) / 7);
    const startColumn = ((adjustedStartDay + daysOffset) % 7) + 1;
    const endColumn = ((adjustedEndDay + daysOffset) % 7) + 1;

    return { startWeek, endWeek, startColumn, endColumn };
  };

  // Group tasks by start date to handle overlapping tasks
  const tasksByStartDate = useMemo(() => {
    const grouped: { [key: string]: CalendarEvent[] } = {};
    events.forEach((event) => {
      const startDateKey = format(event.start, "yyyy-MM-dd");
      if (!grouped[startDateKey]) {
        grouped[startDateKey] = [];
      }
      grouped[startDateKey].push(event);
    });
    return grouped;
  }, [events]);

  const handlePreviousMonth = () => {
    setCurrentMonth(
      (prev) => new Date(prev.getFullYear(), prev.getMonth() - 1, 1)
    );
  };

  const handleNextMonth = () => {
    setCurrentMonth(
      (prev) => new Date(prev.getFullYear(), prev.getMonth() + 1, 1)
    );
  };

  const handleToday = () => {
    setCurrentMonth(new Date());
  };

  if (loading) {
    return (
      <Container className="calendar-container">
        <div className="loading">Chargement...</div>
      </Container>
    );
  }

  if (error) {
    return (
      <Container className="calendar-container">
        <div className="error">{error}</div>
      </Container>
    );
  }

  return (
    <Container fluid className="calendar-container">
      <div className="calendar-header">
        <div className="calendar-toolbar">
          <button
            onClick={handlePreviousMonth}
            className="calendar-btn calendar-btn-nav"
          >
            <FontAwesomeIcon icon={faChevronLeft} />
          </button>
          <button
            onClick={handleToday}
            className="calendar-btn calendar-btn-today"
          >
            <FontAwesomeIcon icon={faCalendarDay} className="btn-icon" />
            Today
          </button>
          <button
            onClick={handleNextMonth}
            className="calendar-btn calendar-btn-nav"
          >
            <FontAwesomeIcon icon={faChevronRight} />
          </button>
        </div>
        <div className="calendar-controls">
          <label>
            Box Width (px):
            <input
              type="number"
              value={cellWidth}
              onChange={(e) => setCellWidth(Number(e.target.value))}
              min="100"
              max="300"
              className="size-input"
            />
          </label>
          <label>
            Box Height (px):
            <input
              type="number"
              value={cellHeight}
              onChange={(e) => setCellHeight(Number(e.target.value))}
              min="80"
              max="200"
              className="size-input"
            />
          </label>
        </div>
        <h2 className="calendar-title">
          {format(currentMonth, "MMMM yyyy", { locale: enUS })}
        </h2>
      </div>
      <div
        className="calendar-grid"
        style={
          {
            "--cell-width": `${cellWidth}px`,
            "--cell-height": `${cellHeight}px`,
          } as React.CSSProperties
        }
      >
        {/* Days of the week header */}
        <div className="calendar-days-header">
          {["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"].map(
            (dayName, index) => (
              <div key={index} className="calendar-day-header">
                {dayName}
              </div>
            )
          )}
        </div>
        {/* Calendar grid with weeks */}
        <div className="calendar-body">
          {weeks.map((week, weekIndex) => (
            <div key={weekIndex} className="calendar-week">
              {week.map((day, dayIndex) => (
                <div
                  key={`${weekIndex}-${dayIndex}`}
                  className={`calendar-day ${
                    day &&
                    format(day, "d") === format(new Date(), "d") &&
                    format(day, "M") === format(new Date(), "M")
                      ? "today"
                      : ""
                  }`}
                >
                  {day ? (
                    <span className="day-number">{getDate(day)}</span>
                  ) : null}
                </div>
              ))}
            </div>
          ))}
          {/* Task bars */}
          {events.map((event, globalIndex) => {
            const position = getTaskPosition(event);
            if (!position) return null;
            const { startWeek, endWeek, startColumn, endColumn } = position;
            const barColorClass = `task-bar-color-${globalIndex % 3}`;
            const width =
              endWeek === startWeek
                ? `${(endColumn - startColumn + 1) * 100}%`
                : "100%";
            const startDateKey = format(event.start, "yyyy-MM-dd");
            const taskIndex = tasksByStartDate[startDateKey].findIndex(
              (e) => e.id === event.id
            );

            return (
              <div
                key={event.id}
                className={`calendar-task ${barColorClass}`}
                style={{
                  gridRow: startWeek + 2, // +2 to account for header row
                  gridColumn: `${startColumn} / ${
                    endWeek > startWeek ? 8 : endColumn + 1
                  }`,
                  top: `${taskIndex * 50}px`, // Stack tasks vertically if they start on the same date
                }}
              >
                <div className="task-bar">
                  <FontAwesomeIcon icon={faCircle} className="task-icon" />
                  <span className="task-title">{event.title}</span>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </Container>
  );
}
