import { describe, it, expect, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import BookingCard from '../components/BookingCard';

const BASE_BOOKING = {
  bookingId: 42,
  vehicleSummary: '2022 Toyota Camry',
  status: 'PENDING',
  startTime: '2030-06-01T08:00:00',
  endTime: '2030-06-01T13:00:00',
  totalPrice: 125.0,
  totalHours: 5,
  driverFullName: null,
};

describe('BookingCard', () => {
  it('renders the vehicle summary', () => {
    renderWithProviders(<BookingCard booking={BASE_BOOKING} onClick={vi.fn()} />);
    expect(screen.getByText('2022 Toyota Camry')).toBeInTheDocument();
  });

  it('renders the booking ID', () => {
    renderWithProviders(<BookingCard booking={BASE_BOOKING} onClick={vi.fn()} />);
    expect(screen.getByText('Booking #42')).toBeInTheDocument();
  });

  it('renders total price and hours', () => {
    renderWithProviders(<BookingCard booking={BASE_BOOKING} onClick={vi.fn()} />);
    expect(screen.getByText(/\$125\.00.*5h/)).toBeInTheDocument();
  });

  it('renders the status badge for PENDING', () => {
    renderWithProviders(<BookingCard booking={BASE_BOOKING} onClick={vi.fn()} />);
    expect(screen.getByText('Pending')).toBeInTheDocument();
  });

  it('renders the status badge for CONFIRMED', () => {
    const booking = { ...BASE_BOOKING, status: 'CONFIRMED' };
    renderWithProviders(<BookingCard booking={booking} onClick={vi.fn()} />);
    expect(screen.getByText('Confirmed')).toBeInTheDocument();
  });

  it('renders the status badge for IN_PROGRESS', () => {
    const booking = { ...BASE_BOOKING, status: 'IN_PROGRESS' };
    renderWithProviders(<BookingCard booking={booking} onClick={vi.fn()} />);
    expect(screen.getByText('In Progress')).toBeInTheDocument();
  });

  it('renders the status badge for COMPLETED', () => {
    const booking = { ...BASE_BOOKING, status: 'COMPLETED' };
    renderWithProviders(<BookingCard booking={booking} onClick={vi.fn()} />);
    expect(screen.getByText('Completed')).toBeInTheDocument();
  });

  it('renders the status badge for CANCELLED', () => {
    const booking = { ...BASE_BOOKING, status: 'CANCELLED' };
    renderWithProviders(<BookingCard booking={booking} onClick={vi.fn()} />);
    expect(screen.getByText('Cancelled')).toBeInTheDocument();
  });

  it('renders the status badge for REJECTED', () => {
    const booking = { ...BASE_BOOKING, status: 'REJECTED' };
    renderWithProviders(<BookingCard booking={booking} onClick={vi.fn()} />);
    expect(screen.getByText('Rejected')).toBeInTheDocument();
  });

  it('calls onClick when the card is clicked', async () => {
    const user = userEvent.setup();
    const handleClick = vi.fn();
    renderWithProviders(<BookingCard booking={BASE_BOOKING} onClick={handleClick} />);

    await user.click(screen.getByText('2022 Toyota Camry'));

    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('shows driver name when driverFullName is present', () => {
    const booking = { ...BASE_BOOKING, driverFullName: 'Alice Driver' };
    renderWithProviders(<BookingCard booking={booking} onClick={vi.fn()} />);
    expect(screen.getByText('Alice Driver')).toBeInTheDocument();
  });

  it('does not show driver name section when driverFullName is null', () => {
    renderWithProviders(<BookingCard booking={BASE_BOOKING} onClick={vi.fn()} />);
    expect(screen.queryByText(/Driver:/)).not.toBeInTheDocument();
  });
});
