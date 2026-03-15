import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import VehicleClassificationModal from '../components/VehicleClassificationModal';
import { approveVehicle } from '../api/adminVehicleApi';
import { getAdminDocumentViewUrl } from '../api/adminDocumentApi';
import toast from 'react-hot-toast';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));
vi.mock('../api/adminVehicleApi', () => ({
  checkVehicleClassification: vi.fn(),
  approveVehicle: vi.fn(),
}));
vi.mock('../api/adminDocumentApi', async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getAdminDocumentViewUrl: vi.fn((id) => `/admin/documents/${id}/view`),
  };
});
vi.mock('react-pdf', () => ({
  Document: ({ children }) => <div>{children}</div>,
  Page: () => <div />,
  pdfjs: { GlobalWorkerOptions: {}, version: '0.0.0' },
}));
vi.mock('../../../shared/components/common/FilePreviewModal', () => ({
  default: ({ fileUrl, fileName, onClose }) => (
    <div data-testid="file-preview-modal">
      <span>{fileName}</span>
      <span>{fileUrl}</span>
      <button onClick={onClose}>Close Preview</button>
    </div>
  ),
}));

const CLASSIFICATION_DATA_MULTI = {
  readyForClassification: true,
  vehicleId: 17,
  vehicleYear: 2023,
  vehicleMake: 'Toyota',
  vehicleModel: 'Camry',
  hasApprovedInspection: true,
  availableServiceTypes: ['TAXI_AND_DELIVERY', 'DELIVERY_ONLY'],
  documents: [
    { documentId: 30, documentType: 'INSURANCE', status: 'APPROVED', fileName: 'insurance.pdf' },
    { documentId: 31, documentType: 'VEHICLE_REGISTRATION', status: 'APPROVED', fileName: 'registration.pdf' },
    { documentId: 32, documentType: 'INSPECTION_REPORT', status: 'APPROVED', fileName: 'inspection.pdf' },
  ],
};

const CLASSIFICATION_DATA_SINGLE_NO_INSPECTION = {
  readyForClassification: true,
  vehicleId: 18,
  vehicleYear: 2023,
  vehicleMake: 'Honda',
  vehicleModel: 'Civic',
  hasApprovedInspection: false,
  availableServiceTypes: ['DELIVERY_ONLY'],
  documents: [
    { documentId: 40, documentType: 'INSURANCE', status: 'APPROVED', fileName: 'ins.pdf' },
    { documentId: 41, documentType: 'VEHICLE_REGISTRATION', status: 'APPROVED', fileName: 'reg.pdf' },
  ],
};

const CLASSIFICATION_DATA_SINGLE_OLD_VEHICLE = {
  readyForClassification: true,
  vehicleId: 19,
  vehicleYear: 2015,
  vehicleMake: 'Ford',
  vehicleModel: 'Focus',
  hasApprovedInspection: true,
  availableServiceTypes: ['DELIVERY_ONLY'],
  documents: [
    { documentId: 50, documentType: 'INSURANCE', status: 'APPROVED', fileName: 'ins.pdf' },
    { documentId: 51, documentType: 'VEHICLE_REGISTRATION', status: 'APPROVED', fileName: 'reg.pdf' },
    { documentId: 52, documentType: 'INSPECTION_REPORT', status: 'APPROVED', fileName: 'insp.pdf' },
  ],
};

describe('VehicleClassificationModal — rendering', () => {
  const onClassified = vi.fn();
  const onClose = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders vehicle info (year, make, model, vehicle ID)', () => {
    renderWithProviders(
      <VehicleClassificationModal
        classificationData={CLASSIFICATION_DATA_MULTI}
        onClassified={onClassified}
        onClose={onClose}
      />
    );

    expect(screen.getByText('2023 Toyota Camry')).toBeInTheDocument();
    expect(screen.getByText('Vehicle ID: 17')).toBeInTheDocument();
  });

  it('shows "All documents approved" subtitle', () => {
    renderWithProviders(
      <VehicleClassificationModal
        classificationData={CLASSIFICATION_DATA_MULTI}
        onClassified={onClassified}
        onClose={onClose}
      />
    );

    expect(screen.getByText('All documents approved')).toBeInTheDocument();
  });

  it('shows "Approved Documents" section with document list', () => {
    renderWithProviders(
      <VehicleClassificationModal
        classificationData={CLASSIFICATION_DATA_MULTI}
        onClassified={onClassified}
        onClose={onClose}
      />
    );

    expect(screen.getByText('Approved Documents')).toBeInTheDocument();
    expect(screen.getByText('Insurance Certificate')).toBeInTheDocument();
    expect(screen.getByText('Vehicle Registration')).toBeInTheDocument();
    expect(screen.getByText('Inspection Report (CVIP)')).toBeInTheDocument();
  });

  it('each document shows type label and View button', () => {
    renderWithProviders(
      <VehicleClassificationModal
        classificationData={CLASSIFICATION_DATA_MULTI}
        onClassified={onClassified}
        onClose={onClose}
      />
    );

    const viewButtons = screen.getAllByText('View');
    expect(viewButtons).toHaveLength(3);
  });
});

describe('VehicleClassificationModal — single option (no inspection)', () => {
  const onClassified = vi.fn();
  const onClose = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows "No inspection report" warning alert when only DELIVERY_ONLY due to missing inspection', () => {
    renderWithProviders(
      <VehicleClassificationModal
        classificationData={CLASSIFICATION_DATA_SINGLE_NO_INSPECTION}
        onClassified={onClassified}
        onClose={onClose}
      />
    );

    expect(
      screen.getByText('No inspection report — only Delivery service available')
    ).toBeInTheDocument();
  });

  it('shows the single option pre-selected with blue styling', () => {
    renderWithProviders(
      <VehicleClassificationModal
        classificationData={CLASSIFICATION_DATA_SINGLE_NO_INSPECTION}
        onClassified={onClassified}
        onClose={onClose}
      />
    );

    expect(screen.getByText('Delivery Only')).toBeInTheDocument();
  });
});

describe('VehicleClassificationModal — single option (old vehicle)', () => {
  const onClassified = vi.fn();
  const onClose = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows "Vehicle age exceeds" warning when only DELIVERY_ONLY due to age', () => {
    renderWithProviders(
      <VehicleClassificationModal
        classificationData={CLASSIFICATION_DATA_SINGLE_OLD_VEHICLE}
        onClassified={onClassified}
        onClose={onClose}
      />
    );

    expect(
      screen.getByText('Vehicle age exceeds taxi eligibility — only Delivery service available')
    ).toBeInTheDocument();
  });

  it('renders vehicle info for old vehicle', () => {
    renderWithProviders(
      <VehicleClassificationModal
        classificationData={CLASSIFICATION_DATA_SINGLE_OLD_VEHICLE}
        onClassified={onClassified}
        onClose={onClose}
      />
    );

    expect(screen.getByText('2015 Ford Focus')).toBeInTheDocument();
    expect(screen.getByText('Vehicle ID: 19')).toBeInTheDocument();
  });
});

describe('VehicleClassificationModal — multi-option selection', () => {
  const onClassified = vi.fn();
  const onClose = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows both TAXI_AND_DELIVERY and DELIVERY_ONLY options as buttons', () => {
    renderWithProviders(
      <VehicleClassificationModal
        classificationData={CLASSIFICATION_DATA_MULTI}
        onClassified={onClassified}
        onClose={onClose}
      />
    );

    expect(screen.getByText('Taxi + Delivery')).toBeInTheDocument();
    expect(screen.getByText('Delivery Only')).toBeInTheDocument();
    expect(screen.getByText('Vehicle can be used for taxi and delivery services')).toBeInTheDocument();
    expect(screen.getByText('Vehicle can only be used for delivery services')).toBeInTheDocument();
  });

  it('selecting a service type highlights it with blue styling', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <VehicleClassificationModal
        classificationData={CLASSIFICATION_DATA_MULTI}
        onClassified={onClassified}
        onClose={onClose}
      />
    );

    const taxiOption = screen.getByText('Taxi + Delivery').closest('button');
    await user.click(taxiOption);

    expect(taxiOption).toHaveClass('border-blue-500');
    expect(taxiOption).toHaveClass('bg-blue-50');
  });

  it('Classify button is disabled when no option is selected', () => {
    renderWithProviders(
      <VehicleClassificationModal
        classificationData={CLASSIFICATION_DATA_MULTI}
        onClassified={onClassified}
        onClose={onClose}
      />
    );

    const classifyButton = screen.getByRole('button', { name: /classify vehicle/i });
    expect(classifyButton).toBeDisabled();
  });

  it('Classify button is enabled after selecting an option', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <VehicleClassificationModal
        classificationData={CLASSIFICATION_DATA_MULTI}
        onClassified={onClassified}
        onClose={onClose}
      />
    );

    const taxiOption = screen.getByText('Taxi + Delivery').closest('button');
    await user.click(taxiOption);

    const classifyButton = screen.getByRole('button', { name: /classify vehicle/i });
    expect(classifyButton).not.toBeDisabled();
  });

  it('Classify button is enabled when single option (no selection needed)', () => {
    renderWithProviders(
      <VehicleClassificationModal
        classificationData={CLASSIFICATION_DATA_SINGLE_NO_INSPECTION}
        onClassified={onClassified}
        onClose={onClose}
      />
    );

    const classifyButton = screen.getByRole('button', { name: /classify vehicle/i });
    expect(classifyButton).not.toBeDisabled();
  });
});

describe('VehicleClassificationModal — classification actions', () => {
  const onClassified = vi.fn();
  const onClose = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('clicking "Classify Vehicle" calls approveVehicle API with selected type', async () => {
    const user = userEvent.setup();
    approveVehicle.mockResolvedValue({ data: {} });
    renderWithProviders(
      <VehicleClassificationModal
        classificationData={CLASSIFICATION_DATA_MULTI}
        onClassified={onClassified}
        onClose={onClose}
      />
    );

    const taxiOption = screen.getByText('Taxi + Delivery').closest('button');
    await user.click(taxiOption);

    await user.click(screen.getByRole('button', { name: /classify vehicle/i }));

    await waitFor(() => {
      expect(approveVehicle).toHaveBeenCalledWith(17, 'TAXI_AND_DELIVERY');
    });
  });

  it('clicking "Classify Vehicle" with single option uses that option automatically', async () => {
    const user = userEvent.setup();
    approveVehicle.mockResolvedValue({ data: {} });
    renderWithProviders(
      <VehicleClassificationModal
        classificationData={CLASSIFICATION_DATA_SINGLE_NO_INSPECTION}
        onClassified={onClassified}
        onClose={onClose}
      />
    );

    await user.click(screen.getByRole('button', { name: /classify vehicle/i }));

    await waitFor(() => {
      expect(approveVehicle).toHaveBeenCalledWith(18, 'DELIVERY_ONLY');
    });
  });

  it('shows success toast after classification', async () => {
    const user = userEvent.setup();
    approveVehicle.mockResolvedValue({ data: {} });
    renderWithProviders(
      <VehicleClassificationModal
        classificationData={CLASSIFICATION_DATA_MULTI}
        onClassified={onClassified}
        onClose={onClose}
      />
    );

    const deliveryOption = screen.getByText('Delivery Only').closest('button');
    await user.click(deliveryOption);

    await user.click(screen.getByRole('button', { name: /classify vehicle/i }));

    await waitFor(() => {
      expect(toast.success).toHaveBeenCalledWith('Vehicle classified as Delivery Only');
    });
  });

  it('calls onClassified callback after successful classification', async () => {
    const user = userEvent.setup();
    approveVehicle.mockResolvedValue({ data: {} });
    renderWithProviders(
      <VehicleClassificationModal
        classificationData={CLASSIFICATION_DATA_MULTI}
        onClassified={onClassified}
        onClose={onClose}
      />
    );

    const taxiOption = screen.getByText('Taxi + Delivery').closest('button');
    await user.click(taxiOption);

    await user.click(screen.getByRole('button', { name: /classify vehicle/i }));

    await waitFor(() => {
      expect(onClassified).toHaveBeenCalled();
    });
  });

  it('shows error toast on API failure', async () => {
    const user = userEvent.setup();
    approveVehicle.mockRejectedValue({ response: { data: { message: 'Vehicle not eligible' } } });
    renderWithProviders(
      <VehicleClassificationModal
        classificationData={CLASSIFICATION_DATA_MULTI}
        onClassified={onClassified}
        onClose={onClose}
      />
    );

    const taxiOption = screen.getByText('Taxi + Delivery').closest('button');
    await user.click(taxiOption);

    await user.click(screen.getByRole('button', { name: /classify vehicle/i }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Vehicle not eligible');
    });
    expect(onClassified).not.toHaveBeenCalled();
  });
});

describe('VehicleClassificationModal — document preview', () => {
  const onClassified = vi.fn();
  const onClose = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('clicking View button on a document opens FilePreviewModal', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <VehicleClassificationModal
        classificationData={CLASSIFICATION_DATA_MULTI}
        onClassified={onClassified}
        onClose={onClose}
      />
    );

    expect(screen.queryByTestId('file-preview-modal')).not.toBeInTheDocument();

    const viewButtons = screen.getAllByText('View');
    await user.click(viewButtons[0]);

    expect(screen.getByTestId('file-preview-modal')).toBeInTheDocument();
    expect(screen.getByText('insurance.pdf')).toBeInTheDocument();
    expect(screen.getByText('/admin/documents/30/view')).toBeInTheDocument();
  });

  it('closing FilePreviewModal removes it from the DOM', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <VehicleClassificationModal
        classificationData={CLASSIFICATION_DATA_MULTI}
        onClassified={onClassified}
        onClose={onClose}
      />
    );

    const viewButtons = screen.getAllByText('View');
    await user.click(viewButtons[1]);

    expect(screen.getByTestId('file-preview-modal')).toBeInTheDocument();

    await user.click(screen.getByText('Close Preview'));

    expect(screen.queryByTestId('file-preview-modal')).not.toBeInTheDocument();
  });
});
