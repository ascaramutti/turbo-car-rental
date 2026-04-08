import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import DriverSearchPage from '../pages/DriverSearchPage';
import * as bookingApi from '../api/bookingApi';

vi.mock('../api/bookingApi');
vi.mock('../components/LocationPicker', () => ({
  default: ({ onLocationSelect, markers = [] }) => (
    <div>
      <button type="button" onClick={() => onLocationSelect?.(49.2827, -123.1207)}>
        Move map
      </button>
      <p>Map markers: {markers.length}</p>
    </div>
  ),
}));
vi.mock('../components/VehicleDetailModal', () => ({
  default: () => <div>Vehicle Detail Modal</div>,
}));

const SEARCH_RESULT = {
  vehicleId: 5,
  year: 2022,
  make: 'Toyota',
  model: 'Prius',
  serviceType: 'TAXI_AND_DELIVERY',
  effectiveServiceType: 'TAXI_AND_DELIVERY',
  generalLocation: 'Downtown Vancouver',
  ownerFullName: 'Sarah Smith',
  ownerRating: 4.8,
  hourlyRate: 25,
  maskedLatitude: 49.28,
  maskedLongitude: -123.12,
};

describe('DriverSearchPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    Object.defineProperty(globalThis.navigator, 'geolocation', {
      value: undefined,
      configurable: true,
    });
  });

  it('searches nearby vehicles when the map location changes', async () => {
    const user = userEvent.setup();
    bookingApi.searchVehicles.mockResolvedValue({ data: [SEARCH_RESULT] });

    renderWithProviders(<DriverSearchPage />);

    await user.click(screen.getByText('Move map'));

    await waitFor(() => {
      expect(bookingApi.searchVehicles).toHaveBeenCalledWith(expect.objectContaining({
        latitude: '49.2827',
        longitude: '-123.1207',
      }));
    });

    expect(await screen.findByText('2022 Toyota Prius')).toBeInTheDocument();
    expect(screen.getByText('Map markers: 1')).toBeInTheDocument();
  });
});