import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import DriverBookingDetailPage from '../pages/DriverBookingDetailPage';
import * as bookingApi from '../api/bookingApi';

vi.mock('../api/bookingApi');

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
  vehicleSummary: '2022 Toyota Camry',
  status: 'PENDING',
  startTime: '2030-06-01T08:00:00',
  endTime: '2030-06-01T13:00:00',
  totalPrice: 125.0,
  totalHours: 5,
  ownerFullName: 'Bob Owner',
  vehicleMake: 'Toyota',
  vehicleModel: 'Camry',
  vehicleYear: 2022,
  vehicleLicensePlate: 'ABC 123',
  vehicleCategory: 'SEDAN',
  vehicleHourlyRate: 25.0,
  pickupLocation: 'Downtown Vancouver',
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
    'https://example.com/return2.jpg',
    'https://example.com/return3.jpg',
  ],
};
const CANCELLED_BOOKING = {
  ...BASE_BOOKING,
  status: 'CANCELLED',
  cancellationReason: 'Changed my plans.',
  cancelledBy: 'DRIVER',
};
const REJECTED_BOOKING = {
  ...BASE_BOOKING,
  status: 'REJECTED',
  cancellationReason: 'Vehicle unavailable.',
};

describe('DriverBookingDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Default: location fetch returns nothing (non-critical)
    bookingApi.getVehicleLocation.mockRejectedValue(new Error('location unavailable'));
  });

  // ── Loading and basic render ────────────────────────────────────────────

  it('shows loading spinner initially', () => {
    bookingApi.getDriverBookingDetail.mockReturnValue(new Promise(() => {}));
    renderWithProviders(<DriverBookingDetailPage />);
    // During loading, vehicle summary is not yet visible
    expect(screen.queryByText('2022 Toyota Camry')).not.toBeInTheDocument();
  });

  it('shows vehicle summary and booking ID after load', async () => {
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      // vehicleSummary appears as page h1 heading
      expect(screen.getAllByText('2022 Toyota Camry').length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText('Booking #42')).toBeInTheDocument();
    });
  });

  it('shows status badge after load', async () => {
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Pending')).toBeInTheDocument();
    });
  });

  it('shows "Back to My Bookings" button', async () => {
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Back to My Bookings')).toBeInTheDocument();
    });
  });

  it('navigates to /driver/bookings when back button is clicked', async () => {
    const user = userEvent.setup();
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Back to My Bookings')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Back to My Bookings'));

    expect(mockNavigate).toHaveBeenCalledWith('/driver/bookings');
  });

  it('shows booking details: total hours and price', async () => {
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('5h')).toBeInTheDocument();
      expect(screen.getByText('$125.00')).toBeInTheDocument();
    });
  });

  it('navigates to /driver/bookings and shows error toast on fetch failure', async () => {
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');

    bookingApi.getDriverBookingDetail.mockRejectedValue({
      response: { data: { message: 'Booking not found' } },
    });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Booking not found');
      expect(mockNavigate).toHaveBeenCalledWith('/driver/bookings');
    });
  });

  // ── Cancel button: PENDING ──────────────────────────────────────────────

  it('shows Cancel Booking button for PENDING status', async () => {
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Cancel Booking')).toBeInTheDocument();
    });
  });

  it('shows Cancel Booking button for CONFIRMED status', async () => {
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: CONFIRMED_BOOKING });
    bookingApi.getVehicleLocation.mockResolvedValue({
      data: { generalLocation: 'Downtown', isExactLocation: false, latitude: 49.28, longitude: -123.12, message: null },
    });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Cancel Booking')).toBeInTheDocument();
    });
  });

  // ── Start Shift button: CONFIRMED ───────────────────────────────────────

  it('shows Start Shift button for CONFIRMED status', async () => {
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: CONFIRMED_BOOKING });
    bookingApi.getVehicleLocation.mockResolvedValue({
      data: { generalLocation: 'Downtown', isExactLocation: false, latitude: 49.28, longitude: -123.12, message: null },
    });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Start Shift')).toBeInTheDocument();
    });
  });

  it('clicking Start Shift reveals the photo upload form', async () => {
    const user = userEvent.setup();
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: CONFIRMED_BOOKING });
    bookingApi.getVehicleLocation.mockResolvedValue({
      data: { generalLocation: 'Downtown', isExactLocation: false, latitude: 49.28, longitude: -123.12, message: null },
    });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Start Shift')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Start Shift'));

    expect(screen.getByText('Upload Pickup Photo')).toBeInTheDocument();
    expect(screen.getByText('Confirm Start')).toBeInTheDocument();
  });

  // ── Complete Shift button: IN_PROGRESS ──────────────────────────────────

  it('shows Complete Shift button for IN_PROGRESS status', async () => {
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: IN_PROGRESS_BOOKING });
    bookingApi.getVehicleLocation.mockResolvedValue({
      data: { generalLocation: 'Downtown', isExactLocation: true, latitude: 49.28, longitude: -123.12, message: 'Exact location' },
    });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Complete Shift')).toBeInTheDocument();
    });
  });

  it('clicking Complete Shift reveals the return photo upload form', async () => {
    const user = userEvent.setup();
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: IN_PROGRESS_BOOKING });
    bookingApi.getVehicleLocation.mockResolvedValue({
      data: { generalLocation: 'Downtown', isExactLocation: true, latitude: 49.28, longitude: -123.12, message: 'Exact location' },
    });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Complete Shift')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Complete Shift'));

    expect(screen.getByText('Upload Return Photo')).toBeInTheDocument();
    expect(screen.getByText('Confirm Completion')).toBeInTheDocument();
  });

  // ── No action buttons for terminal statuses ─────────────────────────────

  it('shows no action buttons for COMPLETED status', async () => {
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: COMPLETED_BOOKING });
    bookingApi.getVehicleLocation.mockResolvedValue({
      data: { generalLocation: 'Downtown', isExactLocation: true, latitude: 49.28, longitude: -123.12, message: null },
    });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.queryByText('Cancel Booking')).not.toBeInTheDocument();
      expect(screen.queryByText('Start Shift')).not.toBeInTheDocument();
      expect(screen.queryByText('Complete Shift')).not.toBeInTheDocument();
    });
  });

  it('shows no action buttons for CANCELLED status', async () => {
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: CANCELLED_BOOKING });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.queryByText('Cancel Booking')).not.toBeInTheDocument();
      expect(screen.queryByText('Start Shift')).not.toBeInTheDocument();
    });
  });

  it('shows no action buttons for REJECTED status', async () => {
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: REJECTED_BOOKING });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.queryByText('Cancel Booking')).not.toBeInTheDocument();
      expect(screen.queryByText('Start Shift')).not.toBeInTheDocument();
    });
  });

  // ── Cancel action flow ──────────────────────────────────────────────────

  it('shows cancellation reason form when Cancel Booking is clicked', async () => {
    const user = userEvent.setup();
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Cancel Booking')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Cancel Booking'));

    expect(screen.getByText('Cancellation Reason')).toBeInTheDocument();
    expect(screen.getByText('Confirm Cancellation')).toBeInTheDocument();
  });

  it('shows reason error on blur when reason is empty', async () => {
    const user = userEvent.setup();
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<DriverBookingDetailPage />);

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

  it('shows reason error on blur when reason contains invalid characters', async () => {
    const user = userEvent.setup();
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Cancel Booking')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Cancel Booking'));

    const textarea = screen.getByPlaceholderText(/Provide a reason for cancellation/i);
    await user.type(textarea, '<script>alert(1)</script>');
    await user.tab();

    await waitFor(() => {
      expect(screen.getByText(/only contain letters, numbers/i)).toBeInTheDocument();
    });
  });

  it('does not show reason error on blur when reason is valid', async () => {
    const user = userEvent.setup();
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Cancel Booking')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Cancel Booking'));

    const textarea = screen.getByPlaceholderText(/Provide a reason for cancellation/i);
    await user.type(textarea, 'Changed my plans.');
    await user.tab();

    await waitFor(() => {
      expect(screen.queryByText('Reason is required')).not.toBeInTheDocument();
    });
  });

  it('submits cancellation and shows success toast', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const successSpy = vi.spyOn(toast.default, 'success');
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    bookingApi.cancelDriverBooking.mockResolvedValue({ data: CANCELLED_BOOKING });

    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Cancel Booking')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Cancel Booking'));
    const textarea = screen.getByPlaceholderText(/Provide a reason for cancellation/i);
    await user.type(textarea, 'Changed my plans.');
    await user.click(screen.getByText('Confirm Cancellation'));

    await waitFor(() => {
      expect(bookingApi.cancelDriverBooking).toHaveBeenCalledWith('42', 'Changed my plans.');
      expect(successSpy).toHaveBeenCalledWith('Booking cancelled successfully');
    });
  });

  it('shows error toast when cancellation API call fails', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const errorSpy = vi.spyOn(toast.default, 'error');
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: BASE_BOOKING });
    bookingApi.cancelDriverBooking.mockRejectedValue({
      response: { data: { message: 'Cannot cancel booking' } },
    });

    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Cancel Booking')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Cancel Booking'));
    const textarea = screen.getByPlaceholderText(/Provide a reason for cancellation/i);
    await user.type(textarea, 'Changed my plans.');
    await user.click(screen.getByText('Confirm Cancellation'));

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('Cannot cancel booking');
    });
  });

  // ── Start Shift action flow ─────────────────────────────────────────────

  it('shows pickup photo error when Confirm Start is clicked without photos', async () => {
    const user = userEvent.setup();
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: CONFIRMED_BOOKING });
    bookingApi.getVehicleLocation.mockResolvedValue({
      data: { generalLocation: 'Downtown', isExactLocation: false, latitude: 49.28, longitude: -123.12, message: null },
    });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Start Shift')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Start Shift'));
    await user.click(screen.getByText('Confirm Start'));

    await waitFor(() => {
      expect(
        screen.getByText('At least one pickup photo is required to start the shift')
      ).toBeInTheDocument();
    });

    expect(bookingApi.startBooking).not.toHaveBeenCalled();
  });

  it('submits start shift with array of photos and shows success toast', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const successSpy = vi.spyOn(toast.default, 'success');
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: CONFIRMED_BOOKING });
    bookingApi.getVehicleLocation.mockResolvedValue({
      data: { generalLocation: 'Downtown', isExactLocation: false, latitude: 49.28, longitude: -123.12, message: null },
    });
    bookingApi.startBooking.mockResolvedValue({ data: IN_PROGRESS_BOOKING });

    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Start Shift')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Start Shift'));

    const photoInput = document.querySelector('input[type="file"]');
    const photoFile = new File(['photo'], 'pickup.jpg', { type: 'image/jpeg' });
    fireEvent.change(photoInput, { target: { files: [photoFile] } });

    await user.click(screen.getByText('Confirm Start'));

    await waitFor(() => {
      expect(bookingApi.startBooking).toHaveBeenCalledWith('42', [photoFile]);
      expect(successSpy).toHaveBeenCalledWith('Shift started! Have a safe trip.');
    });
  });

  it('submits start shift with multiple photos', async () => {
    const user = userEvent.setup();
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: CONFIRMED_BOOKING });
    bookingApi.getVehicleLocation.mockResolvedValue({
      data: { generalLocation: 'Downtown', isExactLocation: false, latitude: 49.28, longitude: -123.12, message: null },
    });
    bookingApi.startBooking.mockResolvedValue({ data: IN_PROGRESS_BOOKING });

    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Start Shift')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Start Shift'));

    const photoInput = document.querySelector('input[type="file"]');
    const photo1 = new File(['photo1'], 'pickup1.jpg', { type: 'image/jpeg' });
    const photo2 = new File(['photo2'], 'pickup2.jpg', { type: 'image/jpeg' });
    fireEvent.change(photoInput, { target: { files: [photo1, photo2] } });

    await user.click(screen.getByText('Confirm Start'));

    await waitFor(() => {
      expect(bookingApi.startBooking).toHaveBeenCalledWith('42', [photo1, photo2]);
    });
  });

  // ── Complete Shift action flow ──────────────────────────────────────────

  it('shows return photo error when Confirm Completion is clicked without photos', async () => {
    const user = userEvent.setup();
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: IN_PROGRESS_BOOKING });
    bookingApi.getVehicleLocation.mockResolvedValue({
      data: { generalLocation: 'Downtown', isExactLocation: true, latitude: 49.28, longitude: -123.12, message: 'Exact location' },
    });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Complete Shift')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Complete Shift'));
    await user.click(screen.getByText('Confirm Completion'));

    await waitFor(() => {
      expect(
        screen.getByText('At least one return photo is required to complete the shift')
      ).toBeInTheDocument();
    });

    expect(bookingApi.completeBooking).not.toHaveBeenCalled();
  });

  it('submits complete shift with array of photos and shows success toast', async () => {
    const user = userEvent.setup();
    const toast = await import('react-hot-toast');
    const successSpy = vi.spyOn(toast.default, 'success');
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: IN_PROGRESS_BOOKING });
    bookingApi.getVehicleLocation.mockResolvedValue({
      data: { generalLocation: 'Downtown', isExactLocation: true, latitude: 49.28, longitude: -123.12, message: 'Exact location' },
    });
    bookingApi.completeBooking.mockResolvedValue({ data: COMPLETED_BOOKING });

    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Complete Shift')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Complete Shift'));

    const photoInput = document.querySelector('input[type="file"]');
    const returnFile = new File(['return photo'], 'return.jpg', { type: 'image/jpeg' });
    fireEvent.change(photoInput, { target: { files: [returnFile] } });

    await user.click(screen.getByText('Confirm Completion'));

    await waitFor(() => {
      expect(bookingApi.completeBooking).toHaveBeenCalledWith('42', [returnFile]);
      expect(successSpy).toHaveBeenCalledWith('Shift completed successfully');
    });
  });

  it('submits complete shift with multiple return photos', async () => {
    const user = userEvent.setup();
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: IN_PROGRESS_BOOKING });
    bookingApi.getVehicleLocation.mockResolvedValue({
      data: { generalLocation: 'Downtown', isExactLocation: true, latitude: 49.28, longitude: -123.12, message: 'Exact location' },
    });
    bookingApi.completeBooking.mockResolvedValue({ data: COMPLETED_BOOKING });

    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Complete Shift')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Complete Shift'));

    const photoInput = document.querySelector('input[type="file"]');
    const return1 = new File(['return1'], 'return1.jpg', { type: 'image/jpeg' });
    const return2 = new File(['return2'], 'return2.jpg', { type: 'image/jpeg' });
    fireEvent.change(photoInput, { target: { files: [return1, return2] } });

    await user.click(screen.getByText('Confirm Completion'));

    await waitFor(() => {
      expect(bookingApi.completeBooking).toHaveBeenCalledWith('42', [return1, return2]);
    });
  });

  // ── Photo grid display for COMPLETED booking ────────────────────────────

  it('displays multiple pickup photos for a completed booking', async () => {
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: COMPLETED_BOOKING });
    bookingApi.getVehicleLocation.mockResolvedValue({
      data: { generalLocation: 'Downtown', isExactLocation: true, latitude: 49.28, longitude: -123.12, message: null },
    });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Pickup Photos (2)')).toBeInTheDocument();
      expect(screen.getByAltText('Pickup photo 1')).toBeInTheDocument();
      expect(screen.getByAltText('Pickup photo 2')).toBeInTheDocument();
    });
  });

  it('displays multiple return photos for a completed booking', async () => {
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: COMPLETED_BOOKING });
    bookingApi.getVehicleLocation.mockResolvedValue({
      data: { generalLocation: 'Downtown', isExactLocation: true, latitude: 49.28, longitude: -123.12, message: null },
    });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Return Photos (3)')).toBeInTheDocument();
      expect(screen.getByAltText('Return photo 1')).toBeInTheDocument();
      expect(screen.getByAltText('Return photo 2')).toBeInTheDocument();
      expect(screen.getByAltText('Return photo 3')).toBeInTheDocument();
    });
  });

  // ── Cancellation reason display ─────────────────────────────────────────

  it('shows cancellation reason when booking is CANCELLED', async () => {
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: CANCELLED_BOOKING });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText('Changed my plans.')).toBeInTheDocument();
    });
  });

  it('shows "Rejection Reason" label when booking is REJECTED', async () => {
    bookingApi.getDriverBookingDetail.mockResolvedValue({ data: REJECTED_BOOKING });
    renderWithProviders(<DriverBookingDetailPage />);

    await waitFor(() => {
      expect(screen.getByText(/Rejection Reason/i)).toBeInTheDocument();
    });
  });
});
