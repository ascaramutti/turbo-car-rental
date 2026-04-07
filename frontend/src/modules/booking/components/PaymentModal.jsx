import { useState, useEffect } from 'react';
import { loadStripe } from '@stripe/stripe-js';
import { Elements, PaymentElement, useStripe, useElements } from '@stripe/react-stripe-js';
import { X, Loader2, CreditCard, CheckCircle, AlertCircle } from 'lucide-react';
import { createPaymentIntent, confirmPayment } from '../api/bookingPaymentApi';
import { extractErrorMessage } from '../../auth/utils/validation';

const stripePublishableKey = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY;
const stripePromise = stripePublishableKey ? loadStripe(stripePublishableKey) : null;

/**
 * Inner checkout form rendered inside Stripe Elements provider.
 * Handles submission to Stripe and displays success/failure.
 */
function CheckoutForm({ bookingId, paymentIntentId, onSuccess, onClose }) {
  const stripe = useStripe();
  const elements = useElements();
  const [isProcessing, setIsProcessing] = useState(false);
  const [paymentError, setPaymentError] = useState(null);
  const [paymentSuccess, setPaymentSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!stripe || !elements) return;

    setIsProcessing(true);
    setPaymentError(null);

    const { error } = await stripe.confirmPayment({
      elements,
      confirmParams: { return_url: window.location.href },
      redirect: 'if_required',
    });

    if (error) {
      setPaymentError(error.message);
      setIsProcessing(false);
    } else {
      try {
        await confirmPayment(bookingId, paymentIntentId);
      } catch {
        // Non-critical — webhook will also update status
      }
      setPaymentSuccess(true);
      setIsProcessing(false);
      setTimeout(() => onSuccess(), 1500);
    }
  };

  if (paymentSuccess) {
    return (
      <div className="flex flex-col items-center gap-3 py-6">
        <CheckCircle size={48} className="text-green-500" />
        <p className="text-lg font-bold text-text-dark">Payment Successful!</p>
        <p className="text-sm text-text-gray">Redirecting...</p>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <PaymentElement />
      {paymentError && (
        <div className="flex items-center gap-2 text-sm text-red-500 bg-red-50 p-3 rounded-lg">
          <AlertCircle size={16} />
          {paymentError}
        </div>
      )}
      <div className="flex gap-2">
        <button
          type="submit"
          disabled={!stripe || isProcessing}
          className="flex-1 flex items-center justify-center gap-2 py-2.5 bg-accent-orange text-white font-bold rounded-full hover:bg-accent-orange-light transition-colors disabled:opacity-50"
        >
          {isProcessing ? <Loader2 size={16} className="animate-spin" /> : <CreditCard size={16} />}
          {isProcessing ? 'Processing...' : 'Pay Now'}
        </button>
        <button
          type="button"
          onClick={onClose}
          disabled={isProcessing}
          className="px-4 py-2.5 border-2 border-gray-200 text-text-gray font-semibold rounded-full hover:bg-gray-50 transition-colors disabled:opacity-50"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}

/**
 * PaymentModal — Creates a Stripe PaymentIntent and renders the checkout form.
 *
 * @param {Object} props
 * @param {number} props.bookingId - Booking to create payment for
 * @param {Function} props.onSuccess - Callback after successful payment
 * @param {Function} props.onClose - Callback to close the modal
 */
export default function PaymentModal({ bookingId, onSuccess, onClose }) {
  const [clientSecret, setClientSecret] = useState(null);
  const [paymentDetails, setPaymentDetails] = useState(null);
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const initPayment = async () => {
      try {
        const { data } = await createPaymentIntent(bookingId);
        setClientSecret(data.clientSecret);
        setPaymentDetails(data);
      } catch (err) {
        setError(extractErrorMessage(err));
      } finally {
        setIsLoading(false);
      }
    };
    initPayment();
  }, [bookingId]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md mx-4 p-6">
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold text-text-dark flex items-center gap-2">
            <CreditCard size={20} className="text-accent-orange" />
            Payment
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="text-text-gray hover:text-text-dark transition-colors"
          >
            <X size={20} />
          </button>
        </div>

        {/* Loading */}
        {isLoading && (
          <div className="flex items-center justify-center py-12">
            <Loader2 size={32} className="animate-spin text-accent-orange" />
          </div>
        )}

        {/* Error */}
        {error && (
          <div className="flex flex-col items-center gap-3 py-6">
            <AlertCircle size={32} className="text-red-500" />
            <p className="text-sm text-red-500 text-center">{error}</p>
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border-2 border-gray-200 text-text-gray font-semibold rounded-full hover:bg-gray-50 transition-colors"
            >
              Close
            </button>
          </div>
        )}

        {/* Payment breakdown + Stripe form */}
        {!isLoading && !error && clientSecret && paymentDetails && stripePromise && (
          <>
            <div className="bg-gray-50 rounded-lg p-4 mb-4 space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-text-gray">Rental Total</span>
                <span className="font-medium text-text-dark">
                  ${(paymentDetails.amount - paymentDetails.securityDeposit).toFixed(2)} {paymentDetails.currency.toUpperCase()}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-text-gray">Security Deposit</span>
                <span className="font-medium text-text-dark">
                  ${Number(paymentDetails.securityDeposit).toFixed(2)} {paymentDetails.currency.toUpperCase()}
                </span>
              </div>
              <div className="border-t border-gray-200 pt-2 flex justify-between font-bold">
                <span className="text-text-dark">Total</span>
                <span className="text-accent-orange">
                  ${Number(paymentDetails.amount).toFixed(2)} {paymentDetails.currency.toUpperCase()}
                </span>
              </div>
            </div>

            <Elements
              stripe={stripePromise}
              options={{ clientSecret, appearance: { theme: 'stripe' } }}
            >
              <CheckoutForm
                bookingId={bookingId}
                paymentIntentId={paymentDetails.paymentIntentId}
                onSuccess={onSuccess}
                onClose={onClose}
              />
            </Elements>
          </>
        )}

        {/* Stripe not configured */}
        {!isLoading && !error && !stripePromise && (
          <div className="flex flex-col items-center gap-3 py-6">
            <AlertCircle size={32} className="text-amber-500" />
            <p className="text-sm text-text-gray text-center">
              Payment processing is not configured. Please set the VITE_STRIPE_PUBLISHABLE_KEY environment variable.
            </p>
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border-2 border-gray-200 text-text-gray font-semibold rounded-full hover:bg-gray-50 transition-colors"
            >
              Close
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
