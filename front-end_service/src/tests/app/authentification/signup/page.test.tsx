import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { useSearchParams, useRouter } from 'next/navigation';
import axios, { AxiosError, AxiosHeaders } from 'axios';
import SignupPage from '../../../../app/authentification/signup/page';

expect.extend(toHaveNoViolations);

jest.mock('next/navigation', () => ({
  useSearchParams: jest.fn(),
  useRouter: jest.fn(),
}));

jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

// Mock axios.isAxiosError to recognize our AxiosError instances
jest.spyOn(axios, 'isAxiosError').mockImplementation((error: any) => {
  return error instanceof AxiosError || (error && error.isAxiosError === true);
});

// Define mocks globally
const mockPush = jest.fn();
const mockPrefetch = jest.fn();
const mockSearchParams = {
  get: jest.fn(),
};

describe('SignupPage Component', () => {
  beforeEach(() => {
    process.env.NEXT_PUBLIC_API_AUTHENTICATION_SERVICE_URL = 'http://localhost:8080';
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue({
      push: mockPush,
      replace: mockPush,
      prefetch: mockPrefetch,
    });
    (useSearchParams as jest.Mock).mockReturnValue(mockSearchParams);
    mockSearchParams.get.mockReturnValue(null);
  });

  afterEach(() => {
    jest.useRealTimers(); // Reset timers after each test
  });

  it('renders the SignupPage form correctly', async () => {
    render(<SignupPage />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Get started with AGILIA/i })).toBeInTheDocument();
      expect(screen.getByText(/It’s free for up to 10 users - no credit card needed./i)).toBeInTheDocument();
      expect(screen.getByAltText('Google')).toBeInTheDocument();
      expect(screen.getByAltText('Facebook')).toBeInTheDocument();
      expect(screen.getByAltText('Microsoft')).toBeInTheDocument();
      expect(screen.getByAltText('Apple')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('First Name')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('Last Name')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('User name')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('Professionnel e-mail')).toBeInTheDocument();
      expect(screen.getByPlaceholderText(/minimum 8 caract/i)).toBeInTheDocument();
      expect(screen.getByLabelText('I agree to receive marketing emails.')).toBeInTheDocument();
      expect(screen.getByLabelText(/I agree the terms of use and the privacy policy/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Sign Up/i })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /Sign In/i })).toHaveAttribute('href', '/authentification/signin');
      expect(screen.getByAltText('Signup Illustration')).toBeInTheDocument(); // Changed from getByLabelText to getByAltText
    });
  });

  it('updates form inputs correctly', async () => {
    render(<SignupPage />);

    await waitFor(() => {
      fireEvent.change(screen.getByPlaceholderText('First Name'), { target: { value: 'John' } });
      fireEvent.change(screen.getByPlaceholderText('Last Name'), { target: { value: 'Doe' } });
      fireEvent.change(screen.getByPlaceholderText('User name'), { target: { value: 'johndoe' } });
      fireEvent.change(screen.getByPlaceholderText('Professionnel e-mail'), { target: { value: 'john@example.com' } });
      fireEvent.change(screen.getByPlaceholderText(/minimum 8 caract/i), { target: { value: 'password123' } });
      fireEvent.click(screen.getByLabelText('I agree to receive marketing emails.'));
      fireEvent.click(screen.getByLabelText(/I agree the terms of use and the privacy policy/i));

      expect(screen.getByPlaceholderText('First Name')).toHaveValue('John');
      expect(screen.getByPlaceholderText('Last Name')).toHaveValue('Doe');
      expect(screen.getByPlaceholderText('User name')).toHaveValue('johndoe');
      expect(screen.getByPlaceholderText('Professionnel e-mail')).toHaveValue('john@example.com');
      expect(screen.getByPlaceholderText(/minimum 8 caract/i)).toHaveValue('password123');
      expect(screen.getByLabelText('I agree to receive marketing emails.')).toBeChecked();
      expect(screen.getByLabelText(/I agree the terms of use and the privacy policy/i)).toBeChecked();
    });
  });

  it('handles successful form submission', async () => {
    mockedAxios.post.mockResolvedValueOnce({
      status: 201,
      statusText: 'Created',
      headers: {},
      config: { headers: new AxiosHeaders() },
      data: {},
    });
    render(<SignupPage />);

    fireEvent.change(screen.getByPlaceholderText('First Name'), { target: { value: 'John' } });
    fireEvent.change(screen.getByPlaceholderText('Last Name'), { target: { value: 'Doe' } });
    fireEvent.change(screen.getByPlaceholderText('User name'), { target: { value: 'johndoe' } });
    fireEvent.change(screen.getByPlaceholderText('Professionnel e-mail'), { target: { value: 'john@example.com' } });
    fireEvent.change(screen.getByPlaceholderText(/minimum 8 caract/i), { target: { value: 'password123' } });
    fireEvent.click(screen.getByLabelText(/I agree the terms of use and the privacy policy/i));

    fireEvent.click(screen.getByRole('button', { name: /Sign Up/i }));

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Success/i })).toBeInTheDocument();
      expect(screen.getByText(/Registration successful! Please check your email to confirm/i)).toBeInTheDocument();
      expect(screen.getByText(/Redirect to signin/i)).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/authentification/signin');
    }, { timeout: 4000 });
  });

  it('handles form submission error for existing email', async () => {
    const error = new AxiosError('Request failed', '400');
    error.isAxiosError = true;
    error.response = {
      status: 400,
      statusText: 'Bad Request',
      headers: {},
      config: { headers: new AxiosHeaders() },
      data: 'Email already exists',
    };
    mockedAxios.post.mockRejectedValueOnce(error);
    render(<SignupPage />);

    fireEvent.change(screen.getByPlaceholderText('First Name'), { target: { value: 'John' } });
    fireEvent.change(screen.getByPlaceholderText('Last Name'), { target: { value: 'Doe' } });
    fireEvent.change(screen.getByPlaceholderText('User name'), { target: { value: 'johndoe' } });
    fireEvent.change(screen.getByPlaceholderText('Professionnel e-mail'), { target: { value: 'john@example.com' } });
    fireEvent.change(screen.getByPlaceholderText(/minimum 8 caract/i), { target: { value: 'password123' } });
    fireEvent.click(screen.getByLabelText(/I agree the terms of use and the privacy policy/i));

    fireEvent.click(screen.getByRole('button', { name: /Sign Up/i }));

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Error/i })).toBeInTheDocument();
      expect(screen.getByText(/Email already exists/i)).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /Go Back To SignUp/i })).toHaveAttribute('href', '/authentification/signup');
    });
  });

  it('handles valid invitation token', async () => {
    mockSearchParams.get.mockReturnValue('valid-token');
    mockedAxios.get.mockResolvedValueOnce({
      data: {
        email: 'invited@example.com',
        role: 'USER',
        entrepriseId: '123',
        project: 'ProjectX',
      },
      status: 200,
      statusText: 'OK',
      headers: {},
      config: { headers: new AxiosHeaders() },
    });

    render(<SignupPage />);

    await waitFor(() => {
      expect(screen.getByPlaceholderText('Professionnel e-mail')).toHaveValue('invited@example.com');
      expect(screen.getByPlaceholderText('Professionnel e-mail')).toHaveAttribute('readOnly');
    });
  });

  it('handles invalid invitation token', async () => {
    mockSearchParams.get.mockReturnValue('invalid-token');
    const error = new AxiosError('Request failed', '400');
    error.isAxiosError = true;
    error.response = {
      status: 400,
      statusText: 'Bad Request',
      headers: {},
      config: { headers: new AxiosHeaders() },
      data: 'Invalid token',
    };
    mockedAxios.get.mockRejectedValueOnce(error);

    render(<SignupPage />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Error/i })).toBeInTheDocument();
      expect(screen.getByText('Invalid token')).toBeInTheDocument(); // Changed to match actual error message
      expect(screen.getByRole('link', { name: /Go Back To SignUp/i })).toHaveAttribute('href', '/authentification/signup');
    });
  });

  it('is accessible', async () => {
    const { container } = render(<SignupPage />);
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});