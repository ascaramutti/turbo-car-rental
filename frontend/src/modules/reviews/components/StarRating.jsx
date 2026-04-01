import { Star } from 'lucide-react';
import { REVIEW_CONSTRAINTS } from '../constants/reviewConstants';

/**
 * Interactive or read-only star rating component.
 * @param {{ value: number, onChange?: Function, readonly?: boolean, size?: number }} props
 */
export default function StarRating({ value = 0, onChange, readonly = false, size = 20 }) {
  return (
    <div className="flex items-center gap-0.5">
      {Array.from({ length: REVIEW_CONSTRAINTS.MAX_RATING }, (_, i) => {
        const starValue = i + 1;
        const filled = starValue <= value;

        return (
          <button
            key={starValue}
            type="button"
            disabled={readonly}
            onClick={() => onChange?.(starValue)}
            className={`transition-colors ${readonly ? 'cursor-default' : 'cursor-pointer hover:scale-110'}`}
          >
            <Star
              size={size}
              className={filled ? 'text-turbo-yellow fill-turbo-yellow' : 'text-gray-300'}
            />
          </button>
        );
      })}
    </div>
  );
}
