import React from "react";
import {
  render,
  screen,
  fireEvent,
  waitFor,
  act,
} from "@testing-library/react";
import { axe, toHaveNoViolations } from "jest-axe";
import { AxiosHeaders } from "axios";
import CompanyRegistrationPage from "../../../../app/company/company-registration/page";
import { useRouter } from "next/navigation";
import useAxios from "../../../../hooks/useAxios";

// Mock the PROJECT_SERVICE_URL import
jest.mock("../../../../config/useApi", () => ({
  PROJECT_SERVICE_URL: "http://localhost:8085",
}));

expect.extend(toHaveNoViolations);

// Mock next/navigation
jest.mock("next/navigation", () => ({
  useRouter: jest.fn(),
}));

// Mock useAxios
jest.mock("../../../../hooks/useAxios", () => jest.fn());

const mockAxiosInstance = {
  post: jest.fn(),
  get: jest.fn(),
};
const mockPush = jest.fn();
const mockPrefetch = jest.fn();

describe("CompanyRegistrationPage Component", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue({
      push: mockPush,
      replace: jest.fn(),
      prefetch: mockPrefetch,
    });
    (useAxios as jest.Mock).mockReturnValue(mockAxiosInstance);
    process.env.NEXT_PUBLIC_PROJECT_SERVICE_URL = "http://localhost:8085";
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("renders step 1 (Your Company) correctly", async () => {
    render(<CompanyRegistrationPage />);
    await waitFor(() => {
      expect(
        screen.getByRole("heading", { name: /Welcome to Your Setup/i })
      ).toBeInTheDocument();
      expect(
        screen.getByText(/Complete the 4 steps to get started/i)
      ).toBeInTheDocument();
      expect(
        screen.getByRole("heading", { name: /Your Company/i })
      ).toBeInTheDocument();
      expect(screen.getByLabelText(/Organization Name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Industry/i)).toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: /Next →/i })
      ).toBeInTheDocument();
    });
  });

  it("updates form inputs in step 1", async () => {
    render(<CompanyRegistrationPage />);
    await waitFor(() => {
      fireEvent.change(screen.getByLabelText(/Organization Name/i), {
        target: { value: "Test Corp" },
      });
      fireEvent.change(screen.getByLabelText(/Industry/i), {
        target: { value: "Tech" },
      });
      expect(screen.getByLabelText(/Organization Name/i)).toHaveValue(
        "Test Corp"
      );
      expect(screen.getByLabelText(/Industry/i)).toHaveValue("Tech");
    });
  });

  it("navigates to step 2 (Your Team) and renders correctly", async () => {
    render(<CompanyRegistrationPage />);
    await waitFor(() => {
      fireEvent.click(screen.getByRole("button", { name: /Next →/i }));
      expect(
        screen.getByRole("heading", { name: /Your Team/i })
      ).toBeInTheDocument();
      expect(screen.getByLabelText(/Team Name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Number of Employees/i)).toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: /Next →/i })
      ).toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: /← Back/i })
      ).toBeInTheDocument();
    });
  });

  it("updates form inputs in step 2", async () => {
    render(<CompanyRegistrationPage />);
    await waitFor(() => {
      fireEvent.click(screen.getByRole("button", { name: /Next →/i }));
      fireEvent.change(screen.getByLabelText(/Team Name/i), {
        target: { value: "Dev Team" },
      });
      fireEvent.change(screen.getByLabelText(/Number of Employees/i), {
        target: { value: "50" },
      });
      expect(screen.getByLabelText(/Team Name/i)).toHaveValue("Dev Team");
      expect(screen.getByLabelText(/Number of Employees/i)).toHaveValue("50");
    });
  });

  it("navigates to step 3 (About You) and renders correctly", async () => {
    render(<CompanyRegistrationPage />);
    await waitFor(() => {
      fireEvent.click(screen.getByRole("button", { name: /Next →/i }));
      fireEvent.click(screen.getByRole("button", { name: /Next →/i }));
      expect(
        screen.getByRole("heading", { name: /About You/i })
      ).toBeInTheDocument();
      expect(screen.getByLabelText(/Department/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Your Role/i)).toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: /Next →/i })
      ).toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: /← Back/i })
      ).toBeInTheDocument();
    });
  });

  it("updates form inputs in step 3", async () => {
    render(<CompanyRegistrationPage />);
    await waitFor(() => {
      fireEvent.click(screen.getByRole("button", { name: /Next →/i }));
      fireEvent.click(screen.getByRole("button", { name: /Next →/i }));
      fireEvent.change(screen.getByLabelText(/Department/i), {
        target: { value: "Engineering" },
      });
      fireEvent.change(screen.getByLabelText(/Your Role/i), {
        target: { value: "Manager" },
      });
      expect(screen.getByLabelText(/Department/i)).toHaveValue("Engineering");
      expect(screen.getByLabelText(/Your Role/i)).toHaveValue("Manager");
    });
  });

  it("navigates to step 4 (Your Project) and renders correctly", async () => {
    render(<CompanyRegistrationPage />);
    await waitFor(() => {
      fireEvent.click(screen.getByRole("button", { name: /Next →/i }));
      fireEvent.click(screen.getByRole("button", { name: /Next →/i }));
      fireEvent.click(screen.getByRole("button", { name: /Next →/i }));
      expect(
        screen.getByRole("heading", { name: /Your Project/i })
      ).toBeInTheDocument();
      expect(screen.getByLabelText(/Project Name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Description/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Start Date/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Deadline/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Status/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Phase/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Priority/i)).toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: /Launch/i })
      ).toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: /← Back/i })
      ).toBeInTheDocument();
    });
  });

  it("updates form inputs in step 4", async () => {
    render(<CompanyRegistrationPage />);
    await waitFor(() => {
      fireEvent.click(screen.getByRole("button", { name: /Next →/i }));
      fireEvent.click(screen.getByRole("button", { name: /Next →/i }));
      fireEvent.click(screen.getByRole("button", { name: /Next →/i }));
      fireEvent.change(screen.getByLabelText(/Project Name/i), {
        target: { value: "Project X" },
      });
      fireEvent.change(screen.getByLabelText(/Description/i), {
        target: { value: "A new project" },
      });
      fireEvent.change(screen.getByLabelText(/Start Date/i), {
        target: { value: "2025-06-10" },
      });
      fireEvent.change(screen.getByLabelText(/Deadline/i), {
        target: { value: "2025-12-31" },
      });
      fireEvent.change(screen.getByLabelText(/Status/i), {
        target: { value: "START" },
      });
      fireEvent.change(screen.getByLabelText(/Phase/i), {
        target: { value: "PLANIFICATION" },
      });
      fireEvent.change(screen.getByLabelText(/Priority/i), {
        target: { value: "HIGH" },
      });
      expect(screen.getByLabelText(/Project Name/i)).toHaveValue("Project X");
      expect(screen.getByLabelText(/Description/i)).toHaveValue(
        "A new project"
      );
      expect(screen.getByLabelText(/Start Date/i)).toHaveValue("2025-06-10");
      expect(screen.getByLabelText(/Deadline/i)).toHaveValue("2025-12-31");
      expect(screen.getByLabelText(/Status/i)).toHaveValue("START");
      expect(screen.getByLabelText(/Phase/i)).toHaveValue("PLANIFICATION");
      expect(screen.getByLabelText(/Priority/i)).toHaveValue("HIGH");
    });
  });

  it("navigates back to previous steps", async () => {
    render(<CompanyRegistrationPage />);
    await waitFor(() => {
      fireEvent.click(screen.getByRole("button", { name: /Next →/i }));
      fireEvent.click(screen.getByRole("button", { name: /Next →/i }));
      fireEvent.click(screen.getByRole("button", { name: /← Back/i }));
      expect(
        screen.getByRole("heading", { name: /Your Team/i })
      ).toBeInTheDocument();
      fireEvent.click(screen.getByRole("button", { name: /← Back/i }));
      expect(
        screen.getByRole("heading", { name: /Your Company/i })
      ).toBeInTheDocument();
    });
  });

  it("handles successful form submission", async () => {
    mockAxiosInstance.post.mockResolvedValueOnce({
      status: 201,
      statusText: "Created",
      headers: {},
      config: { headers: new AxiosHeaders() },
      data: { message: "Company and project created" },
    });
    render(<CompanyRegistrationPage />);
    fireEvent.click(screen.getByRole("button", { name: /Next →/i }));
    fireEvent.click(screen.getByRole("button", { name: /Next →/i }));
    fireEvent.click(screen.getByRole("button", { name: /Next →/i }));
    await waitFor(() => {
      fireEvent.change(screen.getByLabelText(/Project Name/i), {
        target: { value: "Project X" },
      });
      fireEvent.change(screen.getByLabelText(/Description/i), {
        target: { value: "A new project" },
      });
      fireEvent.change(screen.getByLabelText(/Start Date/i), {
        target: { value: "2025-06-10" },
      });
      fireEvent.change(screen.getByLabelText(/Deadline/i), {
        target: { value: "2025-12-31" },
      });
      fireEvent.change(screen.getByLabelText(/Status/i), {
        target: { value: "START" },
      });
      fireEvent.change(screen.getByLabelText(/Phase/i), {
        target: { value: "PLANIFICATION" },
      });
      fireEvent.change(screen.getByLabelText(/Priority/i), {
        target: { value: "HIGH" },
      });
    });
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /Launch/i }));
    });
    await waitFor(() => {
      expect(mockAxiosInstance.post).toHaveBeenCalledWith(
        `${process.env.NEXT_PUBLIC_PROJECT_SERVICE_URL}/api/create-initial-project`,
        expect.objectContaining({
          companyName: "",
          industry: "",
          teamName: "",
          numEmployees: "",
          department: "",
          role: "",
          projectName: "Project X",
          projectDescription: "A new project",
          creationDate: "",
          startDate: "2025-06-10",
          deadline: "2025-12-31",
          status: "START",
          phase: "PLANIFICATION",
          priority: "HIGH",
        })
      );
      expect(
        screen.getByText(/Company created successfully!/i)
      ).toBeInTheDocument();
    });
  });

  it("handles form submission error", async () => {
    const error = {
      isAxiosError: true,
      response: {
        status: 400,
        statusText: "Bad Request",
        headers: {},
        config: { headers: new AxiosHeaders() },
        data: "Invalid project data",
      },
    };
    mockAxiosInstance.post.mockRejectedValueOnce(error);
    render(<CompanyRegistrationPage />);
    fireEvent.click(screen.getByRole("button", { name: /Next →/i }));
    fireEvent.click(screen.getByRole("button", { name: /Next →/i }));
    fireEvent.click(screen.getByRole("button", { name: /Next →/i }));
    await waitFor(() => {
      fireEvent.change(screen.getByLabelText(/Project Name/i), {
        target: { value: "Project X" },
      });
      fireEvent.change(screen.getByLabelText(/Description/i), {
        target: { value: "A new project" },
      });
      fireEvent.change(screen.getByLabelText(/Start Date/i), {
        target: { value: "2025-06-10" },
      });
      fireEvent.change(screen.getByLabelText(/Deadline/i), {
        target: { value: "2025-12-31" },
      });
      fireEvent.change(screen.getByLabelText(/Status/i), {
        target: { value: "START" },
      });
      fireEvent.change(screen.getByLabelText(/Phase/i), {
        target: { value: "PLANIFICATION" },
      });
      fireEvent.change(screen.getByLabelText(/Priority/i), {
        target: { value: "HIGH" },
      });
    });
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /Launch/i }));
    });
    await waitFor(() => {
      expect(mockAxiosInstance.post).toHaveBeenCalledWith(
        `${process.env.NEXT_PUBLIC_PROJECT_SERVICE_URL}/api/create-initial-project`,
        expect.objectContaining({
          companyName: "",
          industry: "",
          teamName: "",
          numEmployees: "",
          department: "",
          role: "",
          projectName: "Project X",
          projectDescription: "A new project",
          creationDate: "",
          startDate: "2025-06-10",
          deadline: "2025-12-31",
          status: "START",
          phase: "PLANIFICATION",
          priority: "HIGH",
        })
      );
      expect(
        screen.queryByText(/Company created successfully!/i)
      ).not.toBeInTheDocument();
      expect(
        screen.getByRole("heading", { name: /Your Project/i })
      ).toBeInTheDocument();
    });
  });

  it("is accessible", async () => {
    const { container } = render(<CompanyRegistrationPage />);
    await waitFor(
      async () => {
        const results = await axe(container);
        expect(results).toHaveNoViolations();
      },
      { timeout: 10000 }
    );
  }, 10000);
});
