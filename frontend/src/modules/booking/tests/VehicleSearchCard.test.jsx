import { describe, it, expect, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import VehicleSearchCard from '../components/VehicleSearchCard';

const BASE_VEHICLE = {
  vehicleId: 10,
  make: 'Toyota',
  model: 'Camry',
  year: 2022,
  category: 'SEDAN',
  hourlyRate: 25.0,
  generalLocation: 'Downtown Vancouver',
  ownerFullName: 'Bob Owner',
  ownerRating: 4.5,
  serviceType: 'TAXI_AND_DELIVERY',
  effectiveServiceType: 'TAXI_AND_DELIVERY',
  serviceTypeWarning: null,
};

describe('VehicleSearchCard', () => {
  it('renders the vehicle year, make, and model', () => {
    renderWithProviders(<VehicleSearchCard vehicle={BASE_VEHICLE} onClick={vi.fn()} />);
    expect(screen.getByText('2022 Toyota Camry')).toBeInTheDocument();
  });

  it('renders the hourly rate', () => {
    renderWithProviders(<VehicleSearchCard vehicle={BASE_VEHICLE} onClick={vi.fn()} />);
    expect(screen.getByText('$25.00/hr')).toBeInTheDocument();
  });

  it('renders the masked general location', () => {
    renderWithProviders(<VehicleSearchCard vehicle={BASE_VEHICLE} onClick={vi.fn()} />);
    expect(screen.getByText('Downtown Vancouver')).toBeInTheDocument();
  });

  it('renders the owner full name', () => {
    renderWithProviders(<VehicleSearchCard vehicle={BASE_VEHICLE} onClick={vi.fn()} />);
    expect(screen.getByText('Bob Owner')).toBeInTheDocument();
  });

  it('renders the owner rating when provided', () => {
    renderWithProviders(<VehicleSearchCard vehicle={BASE_VEHICLE} onClick={vi.fn()} />);
    expect(screen.getByText(/4\.5/)).toBeInTheDocument();
  });

  it('does not render the owner rating when null', () => {
    const vehicle = { ...BASE_VEHICLE, ownerRating: null };
    renderWithProviders(<VehicleSearchCard vehicle={vehicle} onClick={vi.fn()} />);
    expect(screen.queryByText(/★/)).not.toBeInTheDocument();
  });

  it('shows service type warning banner when serviceTypeWarning is present', () => {
    const vehicle = {
      ...BASE_VEHICLE,
      serviceTypeWarning: 'Your license does not cover Taxi service.',
    };
    renderWithProviders(<VehicleSearchCard vehicle={vehicle} onClick={vi.fn()} />);
    expect(screen.getByText('Your license does not cover Taxi service.')).toBeInTheDocument();
  });

  it('does not show service type warning banner when serviceTypeWarning is null', () => {
    renderWithProviders(<VehicleSearchCard vehicle={BASE_VEHICLE} onClick={vi.fn()} />);
    expect(screen.queryByText(/license does not cover/i)).not.toBeInTheDocument();
  });

  it('calls onClick when the card is clicked', async () => {
    const user = userEvent.setup();
    const handleClick = vi.fn();
    renderWithProviders(<VehicleSearchCard vehicle={BASE_VEHICLE} onClick={handleClick} />);

    await user.click(screen.getByText('2022 Toyota Camry'));

    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('renders the vehicle category', () => {
    renderWithProviders(<VehicleSearchCard vehicle={BASE_VEHICLE} onClick={vi.fn()} />);
    expect(screen.getByText('SEDAN')).toBeInTheDocument();
  });

  it('renders effective service type when provided', () => {
    renderWithProviders(<VehicleSearchCard vehicle={BASE_VEHICLE} onClick={vi.fn()} />);
    expect(screen.getByText('TAXI_AND_DELIVERY')).toBeInTheDocument();
  });
});
