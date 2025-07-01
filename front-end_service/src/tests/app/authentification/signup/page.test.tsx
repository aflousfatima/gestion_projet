/* eslint-disable */
import React from "react";
import {
  render,
  screen,
  fireEvent,
  waitFor,
  act,
} from "@testing-library/react";
import { axe, toHaveNoViolations } from "jest-axe";
import axios, { AxiosError, AxiosHeaders } from "axios";
import SignupPage from "../../../../app/authentification/signup/page";
import { useRouter } from "next/navigation";

expect.extend(toHaveNoViolations);

// Mock next/link to avoid async updates from use-intersection
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

jest.mock("axios");
const mockedAxios = axios as jest.Mocked<typeof axios>;

// Mock next/navigation
jest.mock("next/navigation", () => ({
  useRouter: jest.fn(),
}));

// Mock axios.isAxiosError
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

describe("SignupPage Component", () => {
  let mockURLSearchParams: jest.Mock;

  beforeEach(() => {
    process.env.NEXT_PUBLIC_API_AUTHENTICATION_SERVICE_URL =
      "http://localhost:8080";
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue({
      push: mockPush,
      replace: mockPush,
      prefetch: mockPrefetch,
    });
    // Mock URLSearchParams
    mockURLSearchParams = jest.fn().mockReturnValue({
      get: jest.fn().mockReturnValue(null), // Default: no token
    });
    global.URLSearchParams = mockURLSearchParams as any;
    // Mock setTimeout
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks(); // Restore mocks to avoid affecting other tests
  });

  it("renders the SignupPage form correctly", async () => {
    render(<SignupPage />);

    await waitFor(() => {
      expect(
        screen.getByRole("heading", { name: /Get started with AGILIA/i })
      ).toBeInTheDocument();
      expect(
        screen.getByText(/Free for up to 10 users - no credit card needed/i)
      ).toBeInTheDocument();
      expect(screen.getByAltText("Google")).toBeInTheDocument();
      expect(screen.getByAltText("Facebook")).toBeInTheDocument();
      expect(screen.getByAltText("Microsoft")).toBeInTheDocument();
      expect(screen.getByAltText("Apple")).toBeInTheDocument();
      expect(screen.getByPlaceholderText("First Name")).toBeInTheDocument();
      expect(screen.getByPlaceholderText("Last Name")).toBeInTheDocument();
      expect(screen.getByPlaceholderText("Username")).toBeInTheDocument();
      expect(
        screen.getByPlaceholderText("Professional email")
      ).toBeInTheDocument();
      expect(
        screen.getByPlaceholderText("Minimum 8 characters")
      ).toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: /Sign Up/i })
      ).toBeInTheDocument();
      expect(screen.getByRole("link", { name: /Sign In/i })).toHaveAttribute(
        "href",
        "/authentification/signin"
      );
      expect(screen.getByAltText("Signup Illustration")).toBeInTheDocument();
    });
  });

  it("updates form inputs correctly", async () => {
    render(<SignupPage />);

    await waitFor(() => {
      fireEvent.change(screen.getByPlaceholderText("First Name"), {
        target: { value: "John" },
      });
      fireEvent.change(screen.getByPlaceholderText("Last Name"), {
        target: { value: "Doe" },
      });
      fireEvent.change(screen.getByPlaceholderText("Username"), {
        target: { value: "johndoe" },
      });
      fireEvent.change(screen.getByPlaceholderText("Professional email"), {
        target: { value: "john@example.com" },
      });
      fireEvent.change(screen.getByPlaceholderText("Minimum 8 characters"), {
        target: { value: "password123" },
      });
      fireEvent.click(
        screen.getByLabelText(/I agree to receive marketing emails/i)
      );
      fireEvent.click(screen.getByLabelText(/I agree to the terms of use/i));

      expect(screen.getByPlaceholderText("First Name")).toHaveValue("John");
      expect(screen.getByPlaceholderText("Last Name")).toHaveValue("Doe");
      expect(screen.getByPlaceholderText("Username")).toHaveValue("johndoe");
      expect(screen.getByPlaceholderText("Professional email")).toHaveValue(
        "john@example.com"
      );
      expect(screen.getByPlaceholderText("Minimum 8 characters")).toHaveValue(
        "password123"
      );
      expect(
        screen.getByLabelText(/I agree to receive marketing emails/i)
      ).toBeChecked();
      expect(
        screen.getByLabelText(/I agree to the terms of use/i)
      ).toBeChecked();
    });
  });

  it("handles successful form submission", async () => {
    mockedAxios.post.mockResolvedValueOnce({
      status: 201,
      statusText: "Created",
      headers: {},
      config: { headers: new AxiosHeaders() },
      data: { message: "User created" },
    });
    render(<SignupPage />);

    fireEvent.change(screen.getByPlaceholderText("First Name"), {
      target: { value: "John" },
    });
    fireEvent.change(screen.getByPlaceholderText("Last Name"), {
      target: { value: "Doe" },
    });
    fireEvent.change(screen.getByPlaceholderText("Username"), {
      target: { value: "johndoe" },
    });
    fireEvent.change(screen.getByPlaceholderText("Professional email"), {
      target: { value: "john@example.com" },
    });
    fireEvent.change(screen.getByPlaceholderText("Minimum 8 characters"), {
      target: { value: "password123" },
    });
    fireEvent.click(
      screen.getByLabelText(/I agree to receive marketing emails/i)
    );
    fireEvent.click(screen.getByLabelText(/I agree to the terms of use/i));

    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: /Sign Up/i }));
      jest.runAllTimers(); // Advance timers for async operations
    });

    await waitFor(
      () => {
        expect(
          screen.getByRole("heading", { name: /Success/i })
        ).toBeInTheDocument();
        expect(
          screen.getByText(/Registration successful!/i)
        ).toBeInTheDocument();
        expect(mockPush).toHaveBeenCalledWith("/authentification/signin");
      },
      { timeout: 5000 } // Increase timeout for redirect
    );
  });

  it("handles form submission error for existing email", async () => {
    const error = new AxiosError("Request failed", "400");
    error.isAxiosError = true;
    error.response = {
      status: 400,
      statusText: "Bad Request",
      headers: {},
      config: { headers: new AxiosHeaders() },
      data: { message: "Email already exists" },
    };
    mockedAxios.post.mockRejectedValueOnce(error);
    render(<SignupPage />);

    fireEvent.change(screen.getByPlaceholderText("First Name"), {
      target: { value: "John" },
    });
    fireEvent.change(screen.getByPlaceholderText("Last Name"), {
      target: { value: "Doe" },
    });
    fireEvent.change(screen.getByPlaceholderText("Username"), {
      target: { value: "johndoe" },
    });
    fireEvent.change(screen.getByPlaceholderText("Professional email"), {
      target: { value: "john@example.com" },
    });
    fireEvent.change(screen.getByPlaceholderText("Minimum 8 characters"), {
      target: { value: "password123" },
    });
    fireEvent.click(
      screen.getByLabelText(/I agree to receive marketing emails/i)
    );
    fireEvent.click(screen.getByLabelText(/I agree to the terms of use/i));

    fireEvent.click(screen.getByRole("button", { name: /Sign Up/i }));

    await waitFor(() => {
      expect(
        screen.getByRole("heading", { name: /Error/i })
      ).toBeInTheDocument();
      expect(screen.getByText(/Email already exists/i)).toBeInTheDocument();
    });
  });

  it("handles valid invitation token", async () => {
    mockURLSearchParams.mockReturnValue({
      get: jest.fn().mockImplementation((key) => {
        if (key === "token") return "valid-token";
        return null;
      }),
    });
    mockedAxios.get.mockResolvedValueOnce({
      status: 200,
      statusText: "OK",
      headers: {},
      config: { headers: new AxiosHeaders() },
      data: {
        email: "invited@example.com",
        role: "member",
        entrepriseId: "123",
        project: "Test Project",
      },
    });
    render(<SignupPage />);

    await waitFor(() => {
      expect(screen.getByPlaceholderText("Professional email")).toHaveValue(
        "invited@example.com"
      );
      expect(screen.getByPlaceholderText("Professional email")).toHaveAttribute(
        "readOnly"
      );
    });
  });

  it("handles invalid invitation token", async () => {
    mockURLSearchParams.mockReturnValue({
      get: jest.fn().mockImplementation((key) => {
        if (key === "token") return "invalid-token";
        return null;
      }),
    });
    const error = new AxiosError("Request failed", "400");
    error.isAxiosError = true;
    error.response = {
      status: 400,
      statusText: "Bad Request",
      headers: {},
      config: { headers: new AxiosHeaders() },
      data: { message: "Invalid token" },
    };
    mockedAxios.get.mockRejectedValueOnce(error);
    render(<SignupPage />);

    await waitFor(() => {
      expect(
        screen.getByRole("heading", { name: /Error/i })
      ).toBeInTheDocument();
      expect(screen.getByText(/Invalid token/i)).toBeInTheDocument();
    });
  });

  it("is accessible", async () => {
    const { container } = render(<SignupPage />);
    await waitFor(
      async () => {
        const results = await axe(container);
        expect(results).toHaveNoViolations();
      },
      { timeout: 10000 } // Increase timeout for accessibility test
    );
  }, 10000); // Increase test timeout
});
