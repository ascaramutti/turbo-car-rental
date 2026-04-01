import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Car, Calendar, DollarSign, Star, FileText } from 'lucide-react';
import { USER_ROLES } from '../../constants/roles';

/** Owner sidebar navigation link definitions. */
const OWNER_NAV_ITEMS = [
  { to: '/owner/dashboard', icon: LayoutDashboard, label: 'Dashboard', disabled: false },
  { to: '/owner/vehicles', icon: Car, label: 'My Vehicles', disabled: false },
  { to: '/owner/bookings', icon: Calendar, label: 'Bookings', disabled: false },
  { to: '/owner/earnings', icon: DollarSign, label: 'Earnings', disabled: false },
  { to: '/owner/reviews', icon: Star, label: 'Reviews', disabled: true, tooltip: 'Coming soon' },
];

/** Admin sidebar navigation link definitions. */
const ADMIN_NAV_ITEMS = [
  { to: '/admin/documents', icon: FileText, label: 'Documents', disabled: false },
  { to: '/admin/bookings', icon: Calendar, label: 'Bookings', disabled: false },
];

/**
 * Role-aware sidebar navigation.
 * Drivers have no sidebar (the search page IS their dashboard).
 * Owners and Admins see a vertical stack of nav links.
 *
 * @param {Object} props
 * @param {string} props.role - User role from auth context
 */
export default function Sidebar({ role }) {
  if (role === USER_ROLES.DRIVER) return null;

  const navItems = role === USER_ROLES.ADMIN ? ADMIN_NAV_ITEMS : OWNER_NAV_ITEMS;

  return (
    <aside className="hidden sm:flex flex-col w-56 shrink-0 bg-white border-r border-border min-h-full">
      <nav className="flex flex-col gap-1 p-3 pt-6">
        {navItems.map((item) => {
          const Icon = item.icon;

          if (item.disabled) {
            return (
              <div
                key={item.to}
                title={item.tooltip}
                className="relative group flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-gray-400 cursor-not-allowed select-none"
              >
                <Icon size={18} />
                <span>{item.label}</span>
                {/* Tooltip */}
                <span className="absolute left-full ml-2 px-2 py-1 text-xs text-white bg-gray-700 rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap z-10 pointer-events-none">
                  {item.tooltip}
                </span>
              </div>
            );
          }

          return (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-blue-50 text-blue-600 font-semibold'
                    : 'text-text-gray hover:bg-gray-100 hover:text-text-dark'
                }`
              }
            >
              <Icon size={18} />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </nav>
    </aside>
  );
}
