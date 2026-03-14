import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../modules/auth/context/useAuth';
import { LogOut, Menu, X } from 'lucide-react';
import { useState } from 'react';

/** Formats a role enum value into a readable label. */
const formatRole = (role) => {
  if (role === 'CAR_OWNER') return 'Owner';
  return role.charAt(0) + role.slice(1).toLowerCase();
};

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <nav className="bg-white border-b border-border sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <Link to="/" className="flex items-center gap-2">
            <span className="text-2xl">🚗</span>
            <span className="text-xl font-extrabold text-turbo-yellow tracking-wide">
              TURBO
            </span>
          </Link>

          {/* Desktop nav */}
          <div className="hidden sm:flex items-center gap-3">
            {user ? (
              <>
                <span className="text-sm text-text-gray">
                  {user.firstName} {user.lastName}
                </span>
                <span className="text-xs px-2 py-1 rounded-full font-medium bg-bg-light text-text-dark">
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
