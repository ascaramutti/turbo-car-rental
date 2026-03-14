import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import {
  validateField,
  validateDateOfBirth,
  validatePasswordMatch,
  extractErrorMessage,
  VALIDATION_PATTERNS,
} from '../utils/validation';
import { Eye, EyeOff, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';

const INPUT_BASE = 'w-full px-4 py-3 border-2 rounded-lg bg-bg-light focus:bg-white focus:outline-none focus:ring-2 transition-colors';
const DATE_PLACEHOLDER = '[color-scheme:light] [&:not(:focus)]:text-gray-400';
const INPUT_NORMAL = `${INPUT_BASE} border-border focus:ring-accent-orange/30 focus:border-accent-orange`;
const INPUT_ERROR = `${INPUT_BASE} border-danger focus:ring-danger/30 focus:border-danger`;

export default function SignUpPage() {
  const location = useLocation();
  const preselectedRole = location.state?.role || '';

  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    dateOfBirth: '',
    phoneNumber: '',
    email: '',
    password: '',
    confirmPassword: '',
    role: preselectedRole,
    streetAddress: '',
    city: '',
    province: 'British Columbia',
    postalCode: '',
    country: 'Canada',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [errors, setErrors] = useState({});
  const [isLoading, setIsLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) setErrors((prev) => ({ ...prev, [name]: null }));
  };

  const ADDRESS_FIELDS = new Set(['streetAddress', 'city', 'province', 'postalCode', 'country']);

  /** Validates a single field on blur for immediate feedback. */
  const handleBlur = (e) => {
    const { name, value } = e.target;
    let error = null;

    if (name === 'dateOfBirth') {
      error = validateDateOfBirth(value);
    } else if (name === 'confirmPassword') {
      error = validatePasswordMatch(formData.password, value);
    } else if (name === 'phoneNumber') {
      if (value && !VALIDATION_PATTERNS.phoneNumber.test(value)) {
        error = 'Invalid phone number format (e.g. +1 604 555-0001)';
      }
    } else {
      error = validateField(name, value);
    }

    setErrors((prev) => ({ ...prev, [name]: error }));
  };

  const selectRole = (role) => {
    setFormData((prev) => ({ ...prev, role }));
    if (errors.role) setErrors((prev) => ({ ...prev, role: null }));
  };

  /** Validates all fields before submit. Returns true if the form is valid. */
  const validateAll = () => {
    const { firstName, lastName, dateOfBirth, phoneNumber, email, password, confirmPassword, role } = formData;
    const newErrors = {};

    const firstErr = validateField('firstName', firstName);
    const lastErr = validateField('lastName', lastName);
    const dobErr = validateDateOfBirth(dateOfBirth);
    const emailErr = validateField('email', email);
    const pwErr = validateField('password', password);
    const confirmErr = validatePasswordMatch(password, confirmPassword);

    if (firstErr) newErrors.firstName = firstErr;
    if (lastErr) newErrors.lastName = lastErr;
    if (dobErr) newErrors.dateOfBirth = dobErr;
    if (emailErr) newErrors.email = emailErr;
    if (pwErr) newErrors.password = pwErr;
    if (confirmErr) newErrors.confirmPassword = confirmErr;

    if (phoneNumber && !VALIDATION_PATTERNS.phoneNumber.test(phoneNumber)) {
      newErrors.phoneNumber = 'Invalid phone number format';
    }

    ADDRESS_FIELDS.forEach((field) => {
      const err = validateField(field, formData[field]);
      if (err) newErrors[field] = err;
    });

    if (!role) newErrors.role = 'Please select a role';

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateAll()) return;

    setIsLoading(true);
    try {
      const { confirmPassword: _, streetAddress, city, province, postalCode, country, ...rest } = formData;
      const submitData = {
        ...rest,
        address: { streetAddress, city, province, postalCode, country },
      };
      if (!submitData.phoneNumber) delete submitData.phoneNumber;
      await register(submitData);
      toast.success('Check your email for the verification code.');
      navigate('/verify-otp', { state: { email: formData.email } });
    } catch (err) {
      toast.error(extractErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="bg-bg-light flex-1 flex items-center justify-center px-4 py-12">
      <div className="bg-white rounded-2xl shadow-lg p-8 sm:p-10 w-full max-w-2xl">
        {/* Logo */}
        <div className="text-center mb-2">
          <span className="text-5xl">🚗</span>
        </div>
        <h1 className="text-3xl font-bold text-accent-orange text-center mb-8">
          JOIN TURBO!
        </h1>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Your Name */}
          <fieldset>
            <legend className="text-sm font-bold text-driver-blue mb-2">Your Name</legend>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <input
                  type="text" name="firstName" value={formData.firstName}
                  onChange={handleChange} onBlur={handleBlur}
                  placeholder="First Name"
                  className={errors.firstName ? INPUT_ERROR : INPUT_NORMAL}
                />
                {errors.firstName && <p className="mt-1 text-xs text-danger">{errors.firstName}</p>}
              </div>
              <div>
                <input
                  type="text" name="lastName" value={formData.lastName}
                  onChange={handleChange} onBlur={handleBlur}
                  placeholder="Last Name"
                  className={errors.lastName ? INPUT_ERROR : INPUT_NORMAL}
                />
                {errors.lastName && <p className="mt-1 text-xs text-danger">{errors.lastName}</p>}
              </div>
            </div>
          </fieldset>

          {/* Date of Birth */}
          <fieldset>
            <legend className="text-sm font-bold text-driver-blue mb-2">Date of Birth</legend>
            <input
              type="date" name="dateOfBirth" value={formData.dateOfBirth}
              onChange={handleChange} onBlur={handleBlur}
              max={new Date().toISOString().split('T')[0]}
              className={`${errors.dateOfBirth ? INPUT_ERROR : INPUT_NORMAL} ${!formData.dateOfBirth ? DATE_PLACEHOLDER : ''}`}
            />
            {errors.dateOfBirth && <p className="mt-1 text-xs text-danger">{errors.dateOfBirth}</p>}
          </fieldset>

          {/* Contact */}
          <fieldset>
            <legend className="text-sm font-bold text-driver-blue mb-2">Contact Number</legend>
            <input
              type="tel" name="phoneNumber" value={formData.phoneNumber}
              onChange={handleChange} onBlur={handleBlur}
              placeholder="+1 (604) 555-0001  (optional)"
              className={errors.phoneNumber ? INPUT_ERROR : INPUT_NORMAL}
            />
            {errors.phoneNumber && <p className="mt-1 text-xs text-danger">{errors.phoneNumber}</p>}
          </fieldset>

          {/* Address */}
          <fieldset>
            <legend className="text-sm font-bold text-driver-blue mb-2">Address</legend>
            <div className="space-y-3">
              <div>
                <input
                  type="text" name="streetAddress" value={formData.streetAddress}
                  onChange={handleChange} onBlur={handleBlur}
                  placeholder="Street Address"
                  className={errors.streetAddress ? INPUT_ERROR : INPUT_NORMAL}
                />
                {errors.streetAddress && <p className="mt-1 text-xs text-danger">{errors.streetAddress}</p>}
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <input
                    type="text" name="city" value={formData.city}
                    onChange={handleChange} onBlur={handleBlur}
                    placeholder="City"
                    className={errors.city ? INPUT_ERROR : INPUT_NORMAL}
                  />
                  {errors.city && <p className="mt-1 text-xs text-danger">{errors.city}</p>}
                </div>
                <div>
                  <input
                    type="text" name="province" value={formData.province}
                    onChange={handleChange} onBlur={handleBlur}
                    placeholder="Province"
                    className={`${errors.province ? INPUT_ERROR : INPUT_NORMAL} bg-gray-100`}
                    readOnly
                  />
                  {errors.province && <p className="mt-1 text-xs text-danger">{errors.province}</p>}
                </div>
                <div>
                  <input
                    type="text" name="postalCode" value={formData.postalCode}
                    onChange={handleChange} onBlur={handleBlur}
                    placeholder="V6B 1A1"
                    className={errors.postalCode ? INPUT_ERROR : INPUT_NORMAL}
                  />
                  {errors.postalCode && <p className="mt-1 text-xs text-danger">{errors.postalCode}</p>}
                </div>
                <div>
                  <input
                    type="text" name="country" value={formData.country}
                    onChange={handleChange} onBlur={handleBlur}
                    placeholder="Country"
                    className={`${errors.country ? INPUT_ERROR : INPUT_NORMAL} bg-gray-100`}
                    readOnly
                  />
                  {errors.country && <p className="mt-1 text-xs text-danger">{errors.country}</p>}
                </div>
              </div>
            </div>
          </fieldset>

          {/* Email */}
          <fieldset>
            <legend className="text-sm font-bold text-driver-blue mb-2">E-mail Address</legend>
            <input
              type="email" name="email" value={formData.email}
              onChange={handleChange} onBlur={handleBlur}
              placeholder="you@example.com"
              className={errors.email ? INPUT_ERROR : INPUT_NORMAL}
            />
            {errors.email && <p className="mt-1 text-xs text-danger">{errors.email}</p>}
          </fieldset>

          {/* Password */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <fieldset>
              <legend className="text-sm font-bold text-driver-blue mb-2">Password</legend>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password" value={formData.password}
                  onChange={handleChange} onBlur={handleBlur}
                  placeholder="Min. 6 characters"
                  className={errors.password ? INPUT_ERROR : INPUT_NORMAL}
                />
                <button
                  type="button" onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-text-gray hover:text-text-dark"
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {errors.password && <p className="mt-1 text-xs text-danger">{errors.password}</p>}
            </fieldset>

            <fieldset>
              <legend className="text-sm font-bold text-driver-blue mb-2">Confirm Password</legend>
              <div className="relative">
                <input
                  type={showConfirm ? 'text' : 'password'}
                  name="confirmPassword" value={formData.confirmPassword}
                  onChange={handleChange} onBlur={handleBlur}
                  placeholder="Re-enter password"
                  className={errors.confirmPassword ? INPUT_ERROR : INPUT_NORMAL}
                />
                <button
                  type="button" onClick={() => setShowConfirm(!showConfirm)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-text-gray hover:text-text-dark"
                >
                  {showConfirm ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {errors.confirmPassword && <p className="mt-1 text-xs text-danger">{errors.confirmPassword}</p>}
            </fieldset>
          </div>

          {/* Role */}
          <div>
            <p className="text-sm font-bold text-driver-blue mb-3">I want to join as...</p>
            <div className="flex gap-4">
              <button
                type="button" onClick={() => selectRole('DRIVER')}
                className={`flex-1 py-3 rounded-xl font-bold text-sm transition-all ${
                  formData.role === 'DRIVER'
                    ? 'bg-driver-blue text-white shadow-md scale-[1.02]'
                    : 'border-2 border-driver-blue text-driver-blue hover:bg-driver-blue/5'
                }`}
              >
                DRIVER
              </button>
              <button
                type="button" onClick={() => selectRole('CAR_OWNER')}
                className={`flex-1 py-3 rounded-xl font-bold text-sm transition-all ${
                  formData.role === 'CAR_OWNER'
                    ? 'bg-owner-red text-white shadow-md scale-[1.02]'
                    : 'border-2 border-owner-red text-owner-red hover:bg-owner-red/5'
                }`}
              >
                CAR OWNER
              </button>
            </div>
            {errors.role && <p className="mt-1 text-xs text-danger">{errors.role}</p>}
          </div>

          <button
            type="submit" disabled={isLoading}
            className="w-full py-3 bg-accent-orange text-white font-bold rounded-full hover:bg-accent-orange-light transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 shadow-md"
          >
            {isLoading && <Loader2 size={18} className="animate-spin" />}
            {isLoading ? 'Creating account...' : 'SIGN UP'}
          </button>
        </form>

        <p className="text-center text-sm text-text-gray mt-6">
          Already have an account?{' '}
          <Link to="/login" className="text-accent-orange font-semibold hover:underline">
            Log In
          </Link>
        </p>
      </div>
    </div>
  );
}
