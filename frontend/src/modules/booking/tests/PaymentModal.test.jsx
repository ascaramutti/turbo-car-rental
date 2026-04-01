import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../../test/renderWithProviders';
import PaymentModal from '../components/PaymentModal';
import * as bookingPaymentApi from '../api/bookingPaymentApi';

// ── Hoisted mocks (available before vi.mock hoisting) ─────────────
const { mockConfirmPayment, mockStripe, mockElements } = vi.hoisted(() => {
  const mockConfirmPayment = vi.fn();
  const mockStripe = { confirmPayment: mockConfirmPayment };
  const mockElements = {};
  return { mockConfirmPayment, mockStripe, mockElements };
});

// ── Mock API ──────────────────────────────────────────────────────
vi.mock('../api/bookingPaymentApi');

// ── Mock Stripe ───────────────────────────────────────────────────
vi.mock('@stripe/react-stripe-js', () => ({
  Elements: ({ children }) => <div data-testid="stripe-elements">{children}</div>,
  PaymentElement: () => <div data-testid="stripe-payment-element">Card Input</div>,
  useStripe: () => mockStripe,
  useElements: () => mockElements,
}));

vi.mock('@stripe/stripe-js', () => ({
  loadStripe: vi.fn(() => Promise.resolve(mockStripe)),
}));

// ── Test data ─────────────────────────────────────────────────────
const PAYMENT_INTENT_RESPONSE = {
  paymentId: 1,
  paymentIntentId: 'pi_test123abc',
  clientSecret: 'pi_test123abc_secret_xyz',
  status: 'PENDING',
  amount: 300.0,
  platformFee: 20.0,
  ownerPayout: 80.0,
  securityDeposit: 200.0,
  currency: 'cad',
};

const onSuccess = vi.fn();
const onClose = vi.fn();

describe('PaymentModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    bookingPaymentApi.createPaymentIntent.mockResolvedValue({
      data: PAYMENT_INTENT_RESPONSE,
    });
    mockConfirmPayment.mockResolvedValue({ error: null });
    bookingPaymentApi.confirmPayment.mockResolvedValue({ data: {} });
  });

  // ── Loading state ─────────────────────────────────────────────

  it('shows loading spinner while creating payment intent', () => {
    bookingPaymentApi.createPaymentIntent.mockReturnValue(new Promise(() => {}));
    renderWithProviders(
      <PaymentModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    expect(screen.getByText('Payment')).toBeInTheDocument();
    // Loader is visible (animate-spin class on Loader2 component)
    expect(document.querySelector('.animate-spin')).not.toBeNull();
  });

  // ── Payment breakdown ─────────────────────────────────────────

  it('displays payment breakdown after intent is created', async () => {
    renderWithProviders(
      <PaymentModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    await waitFor(() => {
      expect(screen.getByText('Rental Total')).toBeInTheDocument();
    });

    expect(screen.getByText('Security Deposit')).toBeInTheDocument();
    expect(screen.getByText('Total')).toBeInTheDocument();
    // Rental Total = 300 - 200 = $100.00
    expect(screen.getByText('$100.00 CAD')).toBeInTheDocument();
    // Security Deposit = $200.00
    expect(screen.getByText('$200.00 CAD')).toBeInTheDocument();
    // Total = $300.00
    expect(screen.getByText('$300.00 CAD')).toBeInTheDocument();
  });

  it('renders Stripe PaymentElement inside Elements provider', async () => {
    renderWithProviders(
      <PaymentModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    await waitFor(() => {
      expect(screen.getByTestId('stripe-elements')).toBeInTheDocument();
    });
    expect(screen.getByTestId('stripe-payment-element')).toBeInTheDocument();
  });

  it('calls createPaymentIntent with bookingId on mount', async () => {
    renderWithProviders(
      <PaymentModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    await waitFor(() => {
      expect(bookingPaymentApi.createPaymentIntent).toHaveBeenCalledWith(42);
    });
  });

  // ── Error state ───────────────────────────────────────────────

  it('shows error message when createPaymentIntent fails', async () => {
    bookingPaymentApi.createPaymentIntent.mockRejectedValue({
      response: { data: { message: 'Only confirmed bookings can be paid' } },
    });

    renderWithProviders(
      <PaymentModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    await waitFor(() => {
      expect(screen.getByText('Only confirmed bookings can be paid')).toBeInTheDocument();
    });

    expect(screen.getByText('Close')).toBeInTheDocument();
  });

  // ── Payment submission ────────────────────────────────────────

  it('calls stripe.confirmPayment and backend confirmPayment on submit', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <PaymentModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    await waitFor(() => {
      expect(screen.getByText('Pay Now')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Pay Now'));

    await waitFor(() => {
      expect(mockConfirmPayment).toHaveBeenCalledWith({
        elements: mockElements,
        confirmParams: { return_url: expect.any(String) },
        redirect: 'if_required',
      });
    });

    await waitFor(() => {
      expect(bookingPaymentApi.confirmPayment).toHaveBeenCalledWith(42, 'pi_test123abc');
    });
  });

  it('shows success message after payment succeeds', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <PaymentModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    await waitFor(() => {
      expect(screen.getByText('Pay Now')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Pay Now'));

    await waitFor(() => {
      expect(screen.getByText('Payment Successful!')).toBeInTheDocument();
    });
    expect(screen.getByText('Redirecting...')).toBeInTheDocument();
  });

  it('shows Stripe error message when payment fails', async () => {
    mockConfirmPayment.mockResolvedValue({
      error: { message: 'Your card was declined.' },
    });

    const user = userEvent.setup();
    renderWithProviders(
      <PaymentModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    await waitFor(() => {
      expect(screen.getByText('Pay Now')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Pay Now'));

    await waitFor(() => {
      expect(screen.getByText('Your card was declined.')).toBeInTheDocument();
    });
  });

  // ── Close/cancel ──────────────────────────────────────────────

  it('calls onClose when X button is clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <PaymentModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    // The X button is the first button in the header
    const closeButtons = document.querySelectorAll('button');
    await user.click(closeButtons[0]);

    expect(onClose).toHaveBeenCalled();
  });

  it('calls onClose when Cancel button is clicked in checkout form', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <PaymentModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    await waitFor(() => {
      expect(screen.getByText('Cancel')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Cancel'));
    expect(onClose).toHaveBeenCalled();
  });

  it('calls onClose when Close button is clicked in error state', async () => {
    bookingPaymentApi.createPaymentIntent.mockRejectedValue({
      response: { data: { message: 'Error' } },
    });

    const user = userEvent.setup();
    renderWithProviders(
      <PaymentModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    await waitFor(() => {
      expect(screen.getByText('Close')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Close'));
    expect(onClose).toHaveBeenCalled();
  });

  // ── Backend confirm failure is non-critical ───────────────────

  it('still shows success even if backend confirmPayment fails', async () => {
    bookingPaymentApi.confirmPayment.mockRejectedValue(new Error('network error'));

    const user = userEvent.setup();
    renderWithProviders(
      <PaymentModal bookingId={42} onSuccess={onSuccess} onClose={onClose} />
    );

    await waitFor(() => {
      expect(screen.getByText('Pay Now')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Pay Now'));

    await waitFor(() => {
      expect(screen.getByText('Payment Successful!')).toBeInTheDocument();
    });
  });
});
