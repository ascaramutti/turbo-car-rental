import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import DriverBookingsPage from '../pages/DriverBookingsPage';
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
  driverFullName: null,
};

const CONFIRMED_BOOKING = {
  ...PENDING_BOOKING,
  bookingId: 2,
  vehicleSummary: '2021 Honda Civic',
  status: 'CONFIRMED',
};

const COMPLETED_BOOKING = {
  ...PENDING_BOOKING,
  bookingId: 3,
  vehicleSummary: '2020 Ford Focus',
  status: 'COMPLETED',
};

describe('DriverBookingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows page title "My Bookings"', async () => {
    bookingApi.getDriverBookings.mockResolvedValue({ data: [] });
    renderWithProviders(<DriverBookingsPage />);

    await waitFor(() => {
      expect(screen.getByText('My Bookings')).toBeInTheDocument();
    });
  });

  it('shows subtitle text', async () => {
    bookingApi.getDriverBookings.mockResolvedValue({ data: [] });
    renderWithProviders(<DriverBookingsPage />);

    await waitFor(() => {
      expect(screen.getByText(/Track all your vehicle bookings/i)).toBeInTheDocument();
    });
  });

  it('shows "Find a Vehicle" navigation button', async () => {
    bookingApi.getDriverBookings.mockResolvedValue({ data: [] });
    renderWithProviders(<DriverBookingsPage />);

    await waitFor(() => {
      expect(screen.getByText('Find a Vehicle')).toBeInTheDocument();
    });
  });

  it('"Find a Vehicle" button navigates to /driver/search', async () => {
    const user = userEvent.setup();
    bookingApi.getDriverBookings.mockResolvedValue({ data: [] });
    renderWithProviders(<DriverBookingsPage />);

    await waitFor(() => {
      expect(screen.getByText('Find a Vehicle')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Find a Vehicle'));

    expect(mockNavigate).toHaveBeenCalledWith('/driver/search');
  });

  it('shows all status filter tabs', async () => {
    bookingApi.getDriverBookings.mockResolvedValue({ data: [] });
    renderWithProviders(<DriverBookingsPage />);

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
    bookingApi.getDriverBookings.mockResolvedValue({ data: [] });
    renderWithProviders(<DriverBookingsPage />);

    await waitFor(() => {
      expect(
        screen.getByText('No bookings found for the selected status.')
      ).toBeInTheDocument();
    });
  });

  it('renders booking cards after data loads', async () => {
    bookingApi.getDriverBookings.mockResolvedValue({
      data: [PENDING_BOOKING, CONFIRMED_BOOKING],
    });
    renderWithProviders(<DriverBookingsPage />);

    await waitFor(() => {
      expect(screen.getByText('2022 Toyota Camry')).toBeInTheDocument();
      expect(screen.getByText('2021 Honda Civic')).toBeInTheDocument();
    });
  });

  it('clicking a booking card navigates to the detail page', async () => {
    const user = userEvent.setup();
    bookingApi.getDriverBookings.mockResolvedValue({ data: [PENDING_BOOKING] });
    renderWithProviders(<DriverBookingsPage />);

    await waitFor(() => {
      expect(screen.getByText('2022 Toyota Camry')).toBeInTheDocument();
    });

    await user.click(screen.getByText('2022 Toyota Camry'));

    expect(mockNavigate).toHaveBeenCalledWith('/driver/bookings/1');
  });

  it('clicking a status filter tab re-fetches with that status', async () => {
    const user = userEvent.setup();
    bookingApi.getDriverBookings.mockResolvedValue({ data: [] });
    renderWithProviders(<DriverBookingsPage />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Pending' })).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: 'Pending' }));

    await waitFor(() => {
      expect(bookingApi.getDriverBookings).toHaveBeenCalledWith('PENDING');
    });
  });

  it('clicking the "All" tab re-fetches with null status', async () => {
    const user = userEvent.setup();
    bookingApi.getDriverBookings.mockResolvedValue({ data: [] });
    renderWithProviders(<DriverBookingsPage />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'All' })).toBeInTheDocument();
    });

    // First click another tab then click All
    await user.click(screen.getByRole('button', { name: 'Confirmed' }));
    await user.click(screen.getByRole('button', { name: 'All' }));

    await waitFor(() => {
      expect(bookingApi.getDriverBookings).toHaveBeenCalledWith(null);
    });
  });

  it('shows error toast when API call fails', async () => {
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');

    bookingApi.getDriverBookings.mockRejectedValue({
      response: { data: { message: 'Failed to load bookings' } },
    });
    renderWithProviders(<DriverBookingsPage />);

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Failed to load bookings');
    });
  });

  it('shows multiple bookings with correct status badges', async () => {
    bookingApi.getDriverBookings.mockResolvedValue({
      data: [PENDING_BOOKING, CONFIRMED_BOOKING, COMPLETED_BOOKING],
    });
    renderWithProviders(<DriverBookingsPage />);

    await waitFor(() => {
      expect(screen.getByText('Pending')).toBeInTheDocument();
      expect(screen.getByText('Confirmed')).toBeInTheDocument();
      expect(screen.getByText('Completed')).toBeInTheDocument();
    });
  });
});
