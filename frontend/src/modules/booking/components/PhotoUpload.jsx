import { useRef } from 'react';
import { Upload, X, FileImage } from 'lucide-react';
import {
  ACCEPTED_PHOTO_FORMATS,
  MAX_PHOTO_SIZE_BYTES,
  MAX_PHOTO_SIZE_LABEL,
  MAX_PHOTOS_PER_UPLOAD,
  MIN_PHOTOS_PER_UPLOAD,
} from '../constants/bookingConstants';

/** Accepted MIME types matching ACCEPTED_PHOTO_FORMATS (.jpg, .jpeg, .png). */
const ACCEPTED_MIME_TYPES = ['image/jpeg', 'image/png'];

/**
 * Multi-file input component for uploading pickup or return condition photos.
 * Accepts JPG, JPEG, PNG up to 5MB each. Minimum 1, maximum 10 files.
 * @param {Object} props
 * @param {string} props.label - Input label text
 * @param {File[]} props.files - Currently selected files array
 * @param {Function} props.onFilesChange - Callback with updated files array
 * @param {string|null} props.error - Validation error message
 * @param {number} [props.maxFiles] - Maximum number of files allowed (default 10)
 */
export default function PhotoUpload({
  label,
  files = [],
  onFilesChange,
  error,
  maxFiles = MAX_PHOTOS_PER_UPLOAD,
}) {
  const inputRef = useRef(null);

  /**
   * Validates each incoming file for type and size, then merges with
   * existing selection up to the maxFiles limit.
   */
  const handleChange = (e) => {
    const incoming = Array.from(e.target.files || []);
    if (!incoming.length) return;

    // Validate MIME type
    const invalidType = incoming.find(
      (f) => !ACCEPTED_MIME_TYPES.includes(f.type)
    );
    if (invalidType) {
      onFilesChange(files, 'Only JPG and PNG files are accepted');
      if (inputRef.current) inputRef.current.value = '';
      return;
    }

    // Validate individual file sizes
    const oversized = incoming.find((f) => f.size > MAX_PHOTO_SIZE_BYTES);
    if (oversized) {
      onFilesChange(
        files,
        `File size must not exceed ${MAX_PHOTO_SIZE_LABEL}`
      );
      if (inputRef.current) inputRef.current.value = '';
      return;
    }

    const merged = [...files, ...incoming];

    if (merged.length > maxFiles) {
      onFilesChange(
        files,
        `You can upload a maximum of ${maxFiles} photos`
      );
      if (inputRef.current) inputRef.current.value = '';
      return;
    }

    onFilesChange(merged, null);
    if (inputRef.current) inputRef.current.value = '';
  };

  /** Removes a single file from the selection by index. */
  const handleRemove = (index) => {
    const updated = files.filter((_, i) => i !== index);
    onFilesChange(updated, null);
  };

  const hasFiles = files.length > 0;
  const atMax = files.length >= maxFiles;

  return (
    <div>
      {label && (
        <label className="block text-sm font-semibold text-text-dark mb-1">
          {label}
        </label>
      )}

      {/* Count indicator */}
      {hasFiles && (
        <p className="text-xs text-text-gray mb-2">
          {files.length}/{maxFiles} photos selected
        </p>
      )}

      {/* Thumbnail grid for selected files */}
      {hasFiles && (
        <div className="space-y-1 mb-2">
          {files.map((file, index) => (
            <div
              key={`${file.name}-${index}`}
              className="flex items-center justify-between bg-gray-50 border-2 border-gray-200 rounded-lg px-3 py-2.5"
            >
              <div className="flex items-center gap-2 min-w-0">
                <FileImage size={16} className="shrink-0 text-text-gray" />
                <span className="text-sm text-text-dark truncate">
                  {file.name}
                </span>
                <span className="text-xs text-text-gray shrink-0">
                  ({(file.size / (1024 * 1024)).toFixed(2)} MB)
                </span>
              </div>
              <button
                type="button"
                onClick={() => handleRemove(index)}
                className="p-1 hover:bg-gray-200 rounded transition-colors"
                aria-label={`Remove ${file.name}`}
              >
                <X size={14} className="text-text-gray" />
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Add more button — hidden when at max */}
      {!atMax && (
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          className={`w-full flex items-center justify-center gap-2 px-3 py-3 border-2 border-dashed rounded-lg text-sm transition-colors ${
            error
              ? 'border-red-400 text-red-500 hover:bg-red-50'
              : 'border-gray-300 text-text-gray hover:border-accent-orange hover:text-accent-orange hover:bg-accent-orange/5'
          }`}
        >
          <Upload size={16} />
          <span>
            {hasFiles
              ? `Add more photos (JPG, PNG — max ${MAX_PHOTO_SIZE_LABEL})`
              : `Choose photos (JPG, PNG — max ${MAX_PHOTO_SIZE_LABEL})`}
          </span>
        </button>
      )}

      <input
        ref={inputRef}
        type="file"
        accept={ACCEPTED_PHOTO_FORMATS}
        multiple
        onChange={handleChange}
        className="hidden"
        aria-label={label}
      />

      {error && <p className="text-xs text-red-500 mt-1">{error}</p>}
    </div>
  );
}

export { MIN_PHOTOS_PER_UPLOAD };
