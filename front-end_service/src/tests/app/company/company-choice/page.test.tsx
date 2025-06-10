import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { useRouter } from 'next/navigation';
import ChooseCompanyPage from '../../../../app/company/company-choice/page';

expect.extend(toHaveNoViolations);

// Mock next/navigation to control router behavior
jest.mock('next/navigation', () => ({
  useRouter: jest.fn(),
}));

// Define mocks globally
const mockPush = jest.fn();
const mockPrefetch = jest.fn();

describe('ChooseCompanyPage Component', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (useRouter as jest.Mock).mockReturnValue({
      push: mockPush,
      replace: jest.fn(),
      prefetch: mockPrefetch,
    });
  });

  afterEach(() => {
    jest.useRealTimers(); // Reset timers after each test
  });

  it('renders the ChooseCompanyPage correctly', async () => {
    render(<ChooseCompanyPage />);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Embark on Your Agile Journey/i })).toBeInTheDocument();
      expect(screen.getByText(/Will you lead your own empire or join a thriving team\? The choice is yours\./i)).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: /Create a Company/i })).toBeInTheDocument();
      expect(screen.getByText(/Build your vision from the ground up and lead with purpose\./i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Start Now/i })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: /Join a Company/i })).toBeInTheDocument();
      expect(screen.getByText(/Collaborate with innovators and contribute to success\./i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Join Now/i })).toBeInTheDocument();
      expect(screen.getByText(/Agilia - Where Agile Dreams Take Flight/i)).toBeInTheDocument();
    });
  });

  it('navigates to company registration when clicking Create a Company card', async () => {
    render(<ChooseCompanyPage />);

    await waitFor(() => {
      const createCard = screen.getByText(/Create a Company/i).closest('.choice-card');
      if (createCard) {
        fireEvent.click(createCard);
      }
      expect(mockPush).toHaveBeenCalledWith('/company/company-registration');
    });
  });

  it('navigates to company registration when clicking Start Now button', async () => {
    render(<ChooseCompanyPage />);

    await waitFor(() => {
      fireEvent.click(screen.getByRole('button', { name: /Start Now/i }));
      expect(mockPush).toHaveBeenCalledWith('/company/company-registration');
    });
  });

  it('navigates to user dashboard when clicking Join a Company card', async () => {
    render(<ChooseCompanyPage />);

    await waitFor(() => {
      const joinCard = screen.getByText(/Join a Company/i).closest('.choice-card');
      if (joinCard) {
        fireEvent.click(joinCard);
      }
      expect(mockPush).toHaveBeenCalledWith('/user/dashboard/home');
    });
  });

  it('navigates to user dashboard when clicking Join Now button', async () => {
    render(<ChooseCompanyPage />);

    await waitFor(() => {
      fireEvent.click(screen.getByRole('button', { name: /Join Now/i }));
      expect(mockPush).toHaveBeenCalledWith('/user/dashboard/home');
    });
  });

  it('is accessible', async () => {
    const { container } = render(<ChooseCompanyPage />);
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});