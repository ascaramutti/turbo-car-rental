import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../modules/auth/context/useAuth';
import { LogOut, Menu, X, Clock, Search, Calendar, Star, FileText, AlertCircle } from 'lucide-react';
import { useState, useEffect } from 'react';
import { USER_ROLES } from '../../constants/roles';
import { getDriverHoursSummary } from '../../../modules/booking/api/bookingApi';
import turboLogo from "../../../assets/turboNoName.png";

/** Formats a role enum value into a readable label. */
const formatRole = (role) => {
  if (role === 'CAR_OWNER') return 'Car Owner';
  return role.charAt(0) + role.slice(1).toLowerCase();
};

/**
 * Hours-used badge shown in the Navbar for drivers.
 * Fetches the weekly summary from the API and renders a compact indicator.
 */
function DriverHoursBadge() {
  const [hours, setHours] = useState(null);

  useEffect(() => {
    getDriverHoursSummary()
      .then(({ data }) => setHours(data))
      .catch(() => {
        /* Non-critical — silently ignore if endpoint not yet implemented */
      });
  }, []);

  if (hours == null) return null;

  const used = Number(hours.hoursUsedThisWeek ?? 0);
  const limit = Number(hours.maxHoursPerWeek ?? 24);
  const pct = Math.min((used / limit) * 100, 100);
  const isLow = limit - used <= 4;

  return (
    <div
      className="flex items-center gap-1.5 px-2.5 py-1 bg-bg-light border border-border rounded-full text-xs font-semibold"
      title={`${used} of ${limit} hours used this week`}
    >
      <Clock size={13} className={isLow ? 'text-red-500' : 'text-accent-orange'} />
      <span className={isLow ? 'text-red-500' : 'text-text-dark'}>
        {used} / {limit} hrs
      </span>
      {/* Thin progress bar */}
      <div className="w-12 h-1.5 bg-gray-200 rounded-full overflow-hidden">
        <div
          className={`h-full rounded-full ${isLow ? 'bg-red-400' : 'bg-accent-orange'}`}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  );
}

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const isDriver = user?.role === USER_ROLES.DRIVER;
  const showDocumentsAlert = isDriver && user?.isVerified === false;

  return (
    <nav className="bg-white border-b border-border sticky top-0 z-50">
      <div className="px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2 shrink-0">
            <img src={turboLogo} alt="Turbo Logo" className="h-15 w-auto object-contain" />
            <span className="text-xl font-extrabold text-turbo-yellow tracking-wide">
              TURBO
            </span>
          </Link>

          {/* Desktop nav */}
          <div className="hidden sm:flex items-center gap-3">
            {user ? (
              <>
                {isDriver && (
                  <div className="flex items-center gap-1">
                    <NavLink
                      to="/driver/search"
                      className={({ isActive }) =>
                        `flex items-center gap-1 px-3 py-1.5 text-sm font-semibold rounded-full transition-colors ${
                          isActive ? 'bg-accent-orange text-white' : 'text-text-gray hover:text-text-dark'
                        }`
                      }
                    >
                      <Search size={14} />
                      Search
                    </NavLink>
                    <NavLink
                      to="/driver/bookings"
                      className={({ isActive }) =>
                        `flex items-center gap-1 px-3 py-1.5 text-sm font-semibold rounded-full transition-colors ${
                          isActive ? 'bg-accent-orange text-white' : 'text-text-gray hover:text-text-dark'
                        }`
                      }
                    >
                      <Calendar size={14} />
                      My Bookings
                    </NavLink>
                    <NavLink
                      to="/driver/reviews"
                      className={({ isActive }) =>
                        `flex items-center gap-1 px-3 py-1.5 text-sm font-semibold rounded-full transition-colors ${
                          isActive ? 'bg-accent-orange text-white' : 'text-text-gray hover:text-text-dark'
                        }`
                      }
                    >
                      <Star size={14} />
                      My Reviews
                    </NavLink>
                    <NavLink
                      to="/driver/documents"
                      className={({ isActive }) =>
                        `relative flex items-center gap-1 px-3 py-1.5 text-sm font-semibold rounded-full transition-colors ${
                          isActive ? 'bg-accent-orange text-white' : 'text-text-gray hover:text-text-dark'
                        }`
                      }
                    >
                      <FileText size={14} />
                      My Documents
                      {showDocumentsAlert && (
                        <AlertCircle
                          size={14}
                          className="text-red-500 fill-red-100"
                          aria-label="Documents pending verification"
                        />
                      )}
                    </NavLink>
                  </div>
                )}
                {isDriver && <DriverHoursBadge />}
                <span className="text-sm text-text-gray">
                  {user.firstName} {user.lastName}
                </span>
                <span className="text-xs px-2 py-1 rounded-full font-medium bg-bg-light text-text-dark border border-border">
                  {formatRole(user.role)}
                </span>
                <button
                  onClick={handleLogout}
                  className="flex items-center gap-1 text-sm text-text-gray hover:text-danger transition-colors"
                >
                  <LogOut size={16} />
                  Log out
                </button>
              </>
            ) : (
              <>
                <Link
                  to="/login"
                  className="px-5 py-2 text-sm font-semibold border-2 border-turbo-yellow text-text-dark rounded-full hover:bg-turbo-yellow/10 transition-colors"
                >
                  LOG IN
                </Link>
                <Link
                  to="/signup"
                  className="px-5 py-2 text-sm font-semibold border-2 border-turbo-yellow text-text-dark rounded-full hover:bg-turbo-yellow/10 transition-colors"
                >
                  SIGN UP
                </Link>
              </>
            )}
          </div>

          {/* Mobile hamburger */}
          <button
            className="sm:hidden p-2 text-text-dark"
            onClick={() => setMenuOpen(!menuOpen)}
          >
            {menuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>

        {/* Mobile menu */}
        {menuOpen && (
          <div className="sm:hidden pb-4 space-y-2">
            {user ? (
              <>
                <div className="text-sm text-text-gray px-2">
                  {user.firstName} {user.lastName} ({formatRole(user.role)})
                </div>
                {isDriver && (
                  <>
                    <Link
                      to="/driver/search"
                      onClick={() => setMenuOpen(false)}
                      className="block px-2 py-2 text-sm text-text-dark hover:bg-gray-100 rounded"
                    >
                      Search Vehicles
                    </Link>
                    <Link
                      to="/driver/bookings"
                      onClick={() => setMenuOpen(false)}
                      className="block px-2 py-2 text-sm text-text-dark hover:bg-gray-100 rounded"
                    >
                      My Bookings
                    </Link>
                    <Link
                      to="/driver/reviews"
                      onClick={() => setMenuOpen(false)}
                      className="block px-2 py-2 text-sm text-text-dark hover:bg-gray-100 rounded"
                    >
                      My Reviews
                    </Link>
                    <Link
                      to="/driver/documents"
                      onClick={() => setMenuOpen(false)}
                      className="flex items-center gap-2 px-2 py-2 text-sm text-text-dark hover:bg-gray-100 rounded"
                    >
                      My Documents
                      {showDocumentsAlert && (
                        <AlertCircle
                          size={14}
                          className="text-red-500 fill-red-100"
                          aria-label="Documents pending verification"
                        />
                      )}
                    </Link>
                    <div className="px-2">
                      <DriverHoursBadge />
                    </div>
                  </>
                )}
                <button
                  onClick={() => { handleLogout(); setMenuOpen(false); }}
                  className="block w-full text-left px-2 py-2 text-sm text-danger hover:bg-danger/5 rounded"
                >
                  Log out
                </button>
              </>
            ) : (
              <>
                <Link
                  to="/login"
                  onClick={() => setMenuOpen(false)}
                  className="block px-4 py-2 text-sm font-semibold text-center border-2 border-turbo-yellow rounded-full"
                >
                  LOG IN
                </Link>
                <Link
                  to="/signup"
                  onClick={() => setMenuOpen(false)}
                  className="block px-4 py-2 text-sm font-semibold text-center border-2 border-turbo-yellow rounded-full"
                >
                  SIGN UP
                </Link>
              </>
            )}
          </div>
        )}
      </div>
    </nav>
  );
}
