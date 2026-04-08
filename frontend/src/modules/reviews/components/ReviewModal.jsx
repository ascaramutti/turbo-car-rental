import { useState } from 'react';
import { X, Loader2, Send } from 'lucide-react';
import toast from 'react-hot-toast';
import StarRating from './StarRating';
import { createReview } from '../api/reviewApi';
import { extractErrorMessage } from '../../auth/utils/validation';
import { REVIEW_CONSTRAINTS } from '../constants/reviewConstants';

/**
 * Modal for submitting a review after a completed booking.
 * @param {{ bookingId: number, onSuccess: Function, onClose: Function }} props
 */
export default function ReviewModal({ bookingId, onSuccess, onClose }) {
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState('');
  const [commentError, setCommentError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validateComment = (value) => {
    if (value && !REVIEW_CONSTRAINTS.COMMENT_PATTERN.test(value)) {
      return 'Comment can only contain letters, numbers, spaces, and basic punctuation';
    }
    if (value && value.length > REVIEW_CONSTRAINTS.MAX_COMMENT_LENGTH) {
      return `Comment must not exceed ${REVIEW_CONSTRAINTS.MAX_COMMENT_LENGTH} characters`;
    }
    return null;
  };

  const handleCommentBlur = () => {
    setCommentError(validateComment(comment));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (rating === 0) {
      toast.error('Please select a rating');
      return;
    }
    const error = validateComment(comment);
    if (error) {
      setCommentError(error);
      return;
    }

    setIsSubmitting(true);
    try {
      await createReview(bookingId, {
        bookingId,
        rating,
        comment: comment.trim() || null,
      });
      toast.success('Review submitted!');
      onSuccess();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md mx-4 p-6">
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold text-text-dark">Leave a Review</h2>
          <button type="button" onClick={onClose} className="text-text-gray hover:text-text-dark transition-colors">
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Rating */}
          <div>
            <label className="block text-sm font-semibold text-text-dark mb-2">Rating</label>
            <StarRating value={rating} onChange={setRating} size={28} />
          </div>

          {/* Comment */}
          <div>
            <label className="block text-sm font-semibold text-text-dark mb-1">
              Comment <span className="text-text-gray font-normal">(optional)</span>
            </label>
            <textarea
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              onBlur={handleCommentBlur}
              placeholder="Share your experience..."
              maxLength={REVIEW_CONSTRAINTS.MAX_COMMENT_LENGTH}
              rows={3}
              className="w-full px-3 py-2 border-2 border-gray-200 rounded-lg text-sm focus:border-accent-orange focus:outline-none transition-colors resize-none"
            />
            {commentError && (
              <p className="mt-1 text-xs text-red-500">{commentError}</p>
            )}
            <p className="mt-1 text-xs text-text-gray text-right">
              {comment.length}/{REVIEW_CONSTRAINTS.MAX_COMMENT_LENGTH}
            </p>
          </div>

          {/* Buttons */}
          <div className="flex gap-2">
            <button
              type="submit"
              disabled={isSubmitting || rating === 0}
              className="flex-1 flex items-center justify-center gap-2 py-2.5 bg-accent-orange text-white font-bold rounded-full hover:bg-accent-orange-light transition-colors disabled:opacity-50"
            >
              {isSubmitting ? <Loader2 size={16} className="animate-spin" /> : <Send size={16} />}
              {isSubmitting ? 'Submitting...' : 'Submit Review'}
            </button>
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2.5 border-2 border-gray-200 text-text-gray font-semibold rounded-full hover:bg-gray-50 transition-colors disabled:opacity-50"
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
