import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import Home from '../../../../../app/user/dashboard/home/page';
import { useAuth } from '../../../../../context/AuthContext';
import { useProjects } from '../../../../../hooks/useProjects';

// Extend jest-axe matchers
expect.extend(toHaveNoViolations);

// Mock useAuth hook
jest.mock('../../../../../context/AuthContext', () => ({
  useAuth: jest.fn(),
}));

// Mock useProjects hook
jest.mock('../../../../../hooks/useProjects', () => ({
  useProjects: jest.fn(),
}));

// Mock ProtectedRoute component
jest.mock('../../../../../components/ProtectedRoute', () => {
  return ({ children }: { children: React.ReactNode }) => <div>{children}</div>;
});

// Mock global fetch
global.fetch = jest.fn();

describe('Home Component', () => {
  const mockAccessToken = 'mock-token';
  const mockProjects = [
    { id: 1, name: 'Project A' },
    { id: 2, name: 'Project B' },
  ];
  const mockTasks = [
    { id: 1, title: 'Task 1', status: 'TO_DO', projectId: 1 },
    { id: 2, title: 'Task 2', status: 'IN_PROGRESS', projectId: 1 },
    { id: 3, title: 'Task 3', status: 'DONE', projectId: 2 },
  ];

  beforeEach(() => {
    // Reset mocks
    jest.clearAllMocks();
    // Mock useAuth
    (useAuth as jest.Mock).mockReturnValue({
      accessToken: mockAccessToken,
      isLoading: false,
    });
    // Mock useProjects
    (useProjects as jest.Mock).mockReturnValue({
      projects: mockProjects,
      loading: false,
      error: null,
    });
    // Mock current date
    jest.useFakeTimers().setSystemTime(new Date('2025-06-09'));
    // Mock fetch with default success responses
    (global.fetch as jest.Mock).mockImplementation(() =>
      Promise.resolve({
        ok: true,
        json: async () => ({ firstName: 'John' }),
      })
    );
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('renders loading state correctly', async () => {
    (useAuth as jest.Mock).mockReturnValue({
      accessToken: null,
      isLoading: true,
    });

    render(<Home />);

    await waitFor(() => {
      expect(screen.getByRole('img', { name: /Loading/i })).toBeInTheDocument();
    });
  });

  it('renders error state correctly', async () => {
    (useProjects as jest.Mock).mockReturnValue({
      projects: [],
      loading: false,
      error: 'Failed to load projects',
    });

    render(<Home />);

    await waitFor(() => {
      expect(screen.getByText(/Erreur : Failed to load projects/i)).toBeInTheDocument();
    });
  });

  it('renders dashboard with user name, date, tasks, and projects', async () => {
    // Mock fetch for user details and tasks
    (global.fetch as jest.Mock)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ firstName: 'John' }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => mockTasks,
      });

    render(<Home />);

    await waitFor(() => {
      // Header
      expect(screen.getByRole('heading', { name: /Hello, John/i })).toBeInTheDocument();
      expect(screen.getByText(/Monday, June 9, 2025/i)).toBeInTheDocument();
      expect(screen.getByText(/1 completed tasks/i)).toBeInTheDocument();

      // Tasks section
      expect(screen.getByRole('heading', { name: /My Tasks/i })).toBeInTheDocument();
      expect(screen.getByText(/Upcoming/i)).toHaveClass('tab active');
      expect(screen.getByText(/Task 1/, { selector: '.task-title' })).toBeInTheDocument();
      expect(screen.getByText(/Projet: Project A/, { selector: '.project-info' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Create Task/i })).toBeInTheDocument();

      // Projects section
      expect(screen.getByRole('heading', { name: /Projects/i })).toBeInTheDocument();
      expect(screen.getByText(/Project A/, { selector: '.project-name' })).toBeInTheDocument();
      expect(screen.getByText(/Project B/, { selector: '.project-name' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Create project/i })).toBeInTheDocument();
    }, { timeout: 10000 });
  });

  it('switches tabs and filters tasks correctly', async () => {
    // Mock fetch for user details and tasks
    (global.fetch as jest.Mock)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ firstName: 'John' }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => mockTasks,
      });

    render(<Home />);

    // Wait for initial render (Upcoming tab)
    await waitFor(() => {
      expect(screen.getByText(/Task 1/, { selector: '.task-title' })).toBeInTheDocument();
      expect(screen.queryByText(/Task 2/, { selector: '.task-title' })).not.toBeInTheDocument();
      expect(screen.queryByText(/Task 3/, { selector: '.task-title' })).not.toBeInTheDocument();
    }, { timeout: 10000 });

    // Switch to Late tab
    await fireEvent.click(screen.getByText('Late', { selector: '.tab' }));
    await waitFor(() => {
      expect(screen.getByText(/Task 2/, { selector: '.task-title' })).toBeInTheDocument();
      expect(screen.queryByText(/Task 1/, { selector: '.task-title' })).not.toBeInTheDocument();
      expect(screen.queryByText(/Task 3/, { selector: '.task-title' })).not.toBeInTheDocument();
      expect(screen.getByText('Late', { selector: '.tab' })).toHaveClass('tab active');
    });

    // Switch to Completed tab
    await fireEvent.click(screen.getByText('Completed', { selector: '.tab' }));
    await waitFor(() => {
      expect(screen.getByText(/Task 3/, { selector: '.task-title' })).toBeInTheDocument();
      expect(screen.queryByText(/Task 1/, { selector: '.task-title' })).not.toBeInTheDocument();
      expect(screen.queryByText(/Task 2/, { selector: '.task-title' })).not.toBeInTheDocument();
      expect(screen.getByText('Completed', { selector: '.tab' })).toHaveClass('tab active');
    });
  });

  it('handles user details fetch error', async () => {
    // Mock fetch to fail for user details
    (global.fetch as jest.Mock)
      .mockRejectedValueOnce(new Error('Fetch error'))
      .mockResolvedValueOnce({
        ok: true,
        json: async () => mockTasks,
      });

    render(<Home />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Hello, User/i })).toBeInTheDocument();
      expect(screen.getByText(/Task 1/, { selector: '.task-title' })).toBeInTheDocument();
    }, { timeout: 10000 });
  });

  it('handles tasks fetch error', async () => {
    // Mock fetch for user details and fail for tasks
    (global.fetch as jest.Mock)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ firstName: 'John' }),
      })
      .mockRejectedValueOnce(new Error('Fetch error'));

    render(<Home />);

    await waitFor(() => {
      expect(screen.getByText(/Erreur : Impossible de charger les tâches/i)).toBeInTheDocument();
    }, { timeout: 10000 });
  });

  it('does not fetch tasks if conditions are not met', async () => {
    (useAuth as jest.Mock).mockReturnValue({
      accessToken: null,
      isLoading: false,
    });

    render(<Home />);

    await waitFor(() => {
      expect(global.fetch).not.toHaveBeenCalledWith(
        expect.stringContaining('http://localhost:8086/api/project/tasks/user/active-sprints'),
        expect.anything()
      );
      expect(screen.getByText(/No Task Available in this Category/i)).toBeInTheDocument();
    });
  });

  it('is accessible', async () => {
    // Mock fetch for user details and tasks
    (global.fetch as jest.Mock)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ firstName: 'John' }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => mockTasks,
      });

    const { container } = render(<Home />);

    await waitFor(async () => {
      const results = await axe(container);
      expect(results).toHaveNoViolations();
    }, { timeout: 10000 });
  });
});