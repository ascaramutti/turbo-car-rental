import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './modules/auth/context/useAuth';

import Navbar from './shared/components/layout/Navbar';
import Footer from './shared/components/layout/Footer';

import HomePage from './modules/auth/pages/HomePage';
import LoginPage from './modules/auth/pages/LoginPage';
import SignUpPage from './modules/auth/pages/SignUpPage';
import VerifyOtpPage from './modules/auth/pages/VerifyOtpPage';

export default function App() {
  const { user } = useAuth();

  const getDashboardPath = () => {
    if (!user) return '/login';
    if (user.role === 'DRIVER') return '/driver/dashboard';
    if (user.role === 'ADMIN') return '/admin/dashboard';
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

          {/* Placeholder dashboard routes (replaced in future modules) */}
          <Route
            path="/driver/dashboard"
            element={
              user ? (
                <div className="flex-1 flex items-center justify-center">
                  <h1 className="text-2xl text-text-dark">Driver Dashboard (Module 5)</h1>
                </div>
              ) : (
                <Navigate to="/login" />
              )
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
            path="/admin/dashboard"
            element={
              user ? (
                <div className="flex-1 flex items-center justify-center">
                  <h1 className="text-2xl text-text-dark">Admin Dashboard (Module 8)</h1>
                </div>
              ) : (
                <Navigate to="/login" />
              )
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
