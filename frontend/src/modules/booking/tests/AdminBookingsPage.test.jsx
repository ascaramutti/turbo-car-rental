import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import AdminBookingsPage from '../pages/AdminBookingsPage';
import * as bookingApi from '../api/bookingApi';

vi.mock('../api/bookingApi');

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

const PENDING_BOOKING = {
  bookingId: 1,
  driverFullName: 'Alice Driver',
  ownerFullName: 'Bob Owner',
  vehicleSummary: '2022 Toyota Camry',
  status: 'PENDING',
  startTime: '2030-06-01T08:00:00',
  endTime: '2030-06-01T13:00:00',
  totalPrice: 125.0,
  totalHours: 5,
};

const CONFIRMED_BOOKING = {
  ...PENDING_BOOKING,
  bookingId: 2,
  driverFullName: 'Charlie Driver',
  ownerFullName: 'Diana Owner',
  vehicleSummary: '2021 Honda Civic',
  status: 'CONFIRMED',
  totalPrice: 90.0,
  totalHours: 3,
};

const COMPLETED_BOOKING = {
  ...PENDING_BOOKING,
  bookingId: 3,
  driverFullName: 'Eve Driver',
  ownerFullName: 'Frank Owner',
  vehicleSummary: '2020 Ford Focus',
  status: 'COMPLETED',
  totalPrice: 200.0,
  totalHours: 8,
};

describe('AdminBookingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows loading state initially', () => {
    bookingApi.getAdminBookings.mockReturnValue(new Promise(() => {}));
    renderWithProviders(<AdminBookingsPage />);

    // While loading, no table or empty message should appear
    expect(screen.queryByText('No bookings found for the selected status.')).not.toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('renders bookings table with correct column headers', async () => {
    bookingApi.getAdminBookings.mockResolvedValue({
      data: [PENDING_BOOKING],
    });
    renderWithProviders(<AdminBookingsPage />);

    await waitFor(() => {
      expect(screen.getByText('ID')).toBeInTheDocument();
      expect(screen.getByText('Driver')).toBeInTheDocument();
      expect(screen.getByText('Owner')).toBeInTheDocument();
      expect(screen.getByText('Vehicle')).toBeInTheDocument();
      expect(screen.getByText('Date & Time')).toBeInTheDocument();
      expect(screen.getByText('Duration')).toBeInTheDocument();
      expect(screen.getByText('Total')).toBeInTheDocument();
      expect(screen.getByText('Status')).toBeInTheDocument();
    });
  });

  it('renders booking rows with correct data', async () => {
    bookingApi.getAdminBookings.mockResolvedValue({
      data: [PENDING_BOOKING, CONFIRMED_BOOKING],
    });
    renderWithProviders(<AdminBookingsPage />);

    await waitFor(() => {
      expect(screen.getByText('Alice Driver')).toBeInTheDocument();
      expect(screen.getByText('Bob Owner')).toBeInTheDocument();
      expect(screen.getByText('2022 Toyota Camry')).toBeInTheDocument();
      expect(screen.getByText('$125.00')).toBeInTheDocument();
      expect(screen.getByText('5h')).toBeInTheDocument();

      expect(screen.getByText('Charlie Driver')).toBeInTheDocument();
      expect(screen.getByText('Diana Owner')).toBeInTheDocument();
      expect(screen.getByText('2021 Honda Civic')).toBeInTheDocument();
      expect(screen.getByText('$90.00')).toBeInTheDocument();
      expect(screen.getByText('3h')).toBeInTheDocument();
    });
  });

  it('shows empty state when no bookings exist', async () => {
    bookingApi.getAdminBookings.mockResolvedValue({ data: [] });
    renderWithProviders(<AdminBookingsPage />);

    await waitFor(() => {
      expect(
        screen.getByText('No bookings found for the selected status.')
      ).toBeInTheDocument();
    });
  });

  it('shows all status filter tabs', async () => {
    bookingApi.getAdminBookings.mockResolvedValue({ data: [] });
    renderWithProviders(<AdminBookingsPage />);

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

  it('clicking a status filter tab re-fetches with that status', async () => {
    const user = userEvent.setup();
    bookingApi.getAdminBookings.mockResolvedValue({ data: [] });
    renderWithProviders(<AdminBookingsPage />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Pending' })).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: 'Pending' }));

    await waitFor(() => {
      expect(bookingApi.getAdminBookings).toHaveBeenCalledWith('PENDING');
    });
  });

  it('clicking Completed tab fetches COMPLETED bookings', async () => {
    const user = userEvent.setup();
    bookingApi.getAdminBookings.mockResolvedValue({ data: [] });
    renderWithProviders(<AdminBookingsPage />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Completed' })).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: 'Completed' }));

    await waitFor(() => {
      expect(bookingApi.getAdminBookings).toHaveBeenCalledWith('COMPLETED');
    });
  });

  it('navigates to detail page on row click', async () => {
    const user = userEvent.setup();
    bookingApi.getAdminBookings.mockResolvedValue({
      data: [PENDING_BOOKING],
    });
    renderWithProviders(<AdminBookingsPage />);

    await waitFor(() => {
      expect(screen.getByText('2022 Toyota Camry')).toBeInTheDocument();
    });

    await user.click(screen.getByText('2022 Toyota Camry'));

    expect(mockNavigate).toHaveBeenCalledWith('/admin/bookings/1');
  });

  it('shows page title "All Bookings"', async () => {
    bookingApi.getAdminBookings.mockResolvedValue({ data: [] });
    renderWithProviders(<AdminBookingsPage />);

    await waitFor(() => {
      expect(screen.getByText('All Bookings')).toBeInTheDocument();
    });
  });

  it('shows error toast when API call fails', async () => {
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');

    bookingApi.getAdminBookings.mockRejectedValue({
      response: { data: { message: 'Failed to load admin bookings' } },
    });
    renderWithProviders(<AdminBookingsPage />);

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Failed to load admin bookings');
    });
  });

  it('shows correct status badges for bookings', async () => {
    bookingApi.getAdminBookings.mockResolvedValue({
      data: [PENDING_BOOKING, CONFIRMED_BOOKING, COMPLETED_BOOKING],
    });
    renderWithProviders(<AdminBookingsPage />);

    await waitFor(() => {
      expect(screen.getByText('Pending')).toBeInTheDocument();
      expect(screen.getByText('Confirmed')).toBeInTheDocument();
      expect(screen.getByText('Completed')).toBeInTheDocument();
    });
  });

  it('initial fetch is called with null (All tab)', () => {
    bookingApi.getAdminBookings.mockResolvedValue({ data: [] });
    renderWithProviders(<AdminBookingsPage />);

    expect(bookingApi.getAdminBookings).toHaveBeenCalledWith(null);
  });
});
