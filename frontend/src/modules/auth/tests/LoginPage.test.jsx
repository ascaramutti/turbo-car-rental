import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import LoginPage from '../pages/LoginPage';
import * as authApi from '../api/authApi';

vi.mock('../api/authApi');

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

  it('shows error when submitting with empty email', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginPage />);

    const passwordInput = screen.getByPlaceholderText('Enter your password');
    await user.type(passwordInput, 'mypassword');

    await user.click(screen.getByRole('button', { name: /log in/i }));

    expect(screen.getByText('Email is required')).toBeInTheDocument();
    expect(screen.queryByText('Password is required')).not.toBeInTheDocument();
  });

  it('shows error when submitting with empty password', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginPage />);

    const emailInput = screen.getByPlaceholderText('you@example.com');
    await user.type(emailInput, 'test@example.com');

    await user.click(screen.getByRole('button', { name: /log in/i }));

    expect(screen.getByText('Password is required')).toBeInTheDocument();
    expect(screen.queryByText('Email is required')).not.toBeInTheDocument();
  });

  it('calls login and navigates to /driver/dashboard on successful DRIVER login', async () => {
    const user = userEvent.setup();
    authApi.loginUser.mockResolvedValue({
      data: { token: 'fake-jwt', role: 'DRIVER', email: 'driver@test.com' },
    });

    renderWithProviders(<LoginPage />);

    await user.type(screen.getByPlaceholderText('you@example.com'), 'driver@test.com');
    await user.type(screen.getByPlaceholderText('Enter your password'), 'password123');
    await user.click(screen.getByRole('button', { name: /log in/i }));

    await waitFor(() => {
      expect(authApi.loginUser).toHaveBeenCalledWith('driver@test.com', 'password123');
    });
  });

  it('shows password toggle (eye icon) and toggles password visibility', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginPage />);

    const passwordInput = screen.getByPlaceholderText('Enter your password');
    expect(passwordInput).toHaveAttribute('type', 'password');

    const toggleButton = passwordInput.parentElement.querySelector('button');
    await user.click(toggleButton);

    expect(passwordInput).toHaveAttribute('type', 'text');

    await user.click(toggleButton);

    expect(passwordInput).toHaveAttribute('type', 'password');
  });

  it('shows error toast on invalid credentials', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');

    authApi.loginUser.mockRejectedValue({
      response: { data: { message: 'Invalid email or password' } },
    });

    renderWithProviders(<LoginPage />);

    await user.type(screen.getByPlaceholderText('you@example.com'), 'wrong@test.com');
    await user.type(screen.getByPlaceholderText('Enter your password'), 'badpassword');
    await user.click(screen.getByRole('button', { name: /log in/i }));

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Invalid email or password');
    });
  });
});
