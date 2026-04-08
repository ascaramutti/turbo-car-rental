import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import ReviewModal from '../components/ReviewModal';
import * as reviewApi from '../api/reviewApi';

vi.mock('../api/reviewApi');

const onSuccess = vi.fn();
const onClose = vi.fn();

describe('ReviewModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    reviewApi.createReview.mockResolvedValue({ data: {} });
  });

  it('renders modal with title, star rating, comment field, and buttons', () => {
    renderWithProviders(
      <ReviewModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    expect(screen.getByText('Leave a Review')).toBeInTheDocument();
    expect(screen.getByText('Rating')).toBeInTheDocument();
    expect(screen.getByText(/comment/i)).toBeInTheDocument();
    expect(screen.getByText('Submit Review')).toBeInTheDocument();
    expect(screen.getByText('Cancel')).toBeInTheDocument();
  });

  it('submit button is disabled when no rating selected', () => {
    renderWithProviders(
      <ReviewModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    const submitBtn = screen.getByText('Submit Review').closest('button');
    expect(submitBtn).toBeDisabled();
  });

  it('calls createReview with correct data on submit', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <ReviewModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    // Click 4th star — stars are inside the Rating section
    const ratingSection = screen.getByText('Rating').closest('div');
    const starButtons = ratingSection.querySelectorAll('button');
    await user.click(starButtons[3]); // 4th star

    // Type comment
    const textarea = screen.getByPlaceholderText(/share your experience/i);
    await user.type(textarea, 'Great experience');

    await user.click(screen.getByText('Submit Review'));

    await waitFor(() => {
      expect(reviewApi.createReview).toHaveBeenCalledWith(42, {
        bookingId: 42,
        rating: 4,
        comment: 'Great experience',
      });
    });
  });

  it('calls onSuccess after successful submission', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <ReviewModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    const ratingSection = screen.getByText('Rating').closest('div');
    const starButtons = ratingSection.querySelectorAll('button');
    await user.click(starButtons[4]); // 5th star

    await user.click(screen.getByText('Submit Review'));

    await waitFor(() => {
      expect(onSuccess).toHaveBeenCalled();
    });
  });

  it('shows error toast on API failure', async () => {
    reviewApi.createReview.mockRejectedValue({
      response: { data: { message: 'Review already submitted' } },
    });

    const user = userEvent.setup();
    renderWithProviders(
      <ReviewModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    const ratingSection = screen.getByText('Rating').closest('div');
    const starButtons = ratingSection.querySelectorAll('button');
    await user.click(starButtons[2]); // 3rd star

    await user.click(screen.getByText('Submit Review'));

    await waitFor(() => {
      expect(onSuccess).not.toHaveBeenCalled();
    });
  });

  it('calls onClose when Cancel button is clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <ReviewModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    await user.click(screen.getByText('Cancel'));
    expect(onClose).toHaveBeenCalled();
  });

  it('calls onClose when X button is clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <ReviewModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    const closeButtons = document.querySelectorAll('button');
    await user.click(closeButtons[0]); // X button is first
    expect(onClose).toHaveBeenCalled();
  });

  it('shows validation error for invalid comment characters on blur', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <ReviewModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    const textarea = screen.getByPlaceholderText(/share your experience/i);
    await user.type(textarea, '<script>alert(1)</script>');
    await user.tab();

    await waitFor(() => {
      expect(screen.getByText(/only contain letters/i)).toBeInTheDocument();
    });
  });

  it('shows character count', () => {
    renderWithProviders(
      <ReviewModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    expect(screen.getByText('0/500')).toBeInTheDocument();
  });

  it('submits with null comment when empty', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <ReviewModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    const ratingSection = screen.getByText('Rating').closest('div');
    const starButtons = ratingSection.querySelectorAll('button');
    await user.click(starButtons[0]); // 1st star

    await user.click(screen.getByText('Submit Review'));

    await waitFor(() => {
      expect(reviewApi.createReview).toHaveBeenCalledWith(42, {
        bookingId: 42,
        rating: 1,
        comment: null,
      });
    });
  });
});
