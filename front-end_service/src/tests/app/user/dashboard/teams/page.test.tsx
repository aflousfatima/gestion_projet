import React from "react";
import {
  render,
  screen,
  fireEvent,
  waitFor,
  act,
} from "@testing-library/react";
import { axe, toHaveNoViolations } from "jest-axe";
import Teams from "../../../../../app/user/dashboard/teams/page";
import { useAuth } from "../../../../../context/AuthContext";
import useAxios from "../../../../../hooks/useAxios";
import { useProjects } from "../../../../../hooks/useProjects";
import axios from "axios";

// Extend jest-axe matchers
expect.extend(toHaveNoViolations);

// Mock dependencies
jest.mock("../../../../../context/AuthContext", () => ({
  useAuth: jest.fn(),
}));
jest.mock("../../../../../hooks/useAxios", () => jest.fn());
jest.mock("../../../../../hooks/useProjects", () => ({
  useProjects: jest.fn(),
}));
jest.mock("axios");
jest.mock("../../../../../components/ProtectedRoute", () => {
  const MockProtectedRoute = ({ children }: { children: React.ReactNode }) => (
    <div>{children}</div>
  );
  MockProtectedRoute.displayName = "MockProtectedRoute";
  return MockProtectedRoute;
});

describe("Teams Component", () => {
  const mockAccessToken = "mock-token";
  const mockProjects = [
    {
      id: 1,
      name: "Project A",
      description: "Description A",
      creationDate: "2025-01-01",
      startDate: "2025-02-01",
      deadline: "2025-12-31",
      status: "IN_PROGRESS",
      phase: "DEVELOPPEMENT",
      priority: "HIGH",
    },
    {
      id: 2,
      name: "Project B",
      description: "Description B",
      creationDate: "2025-03-01",
      startDate: "2025-04-01",
      deadline: "2025-11-30",
      status: "START",
      phase: "PLANIFICATION",
      priority: "MEDIUM",
    },
  ];
  const mockTeamMembers = [
    {
      id: "1",
      firstName: "John",
      lastName: "Doe",
      role: "DEVELOPER",
      project: "Project A",
      avatar: "/avatar1.png",
    },
    {
      id: "2",
      firstName: "Jane",
      lastName: "Smith",
      role: "TESTER",
      project: "Project B",
      avatar: "/avatar2.png",
    },
  ];
  const mockCompanyName = "Test Corp";
  const mockAuthId = "auth123";

  const mockAxiosInstance = {
    get: jest.fn(),
    post: jest.fn(),
  };

  beforeEach(async () => {
    jest.clearAllMocks();
    (useAuth as jest.Mock).mockReturnValue({
      accessToken: mockAccessToken,
      isLoading: false,
    });
    (useAxios as jest.Mock).mockReturnValue(mockAxiosInstance);
    (useProjects as jest.Mock).mockReturnValue({
      projects: mockProjects,
      loading: false,
      error: null,
    });
    mockAxiosInstance.get.mockImplementation((url: string) => {
      console.log("Mock GET called for:", url);
      if (url.includes("/api/user-id")) {
        return Promise.resolve({ data: mockAuthId });
      }
      if (url.includes("/api/projects/by-manager")) {
        return Promise.resolve({ data: { companyName: mockCompanyName } });
      }
      if (url.includes("/api/team-members")) {
        return Promise.resolve({ data: mockTeamMembers });
      }
      return Promise.reject(new Error("Unknown URL"));
    });
    mockAxiosInstance.post.mockResolvedValue({ data: {} });
  });

  it("renders loading state correctly", async () => {
    (useProjects as jest.Mock).mockReturnValue({
      projects: [],
      loading: true,
      error: null,
    });

    await act(async () => {
      render(<Teams />);
    });

    await waitFor(() => {
      expect(screen.getByRole("img", { name: /Loading/i })).toBeInTheDocument();
    });
  });

  it("renders error state correctly", async () => {
    (useProjects as jest.Mock).mockReturnValue({
      projects: [],
      loading: false,
      error: "Failed to load projects",
    });

    await act(async () => {
      render(<Teams />);
    });

    await waitFor(() => {
      expect(screen.getByText("Erreur")).toBeInTheDocument();
      expect(screen.getByText("Failed to load projects")).toBeInTheDocument();
    });
  });

  it("renders team members and company name correctly", async () => {
    await act(async () => {
      render(<Teams />);
    });

    await waitFor(() => {
      expect(
        screen.getByText(`Your Team at ${mockCompanyName}`)
      ).toBeInTheDocument();
      expect(
        screen.getByText(
          `See all project members in one place for ${mockCompanyName}.`
        )
      ).toBeInTheDocument();

      expect(screen.getByText("John Doe")).toBeInTheDocument();
      expect(screen.getByText("Rôle : DEVELOPER")).toBeInTheDocument();
      expect(screen.getByText("Projet : Project A")).toBeInTheDocument();
      expect(screen.getByRole("img", { name: "John Doe" })).toHaveAttribute(
        "src",
        "/avatar1.png"
      );

      expect(screen.getByText("Jane Smith")).toBeInTheDocument();
      expect(screen.getByText("Rôle : TESTER")).toBeInTheDocument();
      expect(screen.getByText("Projet : Project B")).toBeInTheDocument();
      expect(screen.getByRole("img", { name: "Jane Smith" })).toHaveAttribute(
        "src",
        "/avatar2.png"
      );

      expect(
        screen.getByRole("button", { name: /Invite collegues/i })
      ).toBeInTheDocument();
    });
  });

  it("renders no team members message when team is empty", async () => {
    mockAxiosInstance.get.mockImplementation((url: string) => {
      if (url.includes("/api/team-members")) {
        return Promise.resolve({ data: [] });
      }
      if (url.includes("/api/user-id")) {
        return Promise.resolve({ data: mockAuthId });
      }
      if (url.includes("/api/projects/by-manager")) {
        return Promise.resolve({ data: { companyName: mockCompanyName } });
      }
      return Promise.reject(new Error("Unknown URL"));
    });

    await act(async () => {
      render(<Teams />);
    });

    await waitFor(() => {
      expect(screen.getByText("No Team Memeber Found.")).toBeInTheDocument();
    });
  });

  it("renders team loading state correctly", async () => {
    (useAuth as jest.Mock).mockReturnValue({
      accessToken: mockAccessToken,
      isLoading: true,
    });

    await act(async () => {
      render(<Teams />);
    });

    await waitFor(() => {
      expect(screen.getByRole("img", { name: /Loading/i })).toBeInTheDocument();
    });
  });

  it("renders team error state correctly", async () => {
    mockAxiosInstance.get.mockImplementation((url: string) => {
      if (url.includes("/api/team-members")) {
        return Promise.reject(new Error("Failed to fetch team members"));
      }
      if (url.includes("/api/user-id")) {
        return Promise.resolve({ data: mockAuthId });
      }
      if (url.includes("/api/projects/by-manager")) {
        return Promise.resolve({ data: { companyName: mockCompanyName } });
      }
      return Promise.reject(new Error("Unknown URL"));
    });

    await act(async () => {
      render(<Teams />);
    });

    await waitFor(() => {
      expect(
        screen.getByText(
          /Erreur lors de la récupération des membres de l'équipe/
        )
      ).toBeInTheDocument();
    });
  });

  it("opens and closes invite modal correctly", async () => {
    await act(async () => {
      render(<Teams />);
    });

    await act(async () => {
      fireEvent.click(
        screen.getByRole("button", { name: /Invite collegues/i })
      );
    });

    await waitFor(() => {
      expect(screen.getByText("Inviter un collègue")).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /Annuler/i }));
    });

    await waitFor(() => {
      expect(screen.queryByText("Inviter un collègue")).not.toBeInTheDocument();
    });
  });

  it("submits invite form correctly", async () => {
    await act(async () => {
      render(<Teams />);
    });

    await act(async () => {
      fireEvent.click(
        screen.getByRole("button", { name: /Invite collegues/i })
      );
    });

    await waitFor(() => {
      expect(screen.getByText("Inviter un collègue")).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.change(screen.getByLabelText("Email"), {
        target: { value: "test@example.com" },
      });
      fireEvent.change(screen.getByLabelText("Projet"), {
        target: { value: "1" },
      });
      fireEvent.change(screen.getByLabelText("Role"), {
        target: { value: "TESTER" },
      });
      fireEvent.submit(screen.getByRole("form"));
    });

    await waitFor(() => {
      expect(mockAxiosInstance.post).toHaveBeenCalledWith(
        "http://localhost:8083/api/invitations",
        {
          email: "test@example.com",
          role: "TESTER",
          projectId: "1",
          entreprise: mockCompanyName,
          headers: { Authorization: `Bearer ${mockAccessToken}` },
        }
      );
      expect(mockAxiosInstance.get).toHaveBeenCalledWith(
        "http://localhost:8083/api/team-members"
      );
      expect(
        screen.getByText("Invitation envoyée avec succès !")
      ).toBeInTheDocument();
      expect(screen.queryByText("Inviter un collègue")).not.toBeInTheDocument();
    });
  });

  it("handles invite form error correctly", async () => {
    mockAxiosInstance.post.mockRejectedValueOnce({
      response: { data: "Invalid email" },
    });

    await act(async () => {
      render(<Teams />);
    });

    await act(async () => {
      fireEvent.click(
        screen.getByRole("button", { name: /Invite collegues/i })
      );
      fireEvent.change(screen.getByLabelText("Email"), {
        target: { value: "invalid-email" },
      });
      fireEvent.submit(screen.getByRole("form"));
    });

    await waitFor(() => {
      expect(
        screen.getByText(
          /Erreur lors de l'envoi de l'invitation : Invalid email/
        )
      ).toBeInTheDocument();
    });
  });

  it("closes modal on overlay click", async () => {
    await act(async () => {
      render(<Teams />);
    });

    await act(async () => {
      fireEvent.click(
        screen.getByRole("button", { name: /Invite collegues/i })
      );
    });

    await waitFor(() => {
      expect(screen.getByText("Inviter un collègue")).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.click(screen.getByTestId("modal-overlay"));
    });

    await waitFor(() => {
      expect(screen.queryByText("Inviter un collègue")).not.toBeInTheDocument();
    });
  });

  it("selects default project correctly", async () => {
    await act(async () => {
      render(<Teams />);
    });

    await act(async () => {
      fireEvent.click(
        screen.getByRole("button", { name: /Invite collegues/i })
      );
    });

    await waitFor(() => {
      expect(screen.getByLabelText("Projet")).toHaveValue("1");
    });
  });

  it("handles no projects available", async () => {
    (useProjects as jest.Mock).mockReturnValue({
      projects: [],
      loading: false,
      error: null,
    });

    await act(async () => {
      render(<Teams />);
    });

    await act(async () => {
      fireEvent.click(
        screen.getByRole("button", { name: /Invite collegues/i })
      );
    });

    await waitFor(() => {
      expect(screen.getByText("No project available.")).toBeInTheDocument();
    });
  });


});
