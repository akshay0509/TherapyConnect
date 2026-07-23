import { createContext, useContext, useState, useCallback, useEffect, useRef } from "react";
import { loginRequest, refreshRequest, logoutRequest } from "../api/auth";
import { setAccessToken, clearAccessToken, setOnTokenRefreshed } from "../api/client";
import SessionExpiry from "../components/SessionExpiry";

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

function getTherapistIdFromToken(token) {
  if (!token) return null;
  const claims = decodeJwt(token);
  return claims.therapistId || null;
}

function getTokenExpiry(token) {
  if (!token) return null;
  const claims = decodeJwt(token);
  return claims.exp ? claims.exp * 1000 : null; // ms
}

export function AuthProvider({ children }) {
  // Access token lives only in memory — never in localStorage
  const [token, setToken]   = useState(null);
  const [user, setUser]     = useState(null);
  const [error, setError]     = useState(null);
  const [loading, setLoading] = useState(true); // true on mount while silent refresh runs
  const [showTimeoutWarning, setShowTimeoutWarning] = useState(false);

  const role = getRoleFromToken(token);
  const therapistId = getTherapistIdFromToken(token);
  const sessionExpiresAt = getTokenExpiry(token);

  const warningTimerRef = useRef(null);
  const expireTimerRef  = useRef(null);

  const clearTimers = () => {
    if (warningTimerRef.current) clearTimeout(warningTimerRef.current);
    if (expireTimerRef.current)  clearTimeout(expireTimerRef.current);
  };

  const logout = useCallback(async (expired = false) => {
    clearTimers();
    clearAccessToken();
    setToken(null);
    setUser(null);
    setShowTimeoutWarning(false);
    await logoutRequest(); // Revoke HttpOnly cookie on the server
    if (expired) {
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
    const WARNING_BEFORE = 2 * 60 * 1000;
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

  // Helper to apply a new access token to state + in-memory store + timers
  const applyToken = useCallback((jwt, userData) => {
    setAccessToken(jwt);
    setToken(jwt);
    setUser(userData);
    scheduleTimeoutWarning(jwt);
  }, [scheduleTimeoutWarning]);

  // Silent refresh on mount — restores session from the HttpOnly refresh token cookie.
  useEffect(() => {
    let cancelled = false;
    refreshRequest()
      .then((data) => {
        if (cancelled) return;
        const jwt = data.token;
        const claims = decodeJwt(jwt);
        applyToken(jwt, { username: claims.sub });
      })
      .catch(() => {
        // No valid cookie — user needs to log in
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => { cancelled = true; };
  }, []);

  // Restart timer when token changes
  useEffect(() => {
    if (token) scheduleTimeoutWarning(token);
    else clearTimers();
    return clearTimers;
  }, [token]);

  // The axios interceptor silently refreshes on a 401 without going through
  // this context. Adopt that token so the expiry timers — and the countdown
  // the user is watching — track the token actually in use.
  useEffect(() => {
    setOnTokenRefreshed((jwt) => {
      setToken(jwt);
      setShowTimeoutWarning(false);
    });
    return () => setOnTokenRefreshed(null);
  }, []);

  const login = useCallback(async (username, password) => {
    setLoading(true); setError(null);
    try {
      const data = await loginRequest(username, password);
      const jwt = data.token || data.accessToken || data.jwt;
      const claims = decodeJwt(jwt);
      const userData = data.user || { username: claims.sub || username };
      applyToken(jwt, userData);
      sessionStorage.removeItem("sessionExpired");
      return { success: true, role: getRoleFromToken(jwt), therapistId: getTherapistIdFromToken(jwt) };
    } catch (err) {
      setError(err.message || "Login failed. Please check your credentials.");
      return { success: false, role: null, therapistId: null };
    } finally {
      setLoading(false);
    }
  }, [applyToken]);

  // Called after therapist setup — silently refreshes to upgrade the token with therapistId
  const completeSetup = useCallback(async () => {
    const data = await refreshRequest();
    const jwt = data.token;
    const claims = decodeJwt(jwt);
    applyToken(jwt, { username: claims.sub });
    return { therapistId: getTherapistIdFromToken(jwt) };
  }, [applyToken]);

  // Called by "Stay signed in" button — performs a silent refresh before the access token expires
  const staySignedIn = useCallback(async () => {
    setShowTimeoutWarning(false);
    try {
      const data = await refreshRequest();
      const jwt = data.token;
      const claims = decodeJwt(jwt);
      applyToken(jwt, { username: claims.sub });
    } catch {
      // Refresh token also expired — log out cleanly
      await logout(true);
    }
  }, [applyToken, logout]);

  return (
    <AuthContext.Provider value={{ token, user, role, therapistId, login, logout, completeSetup, error, loading, showTimeoutWarning, staySignedIn, sessionExpiresAt }}>
      {children}
      {token && (
        <SessionExpiry
          expiresAt={sessionExpiresAt}
          showWarning={showTimeoutWarning}
          onExtend={staySignedIn}
          onSignOut={() => logout(true)}
        />
      )}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
