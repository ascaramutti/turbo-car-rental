import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import OwnerBookingDetailPage from '../pages/OwnerBookingDetailPage';
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
    useParams: () => ({ id: '55' }),
    useNavigate: () => mockNavigate,
  };
});

const BASE_BOOKING = {
  bookingId: 55,
  vehicleSummary: '2021 Honda Civic',
  status: 'PENDING',
  startTime: '2030-06-02T09:00:00',
  endTime: '2030-06-02T15:00:00',
  totalPrice: 150.0,
  totalHours: 6,
  driverFullName: 'Alice Driver',
  vehicleMake: 'Honda',
  vehicleModel: 'Civic',
  vehicleYear: 2021,
  vehicleLicensePlate: 'XYZ 789',
  vehicleCategory: 'SEDAN',
  vehicleHourlyRate: 25.0,
  vehicleServiceType: 'TAXI_AND_DELIVERY',
  cancellationReason: null,
  cancelledBy: null,
  pickupPhotoUrls: [],
  returnPhotoUrls: [],
  confirmedAt: null,
  startedAt: null,
  completedAt: null,
  cancelledAt: null,
};

const CONFIRMED_BOOKING = { ...BASE_BOOKING, status: 'CONFIRMED' };
const IN_PROGRESS_BOOKING = { ...BASE_BOOKING, status: 'IN_PROGRESS' };
const COMPLETED_BOOKING = {
  ...BASE_BOOKING,
  status: 'COMPLETED',
  pickupPhotoUrls: [
    'https://example.com/pickup1.jpg',
    'https://example.com/pickup2.jpg',
  ],
  returnPhotoUrls: [
    'https://example.com/return1.jpg',
  ],
};
const CANCELLED_BOOKING = {
  ...BASE_BOOKING,
  status: 'CANCELLED',
  cancellationReason: 'Owner busy.',
  cancelledBy: 'OWNER',
};
const REJECTED_BOOKING = {
  ...BASE_BOOKING,
  status: 'REJECTED',
  cancellationReason: 'Vehicle not available.',
};

describe('OwnerBookingDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ── Loading and basic render ────────────────────────────────────────────

  it('shows loading spinner initially', () => {
    bookingApi.getOwnerBookingDetail.mockReturnValue(new Promise(() => {}));
    renderWithProviders(<OwnerBookingDetailPage />);
    expect(screen.queryByText('2021 Honda Civic')).not.toBeInTheDocument();
  });

  it('shows vehicle summary and booking ID after load', async () => {
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      // vehicleSummary appears in the h1 and also in the Vehicle info row
      expect(screen.getAllByText('2021 Honda Civic').length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText('Booking #55')).toBeInTheDocument();
    });
  });

  it('shows status badge after load', async () => {
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Pending')).toBeInTheDocument();
    });
  });

  it('shows "Back to Booking Requests" button', async () => {
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Back to Booking Requests')).toBeInTheDocument();
    });
  });

  it('navigates to /owner/bookings when back button is clicked', async () => {
    const user = userEvent.setup();
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Back to Booking Requests')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Back to Booking Requests'));

    expect(mockNavigate).toHaveBeenCalledWith('/owner/bookings');
  });

  it('shows driver name in booking details', async () => {
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Alice Driver')).toBeInTheDocument();
    });
  });

  it('shows total hours and price', async () => {
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('6h')).toBeInTheDocument();
      expect(screen.getByText('$150.00')).toBeInTheDocument();
    });
  });

  it('navigates to /owner/bookings and shows error toast on fetch failure', async () => {
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');

    bookingApi.getOwnerBookingDetail.mockRejectedValue({
      response: { data: { message: 'Booking not found' } },
    });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Booking not found');
      expect(mockNavigate).toHaveBeenCalledWith('/owner/bookings');
    });
  });

  // ── PENDING: Confirm + Reject buttons ──────────────────────────────────

  it('shows Confirm Booking and Reject Booking buttons for PENDING status', async () => {
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Confirm Booking')).toBeInTheDocument();
      expect(screen.getByText('Reject Booking')).toBeInTheDocument();
    });
  });

  it('confirms the booking and shows success toast', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const successSpy = vi.spyOn(toast.default, 'success');
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    bookingApi.confirmBooking.mockResolvedValue({ data: CONFIRMED_BOOKING });

    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Confirm Booking')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Confirm Booking'));

    await waitFor(() => {
      expect(bookingApi.confirmBooking).toHaveBeenCalledWith('55');
      expect(successSpy).toHaveBeenCalledWith(
        'Booking confirmed. The driver will be notified.'
      );
    });
  });

  it('shows error toast when confirm booking fails', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    bookingApi.confirmBooking.mockRejectedValue({
      response: { data: { message: 'Cannot confirm' } },
    });

    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Confirm Booking')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Confirm Booking'));

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Cannot confirm');
    });
  });

  it('shows rejection reason form when Reject Booking is clicked', async () => {
    const user = userEvent.setup();
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Reject Booking')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Reject Booking'));

    expect(screen.getByText('Rejection Reason')).toBeInTheDocument();
    expect(screen.getByText('Confirm Rejection')).toBeInTheDocument();
  });

  // ── Reject form onBlur validation ───────────────────────────────────────

  it('shows reason error on blur when rejection reason is empty', async () => {
    const user = userEvent.setup();
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Reject Booking')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Reject Booking'));

    const textarea = screen.getByPlaceholderText(/Provide a reason for rejection/i);
    await user.click(textarea);
    await user.tab();

    await waitFor(() => {
      expect(screen.getByText('Reason is required')).toBeInTheDocument();
    });
  });

  it('shows reason error on blur when rejection reason contains invalid characters', async () => {
    const user = userEvent.setup();
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Reject Booking')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Reject Booking'));

    const textarea = screen.getByPlaceholderText(/Provide a reason for rejection/i);
    await user.type(textarea, '<script>alert(1)</script>');
    await user.tab();

    await waitFor(() => {
      expect(screen.getByText(/only contain letters, numbers/i)).toBeInTheDocument();
    });
  });

  it('does not show reason error on blur when rejection reason is valid', async () => {
    const user = userEvent.setup();
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Reject Booking')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Reject Booking'));

    const textarea = screen.getByPlaceholderText(/Provide a reason for rejection/i);
    await user.type(textarea, 'Vehicle is not available.');
    await user.tab();

    await waitFor(() => {
      expect(screen.queryByText('Reason is required')).not.toBeInTheDocument();
    });
  });

  it('submits rejection and shows success toast', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const successSpy = vi.spyOn(toast.default, 'success');
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    bookingApi.rejectBooking.mockResolvedValue({ data: REJECTED_BOOKING });

    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Reject Booking')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Reject Booking'));
    const textarea = screen.getByPlaceholderText(/Provide a reason for rejection/i);
    await user.type(textarea, 'Vehicle not available.');
    await user.click(screen.getByText('Confirm Rejection'));

    await waitFor(() => {
      expect(bookingApi.rejectBooking).toHaveBeenCalledWith('55', 'Vehicle not available.');
      expect(successSpy).toHaveBeenCalledWith('Booking rejected.');
    });
  });

  it('shows error toast when rejection API call fails', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    bookingApi.rejectBooking.mockRejectedValue({
      response: { data: { message: 'Cannot reject' } },
    });

    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Reject Booking')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Reject Booking'));
    const textarea = screen.getByPlaceholderText(/Provide a reason for rejection/i);
    await user.type(textarea, 'Vehicle not available.');
    await user.click(screen.getByText('Confirm Rejection'));

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Cannot reject');
    });
  });

  // ── CONFIRMED: Cancel button ────────────────────────────────────────────

  it('shows Cancel Booking button for CONFIRMED status', async () => {
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: CONFIRMED_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Cancel Booking')).toBeInTheDocument();
    });
  });

  it('does not show Confirm/Reject buttons for CONFIRMED status', async () => {
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: CONFIRMED_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.queryByText('Confirm Booking')).not.toBeInTheDocument();
      expect(screen.queryByText('Reject Booking')).not.toBeInTheDocument();
    });
  });

  it('shows cancellation reason form when Cancel Booking is clicked (CONFIRMED)', async () => {
    const user = userEvent.setup();
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: CONFIRMED_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Cancel Booking')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Cancel Booking'));

    expect(screen.getByText('Cancellation Reason')).toBeInTheDocument();
    expect(screen.getByText('Confirm Cancellation')).toBeInTheDocument();
  });

  it('shows reason error on blur when cancellation reason is empty (CONFIRMED)', async () => {
    const user = userEvent.setup();
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: CONFIRMED_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Cancel Booking')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Cancel Booking'));

    const textarea = screen.getByPlaceholderText(/Provide a reason for cancellation/i);
    await user.click(textarea);
    await user.tab();

    await waitFor(() => {
      expect(screen.getByText('Reason is required')).toBeInTheDocument();
    });
  });

  it('submits owner cancellation and shows success toast', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const successSpy = vi.spyOn(toast.default, 'success');
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: CONFIRMED_BOOKING });
    bookingApi.cancelOwnerBooking.mockResolvedValue({ data: CANCELLED_BOOKING });

    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Cancel Booking')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Cancel Booking'));
    const textarea = screen.getByPlaceholderText(/Provide a reason for cancellation/i);
    await user.type(textarea, 'Owner emergency.');
    await user.click(screen.getByText('Confirm Cancellation'));

    await waitFor(() => {
      expect(bookingApi.cancelOwnerBooking).toHaveBeenCalledWith('55', 'Owner emergency.');
      expect(successSpy).toHaveBeenCalledWith('Booking cancelled.');
    });
  });

  it('shows error toast when owner cancellation API call fails', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: CONFIRMED_BOOKING });
    bookingApi.cancelOwnerBooking.mockRejectedValue({
      response: { data: { message: 'Cannot cancel confirmed booking' } },
    });

    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Cancel Booking')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Cancel Booking'));
    const textarea = screen.getByPlaceholderText(/Provide a reason for cancellation/i);
    await user.type(textarea, 'Owner emergency.');
    await user.click(screen.getByText('Confirm Cancellation'));

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Cannot cancel confirmed booking');
    });
  });

  // ── Terminal statuses: no action buttons ────────────────────────────────

  it('shows no action buttons for COMPLETED status', async () => {
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: COMPLETED_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.queryByText('Confirm Booking')).not.toBeInTheDocument();
      expect(screen.queryByText('Reject Booking')).not.toBeInTheDocument();
      expect(screen.queryByText('Cancel Booking')).not.toBeInTheDocument();
    });
  });

  it('shows no action buttons for CANCELLED status', async () => {
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: CANCELLED_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.queryByText('Confirm Booking')).not.toBeInTheDocument();
      expect(screen.queryByText('Reject Booking')).not.toBeInTheDocument();
      expect(screen.queryByText('Cancel Booking')).not.toBeInTheDocument();
    });
  });

  it('shows no action buttons for REJECTED status', async () => {
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: REJECTED_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.queryByText('Confirm Booking')).not.toBeInTheDocument();
      expect(screen.queryByText('Reject Booking')).not.toBeInTheDocument();
      expect(screen.queryByText('Cancel Booking')).not.toBeInTheDocument();
    });
  });

  // ── Reason display in terminal states ───────────────────────────────────

  it('shows cancellation reason when booking is CANCELLED', async () => {
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: CANCELLED_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Owner busy.')).toBeInTheDocument();
    });
  });

  it('shows "Rejection Reason" label when booking is REJECTED', async () => {
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: REJECTED_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText(/Rejection Reason/i)).toBeInTheDocument();
    });
  });

  // ── IN_PROGRESS: no action buttons (not pending, not confirmed) ─────────

  it('shows no action buttons for IN_PROGRESS status', async () => {
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: IN_PROGRESS_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.queryByText('Confirm Booking')).not.toBeInTheDocument();
      expect(screen.queryByText('Reject Booking')).not.toBeInTheDocument();
      expect(screen.queryByText('Cancel Booking')).not.toBeInTheDocument();
    });
  });

  // ── Photo grid display for COMPLETED booking ────────────────────────────

  it('displays multiple pickup photos for a completed booking', async () => {
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: COMPLETED_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Pickup Photos (2)')).toBeInTheDocument();
      expect(screen.getByAltText('Pickup photo 1')).toBeInTheDocument();
      expect(screen.getByAltText('Pickup photo 2')).toBeInTheDocument();
    });
  });

  it('displays return photos for a completed booking', async () => {
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: COMPLETED_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Return Photos (1)')).toBeInTheDocument();
      expect(screen.getByAltText('Return photo 1')).toBeInTheDocument();
    });
  });

  it('does not show photo sections when photo arrays are empty', async () => {
    bookingApi.getOwnerBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<OwnerBookingDetailPage />);

    await waitFor(() => {
      expect(screen.queryByText(/Pickup Photos/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/Return Photos/i)).not.toBeInTheDocument();
    });
  });
});
