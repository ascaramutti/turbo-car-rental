import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import AdminBookingDetailPage from '../pages/AdminBookingDetailPage';
import * as bookingApi from '../api/bookingApi';

vi.mock('../api/bookingApi');
vi.mock('../components/AuthImage', () => ({
  default: ({ src, alt, className }) => <img src={src} alt={alt} className={className} />,
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: '42' }),
    useNavigate: () => mockNavigate,
  };
});

const BASE_BOOKING = {
  bookingId: 42,
  driverFullName: 'Alice Driver',
  ownerFullName: 'Bob Owner',
  vehicleSummary: '2022 Toyota Camry',
  vehicleMake: 'Toyota',
  vehicleModel: 'Camry',
  vehicleYear: 2022,
  vehicleLicensePlate: 'ABC 123',
  vehicleCategory: 'SEDAN',
  vehicleServiceType: 'RIDE_SHARE',
  vehicleHourlyRate: 25.0,
  status: 'PENDING',
  startTime: '2030-06-01T08:00:00',
  endTime: '2030-06-01T13:00:00',
  totalHours: 5,
  totalPrice: 125.0,
  createdAt: '2030-05-28T10:00:00',
  updatedAt: '2030-05-28T10:00:00',
  confirmedAt: null,
  startedAt: null,
  completedAt: null,
  cancelledAt: null,
  cancellationReason: null,
  cancelledBy: null,
  pickupPhotoUrls: [],
  returnPhotoUrls: [],
};

const COMPLETED_BOOKING = {
  ...BASE_BOOKING,
  status: 'COMPLETED',
  confirmedAt: '2030-05-29T09:00:00',
  startedAt: '2030-06-01T08:00:00',
  completedAt: '2030-06-01T13:00:00',
  pickupPhotoUrls: [
    'https://example.com/pickup1.jpg',
    'https://example.com/pickup2.jpg',
  ],
  returnPhotoUrls: [
    'https://example.com/return1.jpg',
    'https://example.com/return2.jpg',
    'https://example.com/return3.jpg',
  ],
};

const CANCELLED_BOOKING = {
  ...BASE_BOOKING,
  status: 'CANCELLED',
  cancellationReason: 'Changed my plans.',
  cancelledBy: 'DRIVER',
  cancelledAt: '2030-05-30T15:00:00',
};

const REJECTED_BOOKING = {
  ...BASE_BOOKING,
  status: 'REJECTED',
  cancellationReason: 'Vehicle unavailable.',
  cancelledBy: 'OWNER',
};

describe('AdminBookingDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // -- Loading and basic render -------------------------------------------

  it('shows loading state initially', () => {
    bookingApi.getAdminBookingDetail.mockReturnValue(new Promise(() => {}));
    renderWithProviders(<AdminBookingDetailPage />);

    // While loading, the booking details should not appear
    expect(screen.queryByText('2022 Toyota Camry')).not.toBeInTheDocument();
    expect(screen.queryByText('Booking #42')).not.toBeInTheDocument();
  });

  it('renders booking details after load', async () => {
    bookingApi.getAdminBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<AdminBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getAllByText('2022 Toyota Camry').length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText('Booking #42')).toBeInTheDocument();
    });
  });

  it('shows driver and owner names', async () => {
    bookingApi.getAdminBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<AdminBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Alice Driver')).toBeInTheDocument();
      expect(screen.getByText('Bob Owner')).toBeInTheDocument();
    });
  });

  it('shows vehicle details', async () => {
    bookingApi.getAdminBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<AdminBookingDetailPage />);

    await waitFor(() => {
      // vehicleSummary appears as heading + Vehicle info item
      expect(screen.getAllByText('2022 Toyota Camry').length).toBeGreaterThanOrEqual(2);
      expect(screen.getByText('ABC 123')).toBeInTheDocument();
    });
  });

  it('shows booking times, total hours, and price', async () => {
    bookingApi.getAdminBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<AdminBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('5h')).toBeInTheDocument();
      expect(screen.getByText('$125.00')).toBeInTheDocument();
    });
  });

  it('shows hourly rate', async () => {
    bookingApi.getAdminBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<AdminBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('$25.00/hr')).toBeInTheDocument();
    });
  });

  it('shows status badge', async () => {
    bookingApi.getAdminBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<AdminBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Pending')).toBeInTheDocument();
    });
  });

  // -- Pickup photos ------------------------------------------------------

  it('shows pickup photos when present', async () => {
    bookingApi.getAdminBookingDetail.mockResolvedValue({ data: COMPLETED_BOOKING });
    renderWithProviders(<AdminBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Pickup Photos (2)')).toBeInTheDocument();
      expect(screen.getByAltText('Pickup photo 1')).toBeInTheDocument();
      expect(screen.getByAltText('Pickup photo 2')).toBeInTheDocument();
    });
  });

  it('does not show pickup photos section when empty', async () => {
    bookingApi.getAdminBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<AdminBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Booking #42')).toBeInTheDocument();
    });

    expect(screen.queryByText(/Pickup Photos/)).not.toBeInTheDocument();
  });

  // -- Return photos ------------------------------------------------------

  it('shows return photos when present', async () => {
    bookingApi.getAdminBookingDetail.mockResolvedValue({ data: COMPLETED_BOOKING });
    renderWithProviders(<AdminBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Return Photos (3)')).toBeInTheDocument();
      expect(screen.getByAltText('Return photo 1')).toBeInTheDocument();
      expect(screen.getByAltText('Return photo 2')).toBeInTheDocument();
      expect(screen.getByAltText('Return photo 3')).toBeInTheDocument();
    });
  });

  it('does not show return photos section when empty', async () => {
    bookingApi.getAdminBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<AdminBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Booking #42')).toBeInTheDocument();
    });

    expect(screen.queryByText(/Return Photos/)).not.toBeInTheDocument();
  });

  // -- Cancellation reason ------------------------------------------------

  it('shows cancellation reason when booking is CANCELLED', async () => {
    bookingApi.getAdminBookingDetail.mockResolvedValue({ data: CANCELLED_BOOKING });
    renderWithProviders(<AdminBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Cancellation Reason')).toBeInTheDocument();
      expect(screen.getByText('Changed my plans.')).toBeInTheDocument();
      expect(screen.getByText('Cancelled by: DRIVER')).toBeInTheDocument();
    });
  });

  it('shows "Rejection Reason" label when booking is REJECTED', async () => {
    bookingApi.getAdminBookingDetail.mockResolvedValue({ data: REJECTED_BOOKING });
    renderWithProviders(<AdminBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Rejection Reason')).toBeInTheDocument();
      expect(screen.getByText('Vehicle unavailable.')).toBeInTheDocument();
    });
  });

  it('does not show cancellation section for non-cancelled bookings', async () => {
    bookingApi.getAdminBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<AdminBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Booking #42')).toBeInTheDocument();
    });

    expect(screen.queryByText('Cancellation Reason')).not.toBeInTheDocument();
    expect(screen.queryByText('Rejection Reason')).not.toBeInTheDocument();
  });

  // -- Navigation ---------------------------------------------------------

  it('shows "Back to All Bookings" button', async () => {
    bookingApi.getAdminBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<AdminBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Back to All Bookings')).toBeInTheDocument();
    });
  });

  it('navigates to /admin/bookings when back button is clicked', async () => {
    const user = userEvent.setup();
    bookingApi.getAdminBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<AdminBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Back to All Bookings')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Back to All Bookings'));

    expect(mockNavigate).toHaveBeenCalledWith('/admin/bookings');
  });

  // -- Error handling -----------------------------------------------------

  it('shows error toast and navigates back on fetch failure', async () => {
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');

    bookingApi.getAdminBookingDetail.mockRejectedValue({
      response: { data: { message: 'Booking not found' } },
    });
    renderWithProviders(<AdminBookingDetailPage />);

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Booking not found');
      expect(mockNavigate).toHaveBeenCalledWith('/admin/bookings');
    });
  });

  it('calls getAdminBookingDetail with the route param id', () => {
    bookingApi.getAdminBookingDetail.mockReturnValue(new Promise(() => {}));
    renderWithProviders(<AdminBookingDetailPage />);

    expect(bookingApi.getAdminBookingDetail).toHaveBeenCalledWith('42');
  });
});
