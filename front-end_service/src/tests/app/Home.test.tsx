import React from 'react';
import { render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import Home from '../../app/page'; // Adjust path to match /app/page.tsx

// Extend Jest with jest-axe for accessibility testing
expect.extend(toHaveNoViolations);

describe('Home Component', () => {
  // Test rendering of the Home component
  it('renders the Home component with all elements', () => {
    render(<Home />);

    // Verify headline and description
    expect(screen.getByRole('heading', { name: /Take Your Projects to the Next Level/i })).toBeInTheDocument();
    expect(screen.getByText(/Tired of complexity?/i)).toBeInTheDocument();
    // Target the specific <p> with class "description"
    expect(screen.getByText((content, element) => {
      return (
        (element?.classList.contains('description') &&
         element?.textContent?.includes('With AGILIA, collaboration is seamless')) ?? false
      );
    })).toBeInTheDocument();

    // Verify hero button
    expect(screen.getByRole('link', { name: /Try it for Free – Get Started Today/i })).toBeInTheDocument();

    // Verify images (using alt text, noting potential issues)
    expect(screen.getAllByAltText('Olivia Smith').length).toBe(3); // home_picture.png, separateur.png, section4.png
    expect(screen.getByAltText('feed back')).toBeInTheDocument();

    // Verify reasons section
    expect(screen.getByRole('heading', { name: /3 Reasons for Choosing Us/i })).toBeInTheDocument();
    expect(screen.getByText('Efficiency')).toBeInTheDocument();
    expect(screen.getByText('Collaboration')).toBeInTheDocument();
    expect(screen.getByText('Flexibility')).toBeInTheDocument();
    expect(screen.getAllByText(/Read more/i).length).toBe(3); // Three "Read more" links

    // Verify section text and button
    expect(screen.getByRole('heading', { name: /Transform Your Projects with Seamless Agile Management/i })).toBeInTheDocument();
    expect(screen.getByText(/In today’s fast-paced world/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Get Started/i })).toBeInTheDocument();
  });

  // Test link and button attributes
  it('links and buttons have the correct attributes', () => {
    render(<Home />);

    // Verify hero button link
    expect(screen.getByRole('link', { name: /Try it for Free – Get Started Today/i })).toHaveAttribute('href', '#');

    // Verify "Read more" links in cards
    const readMoreLinks = screen.getAllByRole('link', { name: /Read more/i });
    expect(readMoreLinks.length).toBe(3);
    readMoreLinks.forEach((link) => {
      expect(link).toHaveAttribute('href', '#');
    });

    // Verify "Get Started" button
    const getStartedButton = screen.getByRole('button', { name: /Get Started/i });
    expect(getStartedButton).toHaveAttribute('type', 'button');
  });

  // Test accessibility
  it('Home component is accessible', async () => {
    const { container } = render(<Home />);
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});