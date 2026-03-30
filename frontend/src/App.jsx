import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './modules/auth/context/useAuth';
import { USER_ROLES } from './shared/constants/roles';

import Navbar from './shared/components/layout/Navbar';
import Footer from './shared/components/layout/Footer';
import DashboardLayout from './shared/components/layout/DashboardLayout';
import ProtectedRoute from './shared/components/common/ProtectedRoute';

import HomePage from './modules/auth/pages/HomePage';
import LoginPage from './modules/auth/pages/LoginPage';
import SignUpPage from './modules/auth/pages/SignUpPage';
import VerifyOtpPage from './modules/auth/pages/VerifyOtpPage';
import DriverDocumentsPage from './modules/documents/pages/DriverDocumentsPage';
import AdminDocumentsPage from './modules/admin/pages/AdminDocumentsPage';
import MyVehiclesPage from './modules/vehicles/pages/MyVehiclesPage';
import VehicleDetailPage from './modules/vehicles/pages/VehicleDetailPage';
import DriverSearchPage from './modules/booking/pages/DriverSearchPage';
import DriverVehicleDetailPage from './modules/booking/pages/DriverVehicleDetailPage';
import DriverBookingsPage from './modules/booking/pages/DriverBookingsPage';
import DriverBookingDetailPage from './modules/booking/pages/DriverBookingDetailPage';
import OwnerDashboardPage from './modules/booking/pages/OwnerDashboardPage';
import OwnerBookingsPage from './modules/booking/pages/OwnerBookingsPage';
import OwnerBookingDetailPage from './modules/booking/pages/OwnerBookingDetailPage';
import AdminBookingsPage from './modules/booking/pages/AdminBookingsPage';
import AdminBookingDetailPage from './modules/booking/pages/AdminBookingDetailPage';

/**
 * Returns the default authenticated landing path based on the user's role.
 * @param {Object|null} user
 * @returns {string}
 */
function getDashboardPath(user) {
  if (!user) return '/login';
  if (user.role === USER_ROLES.DRIVER) return '/driver/search';
  if (user.role === USER_ROLES.ADMIN) return '/admin/documents';
  return '/owner/dashboard';
}

export default function App() {
  const { user } = useAuth();

  return (
    <div className="min-h-screen flex flex-col bg-bg-light">
      <Navbar />

      <div className="flex-1 flex flex-col min-h-0">
        <Routes>
          {/* ── Public routes ─────────────────────────────────────────── */}
          <Route
            path="/"
            element={user ? <Navigate to={getDashboardPath(user)} /> : <HomePage />}
          />
          <Route
            path="/login"
            element={user ? <Navigate to={getDashboardPath(user)} /> : <LoginPage />}
          />
          <Route
            path="/signup"
            element={user ? <Navigate to={getDashboardPath(user)} /> : <SignUpPage />}
          />
          <Route path="/verify-otp" element={<VerifyOtpPage />} />

          {/* ── Driver routes ──────────────────────────────────────────── */}
          <Route
            path="/driver/documents"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.DRIVER]}>
                <DashboardLayout>
                  <DriverDocumentsPage />
                </DashboardLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/driver/search"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.DRIVER]}>
                <DashboardLayout>
                  <DriverSearchPage />
                </DashboardLayout>
              </ProtectedRoute>
            }
          />
          {/* Legacy deep-link to vehicle detail — kept for direct URL access */}
          <Route
            path="/driver/search/:vehicleId"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.DRIVER]}>
                <DashboardLayout>
                  <DriverVehicleDetailPage />
                </DashboardLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/driver/bookings"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.DRIVER]}>
                <DashboardLayout>
                  <DriverBookingsPage />
                </DashboardLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/driver/bookings/:id"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.DRIVER]}>
                <DashboardLayout>
                  <DriverBookingDetailPage />
                </DashboardLayout>
              </ProtectedRoute>
            }
          />
          {/* Legacy dashboard path — redirect to search */}
          <Route
            path="/driver/dashboard"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.DRIVER]}>
                <Navigate to="/driver/search" replace />
              </ProtectedRoute>
            }
          />

          {/* ── Car Owner routes ───────────────────────────────────────── */}
          <Route
            path="/owner/dashboard"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.CAR_OWNER]}>
                <DashboardLayout>
                  <OwnerDashboardPage />
                </DashboardLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/owner/vehicles"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.CAR_OWNER]}>
                <DashboardLayout>
                  <MyVehiclesPage />
                </DashboardLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/owner/vehicles/:id"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.CAR_OWNER]}>
                <DashboardLayout>
                  <VehicleDetailPage />
                </DashboardLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/owner/bookings"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.CAR_OWNER]}>
                <DashboardLayout>
                  <OwnerBookingsPage />
                </DashboardLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/owner/bookings/:id"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.CAR_OWNER]}>
                <DashboardLayout>
                  <OwnerBookingDetailPage />
                </DashboardLayout>
              </ProtectedRoute>
            }
          />

          {/* ── Admin routes ───────────────────────────────────────────── */}
          <Route
            path="/admin/documents"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.ADMIN]}>
                <DashboardLayout>
                  <AdminDocumentsPage />
                </DashboardLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/bookings"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.ADMIN]}>
                <DashboardLayout>
                  <AdminBookingsPage />
                </DashboardLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/bookings/:id"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.ADMIN]}>
                <DashboardLayout>
                  <AdminBookingDetailPage />
                </DashboardLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/dashboard"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.ADMIN]}>
                <DashboardLayout>
                  <div className="flex-1 flex items-center justify-center">
                    <h1 className="text-2xl text-text-dark">Admin Dashboard (Module 8)</h1>
                  </div>
                </DashboardLayout>
              </ProtectedRoute>
            }
          />

          {/* ── Fallback ───────────────────────────────────────────────── */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </div>

      <Footer />
    </div>
  );
}
