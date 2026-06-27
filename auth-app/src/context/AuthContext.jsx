import { createContext, useContext, useState, useCallback, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { loginRequest } from "../api/auth";

const AuthContext = createContext(null);

function decodeJwt(token) {
  try {
    const payload = token.split(".")[1];
    return JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
  } catch { return {}; }
}

function getRoleFromToken(token) {
  if (!token) return null;
  const claims = decodeJwt(token);
  const authorities = claims.authorities || claims.roles || [];
  return authorities[0] ?? null;
}

function getTokenExpiry(token) {
  if (!token) return null;
  const claims = decodeJwt(token);
  return claims.exp ? claims.exp * 1000 : null; // ms
}

export function AuthProvider({ children }) {
  const [token, setToken]   = useState(() => localStorage.getItem("jwt_token"));
  const [user, setUser]     = useState(() => {
    const stored = localStorage.getItem("user");
    return stored ? JSON.parse(stored) : null;
  });
  const [error, setError]     = useState(null);
  const [loading, setLoading] = useState(false);
  const [showTimeoutWarning, setShowTimeoutWarning] = useState(false);

  const role = getRoleFromToken(token);

  const warningTimerRef = useRef(null);
  const expireTimerRef  = useRef(null);

  // Clear both timers
  const clearTimers = () => {
    if (warningTimerRef.current) clearTimeout(warningTimerRef.current);
    if (expireTimerRef.current)  clearTimeout(expireTimerRef.current);
  };

  const logout = useCallback((expired = false) => {
    clearTimers();
    localStorage.removeItem("jwt_token");
    localStorage.removeItem("user");
    setToken(null);
    setUser(null);
    setShowTimeoutWarning(false);
    if (expired) {
      // Navigate handled by consumer — pass state via sessionStorage so LoginPage can read it
      sessionStorage.setItem("sessionExpired", "1");
    }
  }, []);

  // Schedule warning 2 min before expiry, then auto-logout at expiry
  const scheduleTimeoutWarning = useCallback((jwt) => {
    clearTimers();
    const expiry = getTokenExpiry(jwt);
    if (!expiry) return;
    const now = Date.now();
    const msToExpiry = expiry - now;
    if (msToExpiry <= 0) { logout(true); return; }
    const WARNING_BEFORE = 2 * 60 * 1000; // 2 min
    const msToWarning = msToExpiry - WARNING_BEFORE;

    if (msToWarning > 0) {
      warningTimerRef.current = setTimeout(() => setShowTimeoutWarning(true), msToWarning);
    } else {
      setShowTimeoutWarning(true);
    }
    expireTimerRef.current = setTimeout(() => {
      setShowTimeoutWarning(false);
      logout(true);
    }, msToExpiry);
  }, [logout]);

  // Start timer when token changes
  useEffect(() => {
    if (token) scheduleTimeoutWarning(token);
    else clearTimers();
    return clearTimers;
  }, [token]);

  const login = useCallback(async (username, password) => {
    setLoading(true); setError(null);
    try {
      const data = await loginRequest(username, password);
      const jwt = data.token || data.accessToken || data.jwt;
      const claims = decodeJwt(jwt);
      const userData = data.user || { username: claims.sub || username };
      localStorage.setItem("jwt_token", jwt);
      localStorage.setItem("user", JSON.stringify(userData));
      setToken(jwt);
      setUser(userData);
      sessionStorage.removeItem("sessionExpired");
      return { success: true, role: getRoleFromToken(jwt) };
    } catch (err) {
      setError(err.message || "Login failed. Please check your credentials.");
      return { success: false, role: null };
    } finally {
      setLoading(false);
    }
  }, []);

  const dismissTimeoutWarning = () => setShowTimeoutWarning(false);

  return (
    <AuthContext.Provider value={{ token, user, role, login, logout, error, loading, showTimeoutWarning, dismissTimeoutWarning }}>
      {children}
      {/* Session timeout warning modal */}
      {showTimeoutWarning && (
        <div style={{
          position: "fixed", inset: 0, background: "rgba(0,0,0,0.55)", zIndex: 9999,
          display: "flex", alignItems: "center", justifyContent: "center",
        }}>
          <div style={{
            background: "#0f1923", border: "1px solid rgba(245,158,11,0.3)", borderRadius: 16,
            padding: "28px 32px", maxWidth: 400, width: "90%",
            fontFamily: "'DM Sans', sans-serif", color: "#e2e8f0",
            boxShadow: "0 24px 64px rgba(0,0,0,0.6)",
          }}>
            <div style={{ fontSize: "1.8rem", marginBottom: 12 }}>⏱</div>
            <h2 style={{ fontFamily: "'Syne', sans-serif", fontSize: "1.1rem", fontWeight: 800, margin: "0 0 8px", color: "#fbbf24" }}>
              Session expiring soon
            </h2>
            <p style={{ fontSize: "0.875rem", color: "#94a3b8", margin: "0 0 20px", lineHeight: 1.6 }}>
              Your session will expire in less than 2 minutes. You will be signed out automatically.
            </p>
            <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
              <button onClick={() => { logout(true); }} style={{
                padding: "9px 18px", background: "transparent", border: "1px solid rgba(255,255,255,0.12)",
                borderRadius: 9, color: "#64748b", fontSize: "0.875rem", cursor: "pointer",
              }}>Sign out now</button>
              <button onClick={dismissTimeoutWarning} style={{
                padding: "9px 18px", background: "rgba(245,158,11,0.15)", border: "1px solid rgba(245,158,11,0.35)",
                borderRadius: 9, color: "#fbbf24", fontSize: "0.875rem", fontWeight: 700, cursor: "pointer",
              }}>Stay signed in</button>
            </div>
          </div>
        </div>
      )}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
