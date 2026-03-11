import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import MyVehiclesPage from '../pages/MyVehiclesPage';
import * as vehicleApi from '../api/vehicleApi';

vi.mock('../api/vehicleApi');
vi.mock('react-pdf', () => ({
  Document: ({ children }) => <div>{children}</div>,
  Page: () => <div />,
  pdfjs: { GlobalWorkerOptions: {}, version: '0.0.0' },
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

const ACTIVE_VEHICLE = {
  vehicleId: 100, ownerId: 20, ownerFullName: 'Sarah Smith',
  vin: 'WVWZZZ3CZWE123456', make: 'Volkswagen', model: 'Jetta', year: 2022,
  licensePlate: 'ABC 123', category: 'SEDAN', fuelType: 'GASOLINE',
  hourlyRate: 25.00, description: 'Clean sedan', generalLocation: 'Vancouver',
  latitude: 49.2827, longitude: -123.1207, status: 'ACTIVE', isActive: true,
  createdAt: '2026-03-14T10:00:00',
};

const PENDING_VEHICLE = { ...ACTIVE_VEHICLE, vehicleId: 101, status: 'PENDING', isActive: false, make: 'Toyota', model: 'Corolla', year: 2023 };

describe('MyVehiclesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows page title "My Vehicles"', async () => {
    vehicleApi.getMyVehicles.mockResolvedValue({ data: [] });
    renderWithProviders(<MyVehiclesPage />);

    await waitFor(() => {
      expect(screen.getByText('My Vehicles')).toBeInTheDocument();
    });
  });

  it('shows loading state initially', () => {
    vehicleApi.getMyVehicles.mockReturnValue(new Promise(() => {}));
    renderWithProviders(<MyVehiclesPage />);

    expect(screen.queryByText('My Vehicles')).not.toBeInTheDocument();
  });

  it('shows vehicles after fetch', async () => {
    vehicleApi.getMyVehicles.mockResolvedValue({ data: [ACTIVE_VEHICLE, PENDING_VEHICLE] });
    renderWithProviders(<MyVehiclesPage />);

    await waitFor(() => {
      expect(screen.getByText('2022 Volkswagen Jetta')).toBeInTheDocument();
      expect(screen.getByText('2023 Toyota Corolla')).toBeInTheDocument();
    });
  });

  it('shows empty state when no vehicles', async () => {
    vehicleApi.getMyVehicles.mockResolvedValue({ data: [] });
    renderWithProviders(<MyVehiclesPage />);

    await waitFor(() => {
      expect(screen.getByText(/You have not registered any vehicles yet/)).toBeInTheDocument();
    });
  });

  it('shows "Register New Vehicle" button', async () => {
    vehicleApi.getMyVehicles.mockResolvedValue({ data: [] });
    renderWithProviders(<MyVehiclesPage />);

    await waitFor(() => {
      expect(screen.getByText('Register New Vehicle')).toBeInTheDocument();
    });
  });

  it('shows error toast on fetch failure', async () => {
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');

    vehicleApi.getMyVehicles.mockRejectedValue({ response: { data: { message: 'Failed to load vehicles' } } });
    renderWithProviders(<MyVehiclesPage />);

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Failed to load vehicles');
    });
  });

  it('opens register form modal when "Register New Vehicle" button is clicked', async () => {
    const user = userEvent.setup();
    vehicleApi.getMyVehicles.mockResolvedValue({ data: [] });
    renderWithProviders(<MyVehiclesPage />);

    await waitFor(() => {
      expect(screen.getByText('Register New Vehicle')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Register New Vehicle'));

    expect(screen.getByText('Register Vehicle')).toBeInTheDocument();
  });

  // ── New tests for uncovered lines ──────────────────────────────────────

  it('navigates to vehicle detail page when a vehicle card is clicked', async () => {
    const user = userEvent.setup();
    vehicleApi.getMyVehicles.mockResolvedValue({ data: [ACTIVE_VEHICLE] });
    renderWithProviders(<MyVehiclesPage />);

    await waitFor(() => {
      expect(screen.getByText('2022 Volkswagen Jetta')).toBeInTheDocument();
    });

    await user.click(screen.getByText('2022 Volkswagen Jetta'));

    expect(mockNavigate).toHaveBeenCalledWith('/owner/vehicles/100');
  });

  it('closes register modal when X button is clicked', async () => {
    const user = userEvent.setup();
    vehicleApi.getMyVehicles.mockResolvedValue({ data: [] });
    renderWithProviders(<MyVehiclesPage />);

    await waitFor(() => {
      expect(screen.getByText('Register New Vehicle')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Register New Vehicle'));
    expect(screen.getByText('Register New Vehicle', { selector: 'h2' })).toBeInTheDocument();

    // Find the close button in the modal header
    const modalOverlay = document.querySelector('.fixed.inset-0');
    const headerDiv = modalOverlay.querySelector('.border-b');
    const closeBtn = headerDiv.querySelector('button');
    await user.click(closeBtn);

    expect(screen.queryByText('Register Vehicle')).not.toBeInTheDocument();
  });

  it('submits register form successfully and navigates to new vehicle', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const successSpy = vi.spyOn(toast.default, 'success');
    vehicleApi.getMyVehicles.mockResolvedValue({ data: [] });
    vehicleApi.registerVehicle.mockResolvedValue({
      data: { vehicleId: 200, vin: 'WVWZZZ3CZWE999999', make: 'Honda', model: 'Civic', year: 2024 },
    });

    renderWithProviders(<MyVehiclesPage />);

    await waitFor(() => {
      expect(screen.getByText('Register New Vehicle')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Register New Vehicle'));

    // Fill in the registration form
    const vinInput = screen.getByPlaceholderText('17-character Vehicle Identification Number');
    await user.type(vinInput, 'WVWZZZ3CZWE999999');

    const makeInput = screen.getByPlaceholderText('e.g. Toyota');
    await user.type(makeInput, 'Honda');

    const modelInput = screen.getByPlaceholderText('e.g. Corolla');
    await user.type(modelInput, 'Civic');

    const yearInput = screen.getByPlaceholderText(/–/);
    await user.type(yearInput, '2024');

    const plateInput = screen.getByPlaceholderText('e.g. ABC-1234');
    await user.type(plateInput, 'XYZ 789');

    // Select category
    const categorySelect = screen.getByDisplayValue('Select category');
    await user.selectOptions(categorySelect, 'SEDAN');

    // Select fuel type
    const fuelSelect = screen.getByDisplayValue('Select fuel type');
    await user.selectOptions(fuelSelect, 'GASOLINE');

    // Submit the form
    await user.click(screen.getByText('Register Vehicle'));

    await waitFor(() => {
      expect(vehicleApi.registerVehicle).toHaveBeenCalledWith(expect.objectContaining({
        vin: 'WVWZZZ3CZWE999999',
        make: 'Honda',
        model: 'Civic',
      }));
    });

    await waitFor(() => {
      expect(successSpy).toHaveBeenCalledWith('Vehicle registered successfully');
    });

    expect(mockNavigate).toHaveBeenCalledWith('/owner/vehicles/200');
  });

  it('shows error toast when register API call fails', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');
    vehicleApi.getMyVehicles.mockResolvedValue({ data: [] });
    vehicleApi.registerVehicle.mockRejectedValue({
      response: { data: { message: 'Registration failed' } },
    });

    renderWithProviders(<MyVehiclesPage />);

    await waitFor(() => {
      expect(screen.getByText('Register New Vehicle')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Register New Vehicle'));

    // Fill in the form minimally
    await user.type(screen.getByPlaceholderText('17-character Vehicle Identification Number'), 'WVWZZZ3CZWE999999');
    await user.type(screen.getByPlaceholderText('e.g. Toyota'), 'Honda');
    await user.type(screen.getByPlaceholderText('e.g. Corolla'), 'Civic');
    await user.type(screen.getByPlaceholderText(/–/), '2024');
    await user.type(screen.getByPlaceholderText('e.g. ABC-1234'), 'XYZ 789');
    await user.selectOptions(screen.getByDisplayValue('Select category'), 'SEDAN');
    await user.selectOptions(screen.getByDisplayValue('Select fuel type'), 'GASOLINE');

    await user.click(screen.getByText('Register Vehicle'));

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Registration failed');
    });
  });

  it('shows subtitle text', async () => {
    vehicleApi.getMyVehicles.mockResolvedValue({ data: [] });
    renderWithProviders(<MyVehiclesPage />);

    await waitFor(() => {
      expect(screen.getByText('Manage your vehicles, documents, and availability')).toBeInTheDocument();
    });
  });
});
