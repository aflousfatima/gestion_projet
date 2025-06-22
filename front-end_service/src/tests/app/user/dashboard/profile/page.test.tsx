import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import ProfilePage from '../../../../../app/user/dashboard/profile/page';
import { useAuth } from '../../../../../context/AuthContext';
import useAxios from '../../../../../hooks/useAxios';
import { BrowserRouter } from 'react-router-dom';

// Extend jest-axe matchers
expect.extend(toHaveNoViolations);

// Mock useAuth hook
jest.mock('../../../../../context/AuthContext', () => ({
  useAuth: jest.fn(),
}));

// Mock useAxios hook
jest.mock('../../../../../hooks/useAxios', () => jest.fn());

// Mock next/link
jest.mock('next/link', () => {
  const Link = ({
    children,
    href,
  }: {
    children: React.ReactNode;
    href: string;
  }) => <a href={href}>{children}</a>;
  Link.displayName = 'NextLinkMock';
  return Link;
});

// Mock global fetch for axios simulation
global.fetch = jest.fn();

describe('ProfilePage Component', () => {
  const mockAccessToken = 'mock-token';
  const mockUser = {
    id: '1',
    firstName: 'John',
    lastName: 'Doe',
    email: 'john.doe@example.com',
    role: 'USER',
    avatar: 'https://ui-avatars.com/api/?name=J+D',
    bio: 'Software Engineer',
    phone: '123-456-7890',
    notificationPreferences: {
      emailNotifications: true,
      taskUpdates: true,
      deadlineReminders: true,
    },
  };
  const mockProjectRoles = [
    { projectId: 1, projectName: 'Project A', roleInProject: 'Developer' },
    { projectId: 2, projectName: 'Project B', roleInProject: 'Manager' },
  ];

  const mockAxiosInstance = {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
  };

  beforeEach(() => {
    // Reset mocks
    jest.clearAllMocks();
    // Mock useAuth
    (useAuth as jest.Mock).mockReturnValue({
      accessToken: mockAccessToken,
      isLoading: false,
    });
    // Mock useAxios
    (useAxios as jest.Mock).mockReturnValue(mockAxiosInstance);
    // Mock fetch responses
    mockAxiosInstance.get
      .mockResolvedValueOnce({ data: mockUser }) // /api/me
      .mockResolvedValueOnce({ data: { projects: mockProjectRoles } }); // /projects/by-user
  });

  it('renders loading state correctly', async () => {
    (useAuth as jest.Mock).mockReturnValue({
      accessToken: null,
      isLoading: true,
    });

    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByRole('img', { name: /Loading/i })).toBeInTheDocument();
    }, { timeout: 10000 });
  });





  it('updates user profile successfully', async () => {
    mockAxiosInstance.put.mockResolvedValueOnce({ data: {} });

    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    // Enter edit mode
    await waitFor(() => {
      fireEvent.click(screen.getByRole('button', { name: /Edit/i }));
    });

    // Update firstName
    fireEvent.change(screen.getByTestId('first-name'), {
      target: { value: 'Jane' },
    });

    // Submit form
    fireEvent.click(screen.getByRole('button', { name: /Save/i }));

    await waitFor(() => {
      expect(mockAxiosInstance.put).toHaveBeenCalledWith(
        'http://localhost:8083/api/update',
        expect.objectContaining({ firstName: 'Jane' }),
        expect.any(Object)
      );
      expect(
        screen.getByText(/Profile updated successfully!/i)
      ).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Edit/i })).toBeInTheDocument();
    }, { timeout: 10000 });
  });

  it('handles profile update error', async () => {
    mockAxiosInstance.put.mockRejectedValueOnce({
      response: { data: { message: 'Error updating profile' } },
    });

    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    // Enter edit mode
    await waitFor(() => {
      fireEvent.click(screen.getByRole('button', { name: /Edit/i }));
    });

    // Submit form
    fireEvent.click(screen.getByRole('button', { name: /Save/i }));

    await waitFor(() => {
      expect(screen.getByText(/Error updating profile/i)).toBeInTheDocument();
    }, { timeout: 10000 });
  });

  it('changes password successfully', async () => {
    mockAxiosInstance.post.mockResolvedValueOnce({ data: {} });

    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    // Enter password change mode
    await waitFor(() => {
      fireEvent.click(screen.getByRole('button', { name: /Change Password/i }));
    });

    // Fill password form
    fireEvent.change(screen.getByTestId('current-password'), {
      target: { value: 'oldPassword' },
    });
    fireEvent.change(screen.getByTestId('new-password'), {
      target: { value: 'newPassword' },
    });
    fireEvent.change(screen.getByTestId('confirm-password'), {
      target: { value: 'newPassword' },
    });

    // Submit password form
    fireEvent.click(screen.getByRole('button', { name: /Save/i }));

    await waitFor(() => {
      expect(mockAxiosInstance.post).toHaveBeenCalledWith(
        'http://localhost:8083/api/change-password',
        {
          currentPassword: 'oldPassword',
          newPassword: 'newPassword',
        },
        expect.any(Object)
      );
      expect(
        screen.getByText(/Password changed successfully!/i)
      ).toBeInTheDocument();
      expect(
        screen.getByRole('button', { name: /Change Password/i })
      ).toBeInTheDocument();
    }, { timeout: 10000 });
  });

  it('shows error when new passwords do not match', async () => {
    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    // Enter password change mode
    await waitFor(() => {
      fireEvent.click(screen.getByRole('button', { name: /Change Password/i }));
    });

    // Fill password form with mismatched passwords
    fireEvent.change(screen.getByTestId('current-password'), {
      target: { value: 'oldPassword' },
    });
    fireEvent.change(screen.getByTestId('new-password'), {
      target: { value: 'newPassword' },
    });
    fireEvent.change(screen.getByTestId('confirm-password'), {
      target: { value: 'differentPassword' },
    });

    // Submit password form
    fireEvent.click(screen.getByRole('button', { name: /Save/i }));

    await waitFor(() => {
      expect(screen.getByText(/New passwords do not match/i)).toBeInTheDocument();
      expect(mockAxiosInstance.post).not.toHaveBeenCalled();
    }, { timeout: 10000 });
  });

  it('uploads avatar successfully', async () => {
    mockAxiosInstance.post.mockResolvedValueOnce({
      data: { avatarUrl: 'https://new-avatar-url.com' },
    });

    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    // Enter edit mode
    await waitFor(() => {
      fireEvent.click(screen.getByRole('button', { name: /Edit/i }));
    });

    // Simulate avatar file upload
    const file = new File(['avatar'], 'avatar.png', { type: 'image/png' });
    const input = screen.getByTestId('avatar-upload');
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() => {
      expect(mockAxiosInstance.post).toHaveBeenCalledWith(
        'http://localhost:8083/api/user/upload-avatar',
        expect.any(FormData),
        expect.any(Object)
      );
      expect(
        screen.getByText(/Avatar updated successfully!/i)
      ).toBeInTheDocument();
    }, { timeout: 10000 });
  });




  it('does not fetch project roles if conditions are not met', async () => {
    (useAuth as jest.Mock).mockReturnValue({
      accessToken: null,
      isLoading: false,
    });

    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(mockAxiosInstance.get).not.toHaveBeenCalledWith(
        'http://localhost:8085/api/projects/by-user',
        expect.any(Object)
      );
      expect(screen.getByText(/No roles in projects/i)).toBeInTheDocument();
    }, { timeout: 10000 });
  });

  it('updates notification preferences', async () => {
    mockAxiosInstance.put.mockResolvedValueOnce({ data: {} });

    render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    // Enter edit mode
    await waitFor(() => {
      fireEvent.click(screen.getByRole('button', { name: /Edit/i }));
    });

    // Uncheck email notifications
    fireEvent.click(screen.getByTestId('email-notifications'));

    // Submit form
    fireEvent.click(screen.getByRole('button', { name: /Save/i }));

    await waitFor(() => {
      expect(mockAxiosInstance.put).toHaveBeenCalledWith(
        'http://localhost:8083/api/update',
        expect.objectContaining({
          notificationPreferences: expect.objectContaining({
            emailNotifications: false,
            taskUpdates: true,
            deadlineReminders: true,
          }),
        }),
        expect.any(Object)
      );
      expect(
        screen.getByText(/Profile updated successfully!/i)
      ).toBeInTheDocument();
    }, { timeout: 10000 });
  });

  it('is accessible', async () => {
    const { container } = render(
      <BrowserRouter>
        <ProfilePage />
      </BrowserRouter>
    );

    await waitFor(
      async () => {
        const results = await axe(container);
        expect(results).toHaveNoViolations();
      },
      { timeout: 10000 }
    );
  });
});

