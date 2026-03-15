import { useState, useEffect, useMemo } from 'react';
import { X, Loader2, AlertTriangle, Download, ChevronLeft, ChevronRight } from 'lucide-react';
import { Document, Page, pdfjs } from 'react-pdf';
import 'react-pdf/dist/Page/TextLayer.css';
import 'react-pdf/dist/Page/AnnotationLayer.css';
import api from '../../api/axios';
import { BLOB_RESPONSE_CONFIG } from '../../utils/downloadUtils';

pdfjs.GlobalWorkerOptions.workerSrc = `//unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.mjs`;

/** PDF.js standard font data CDN path. */
const STANDARD_FONT_DATA_URL = `//unpkg.com/pdfjs-dist@${pdfjs.version}/standard_fonts/`;

/** MIME type prefix/value constants for file type detection. */
const MIME_IMAGE_PREFIX = 'image/';
const MIME_PDF = 'application/pdf';

/** User-facing messages. */
const LOAD_ERROR_MESSAGE = 'Failed to load document. Please try again.';
const LOADING_MESSAGE = 'Loading document...';
const RENDERING_PDF_MESSAGE = 'Rendering PDF...';
const UNSUPPORTED_MESSAGE = 'Preview not available for this file type.';

/** Max width (px) for rendered PDF pages. */
const PDF_PAGE_MAX_WIDTH = 800;
const PDF_PAGE_HORIZONTAL_PADDING = 80;

/**
 * Modal that fetches and displays a document file (PDF/image) from a protected API endpoint.
 * @param {string} fileUrl - API endpoint path (e.g., /driver/documents/1/view)
 * @param {string} fileName - Display name for the file
 * @param {Function} onClose - Callback to close the modal
 */
export default function FilePreviewModal({ fileUrl, fileName, onClose }) {
  const [objectUrl, setObjectUrl] = useState(null);
  const [fileType, setFileType] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [numPages, setNumPages] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const pdfOptions = useMemo(() => ({ standardFontDataUrl: STANDARD_FONT_DATA_URL }), []);

  useEffect(() => {
    let blobUrl = null;

    /** Fetches the file as a blob and creates an object URL for rendering. */
    const fetchFile = async () => {
      try {
        const response = await api.get(fileUrl, BLOB_RESPONSE_CONFIG);
        const blob = response.data;
        blobUrl = URL.createObjectURL(blob);
        setObjectUrl(blobUrl);
        setFileType(blob.type);
      } catch (err) {
        setError(LOAD_ERROR_MESSAGE);
      } finally {
        setIsLoading(false);
      }
    };
    fetchFile();

    return () => {
      if (blobUrl) URL.revokeObjectURL(blobUrl);
    };
  }, [fileUrl]);

  const isImage = fileType?.startsWith(MIME_IMAGE_PREFIX);
  const isPdf = fileType === MIME_PDF;

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-4xl max-h-[90vh] overflow-hidden flex flex-col">
        <ModalHeader
          fileName={fileName}
          isPdf={isPdf}
          currentPage={currentPage}
          numPages={numPages}
          onClose={onClose}
        />

        {/* Content */}
        <div className={`flex-1 overflow-auto p-4 flex justify-center ${isPdf ? 'items-start bg-gray-200' : 'items-center'}`}>
          {isLoading && <StatusMessage icon={Loader2} message={LOADING_MESSAGE} spin />}
          {error && <StatusMessage icon={AlertTriangle} message={error} variant="error" />}
          {!isLoading && !error && isImage && <ImagePreview objectUrl={objectUrl} fileName={fileName} />}
          {!isLoading && !error && isPdf && (
            <PdfPreview
              objectUrl={objectUrl}
              pdfOptions={pdfOptions}
              currentPage={currentPage}
              onLoadSuccess={(n) => setNumPages(n)}
              onLoadError={() => setError(LOAD_ERROR_MESSAGE)}
            />
          )}
          {!isLoading && !error && !isImage && !isPdf && (
            <UnsupportedPreview objectUrl={objectUrl} fileName={fileName} />
          )}
        </div>

        {isPdf && numPages > 1 && (
          <PdfPagination
            currentPage={currentPage}
            numPages={numPages}
            onPrev={() => setCurrentPage((p) => Math.max(1, p - 1))}
            onNext={() => setCurrentPage((p) => Math.min(numPages, p + 1))}
          />
        )}
      </div>
    </div>
  );
}

/** Header with file name, optional page indicator, and close button. */
function ModalHeader({ fileName, isPdf, currentPage, numPages, onClose }) {
  return (
    <div className="flex items-center justify-between p-4 border-b">
      <div className="flex items-center gap-3 min-w-0">
        <p className="font-bold text-text-dark truncate">{fileName}</p>
        {isPdf && numPages && (
          <span className="text-xs text-text-gray whitespace-nowrap">
            Page {currentPage} / {numPages}
          </span>
        )}
      </div>
      <button onClick={onClose} className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors">
        <X size={20} className="text-text-gray" />
      </button>
    </div>
  );
}

/** Loading or error status indicator. */
function StatusMessage({ icon: Icon, message, variant, spin }) {
  const isError = variant === 'error';
  return (
    <div className="flex flex-col items-center gap-3 text-center">
      <Icon size={32} className={`${spin ? 'animate-spin text-accent-orange' : ''} ${isError ? 'text-red-400' : ''}`} />
      <p className={`text-sm ${isError ? 'text-red-600' : 'text-text-gray'}`}>{message}</p>
    </div>
  );
}

/** Image file preview. */
function ImagePreview({ objectUrl, fileName }) {
  return <img src={objectUrl} alt={fileName} className="max-w-full max-h-[70vh] object-contain rounded-lg" />;
}

/** PDF file preview using react-pdf. */
function PdfPreview({ objectUrl, pdfOptions, currentPage, onLoadSuccess, onLoadError }) {
  return (
    <div className="shadow-lg">
      <Document
        file={objectUrl}
        options={pdfOptions}
        onLoadSuccess={({ numPages: n }) => onLoadSuccess(n)}
        onLoadError={onLoadError}
        loading={<StatusMessage icon={Loader2} message={RENDERING_PDF_MESSAGE} spin />}
      >
        <Page
          pageNumber={currentPage}
          width={Math.min(PDF_PAGE_MAX_WIDTH, window.innerWidth - PDF_PAGE_HORIZONTAL_PADDING)}
          renderTextLayer
          renderAnnotationLayer
        />
      </Document>
    </div>
  );
}

/** Fallback for unsupported file types with download link. */
function UnsupportedPreview({ objectUrl, fileName }) {
  return (
    <div className="flex flex-col items-center gap-3 text-center">
      <p className="text-sm text-text-gray">{UNSUPPORTED_MESSAGE}</p>
      <a
        href={objectUrl}
        download={fileName}
        className="inline-flex items-center gap-2 px-4 py-2 bg-accent-orange text-white text-sm font-bold rounded-full hover:bg-accent-orange-light transition-colors"
      >
        <Download size={16} />
        Download File
      </a>
    </div>
  );
}

/** Previous/Next page controls for multi-page PDFs. */
function PdfPagination({ currentPage, numPages, onPrev, onNext }) {
  return (
    <div className="flex items-center justify-center gap-4 p-3 border-t">
      <button
        onClick={onPrev}
        disabled={currentPage <= 1}
        className="p-1.5 rounded-lg hover:bg-gray-100 transition-colors disabled:opacity-30"
      >
        <ChevronLeft size={20} />
      </button>
      <span className="text-sm font-semibold text-text-dark">
        {currentPage} / {numPages}
      </span>
      <button
        onClick={onNext}
        disabled={currentPage >= numPages}
        className="p-1.5 rounded-lg hover:bg-gray-100 transition-colors disabled:opacity-30"
      >
        <ChevronRight size={20} />
      </button>
    </div>
  );
}
