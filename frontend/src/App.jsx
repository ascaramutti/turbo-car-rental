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
            path="/driver/dashboard"
            element={
              <ProtectedRoute allowedRoles={[USER_ROLES.DRIVER]}>
                <div className="flex-1 flex items-center justify-center">
                  <h1 className="text-2xl text-text-dark">Driver Dashboard (Module 5)</h1>
                </div>
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
