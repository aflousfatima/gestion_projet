import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { axe, toHaveNoViolations } from "jest-axe";
import { useSearchParams, useRouter } from "next/navigation";
import axios, { AxiosError, AxiosHeaders } from "axios";
import SigninPage from "../../../../app/authentification/signin/page";
import { useAuth } from "../../../../context/AuthContext";

expect.extend(toHaveNoViolations);


jest.mock("next/link", () => {
  const MockedLink = ({
    children,
    href,
    ...props
  }: {
    children: React.ReactNode;
    href: string;
  }) => (
    <a href={href} {...props}>
      {children}
    </a>
  );
  MockedLink.displayName = "Link";
  return MockedLink;
});

jest.mock("next/navigation", () => ({
  useSearchParams: jest.fn(),
  useRouter: jest.fn(),
}));

jest.mock("axios");
const mockedAxios = axios as jest.Mocked<typeof axios>;

// Mock useAuth context
jest.mock("../../../../context/AuthContext", () => ({
  useAuth: jest.fn(),
}));

jest
  .spyOn(axios, "isAxiosError")
  .mockImplementation((error: unknown): boolean => {
    if (error instanceof AxiosError) return true;
    if (error && typeof error === "object" && "isAxiosError" in error) {
      return (error as { isAxiosError: boolean }).isAxiosError === true;
    }
    return false;
  });

// Define mocks globally
const mockPush = jest.fn();
const mockPrefetch = jest.fn();
const mockLogin = jest.fn();
const mockSearchParams = {
  get: jest.fn(),
};

describe("SigninPage Component", () => {
  beforeEach(() => {
    process.env.NEXT_PUBLIC_API_AUTHENTICATION_SERVICE_URL =
      "http://localhost:8080";
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue({
      push: mockPush,
      replace: mockPush,
      prefetch: mockPrefetch,
    });
    (useSearchParams as jest.Mock).mockReturnValue(mockSearchParams);
    (useAuth as jest.Mock).mockReturnValue({
      login: mockLogin,
    });
    mockSearchParams.get.mockReturnValue(null);
  });

  afterEach(() => {
    jest.useRealTimers(); // Reset timers after each test
  });

  it("renders the SigninPage form correctly", async () => {
    render(<SigninPage />);

    await waitFor(() => {
      expect(
        screen.getByRole("heading", { name: /Welcome Back to AGILIA/i })
      ).toBeInTheDocument();
      expect(
        screen.getByText(/Sign in to access your workspace/i)
      ).toBeInTheDocument();
      expect(screen.getByAltText("Google")).toBeInTheDocument();
      expect(screen.getByAltText("Facebook")).toBeInTheDocument();
      expect(screen.getByAltText("Microsoft")).toBeInTheDocument();
      expect(screen.getByAltText("Apple")).toBeInTheDocument();
      expect(screen.getByPlaceholderText("e-mail")).toBeInTheDocument();
      expect(screen.getByPlaceholderText("password")).toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: /Sign In/i })
      ).toBeInTheDocument();
      expect(screen.getByRole("link", { name: /Sign Up/i })).toHaveAttribute(
        "href",
        "/authentification/signup"
      );
      expect(
        screen.getByRole("link", { name: /Forgot your password ?/i })
      ).toHaveAttribute("href", "/authentification/signup");
      expect(screen.getByAltText("Signup Illustration")).toBeInTheDocument();
    });
  });

  it("updates form inputs correctly", async () => {
    render(<SigninPage />);

    await waitFor(() => {
      fireEvent.change(screen.getByPlaceholderText("e-mail"), {
        target: { value: "john@example.com" },
      });
      fireEvent.change(screen.getByPlaceholderText("password"), {
        target: { value: "password123" },
      });

      expect(screen.getByPlaceholderText("e-mail")).toHaveValue(
        "john@example.com"
      );
      expect(screen.getByPlaceholderText("password")).toHaveValue(
        "password123"
      );
    });
  });

  it("handles successful form submission", async () => {
    mockedAxios.post.mockResolvedValueOnce({
      status: 200,
      statusText: "OK",
      headers: {},
      config: { headers: new AxiosHeaders() },
      data: { access_token: "mock-token" },
    });
    render(<SigninPage />);

    fireEvent.change(screen.getByPlaceholderText("e-mail"), {
      target: { value: "john@example.com" },
    });
    fireEvent.change(screen.getByPlaceholderText("password"), {
      target: { value: "password123" },
    });

    fireEvent.click(screen.getByRole("button", { name: /Sign In/i }));

    await waitFor(() => {
      expect(screen.getByText(/Login Successful!/i)).toBeInTheDocument();
      expect(mockLogin).toHaveBeenCalledWith("mock-token");
    });

    await waitFor(
      () => {
        expect(mockPush).toHaveBeenCalledWith("/company/company-choice");
      },
      { timeout: 2000 }
    );
  });

  it("handles form submission error for invalid credentials", async () => {
    const error = new AxiosError("Request failed", "401");
    error.isAxiosError = true;
    error.response = {
      status: 401,
      statusText: "Unauthorized",
      headers: {},
      config: { headers: new AxiosHeaders() },
      data: "Invalid credentials",
    };
    mockedAxios.post.mockRejectedValueOnce(error);
    render(<SigninPage />);

    fireEvent.change(screen.getByPlaceholderText("e-mail"), {
      target: { value: "john@example.com" },
    });
    fireEvent.change(screen.getByPlaceholderText("password"), {
      target: { value: "wrongpassword" },
    });

    fireEvent.click(screen.getByRole("button", { name: /Sign In/i }));

    await waitFor(() => {
      expect(
        screen.getByText(/Error trying to connect. Please Retry./i)
      ).toBeInTheDocument();
    });
  });

  it("is accessible", async () => {
    const { container } = render(<SigninPage />);
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});
