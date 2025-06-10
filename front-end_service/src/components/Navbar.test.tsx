import React from 'react';
import { render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import Navbar from './Navbar';

// Ajoute jest-axe au matcher pour les tests d'accessibilité
expect.extend(toHaveNoViolations);

// Mock de next/link pour capturer les props href
jest.mock('next/link', () => {
  const MockLink = ({ href, children, className }: { href: string; children: React.ReactNode; className?: string }) => (
    <a href={href} className={className}>
      {children}
    </a>
  );
  MockLink.displayName = 'MockLink'; // Ajoute displayName pour éviter l'erreur ESLint
  return MockLink;
});

describe('Navbar', () => {
  // Test de rendu
  it('rendre le Navbar avec tous les éléments', () => {
    render(<Navbar />);
    
    // Vérifie le logo
    const logo = screen.getByAltText('logo');
    expect(logo).toBeInTheDocument();
    expect(logo).toHaveAttribute('src', '/logo.png');

    // Vérifie les liens de navigation
    expect(screen.getByText('Home')).toBeInTheDocument();
    expect(screen.getByText('Product')).toBeInTheDocument();
    expect(screen.getByText('Entreprise')).toBeInTheDocument();
    expect(screen.getByText('Resources')).toBeInTheDocument();
    expect(screen.getByText('Pricing')).toBeInTheDocument();

    // Vérifie les boutons d'authentification
    expect(screen.getByText('Log In')).toBeInTheDocument();
    expect(screen.getByText('Start for Free')).toBeInTheDocument();
  });

  // Test des liens
  it('les liens Link ont les bons href', () => {
    render(<Navbar />);
    
    // Vérifie les href des Link
    expect(screen.getByText('Home').closest('a')).toHaveAttribute('href', '/');
    expect(screen.getByText('Product').closest('a')).toHaveAttribute('href', '/authentification/signin');
    expect(screen.getByText('Log In').closest('a')).toHaveAttribute('href', '/authentification/signin');
    expect(screen.getByText('Start for Free').closest('a')).toHaveAttribute('href', '/authentification/signup');
  });

  // Test d'accessibilité
  it('le Navbar est accessible', async () => {
    const { container } = render(<Navbar />);
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});