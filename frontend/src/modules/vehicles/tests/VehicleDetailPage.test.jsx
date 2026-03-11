import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import VehicleDetailPage from '../pages/VehicleDetailPage';
import * as vehicleApi from '../api/vehicleApi';

vi.mock('../api/vehicleApi');
vi.mock('../../booking/components/LocationPicker', () => ({
  default: ({ onLocationSelect }) => (
    <button type="button" onClick={() => onLocationSelect?.(49.2827, -123.1207)}>
      Mock Location Picker
    </button>
  ),
}));
vi.mock('../../booking/utils/geocoder.js', () => ({
  reverseGeocode: vi.fn().mockResolvedValue('Downtown Edmonton'),
}));
vi.mock('react-pdf', () => ({
  Document: ({ children }) => <div>{children}</div>,
  Page: () => <div />,
  pdfjs: { GlobalWorkerOptions: {}, version: '0.0.0' },
}));
vi.mock('../../../shared/utils/downloadUtils', () => ({
  downloadFile: vi.fn(),
  BLOB_RESPONSE_CONFIG: { responseType: 'blob' },
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: '100' }),
    useNavigate: () => mockNavigate,
  };
});

const APPROVED_VEHICLE = {
  vehicleId: 100,
  ownerId: 20,
  ownerFullName: 'Sarah Smith',
  vin: 'WVWZZZ3CZWE123456',
  make: 'Volkswagen',
  model: 'Jetta',
  year: 2022,
  licensePlate: 'ABC 123',
  category: 'SEDAN',
  fuelType: 'GASOLINE',
  hourlyRate: null,
  description: 'Clean sedan',
  generalLocation: 'Vancouver',
  latitude: 49.2827,
  longitude: -123.1207,
  status: 'APPROVED',
  serviceType: 'TAXI_AND_DELIVERY',
  isActive: false,
  availableUntil: null,
  createdAt: '2026-03-14T10:00:00',
};

const PENDING_VEHICLE = {
  ...APPROVED_VEHICLE,
  status: 'PENDING',
  isActive: false,
  serviceType: null,
};

const ACTIVE_VEHICLE = {
  ...APPROVED_VEHICLE,
  hourlyRate: 25.0,
  isActive: true,
  availableUntil: '2099-12-31T23:59:00',
};

describe('VehicleDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows loading state initially', () => {
    vehicleApi.getVehicleById.mockReturnValue(new Promise(() => {}));
    vehicleApi.getVehicleDocuments.mockReturnValue(new Promise(() => {}));

    renderWithProviders(<VehicleDetailPage />);

    expect(screen.queryByText(/Volkswagen/)).not.toBeInTheDocument();
  });

  it('shows vehicle info after fetch (make, model, year)', async () => {
    vehicleApi.getVehicleById.mockResolvedValue({ data: APPROVED_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('2022 Volkswagen Jetta')).toBeInTheDocument();
    });
  });

  it('shows Vehicle Documents section heading', async () => {
    vehicleApi.getVehicleById.mockResolvedValue({ data: APPROVED_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Vehicle Documents')).toBeInTheDocument();
    });
  });

  it('shows datetime input when vehicle is approved but not active', async () => {
    vehicleApi.getVehicleById.mockResolvedValue({ data: APPROVED_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      const datetimeInput = document.querySelector('input[type="datetime-local"]');
      expect(datetimeInput).toBeInTheDocument();
    });
  });

  it('shows "Pending Docs" message when status is PENDING', async () => {
    vehicleApi.getVehicleById.mockResolvedValue({ data: PENDING_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Pending Docs')).toBeInTheDocument();
      expect(screen.getByText(/pending admin approval/i)).toBeInTheDocument();
    });
  });

  it('shows "List for Rent" button when status is APPROVED and isActive is false', async () => {
    vehicleApi.getVehicleById.mockResolvedValue({ data: APPROVED_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('List for Rent')).toBeInTheDocument();
    });
  });

  it('shows "Listed for Rent" badge and "Remove from Rent" when isActive is true', async () => {
    vehicleApi.getVehicleById.mockResolvedValue({ data: ACTIVE_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Listed for Rent')).toBeInTheDocument();
      expect(screen.getByText('Remove from Rent')).toBeInTheDocument();
    });
  });

  it('shows "Available until" when vehicle is active with availableUntil', async () => {
    vehicleApi.getVehicleById.mockResolvedValue({ data: ACTIVE_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByText(/Available until:/)).toBeInTheDocument();
    });
  });

  it('shows edit button', async () => {
    vehicleApi.getVehicleById.mockResolvedValue({ data: APPROVED_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByTitle('Edit vehicle')).toBeInTheDocument();
    });
  });

  it('shows error toast on fetch failure', async () => {
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');

    vehicleApi.getVehicleById.mockRejectedValue({
      response: { data: { message: 'Vehicle not found' } },
    });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Vehicle not found');
    });
  });

  // ── New tests for uncovered lines ──────────────────────────────────────

  it('opens edit modal when edit button is clicked', async () => {
    const user = userEvent.setup();
    vehicleApi.getVehicleById.mockResolvedValue({ data: APPROVED_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByTitle('Edit vehicle')).toBeInTheDocument();
    });

    await user.click(screen.getByTitle('Edit vehicle'));

    expect(screen.getByText('Edit Vehicle')).toBeInTheDocument();
    expect(screen.getByText('Update Vehicle')).toBeInTheDocument();
  });

  it('shows pre-filled fields in the edit modal', async () => {
    const user = userEvent.setup();
    vehicleApi.getVehicleById.mockResolvedValue({ data: APPROVED_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByTitle('Edit vehicle')).toBeInTheDocument();
    });

    await user.click(screen.getByTitle('Edit vehicle'));

    expect(screen.getByDisplayValue('WVWZZZ3CZWE123456')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Volkswagen')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Jetta')).toBeInTheDocument();
    expect(screen.getByDisplayValue('2022')).toBeInTheDocument();
  });

  it('closes edit modal when X button is clicked', async () => {
    const user = userEvent.setup();
    vehicleApi.getVehicleById.mockResolvedValue({ data: APPROVED_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByTitle('Edit vehicle')).toBeInTheDocument();
    });

    await user.click(screen.getByTitle('Edit vehicle'));
    expect(screen.getByText('Edit Vehicle')).toBeInTheDocument();

    // Find the close button in the modal header
    const modalOverlay = document.querySelector('.fixed.inset-0');
    const headerDiv = modalOverlay.querySelector('.border-b');
    const closeBtn = headerDiv.querySelector('button');
    await user.click(closeBtn);

    expect(screen.queryByText('Edit Vehicle')).not.toBeInTheDocument();
  });

  it('submits edit form and shows success toast', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const successSpy = vi.spyOn(toast.default, 'success');
    vehicleApi.getVehicleById.mockResolvedValue({ data: APPROVED_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });
    vehicleApi.updateVehicle.mockResolvedValue({ data: APPROVED_VEHICLE });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByTitle('Edit vehicle')).toBeInTheDocument();
    });

    await user.click(screen.getByTitle('Edit vehicle'));
    expect(screen.getByText('Update Vehicle')).toBeInTheDocument();

    // Submit the form (fields are pre-filled and valid)
    await user.click(screen.getByText('Update Vehicle'));

    await waitFor(() => {
      expect(vehicleApi.updateVehicle).toHaveBeenCalledWith('100', expect.objectContaining({
        vin: 'WVWZZZ3CZWE123456',
        make: 'Volkswagen',
        model: 'Jetta',
      }));
    });

    await waitFor(() => {
      expect(successSpy).toHaveBeenCalledWith('Vehicle updated successfully');
    });
  });

  it('shows error toast when edit fails', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');
    vehicleApi.getVehicleById.mockResolvedValue({ data: APPROVED_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });
    vehicleApi.updateVehicle.mockRejectedValue({
      response: { data: { message: 'Update failed' } },
    });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByTitle('Edit vehicle')).toBeInTheDocument();
    });

    await user.click(screen.getByTitle('Edit vehicle'));
    await user.click(screen.getByText('Update Vehicle'));

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Update failed');
    });
  });

  it('activates vehicle with filled form fields and shows success toast', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const successSpy = vi.spyOn(toast.default, 'success');
    vehicleApi.getVehicleById.mockResolvedValue({ data: APPROVED_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });
    vehicleApi.activateVehicle.mockResolvedValue({ data: ACTIVE_VEHICLE });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('List for Rent')).toBeInTheDocument();
    });

    // Fill in activation form fields
    const futureDate = '2027-06-01T12:00';
    const datetimeInput = document.querySelector('input[type="datetime-local"]');
    await user.clear(datetimeInput);
    await user.type(datetimeInput, futureDate);

    // Pick the location via the mocked LocationPicker — this also reverse-geocodes
    // the general location so the field is auto-filled from the map.
    await user.click(screen.getByText('Mock Location Picker'));

    const rateInput = document.querySelector('input[name="hourlyRate"]');
    await user.type(rateInput, '25.00');

    await user.click(screen.getByText('List for Rent'));

    await waitFor(() => {
      expect(vehicleApi.activateVehicle).toHaveBeenCalledWith('100', expect.objectContaining({
        generalLocation: 'Downtown Edmonton',
        latitude: 49.2827,
        longitude: -123.1207,
      }));
    });

    await waitFor(() => {
      expect(successSpy).toHaveBeenCalledWith('Vehicle is now listed for rent');
    });
  });

  it('shows validation error toast when activation form has missing fields', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');
    vehicleApi.getVehicleById.mockResolvedValue({ data: APPROVED_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('List for Rent')).toBeInTheDocument();
    });

    // Fill only availableUntil to enable the button, but leave other fields empty
    const futureDate = '2027-06-01T12:00';
    const datetimeInput = document.querySelector('input[type="datetime-local"]');
    await user.clear(datetimeInput);
    await user.type(datetimeInput, futureDate);

    await user.click(screen.getByText('List for Rent'));

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalled();
    });

    expect(vehicleApi.activateVehicle).not.toHaveBeenCalled();
  });

  it('shows error toast when activation API fails', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');
    vehicleApi.getVehicleById.mockResolvedValue({ data: APPROVED_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });
    vehicleApi.activateVehicle.mockRejectedValue({
      response: { data: { message: 'Activation failed' } },
    });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('List for Rent')).toBeInTheDocument();
    });

    const futureDate = '2027-06-01T12:00';
    const datetimeInput = document.querySelector('input[type="datetime-local"]');
    await user.clear(datetimeInput);
    await user.type(datetimeInput, futureDate);

    await user.click(screen.getByText('Mock Location Picker'));

    const rateInput = document.querySelector('input[name="hourlyRate"]');
    await user.type(rateInput, '25.00');

    await user.click(screen.getByText('List for Rent'));

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Activation failed');
    });
  });

  it('deactivates vehicle and shows success toast', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const successSpy = vi.spyOn(toast.default, 'success');
    vehicleApi.getVehicleById.mockResolvedValue({ data: ACTIVE_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });
    vehicleApi.deactivateVehicle.mockResolvedValue({ data: {} });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Remove from Rent')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Remove from Rent'));

    await waitFor(() => {
      expect(vehicleApi.deactivateVehicle).toHaveBeenCalledWith('100');
    });

    await waitFor(() => {
      expect(successSpy).toHaveBeenCalledWith('Vehicle removed from rent listings');
    });
  });

  it('shows error toast when deactivation API fails', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');
    vehicleApi.getVehicleById.mockResolvedValue({ data: ACTIVE_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });
    vehicleApi.deactivateVehicle.mockRejectedValue({
      response: { data: { message: 'Deactivation failed' } },
    });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Remove from Rent')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Remove from Rent'));

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Deactivation failed');
    });
  });

  it('navigates back when "Back to My Vehicles" is clicked', async () => {
    const user = userEvent.setup();
    vehicleApi.getVehicleById.mockResolvedValue({ data: APPROVED_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Back to My Vehicles')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Back to My Vehicles'));

    expect(mockNavigate).toHaveBeenCalledWith('/owner/vehicles');
  });

  it('shows Required and Optional labels for document types', async () => {
    vehicleApi.getVehicleById.mockResolvedValue({ data: APPROVED_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Required')).toHaveLength(2);
    });

    expect(screen.getByText(/Optional — Required for Taxi \+ Delivery only/)).toBeInTheDocument();
  });

  it('shows service type label when vehicle is active with serviceType', async () => {
    vehicleApi.getVehicleById.mockResolvedValue({ data: ACTIVE_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByText(/Service: Taxi \+ Delivery/)).toBeInTheDocument();
    });
  });

  it('shows hourly rate and coordinates when vehicle is active', async () => {
    vehicleApi.getVehicleById.mockResolvedValue({ data: ACTIVE_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('$25.00/hr')).toBeInTheDocument();
      expect(screen.getByText('49.2827, -123.1207')).toBeInTheDocument();
      expect(screen.getByText('Vancouver')).toBeInTheDocument();
    });
  });

  it('shows description when vehicle has one', async () => {
    vehicleApi.getVehicleById.mockResolvedValue({ data: APPROVED_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Clean sedan')).toBeInTheDocument();
    });
  });

  it('shows "Not Listed" badge when vehicle is approved but not active', async () => {
    vehicleApi.getVehicleById.mockResolvedValue({ data: APPROVED_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Not Listed')).toBeInTheDocument();
    });
  });

  it('shows document cards for uploaded documents', async () => {
    const docs = [
      {
        documentId: 1, vehicleId: 100, documentType: 'INSURANCE',
        fileName: 'insurance.pdf', fileSize: 2048, status: 'APPROVED',
        uploadedAt: '2026-03-14T10:00:00', reviewedAt: '2026-03-14T11:00:00', rejectionReason: null,
      },
    ];
    vehicleApi.getVehicleById.mockResolvedValue({ data: APPROVED_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: docs });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('insurance.pdf')).toBeInTheDocument();
    });
  });

  it('navigates to /owner/vehicles on fetch vehicle failure', async () => {
    vehicleApi.getVehicleById.mockRejectedValue({
      response: { data: { message: 'Vehicle not found' } },
    });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/owner/vehicles');
    });
  });

  it('shows error toast when fetching documents fails', async () => {
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');

    vehicleApi.getVehicleById.mockResolvedValue({ data: APPROVED_VEHICLE });
    vehicleApi.getVehicleDocuments.mockRejectedValue({
      response: { data: { message: 'Failed to load documents' } },
    });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Failed to load documents');
    });
  });

  it('"List for Rent" button is disabled when availableUntil is empty', async () => {
    vehicleApi.getVehicleById.mockResolvedValue({ data: APPROVED_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('List for Rent')).toBeInTheDocument();
    });

    const listButton = screen.getByText('List for Rent').closest('button');
    expect(listButton).toBeDisabled();
  });

  it('shows activation form labels for all fields', async () => {
    vehicleApi.getVehicleById.mockResolvedValue({ data: APPROVED_VEHICLE });
    vehicleApi.getVehicleDocuments.mockResolvedValue({ data: [] });

    renderWithProviders(<VehicleDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Available Until')).toBeInTheDocument();
      expect(screen.getByText('General Location')).toBeInTheDocument();
      expect(screen.getByText('Your hourly rate ($)')).toBeInTheDocument();
      // Latitude and longitude inputs are intentionally absent — values come from the map.
      expect(screen.queryByText('Latitude')).not.toBeInTheDocument();
      expect(screen.queryByText('Longitude')).not.toBeInTheDocument();
    });
  });
});
