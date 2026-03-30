import { useAuth } from '../../../modules/auth/context/useAuth';
import { USER_ROLES } from '../../constants/roles';
import Sidebar from './Sidebar';

/**
 * Layout wrapper for all authenticated pages.
 * Renders a Sidebar (left) + scrollable content area (right) for Owners and Admins.
 * Drivers receive no sidebar — their search page fills the full content area.
 *
 * @param {Object} props
 * @param {React.ReactNode} props.children - Page content
 */
export default function DashboardLayout({ children }) {
  const { user } = useAuth();
  const role = user?.role;
  const isDriver = role === USER_ROLES.DRIVER;

  if (isDriver) {
    return (
      <div className="flex-1 flex flex-col min-h-0">
        {children}
      </div>
    );
  }

  return (
    <div className="flex flex-1 min-h-0">
      <Sidebar role={role} />
      <main className="flex-1 min-w-0 overflow-y-auto bg-bg-light">
        {children}
      </main>
    </div>
  );
}
