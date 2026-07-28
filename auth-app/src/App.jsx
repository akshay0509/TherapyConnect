import { useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate, useNavigate } from "react-router-dom";
import { AuthProvider, useAuth } from "./context/AuthContext";
import { DeliveryModesProvider } from "./context/DeliveryModesContext";
import CommandPalette from "./components/CommandPalette";
import TherapistShell from "./components/TherapistShell";
import LoginPage from "./pages/LoginPage";
import HomePage from "./pages/HomePage";
import TherapistHomePage from "./pages/TherapistHomePage";
import TherapistProfilePage from "./pages/TherapistProfilePage";
import TherapistSetupPage from "./pages/TherapistSetupPage";
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
import AdminLoginPage from "./pages/AdminLoginPage";
import AdminPage from "./pages/AdminPage";

function RoleRedirect() {
  const { token, role, therapistId } = useAuth();
  if (!token) return <Navigate to="/login" replace />;
  if (role === "THERAPIST") {
    if (!therapistId) return <Navigate to="/therapist/setup" replace />;
    return <Navigate to="/therapist-home" replace />;
  }
  return <Navigate to="/client-home" replace />;
}

function ProtectedRoute({ children, allowedRole }) {
  const { token, role } = useAuth();
  if (!token) return <Navigate to="/login" replace />;
  if (allowedRole && role !== allowedRole) return <RoleRedirect />;
  return children;
}

// Account Settings is reachable by BOTH therapists and clients. Therapists get
// it inside the workspace shell (so the sidebar doesn't vanish mid-flow);
// clients get the standalone page with its own header. Keeping one route means
// no loss of access for client users.
function AccountSettingsRoute() {
  const { role } = useAuth();
  return role === "THERAPIST"
    ? <TherapistShell><AccountSettingsPage embedded /></TherapistShell>
    : <AccountSettingsPage />;
}

// Navigates to login with sessionExpired state when the auth token expires.
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
        {/* Ctrl/Cmd-K jump-to launcher — renders nothing unless a therapist is signed in */}
        <CommandPalette />
        <DeliveryModesProvider>
          <Routes>
            {/* ── Admin routes — independent of therapist/client auth ── */}
            <Route path="/admin-login" element={<AdminLoginPage />} />
            <Route path="/admin" element={<AdminPage />} />

            <Route path="/login" element={<LoginPage />} />
            <Route path="/reset-password" element={<ResetPasswordPage />} />

            {/* Therapist onboarding — shown when therapist account has no profile yet (no shell) */}
            <Route path="/therapist/setup" element={
              <ProtectedRoute allowedRole="THERAPIST"><TherapistSetupPage /></ProtectedRoute>
            } />

            {/* Client home */}
            <Route path="/client-home" element={
              <ProtectedRoute allowedRole="CLIENT"><HomePage /></ProtectedRoute>
            } />

            {/* CLIENT-only pages */}
            <Route path="/therapists" element={
              <ProtectedRoute allowedRole="CLIENT"><TherapistsPage /></ProtectedRoute>
            } />

            {/* ── THERAPIST workspace — wrapped in the persistent sidebar shell ── */}
            <Route element={<ProtectedRoute allowedRole="THERAPIST"><TherapistShell /></ProtectedRoute>}>
              <Route path="/therapist-home" element={<TherapistHomePage />} />
              <Route path="/therapist/profile" element={<TherapistProfilePage />} />
              <Route path="/therapist/services" element={<MyServicesPage />} />
              <Route path="/therapist/availability-rules" element={<AvailabilityRulesPage />} />
              <Route path="/therapist/clients" element={<MyClientsPage />} />
              <Route path="/therapist/clients/:clientId" element={<ClientDetailPage />} />
              <Route path="/therapist/appointments" element={<AppointmentsPage />} />
              <Route path="/therapist/earnings" element={<EarningsPage />} />
              <Route path="/therapist/analytics" element={<AnalyticsPage />} />
            </Route>

            {/* Account settings — any authenticated user; chrome adapts to role */}
            <Route path="/account-settings" element={
              <ProtectedRoute><AccountSettingsRoute /></ProtectedRoute>
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
