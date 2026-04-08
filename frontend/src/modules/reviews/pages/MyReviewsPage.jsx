import { useState, useEffect } from 'react';
import { Loader2, Star } from 'lucide-react';
import toast from 'react-hot-toast';
import { getUserReviews } from '../api/reviewApi';
import { extractErrorMessage } from '../../auth/utils/validation';
import { useAuth } from '../../auth/context/useAuth';
import ReviewCard from '../components/ReviewCard';

export default function MyReviewsPage() {
  const { user } = useAuth();
  const [reviews, setReviews] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (!user?.userId) return;
    getUserReviews(user.userId)
      .then(({ data }) => setReviews(data))
      .catch((err) => toast.error(extractErrorMessage(err)))
      .finally(() => setIsLoading(false));
  }, [user?.userId]);

  const averageRating = reviews.length > 0
    ? (reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length).toFixed(1)
    : null;

  if (isLoading) {
    return (
      <div className="flex-1 flex items-center justify-center py-20">
        <Loader2 size={32} className="animate-spin text-accent-orange" />
      </div>
    );
  }

  return (
    <div className="flex-1 bg-bg-light px-6 py-8">
      <div className="max-w-3xl mx-auto">

        <div className="mb-6">
          <h1 className="text-2xl font-bold text-text-dark">Reviews</h1>
          <p className="text-text-gray text-sm mt-1">Reviews received from other users.</p>
        </div>

        {/* Summary */}
        <div className="bg-white border-2 border-gray-200 rounded-xl p-5 mb-6 flex items-center gap-6">
          <div className="flex items-center gap-2">
            <Star size={20} className="text-turbo-yellow fill-turbo-yellow" />
            <span className="text-2xl font-bold text-text-dark">
              {averageRating ?? 'N/A'}
            </span>
            {averageRating && <span className="text-text-gray text-sm">/ 5</span>}
          </div>
          <div className="text-sm text-text-gray">
            {reviews.length} {reviews.length === 1 ? 'review' : 'reviews'} received
          </div>
        </div>

        {/* Reviews list */}
        {reviews.length === 0 ? (
          <div className="text-center py-10 text-text-gray text-sm">
            No reviews yet. Reviews will appear here after completed bookings.
          </div>
        ) : (
          <div className="space-y-3">
            {reviews.map((review) => (
              <ReviewCard key={review.reviewId} review={review} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
