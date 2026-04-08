import StarRating from './StarRating';
import { REVIEW_TYPE_LABELS } from '../constants/reviewConstants';
import { formatDateTime } from '../../../shared/utils/dateUtils';

/**
 * Displays a single review with rating, reviewer name, comment, and metadata.
 * @param {{ review: Object }} props
 */
export default function ReviewCard({ review }) {
  return (
    <div className="bg-white border-2 border-gray-200 rounded-xl p-4">
      <div className="flex items-start justify-between mb-2">
        <div>
          <p className="text-sm font-bold text-text-dark">{review.reviewerName}</p>
          <p className="text-xs text-text-gray">
            {REVIEW_TYPE_LABELS[review.reviewType] || review.reviewType}
            {review.vehicleSummary && ` — ${review.vehicleSummary}`}
          </p>
        </div>
        <span className="text-xs text-text-gray whitespace-nowrap">
          {formatDateTime(review.createdAt)}
        </span>
      </div>

      <StarRating value={review.rating} readonly size={16} />

      {review.comment && (
        <p className="mt-2 text-sm text-text-gray">{review.comment}</p>
      )}
    </div>
  );
}
