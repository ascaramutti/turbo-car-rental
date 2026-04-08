import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { renderWithProviders } from '../../../test/renderWithProviders';
import MyReviewsPage from '../pages/MyReviewsPage';
import * as reviewApi from '../api/reviewApi';

vi.mock('../api/reviewApi');

const REVIEWS = [
  {
    reviewId: 1,
    reviewerName: 'John Driver',
    reviewType: 'DRIVER_TO_OWNER',
    vehicleSummary: '2022 Toyota Camry',
    rating: 5,
    comment: 'Excellent car',
    createdAt: '2026-04-01T10:00:00',
  },
  {
    reviewId: 2,
    reviewerName: 'Jane Driver',
    reviewType: 'DRIVER_TO_OWNER',
    vehicleSummary: '2020 Honda Civic',
    rating: 3,
    comment: null,
    createdAt: '2026-03-28T14:00:00',
  },
];

// Mock localStorage with a logged-in user
beforeEach(() => {
  vi.clearAllMocks();
  localStorage.setItem('token', 'fake-jwt');
  localStorage.setItem('user', JSON.stringify({ userId: 20, firstName: 'Sarah', lastName: 'Owner', role: 'CAR_OWNER' }));
});

describe('MyReviewsPage', () => {
  it('shows loading spinner initially', () => {
    reviewApi.getUserReviews.mockReturnValue(new Promise(() => {}));
    renderWithProviders(<MyReviewsPage />);
    expect(document.querySelector('.animate-spin')).not.toBeNull();
  });

  it('shows reviews after loading', async () => {
    reviewApi.getUserReviews.mockResolvedValue({ data: REVIEWS });
    renderWithProviders(<MyReviewsPage />);

    await waitFor(() => {
      expect(screen.getByText('John Driver')).toBeInTheDocument();
    });
    expect(screen.getByText('Jane Driver')).toBeInTheDocument();
  });

  it('shows average rating', async () => {
    reviewApi.getUserReviews.mockResolvedValue({ data: REVIEWS });
    renderWithProviders(<MyReviewsPage />);

    await waitFor(() => {
      expect(screen.getByText('4.0')).toBeInTheDocument();
    });
  });

  it('shows review count', async () => {
    reviewApi.getUserReviews.mockResolvedValue({ data: REVIEWS });
    renderWithProviders(<MyReviewsPage />);

    await waitFor(() => {
      expect(screen.getByText('2 reviews received')).toBeInTheDocument();
    });
  });

  it('shows empty state when no reviews', async () => {
    reviewApi.getUserReviews.mockResolvedValue({ data: [] });
    renderWithProviders(<MyReviewsPage />);

    await waitFor(() => {
      expect(screen.getByText(/no reviews yet/i)).toBeInTheDocument();
    });
  });

  it('shows N/A rating when no reviews', async () => {
    reviewApi.getUserReviews.mockResolvedValue({ data: [] });
    renderWithProviders(<MyReviewsPage />);

    await waitFor(() => {
      expect(screen.getByText('N/A')).toBeInTheDocument();
    });
  });
});
