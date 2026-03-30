import { useState, useEffect } from 'react';
import api from '../../../shared/api/axios';

/**
 * Image component that loads via authenticated API request.
 * Regular <img src> cannot send JWT tokens, so this fetches the image
 * as a blob using axios (which includes the Authorization header).
 */
export default function AuthImage({ src, alt, className }) {
  const [blobUrl, setBlobUrl] = useState(null);

  useEffect(() => {
    if (!src) return;
    let objectUrl = null;
    api.get(src, { responseType: 'blob' })
      .then(({ data }) => {
        objectUrl = URL.createObjectURL(data);
        setBlobUrl(objectUrl);
      })
      .catch(() => {});
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [src]);

  if (!blobUrl) return <div className={className + ' bg-gray-100 animate-pulse'} />;

  return <img src={blobUrl} alt={alt} className={className} />;
}
