import { describe, it, expect } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import LoginPage from '../pages/LoginPage';

describe('LoginPage', () => {
  it('renders the login form', () => {
    renderWithProviders(<LoginPage />);

    expect(screen.getByText('LOG IN TO TURBO')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('you@example.com')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Enter your password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /log in/i })).toBeInTheDocument();
  });

  it('shows validation error on blur with invalid email', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginPage />);

    const emailInput = screen.getByPlaceholderText('you@example.com');
    await user.type(emailInput, 'not-an-email');
    await user.tab();

    expect(screen.getByText('Invalid email format')).toBeInTheDocument();
  });

  it('shows required error on blur with empty email', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginPage />);

    const emailInput = screen.getByPlaceholderText('you@example.com');
    await user.click(emailInput);
    await user.tab();

    expect(screen.getByText('Email is required')).toBeInTheDocument();
  });

  it('clears error when user starts typing', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginPage />);

    const emailInput = screen.getByPlaceholderText('you@example.com');
    await user.click(emailInput);
    await user.tab();

    expect(screen.getByText('Email is required')).toBeInTheDocument();

    await user.type(emailInput, 'j');
    expect(screen.queryByText('Email is required')).not.toBeInTheDocument();
  });

  it('shows validation errors on submit with empty fields', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginPage />);

    await user.click(screen.getByRole('button', { name: /log in/i }));

    expect(screen.getByText('Email is required')).toBeInTheDocument();
    expect(screen.getByText('Password is required')).toBeInTheDocument();
  });

  it('has a link to the sign up page', () => {
    renderWithProviders(<LoginPage />);

    const signUpLink = screen.getByRole('link', { name: /sign up/i });
    expect(signUpLink).toHaveAttribute('href', '/signup');
  });
});
