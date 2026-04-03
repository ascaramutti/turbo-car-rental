import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import BookingForm from '../components/BookingForm';

/**
 * Helper: returns a datetime-local string (YYYY-MM-DDTHH:mm) offset from now by `hours`.
 */
function datetimeLocalOffset(hours) {
  const d = new Date();
  d.setHours(d.getHours() + hours);
  // Truncate to minute precision (datetime-local format)
  return d.toISOString().slice(0, 16);
}

describe('BookingForm', () => {
  const mockOnSubmit = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the Start Time and End Time labels', () => {
    renderWithProviders(<BookingForm vehicleId={10} onSubmit={mockOnSubmit} isLoading={false} />);
    expect(screen.getByText('Start Time')).toBeInTheDocument();
    expect(screen.getByText('End Time')).toBeInTheDocument();
  });

  it('renders the "Request Booking" submit button', () => {
    renderWithProviders(<BookingForm vehicleId={10} onSubmit={mockOnSubmit} isLoading={false} />);
    expect(screen.getByRole('button', { name: /Request Booking/i })).toBeInTheDocument();
  });

  it('shows loading button text and disabled state when isLoading is true', () => {
    renderWithProviders(<BookingForm vehicleId={10} onSubmit={mockOnSubmit} isLoading={true} />);
    const btn = screen.getByRole('button', { name: /Booking/i });
    expect(btn).toBeDisabled();
  });

  it('shows min/max shift hint text', () => {
    renderWithProviders(<BookingForm vehicleId={10} onSubmit={mockOnSubmit} isLoading={false} />);
    expect(screen.getByText(/4.*24 hours/i)).toBeInTheDocument();
  });

  // ── onBlur: startTime ───────────────────────────────────────────────────

  it('shows error on blur for startTime when value is a past date', async () => {
    const user = userEvent.setup();
    renderWithProviders(<BookingForm vehicleId={10} onSubmit={mockOnSubmit} isLoading={false} />);

    const startInput = document.querySelector('input[name="startTime"]');
    await user.type(startInput, '2020-01-01T08:00');
    await user.tab();

    await waitFor(() => {
      expect(screen.getByText(/Start time must be in the future/i)).toBeInTheDocument();
    });
  });

  it('shows error on blur for startTime when value is empty', async () => {
    const user = userEvent.setup();
    renderWithProviders(<BookingForm vehicleId={10} onSubmit={mockOnSubmit} isLoading={false} />);

    const startInput = document.querySelector('input[name="startTime"]');
    await user.click(startInput);
    await user.tab();

    await waitFor(() => {
      expect(screen.getByText(/Start time is required/i)).toBeInTheDocument();
    });
  });

  it('does not show startTime error on blur when value is a valid future date', async () => {
    const user = userEvent.setup();
    renderWithProviders(<BookingForm vehicleId={10} onSubmit={mockOnSubmit} isLoading={false} />);

    const startInput = document.querySelector('input[name="startTime"]');
    const futureValue = datetimeLocalOffset(3);
    await user.type(startInput, futureValue);
    await user.tab();

    await waitFor(() => {
      expect(screen.queryByText(/Start time must be in the future/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/Start time is required/i)).not.toBeInTheDocument();
    });
  });

  // ── onBlur: endTime ─────────────────────────────────────────────────────

  it('shows error on blur for endTime when gap is less than 4 hours', async () => {
    const user = userEvent.setup();
    renderWithProviders(<BookingForm vehicleId={10} onSubmit={mockOnSubmit} isLoading={false} />);

    const startInput = document.querySelector('input[name="startTime"]');
    const endInput = document.querySelector('input[name="endTime"]');

    await user.type(startInput, '2030-06-01T08:00');
    await user.type(endInput, '2030-06-01T10:00'); // 2h gap — too short
    await user.tab();

    await waitFor(() => {
      expect(screen.getByText(/Minimum booking duration is 4 hours/i)).toBeInTheDocument();
    });
  });

  it('shows error on blur for endTime when gap is more than 24 hours', async () => {
    const user = userEvent.setup();
    renderWithProviders(<BookingForm vehicleId={10} onSubmit={mockOnSubmit} isLoading={false} />);

    const startInput = document.querySelector('input[name="startTime"]');
    const endInput = document.querySelector('input[name="endTime"]');

    await user.type(startInput, '2030-06-01T08:00');
    await user.type(endInput, '2030-06-03T08:00'); // 48h gap — too long
    await user.tab();

    await waitFor(() => {
      expect(screen.getByText(/Maximum booking duration is 24 hours/i)).toBeInTheDocument();
    });
  });

  it('does not show endTime error on blur when gap is valid', async () => {
    const user = userEvent.setup();
    renderWithProviders(<BookingForm vehicleId={10} onSubmit={mockOnSubmit} isLoading={false} />);

    const startInput = document.querySelector('input[name="startTime"]');
    const endInput = document.querySelector('input[name="endTime"]');

    await user.type(startInput, '2030-06-01T08:00');
    await user.type(endInput, '2030-06-01T14:00'); // 6h gap — valid
    await user.tab();

    await waitFor(() => {
      expect(screen.queryByText(/Minimum booking duration/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/Maximum booking duration/i)).not.toBeInTheDocument();
    });
  });

  it('shows error on blur for endTime when value is empty', async () => {
    const user = userEvent.setup();
    renderWithProviders(<BookingForm vehicleId={10} onSubmit={mockOnSubmit} isLoading={false} />);

    const endInput = document.querySelector('input[name="endTime"]');
    await user.click(endInput);
    await user.tab();

    await waitFor(() => {
      expect(screen.getByText(/End time is required/i)).toBeInTheDocument();
    });
  });

  // ── Submit behaviour ────────────────────────────────────────────────────

  it('shows validation errors and does not call onSubmit when form is empty', async () => {
    const user = userEvent.setup();
    renderWithProviders(<BookingForm vehicleId={10} onSubmit={mockOnSubmit} isLoading={false} />);

    await user.click(screen.getByRole('button', { name: /Request Booking/i }));

    await waitFor(() => {
      expect(screen.getByText(/Start time is required/i)).toBeInTheDocument();
      expect(screen.getByText(/End time is required/i)).toBeInTheDocument();
    });

    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it('calls onSubmit with correct data when form is valid', async () => {
    const user = userEvent.setup();
    renderWithProviders(<BookingForm vehicleId={10} onSubmit={mockOnSubmit} isLoading={false} />);

    const startInput = document.querySelector('input[name="startTime"]');
    const endInput = document.querySelector('input[name="endTime"]');

    await user.type(startInput, '2030-06-01T08:00');
    await user.type(endInput, '2030-06-01T14:00'); // 6h valid gap

    await user.click(screen.getByRole('button', { name: /Request Booking/i }));

    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledTimes(1);
    });

    const callArgs = mockOnSubmit.mock.calls[0][0];
    expect(callArgs.vehicleId).toBe(10);
    expect(callArgs.startTime).toBe('2030-06-01T08:00');
    expect(callArgs.endTime).toBe('2030-06-01T14:00');
  });

  it('does not call onSubmit when endTime gap is invalid', async () => {
    const user = userEvent.setup();
    renderWithProviders(<BookingForm vehicleId={10} onSubmit={mockOnSubmit} isLoading={false} />);

    const startInput = document.querySelector('input[name="startTime"]');
    const endInput = document.querySelector('input[name="endTime"]');

    await user.type(startInput, '2030-06-01T08:00');
    await user.type(endInput, '2030-06-01T09:00'); // only 1h

    await user.click(screen.getByRole('button', { name: /Request Booking/i }));

    await waitFor(() => {
      expect(screen.getByText(/Minimum booking duration is 4 hours/i)).toBeInTheDocument();
    });

    expect(mockOnSubmit).not.toHaveBeenCalled();
  });
});
