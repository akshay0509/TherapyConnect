import { useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate, useNavigate } from "react-router-dom";
import { AuthProvider, useAuth } from "./context/AuthContext";
import { DeliveryModesProvider } from "./context/DeliveryModesContext";
import LoginPage from "./pages/LoginPage";
import HomePage from "./pages/HomePage";
import TherapistHomePage from "./pages/TherapistHomePage";
import TherapistProfilePage from "./pages/TherapistProfilePage";
import MyServicesPage from "./pages/MyServicesPage";
import AvailabilityRulesPage from "./pages/AvailabilityRulesPage";
import MyClientsPage from "./pages/MyClientsPage";
import ClientDetailPage from "./pages/ClientDetailPage";
import AppointmentsPage from "./pages/AppointmentsPage";
import TherapistsPage from "./pages/TherapistsPage";
import ResetPasswordPage from "./pages/ResetPasswordPage";
import AccountSettingsPage from "./pages/AccountSettingsPage";
import EarningsPage from "./pages/EarningsPage";
import AnalyticsPage from "./pages/AnalyticsPage";

function RoleRedirect() {
  const { token, role } = useAuth();
  if (!token) return <Navigate to="/login" replace />;
  if (role === "THERAPIST") return <Navigate to="/therapist-home" replace />;
  return <Navigate to="/client-home" replace />;
}

function ProtectedRoute({ children, allowedRole }) {
  const { token, role } = useAuth();
  if (!token) return <Navigate to="/login" replace />;
  if (allowedRole && role !== allowedRole) return <RoleRedirect />;
  return children;
}

// Navigates to login with sessionExpired state when the auth token expires.
// Must run in an effect — calling navigate() during render causes a React warning
// and can produce extra render cycles.
function SessionExpiredRedirect() {
  const navigate = useNavigate();
  const { token } = useAuth();

  useEffect(() => {
    if (sessionStorage.getItem("sessionExpired") && !token) {
      sessionStorage.removeItem("sessionExpired");
      navigate("/login", { state: { sessionExpired: true }, replace: true });
    }
  }, [token, navigate]);

  return null;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <SessionExpiredRedirect />
        <DeliveryModesProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/reset-password" element={<ResetPasswordPage />} />

            {/* Role-based homes */}
            <Route path="/client-home" element={
              <ProtectedRoute allowedRole="CLIENT"><HomePage /></ProtectedRoute>
            } />
            <Route path="/therapist-home" element={
              <ProtectedRoute allowedRole="THERAPIST"><TherapistHomePage /></ProtectedRoute>
            } />

            {/* CLIENT-only pages */}
            <Route path="/therapists" element={
              <ProtectedRoute allowedRole="CLIENT"><TherapistsPage /></ProtectedRoute>
            } />

            {/* THERAPIST-only pages */}
            <Route path="/therapist/profile" element={
              <ProtectedRoute allowedRole="THERAPIST"><TherapistProfilePage /></ProtectedRoute>
            } />
            <Route path="/therapist/services" element={
              <ProtectedRoute allowedRole="THERAPIST"><MyServicesPage /></ProtectedRoute>
            } />
            <Route path="/therapist/availability-rules" element={
              <ProtectedRoute allowedRole="THERAPIST"><AvailabilityRulesPage /></ProtectedRoute>
            } />
            <Route path="/therapist/clients" element={
              <ProtectedRoute allowedRole="THERAPIST"><MyClientsPage /></ProtectedRoute>
            } />
            <Route path="/therapist/clients/:clientId" element={
              <ProtectedRoute allowedRole="THERAPIST"><ClientDetailPage /></ProtectedRoute>
            } />
            <Route path="/therapist/appointments" element={
              <ProtectedRoute allowedRole="THERAPIST"><AppointmentsPage /></ProtectedRoute>
            } />
            <Route path="/therapist/earnings" element={
              <ProtectedRoute allowedRole="THERAPIST"><EarningsPage /></ProtectedRoute>
            } />
            <Route path="/therapist/analytics" element={
              <ProtectedRoute allowedRole="THERAPIST"><AnalyticsPage /></ProtectedRoute>
            } />

            {/* Account settings — any authenticated user */}
            <Route path="/account-settings" element={
              <ProtectedRoute><AccountSettingsPage /></ProtectedRoute>
            } />

            {/* Root redirects based on role */}
            <Route path="/" element={<RoleRedirect />} />
            <Route path="*" element={<RoleRedirect />} />
          </Routes>
        </DeliveryModesProvider>
      </BrowserRouter>
    </AuthProvider>
  );
}
