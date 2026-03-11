import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import VehicleCard from '../components/VehicleCard';

const ACTIVE_VEHICLE = {
  vehicleId: 100, ownerId: 20, ownerFullName: 'Sarah Smith',
  vin: 'WVWZZZ3CZWE123456', make: 'Volkswagen', model: 'Jetta', year: 2022,
  licensePlate: 'ABC 123', category: 'SEDAN', fuelType: 'GASOLINE',
  hourlyRate: 25.00, description: 'Clean sedan', generalLocation: 'Vancouver',
  latitude: 49.2827, longitude: -123.1207, status: 'APPROVED', isActive: true,
  createdAt: '2026-03-14T10:00:00',
};

const PENDING_VEHICLE = { ...ACTIVE_VEHICLE, vehicleId: 101, status: 'PENDING', isActive: false, hourlyRate: null };
const INACTIVE_VEHICLE = { ...ACTIVE_VEHICLE, vehicleId: 102, status: 'INACTIVE', isActive: false };
describe('VehicleCard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders vehicle make, model, and year', () => {
    renderWithProviders(<VehicleCard vehicle={ACTIVE_VEHICLE} onClick={vi.fn()} />);

    expect(screen.getByText('2022 Volkswagen Jetta')).toBeInTheDocument();
  });

  it('shows the license plate', () => {
    renderWithProviders(<VehicleCard vehicle={ACTIVE_VEHICLE} onClick={vi.fn()} />);

    expect(screen.getByText('ABC 123')).toBeInTheDocument();
  });

  it('shows the hourly rate', () => {
    renderWithProviders(<VehicleCard vehicle={ACTIVE_VEHICLE} onClick={vi.fn()} />);

    expect(screen.getByText('$25.00/hr')).toBeInTheDocument();
  });

  it('shows "Approved" badge for APPROVED status', () => {
    renderWithProviders(<VehicleCard vehicle={ACTIVE_VEHICLE} onClick={vi.fn()} />);

    const badge = screen.getByText('Approved');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('text-blue-600');
  });

  it('shows "Pending Docs" badge for PENDING status', () => {
    renderWithProviders(<VehicleCard vehicle={PENDING_VEHICLE} onClick={vi.fn()} />);

    const badge = screen.getByText('Pending Docs');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('text-amber-500');
  });

  it('shows "Inactive" badge for INACTIVE status', () => {
    renderWithProviders(<VehicleCard vehicle={INACTIVE_VEHICLE} onClick={vi.fn()} />);

    const badge = screen.getByText('Inactive');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('text-gray-500');
  });

  it('calls onClick when the card is clicked', async () => {
    const user = userEvent.setup();
    const handleClick = vi.fn();
    renderWithProviders(<VehicleCard vehicle={ACTIVE_VEHICLE} onClick={handleClick} />);

    await user.click(screen.getByText('2022 Volkswagen Jetta'));

    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('shows the category label', () => {
    renderWithProviders(<VehicleCard vehicle={ACTIVE_VEHICLE} onClick={vi.fn()} />);

    expect(screen.getByText('Sedan')).toBeInTheDocument();
  });

  it('shows service type badge when serviceType is TAXI_AND_DELIVERY', () => {
    const vehicle = { ...ACTIVE_VEHICLE, serviceType: 'TAXI_AND_DELIVERY' };
    renderWithProviders(<VehicleCard vehicle={vehicle} onClick={vi.fn()} />);

    expect(screen.getByText('Taxi + Delivery')).toBeInTheDocument();
  });

  it('shows service type badge when serviceType is DELIVERY_ONLY', () => {
    const vehicle = { ...ACTIVE_VEHICLE, serviceType: 'DELIVERY_ONLY' };
    renderWithProviders(<VehicleCard vehicle={vehicle} onClick={vi.fn()} />);

    expect(screen.getByText('Delivery Only')).toBeInTheDocument();
  });

  it('does not show service type badge when serviceType is null', () => {
    renderWithProviders(<VehicleCard vehicle={ACTIVE_VEHICLE} onClick={vi.fn()} />);

    expect(screen.queryByText('Taxi + Delivery')).not.toBeInTheDocument();
    expect(screen.queryByText('Delivery Only')).not.toBeInTheDocument();
  });

  it('does not show hourly rate when hourlyRate is null (pending vehicle)', () => {
    renderWithProviders(<VehicleCard vehicle={PENDING_VEHICLE} onClick={vi.fn()} />);

    expect(screen.queryByText(/\$.*\/hr/)).not.toBeInTheDocument();
  });
});
