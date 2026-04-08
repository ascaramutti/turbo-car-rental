import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import { validateField, extractErrorMessage } from '../utils/validation';
import { Eye, EyeOff, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';
import turboLogo from "../../../assets/turboLogo.png";

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errors, setErrors] = useState({});
  const { login } = useAuth();
  const navigate = useNavigate();

  /** Validates a single field on blur for immediate feedback. */
  const handleBlur = (field) => {
    const error = field === 'email'
      ? validateField('email', email)
      : (password ? null : 'Password is required');
    setErrors((prev) => ({ ...prev, [field]: error }));
  };

  /** Validates all fields before submit. */
  const validate = () => {
    const newErrors = {};
    const emailErr = validateField('email', email);
    const pwErr = password ? null : 'Password is required';
    if (emailErr) newErrors.email = emailErr;
    if (pwErr) newErrors.password = pwErr;
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    setIsLoading(true);
    try {
      const data = await login(email, password);
      toast.success('Welcome back!');
      if (data.role === 'DRIVER') navigate('/driver/dashboard');
      else if (data.role === 'ADMIN') navigate('/admin/dashboard');
      else navigate('/owner/dashboard');
    } catch (err) {
      const code = err.response?.data?.code;
      if (code === 'AUTH-006') {
        toast.error('Please verify your email first.');
        navigate('/verify-otp', { state: { email } });
      } else {
        toast.error(extractErrorMessage(err));
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="bg-bg-light flex-1 flex items-center justify-center px-4 py-12">
      <div className="bg-white rounded-2xl shadow-lg p-8 sm:p-10 w-full max-w-md">
        {/* Logo */}
        <div className="flex justify-center mb-4">
          <img src={turboLogo} alt="Turbo Logo" className="h-40 w-auto object-contain" />
        </div>

        <h1 className="text-2xl font-bold text-accent-orange text-center mb-8">
          LOG IN TO TURBO
        </h1>

        <form onSubmit={handleSubmit} className="space-y-5">
          {/* Email */}
          <div>
            <label className="block text-sm font-semibold text-owner-red mb-1.5">E-MAIL</label>
            <input
              type="email"
              value={email}
              onChange={(e) => { setEmail(e.target.value); setErrors({ ...errors, email: null }); }}
              onBlur={() => handleBlur('email')}
              placeholder="you@example.com"
              className={`w-full px-4 py-3 border-2 rounded-full focus:outline-none focus:ring-2 transition-colors ${
                errors.email
                  ? 'border-danger focus:ring-danger/30'
                  : 'border-owner-red focus:ring-owner-red/30'
              }`}
            />
            {errors.email && <p className="mt-1 text-xs text-danger">{errors.email}</p>}
          </div>

          {/* Password */}
          <div>
            <label className="block text-sm font-semibold text-driver-blue mb-1.5">PASSWORD</label>
            <div className="relative">
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => { setPassword(e.target.value); setErrors({ ...errors, password: null }); }}
                onBlur={() => handleBlur('password')}
                placeholder="Enter your password"
                className={`w-full px-4 py-3 pr-12 border-2 rounded-full focus:outline-none focus:ring-2 transition-colors ${
                  errors.password
                    ? 'border-danger focus:ring-danger/30'
                    : 'border-driver-blue focus:ring-driver-blue/30'
                }`}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-text-gray hover:text-text-dark"
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
            {errors.password && <p className="mt-1 text-xs text-danger">{errors.password}</p>}
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="w-full py-3 border-2 border-accent-orange text-accent-orange font-bold rounded-full hover:bg-accent-orange hover:text-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            {isLoading && <Loader2 size={18} className="animate-spin" />}
            {isLoading ? 'Logging in...' : 'LOG IN'}
          </button>
        </form>

        <p className="text-center text-sm text-text-gray mt-6">
          Don&apos;t have an account?{' '}
          <Link to="/signup" className="text-accent-orange font-semibold hover:underline">
            Sign Up
          </Link>
        </p>
      </div>
    </div>
  );
}
