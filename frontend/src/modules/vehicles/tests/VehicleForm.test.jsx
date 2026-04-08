import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import VehicleForm from '../components/VehicleForm';

describe('VehicleForm', () => {
  const mockOnSubmit = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders all form fields', () => {
    renderWithProviders(<VehicleForm onSubmit={mockOnSubmit} isLoading={false} />);

    expect(screen.getByPlaceholderText(/17-character/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/e\.g\. Toyota/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/e\.g\. Corolla/i)).toBeInTheDocument();
    expect(screen.getByText('Year')).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/ABC-1234/i)).toBeInTheDocument();
    expect(screen.getByText('Category')).toBeInTheDocument();
    expect(screen.getByText('Fuel Type')).toBeInTheDocument();
  });

  it('shows submit button', () => {
    renderWithProviders(<VehicleForm onSubmit={mockOnSubmit} isLoading={false} />);

    expect(screen.getByRole('button', { name: /Register Vehicle/i })).toBeInTheDocument();
  });

  it('shows loading state when isLoading is true', () => {
    renderWithProviders(<VehicleForm onSubmit={mockOnSubmit} isLoading={true} />);

    expect(screen.getByRole('button', { name: /Saving/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Saving/i })).toBeDisabled();
  });

  it('disables VIN field in edit mode when initialData is provided', () => {
    const initialData = {
      vin: 'WVWZZZ3CZWE123456',
      make: 'Volkswagen',
      model: 'Jetta',
      year: 2022,
      licensePlate: 'ABC 123',
      category: 'SEDAN',
      fuelType: 'GASOLINE',
      description: '',
    };

    renderWithProviders(
      <VehicleForm onSubmit={mockOnSubmit} initialData={initialData} isLoading={false} />
    );

    const vinInput = screen.getByPlaceholderText(/17-character/i);
    expect(vinInput).toBeDisabled();
    expect(vinInput).toHaveValue('WVWZZZ3CZWE123456');
  });

  it('calls onSubmit with form data', async () => {
    const user = userEvent.setup();
    const { container } = renderWithProviders(<VehicleForm onSubmit={mockOnSubmit} isLoading={false} />);

    await user.type(screen.getByPlaceholderText(/17-character/i), 'WVWZZZ3CZWE123456');
    await user.type(screen.getByPlaceholderText(/e\.g\. Toyota/i), 'Toyota');
    await user.type(screen.getByPlaceholderText(/e\.g\. Corolla/i), 'Camry');

    const yearInput = container.querySelector('input[name="year"]');
    await user.type(yearInput, '2022');

    await user.type(screen.getByPlaceholderText(/ABC-1234/i), 'ABC 123');

    const categorySelect = container.querySelector('select[name="category"]');
    await user.selectOptions(categorySelect, 'SEDAN');

    const fuelSelect = container.querySelector('select[name="fuelType"]');
    await user.selectOptions(fuelSelect, 'GASOLINE');

    await user.click(screen.getByRole('button', { name: /Register Vehicle/i }));

    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledTimes(1);
    });

    const callArgs = mockOnSubmit.mock.calls[0][0];
    expect(callArgs.vin).toBe('WVWZZZ3CZWE123456');
    expect(callArgs.make).toBe('Toyota');
    expect(callArgs.model).toBe('Camry');
    expect(callArgs.category).toBe('SEDAN');
    expect(callArgs.fuelType).toBe('GASOLINE');
  });

  it('shows validation errors for missing required fields', async () => {
    const user = userEvent.setup();
    renderWithProviders(<VehicleForm onSubmit={mockOnSubmit} isLoading={false} />);

    await user.click(screen.getByRole('button', { name: /Register Vehicle/i }));

    await waitFor(() => {
      expect(screen.getByText(/VIN is required/i)).toBeInTheDocument();
      expect(screen.getByText(/Make is required/i)).toBeInTheDocument();
      expect(screen.getByText(/Model is required/i)).toBeInTheDocument();
    });

    expect(mockOnSubmit).not.toHaveBeenCalled();
  });
});
