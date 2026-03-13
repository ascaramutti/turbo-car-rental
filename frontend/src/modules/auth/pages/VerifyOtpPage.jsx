import { useState, useRef, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import { extractErrorMessage } from '../utils/validation';
import { Mail, RefreshCw, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';

const OTP_LENGTH = 6;
const RESEND_COOLDOWN_SECONDS = 60;

export default function VerifyOtpPage() {
  const [otp, setOtp] = useState(Array(OTP_LENGTH).fill(''));
  const [isVerifying, setIsVerifying] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const inputsRef = useRef([]);
  const { verifyOtp, resendOtp } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const email = location.state?.email;

  useEffect(() => {
    if (!email) navigate('/signup');
  }, [email, navigate]);

  useEffect(() => {
    if (cooldown > 0) {
      const timer = setTimeout(() => setCooldown(cooldown - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [cooldown]);

  useEffect(() => {
    inputsRef.current[0]?.focus();
  }, []);

  const handleChange = (index, value) => {
    if (!/^\d*$/.test(value)) return;

    const newOtp = [...otp];
    newOtp[index] = value.slice(-1);
    setOtp(newOtp);

    if (value && index < OTP_LENGTH - 1) {
      inputsRef.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      inputsRef.current[index - 1]?.focus();
    }
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, OTP_LENGTH);
    if (pasted.length === OTP_LENGTH) {
      setOtp(pasted.split(''));
      inputsRef.current[OTP_LENGTH - 1]?.focus();
    }
  };

  const handleVerify = async () => {
    const code = otp.join('');
    if (code.length !== OTP_LENGTH) {
      toast.error('Please enter the 6-digit code');
      return;
    }

    setIsVerifying(true);
    try {
      const data = await verifyOtp(email, code);
      toast.success('Email verified successfully!');
      if (data.role === 'DRIVER') navigate('/driver/dashboard');
      else navigate('/owner/dashboard');
    } catch (err) {
      const errorCode = err.response?.data?.code;
      if (errorCode === 'AUTH-003') {
        toast.success('Your email is already verified. You can log in.');
        navigate('/login');
      } else {
        toast.error(extractErrorMessage(err));
        setOtp(Array(OTP_LENGTH).fill(''));
        inputsRef.current[0]?.focus();
      }
    } finally {
      setIsVerifying(false);
    }
  };

  const handleResend = async () => {
    if (cooldown > 0) return;
    setIsResending(true);
    try {
      await resendOtp(email);
      toast.success('New verification code sent!');
      setCooldown(RESEND_COOLDOWN_SECONDS);
      setOtp(Array(OTP_LENGTH).fill(''));
      inputsRef.current[0]?.focus();
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsResending(false);
    }
  };

  if (!email) return null;

  const isOtpComplete = otp.join('').length === OTP_LENGTH;

  return (
    <div className="bg-bg-light flex-1 flex items-center justify-center px-4 py-12">
      <div className="bg-white rounded-2xl shadow-lg p-8 sm:p-10 w-full max-w-md text-center">
        <div className="flex justify-center mb-4">
          <div className="bg-accent-orange/10 p-4 rounded-full">
            <Mail size={40} className="text-accent-orange" />
          </div>
        </div>

        <h1 className="text-2xl font-bold text-text-dark mb-2">Verify Your Email</h1>
        <p className="text-text-gray text-sm mb-8">
          We sent a 6-digit code to<br />
          <strong className="text-text-dark">{email}</strong>
        </p>

        <div className="flex justify-center gap-3 mb-8" onPaste={handlePaste}>
          {otp.map((digit, i) => (
            <input
              key={i}
              ref={(el) => (inputsRef.current[i] = el)}
              type="text"
              inputMode="numeric"
              maxLength={1}
              value={digit}
              onChange={(e) => handleChange(i, e.target.value)}
              onKeyDown={(e) => handleKeyDown(i, e)}
              className={`w-12 h-14 text-center text-2xl font-bold border-2 rounded-xl focus:outline-none focus:ring-2 transition-all ${
                digit
                  ? 'border-accent-orange bg-accent-orange/5 focus:ring-accent-orange/30'
                  : 'border-border focus:border-accent-orange focus:ring-accent-orange/30'
              }`}
            />
          ))}
        </div>

        <button
          onClick={handleVerify}
          disabled={isVerifying || !isOtpComplete}
          className="w-full py-3 bg-accent-orange text-white font-bold rounded-full hover:bg-accent-orange-light transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 mb-4"
        >
          {isVerifying && <Loader2 size={18} className="animate-spin" />}
          {isVerifying ? 'Verifying...' : 'VERIFY EMAIL'}
        </button>

        <div className="text-sm text-text-gray">
          Didn&apos;t receive the code?{' '}
          <button
            onClick={handleResend}
            disabled={isResending || cooldown > 0}
            className="text-accent-orange font-semibold hover:underline disabled:opacity-50 disabled:cursor-not-allowed inline-flex items-center gap-1"
          >
            {isResending ? (
              <><RefreshCw size={14} className="animate-spin" /> Sending...</>
            ) : cooldown > 0 ? (
              `Resend in ${cooldown}s`
            ) : (
              'Resend Code'
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
