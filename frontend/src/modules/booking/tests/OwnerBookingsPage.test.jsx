import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import OwnerBookingsPage from '../pages/OwnerBookingsPage';
import * as bookingApi from '../api/bookingApi';

vi.mock('../api/bookingApi');

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

const PENDING_BOOKING = {
  bookingId: 1,
  vehicleSummary: '2022 Toyota Camry',
  status: 'PENDING',
  startTime: '2030-06-01T08:00:00',
  endTime: '2030-06-01T13:00:00',
  totalPrice: 125.0,
  totalHours: 5,
  driverFullName: 'Alice Driver',
};

const CONFIRMED_BOOKING = {
  ...PENDING_BOOKING,
  bookingId: 2,
  vehicleSummary: '2021 Honda Civic',
  status: 'CONFIRMED',
  driverFullName: 'Bob Driver',
};

const COMPLETED_BOOKING = {
  ...PENDING_BOOKING,
  bookingId: 3,
  vehicleSummary: '2020 Ford Focus',
  status: 'COMPLETED',
};

describe('OwnerBookingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows page title "Upcoming Bookings"', async () => {
    bookingApi.getOwnerBookings.mockResolvedValue({ data: [] });
    renderWithProviders(<OwnerBookingsPage />);

    await waitFor(() => {
      expect(screen.getByText('Upcoming Bookings')).toBeInTheDocument();
    });
  });

  it('shows subtitle text', async () => {
    bookingApi.getOwnerBookings.mockResolvedValue({ data: [] });
    renderWithProviders(<OwnerBookingsPage />);

    await waitFor(() => {
      expect(
        screen.getByText(/Review and manage bookings for your vehicles/i)
      ).toBeInTheDocument();
    });
  });

  it('shows all status filter tabs', async () => {
    bookingApi.getOwnerBookings.mockResolvedValue({ data: [] });
    renderWithProviders(<OwnerBookingsPage />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'All' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Pending' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Confirmed' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'In Progress' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Completed' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Cancelled' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Rejected' })).toBeInTheDocument();
    });
  });

  it('shows empty state message when no bookings exist', async () => {
    bookingApi.getOwnerBookings.mockResolvedValue({ data: [] });
    renderWithProviders(<OwnerBookingsPage />);

    await waitFor(() => {
      expect(
        screen.getByText('No bookings found for the selected status.')
      ).toBeInTheDocument();
    });
  });

  it('renders booking cards after data loads', async () => {
    bookingApi.getOwnerBookings.mockResolvedValue({
      data: [PENDING_BOOKING, CONFIRMED_BOOKING],
    });
    renderWithProviders(<OwnerBookingsPage />);

    await waitFor(() => {
      expect(screen.getByText('2022 Toyota Camry')).toBeInTheDocument();
      expect(screen.getByText('2021 Honda Civic')).toBeInTheDocument();
    });
  });

  it('clicking a booking card navigates to the owner detail page', async () => {
    const user = userEvent.setup();
    bookingApi.getOwnerBookings.mockResolvedValue({ data: [PENDING_BOOKING] });
    renderWithProviders(<OwnerBookingsPage />);

    await waitFor(() => {
      expect(screen.getByText('2022 Toyota Camry')).toBeInTheDocument();
    });

    await user.click(screen.getByText('2022 Toyota Camry'));

    expect(mockNavigate).toHaveBeenCalledWith('/owner/bookings/1');
  });

  it('clicking a status filter tab re-fetches with that status', async () => {
    const user = userEvent.setup();
    bookingApi.getOwnerBookings.mockResolvedValue({ data: [] });
    renderWithProviders(<OwnerBookingsPage />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Pending' })).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: 'Pending' }));

    await waitFor(() => {
      expect(bookingApi.getOwnerBookings).toHaveBeenCalledWith('PENDING');
    });
  });

  it('shows error toast when API call fails', async () => {
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');

    bookingApi.getOwnerBookings.mockRejectedValue({
      response: { data: { message: 'Failed to load owner bookings' } },
    });
    renderWithProviders(<OwnerBookingsPage />);

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Failed to load owner bookings');
    });
  });

  it('shows correct status badges for multiple bookings', async () => {
    bookingApi.getOwnerBookings.mockResolvedValue({
      data: [PENDING_BOOKING, CONFIRMED_BOOKING, COMPLETED_BOOKING],
    });
    renderWithProviders(<OwnerBookingsPage />);

    await waitFor(() => {
      expect(screen.getByText('Pending')).toBeInTheDocument();
      expect(screen.getByText('Confirmed')).toBeInTheDocument();
      expect(screen.getByText('Completed')).toBeInTheDocument();
    });
  });

  it('shows driver names in booking cards', async () => {
    bookingApi.getOwnerBookings.mockResolvedValue({
      data: [PENDING_BOOKING],
    });
    renderWithProviders(<OwnerBookingsPage />);

    await waitFor(() => {
      expect(screen.getByText('Alice Driver')).toBeInTheDocument();
    });
  });
});
