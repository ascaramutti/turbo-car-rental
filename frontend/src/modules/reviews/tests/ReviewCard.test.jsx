import { describe, it, expect } from 'vitest';
import { screen } from '@testing-library/react';
import { renderWithProviders } from '../../../test/renderWithProviders';
import ReviewCard from '../components/ReviewCard';

const REVIEW = {
  reviewId: 1,
  reviewerName: 'John Driver',
  reviewType: 'DRIVER_TO_OWNER',
  vehicleSummary: '2022 Toyota Camry',
  rating: 4,
  comment: 'Great vehicle, very clean.',
  createdAt: '2026-04-01T10:00:00',
};

describe('ReviewCard', () => {
  it('renders reviewer name', () => {
    renderWithProviders(<ReviewCard review={REVIEW} />);
    expect(screen.getByText('John Driver')).toBeInTheDocument();
  });

  it('renders review type label and vehicle summary', () => {
    renderWithProviders(<ReviewCard review={REVIEW} />);
    expect(screen.getByText(/Driver → Owner/)).toBeInTheDocument();
    expect(screen.getByText(/2022 Toyota Camry/)).toBeInTheDocument();
  });

  it('renders comment text', () => {
    renderWithProviders(<ReviewCard review={REVIEW} />);
    expect(screen.getByText('Great vehicle, very clean.')).toBeInTheDocument();
  });

  it('does not render comment when null', () => {
    const noComment = { ...REVIEW, comment: null };
    renderWithProviders(<ReviewCard review={noComment} />);
    expect(screen.queryByText('Great vehicle')).not.toBeInTheDocument();
  });

  it('renders star rating with correct number of filled stars', () => {
    const { container } = renderWithProviders(<ReviewCard review={REVIEW} />);
    const filled = container.querySelectorAll('.fill-turbo-yellow');
    expect(filled).toHaveLength(4);
  });
});
