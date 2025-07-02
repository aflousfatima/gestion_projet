import React from 'react';
import { render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import Footer from './Footer';

expect.extend(toHaveNoViolations);

describe('Footer', () => {
  // Test rendering of the Footer component
  it('renders the Footer with all elements', () => {
    render(<Footer />);

    // Verify logo
    const logo = screen.getByAltText('AGILIA Logo');
    expect(logo).toBeInTheDocument();
    expect(logo).toHaveAttribute('src', '/logo.png');

    // Verify footer text
    expect(screen.getByText('Elevate your workflow. Stay organized. Deliver faster.')).toBeInTheDocument();

    // Verify Quick Links
    expect(screen.getByText('Quick Links')).toBeInTheDocument();
    expect(screen.getByText('About')).toBeInTheDocument();
    expect(screen.getByText('Features')).toBeInTheDocument();
    expect(screen.getByText('Pricing')).toBeInTheDocument();
    expect(screen.getByText('Contact')).toBeInTheDocument();

    // Verify Follow Us section
    expect(screen.getByText('Follow Us')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /facebook/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /twitter/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /linkedin/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /github/i })).toBeInTheDocument();

    // Verify Stay Updated section
    expect(screen.getByText('Stay Updated')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Enter your email')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /subscribe/i })).toBeInTheDocument();

    // Verify footer bottom text and links
    expect(screen.getByText(/© 2025 AGILIA. All rights reserved./i)).toBeInTheDocument();
    expect(screen.getByText('Privacy Policy')).toBeInTheDocument();
    expect(screen.getByText('Terms of Service')).toBeInTheDocument();
  });

  // Test link href attributes
  it('links have the correct href attributes', () => {
    render(<Footer />);

    // Verify Quick Links hrefs
    expect(screen.getByText('About').closest('a')).toHaveAttribute('href', '#');
    expect(screen.getByText('Features').closest('a')).toHaveAttribute('href', '#');
    expect(screen.getByText('Pricing').closest('a')).toHaveAttribute('href', '#');
    expect(screen.getByText('Contact').closest('a')).toHaveAttribute('href', '#');

    // Verify social media links hrefs
    expect(screen.getByRole('link', { name: /facebook/i })).toHaveAttribute('href', '#');
    expect(screen.getByRole('link', { name: /twitter/i })).toHaveAttribute('href', '#');
    expect(screen.getByRole('link', { name: /linkedin/i })).toHaveAttribute('href', '#');
    expect(screen.getByRole('link', { name: /github/i })).toHaveAttribute('href', '#');

    // Verify footer bottom links hrefs
    expect(screen.getByText('Privacy Policy').closest('a')).toHaveAttribute('href', '#');
    expect(screen.getByText('Terms of Service').closest('a')).toHaveAttribute('href', '#');
  });

  // Test accessibility
  it('Footer is accessible', async () => {
    const { container } = render(<Footer />);
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});
