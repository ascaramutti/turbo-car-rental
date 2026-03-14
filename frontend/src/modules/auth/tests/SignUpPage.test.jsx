import { describe, it, expect } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import SignUpPage from '../pages/SignUpPage';

describe('SignUpPage', () => {
  it('renders the sign up form with all fields', () => {
    renderWithProviders(<SignUpPage />);

    expect(screen.getByText('JOIN TURBO!')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('First Name')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Last Name')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('you@example.com')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Min. 6 characters')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Re-enter password')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Street Address')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('City')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('V6B 1A1')).toBeInTheDocument();
    expect(screen.getByDisplayValue('British Columbia')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Canada')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /driver/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /car owner/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign up/i })).toBeInTheDocument();
  });

  it('shows error on blur when first name contains numbers', async () => {
    const user = userEvent.setup();
    renderWithProviders(<SignUpPage />);

    const firstNameInput = screen.getByPlaceholderText('First Name');
    await user.type(firstNameInput, 'John123');
    await user.tab();

    expect(screen.getByText('Only letters, accents, apostrophes, and hyphens allowed')).toBeInTheDocument();
  });

  it('shows error on blur when email is invalid', async () => {
    const user = userEvent.setup();
    renderWithProviders(<SignUpPage />);

    const emailInput = screen.getByPlaceholderText('you@example.com');
    await user.type(emailInput, 'bad-email');
    await user.tab();

    expect(screen.getByText('Invalid email format')).toBeInTheDocument();
  });

  it('shows error on blur when password is too short', async () => {
    const user = userEvent.setup();
    renderWithProviders(<SignUpPage />);

    const pwInput = screen.getByPlaceholderText('Min. 6 characters');
    await user.type(pwInput, '123');
    await user.tab();

    expect(screen.getByText('Password must be between 6 and 100 characters')).toBeInTheDocument();
  });

  it('shows error on blur when confirm password does not match', async () => {
    const user = userEvent.setup();
    renderWithProviders(<SignUpPage />);

    const pwInput = screen.getByPlaceholderText('Min. 6 characters');
    const confirmInput = screen.getByPlaceholderText('Re-enter password');

    await user.type(pwInput, 'secure123');
    await user.type(confirmInput, 'different');
    await user.tab();

    expect(screen.getByText('Passwords do not match')).toBeInTheDocument();
  });

  it('does not show error for empty optional phone field on blur', async () => {
    const user = userEvent.setup();
    renderWithProviders(<SignUpPage />);

    const phoneInput = screen.getByPlaceholderText(/optional/i);
    await user.click(phoneInput);
    await user.tab();

    expect(screen.queryByText(/phone/i)).not.toBeInTheDocument();
  });

  it('shows error for invalid phone format on blur', async () => {
    const user = userEvent.setup();
    renderWithProviders(<SignUpPage />);

    const phoneInput = screen.getByPlaceholderText(/optional/i);
    await user.type(phoneInput, 'abc');
    await user.tab();

    expect(screen.getByText(/invalid phone number format/i)).toBeInTheDocument();
  });

  it('shows all validation errors on submit with empty form', async () => {
    const user = userEvent.setup();
    renderWithProviders(<SignUpPage />);

    await user.click(screen.getByRole('button', { name: /sign up/i }));

    expect(screen.getByText('First name is required')).toBeInTheDocument();
    expect(screen.getByText('Last name is required')).toBeInTheDocument();
    expect(screen.getByText('Date of birth is required')).toBeInTheDocument();
    expect(screen.getByText('Email is required')).toBeInTheDocument();
    expect(screen.getByText('Password is required')).toBeInTheDocument();
    expect(screen.getByText('Please confirm your password')).toBeInTheDocument();
    expect(screen.getByText('Street address is required')).toBeInTheDocument();
    expect(screen.getByText('City is required')).toBeInTheDocument();
    expect(screen.getByText('Postal code is required')).toBeInTheDocument();
    expect(screen.getByText('Please select a role')).toBeInTheDocument();
  });

  it('clears error when user corrects the field', async () => {
    const user = userEvent.setup();
    renderWithProviders(<SignUpPage />);

    const firstNameInput = screen.getByPlaceholderText('First Name');
    await user.type(firstNameInput, 'John123');
    await user.tab();

    expect(screen.getByText('Only letters, accents, apostrophes, and hyphens allowed')).toBeInTheDocument();

    await user.clear(firstNameInput);
    await user.type(firstNameInput, 'John');

    expect(screen.queryByText('Only letters, accents, apostrophes, and hyphens allowed')).not.toBeInTheDocument();
  });

  it('shows error on blur for invalid postal code format', async () => {
    const user = userEvent.setup();
    renderWithProviders(<SignUpPage />);

    const postalInput = screen.getByPlaceholderText('V6B 1A1');
    await user.type(postalInput, '90210');
    await user.tab();

    expect(screen.getByText('Invalid postal code format (e.g. V6B 1A1)')).toBeInTheDocument();
  });

  it('shows error on blur for street address with SQL injection', async () => {
    const user = userEvent.setup();
    renderWithProviders(<SignUpPage />);

    const streetInput = screen.getByPlaceholderText('Street Address');
    await user.type(streetInput, "'; DROP TABLE;");
    await user.tab();

    expect(screen.getByText(/only letters, numbers, spaces/i)).toBeInTheDocument();
  });

  it('shows error on blur for city with numbers', async () => {
    const user = userEvent.setup();
    renderWithProviders(<SignUpPage />);

    const cityInput = screen.getByPlaceholderText('City');
    await user.type(cityInput, 'Vancouver123');
    await user.tab();

    expect(screen.getByText(/only letters, spaces, dots/i)).toBeInTheDocument();
  });

  it('province and country are pre-filled and read-only', () => {
    renderWithProviders(<SignUpPage />);

    const provinceInput = screen.getByDisplayValue('British Columbia');
    const countryInput = screen.getByDisplayValue('Canada');

    expect(provinceInput).toHaveAttribute('readOnly');
    expect(countryInput).toHaveAttribute('readOnly');
  });

  it('has a link to the login page', () => {
    renderWithProviders(<SignUpPage />);

    const loginLink = screen.getByRole('link', { name: /log in/i });
    expect(loginLink).toHaveAttribute('href', '/login');
  });
});
