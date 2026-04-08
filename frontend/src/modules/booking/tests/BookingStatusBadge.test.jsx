import { describe, it, expect } from 'vitest';
import { screen } from '@testing-library/react';
import { renderWithProviders } from '../../../test/renderWithProviders';
import BookingStatusBadge from '../components/BookingStatusBadge';

describe('BookingStatusBadge', () => {
  it('renders "Pending" label for PENDING status', () => {
    renderWithProviders(<BookingStatusBadge status="PENDING" />);
    expect(screen.getByText('Pending')).toBeInTheDocument();
  });

  it('applies amber color class for PENDING status', () => {
    renderWithProviders(<BookingStatusBadge status="PENDING" />);
    const badge = screen.getByText('Pending');
    expect(badge).toHaveClass('text-amber-600');
  });

  it('renders "Confirmed" label for CONFIRMED status', () => {
    renderWithProviders(<BookingStatusBadge status="CONFIRMED" />);
    expect(screen.getByText('Confirmed')).toBeInTheDocument();
  });

  it('applies blue color class for CONFIRMED status', () => {
    renderWithProviders(<BookingStatusBadge status="CONFIRMED" />);
    const badge = screen.getByText('Confirmed');
    expect(badge).toHaveClass('text-blue-600');
  });

  it('renders "In Progress" label for IN_PROGRESS status', () => {
    renderWithProviders(<BookingStatusBadge status="IN_PROGRESS" />);
    expect(screen.getByText('In Progress')).toBeInTheDocument();
  });

  it('applies purple color class for IN_PROGRESS status', () => {
    renderWithProviders(<BookingStatusBadge status="IN_PROGRESS" />);
    const badge = screen.getByText('In Progress');
    expect(badge).toHaveClass('text-purple-600');
  });

  it('renders "Completed" label for COMPLETED status', () => {
    renderWithProviders(<BookingStatusBadge status="COMPLETED" />);
    expect(screen.getByText('Completed')).toBeInTheDocument();
  });

  it('applies green color class for COMPLETED status', () => {
    renderWithProviders(<BookingStatusBadge status="COMPLETED" />);
    const badge = screen.getByText('Completed');
    expect(badge).toHaveClass('text-green-600');
  });

  it('renders "Cancelled" label for CANCELLED status', () => {
    renderWithProviders(<BookingStatusBadge status="CANCELLED" />);
    expect(screen.getByText('Cancelled')).toBeInTheDocument();
  });

  it('applies gray color class for CANCELLED status', () => {
    renderWithProviders(<BookingStatusBadge status="CANCELLED" />);
    const badge = screen.getByText('Cancelled');
    expect(badge).toHaveClass('text-gray-500');
  });

  it('renders "Rejected" label for REJECTED status', () => {
    renderWithProviders(<BookingStatusBadge status="REJECTED" />);
    expect(screen.getByText('Rejected')).toBeInTheDocument();
  });

  it('applies red color class for REJECTED status', () => {
    renderWithProviders(<BookingStatusBadge status="REJECTED" />);
    const badge = screen.getByText('Rejected');
    expect(badge).toHaveClass('text-red-500');
  });

  it('renders raw status text for unknown status values', () => {
    renderWithProviders(<BookingStatusBadge status="UNKNOWN_STATUS" />);
    expect(screen.getByText('UNKNOWN_STATUS')).toBeInTheDocument();
  });

  it('applies fallback gray styling for unknown status values', () => {
    renderWithProviders(<BookingStatusBadge status="UNKNOWN_STATUS" />);
    const badge = screen.getByText('UNKNOWN_STATUS');
    expect(badge).toHaveClass('text-gray-500');
  });
});
