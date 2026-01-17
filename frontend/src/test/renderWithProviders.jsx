import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AuthProvider from '../modules/auth/context/AuthProvider';

/**
 * Renders a component wrapped in Router + AuthProvider for testing.
 * Accepts optional initialEntries for route testing.
 */
export function renderWithProviders(ui, { initialEntries = ['/'] } = {}) {
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <AuthProvider>
        {ui}
      </AuthProvider>
    </MemoryRouter>
  );
}
