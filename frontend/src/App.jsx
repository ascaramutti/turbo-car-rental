import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './modules/auth/context/useAuth';
import { USER_ROLES } from './shared/constants/roles';

import Navbar from './shared/components/layout/Navbar';
import Footer from './shared/components/layout/Footer';
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
import OwnerBookingsPage from './modules/booking/pages/OwnerBookingsPage';
import OwnerBookingDetailPage from './modules/booking/pages/OwnerBookingDetailPage';

export default function App() {
  const { user } = useAuth();

  const getDashboardPath = () => {
    if (!user) return '/login';
    if (user.role === USER_ROLES.DRIVER) return '/driver/documents';
    if (user.role === USER_ROLES.ADMIN) return '/admin/documents';
    return '/owner/dashboard';
  };

  return (
    <div className="min-h-screen flex flex-col bg-bg-light">
      <Navbar />

      <main className="flex-1 flex flex-col">
        <Routes>
          {/* Public routes */}
          <Route
            path="/"
            element={user ? <Navigate to={getDashboardPath()} /> : <HomePage />}
          />
          <Route
            path="/login"
            element={user ? <Navigate to={getDashboardPath()} /> : <LoginPage />}
          />
          <Route
            path="/signup"
            element={user ? <Navigate to={getDashboardPath()} /> : <SignUpPage />}
          />
          <Route path="/verify-otp" element={<VerifyOtpPage />} />

          {/* Driver routes */}
          <Route
            path="/driver/documents"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.DRIVER]}>
                <DriverDocumentsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/driver/search"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.DRIVER]}>
                <DriverSearchPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/driver/search/:vehicleId"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.DRIVER]}>
                <DriverVehicleDetailPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/driver/bookings"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.DRIVER]}>
                <DriverBookingsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/driver/bookings/:id"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.DRIVER]}>
                <DriverBookingDetailPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/driver/dashboard"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.DRIVER]}>
                <div className="flex-1 flex items-center justify-center">
                  <h1 className="text-2xl text-text-dark">Driver Dashboard (Module 5)</h1>
                </div>
              </ProtectedRoute>
            }
          />
          {/* Car Owner routes */}
          <Route
            path="/owner/vehicles"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.CAR_OWNER]}>
                <MyVehiclesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/owner/vehicles/:id"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.CAR_OWNER]}>
                <VehicleDetailPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/owner/bookings"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.CAR_OWNER]}>
                <OwnerBookingsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/owner/bookings/:id"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.CAR_OWNER]}>
                <OwnerBookingDetailPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/owner/dashboard"
            element={
              user ? (
                <div className="flex-1 flex items-center justify-center">
                  <h1 className="text-2xl text-text-dark">Owner Dashboard (Module 6)</h1>
                </div>
              ) : (
                <Navigate to="/login" />
              )
            }
          />
          <Route
            path="/admin/documents"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.ADMIN]}>
                <AdminDocumentsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/dashboard"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.ADMIN]}>
                <div className="flex-1 flex items-center justify-center">
                  <h1 className="text-2xl text-text-dark">Admin Dashboard (Module 8)</h1>
                </div>
              </ProtectedRoute>
            }
          />

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>

      <Footer />
    </div>
  );
}
