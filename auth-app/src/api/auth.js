import axios from "axios";
import api from "./client";

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8091";

const FAILURE_REASON_LABELS = {
  INVALID_CREDENTIALS: "Incorrect username or password.",
  ACCOUNT_LOCKED:      "Your account has been locked. Please contact support.",
  ACCOUNT_DISABLED:    "Your account has been disabled. Please contact support.",
  SERVICE_UNAVAILABLE: "Authentication service is temporarily unavailable. Please try again shortly.",
};

export async function loginRequest(username, password) {
  try {
    const response = await api.post("/auth/login", { username, password });
    return response.data;
  } catch (err) {
    const data = err.response?.data || {};
    // Backend now returns { failureReason: "INVALID_CREDENTIALS" } with HTTP 401
    const reason = data.failureReason || data["Failure Reason"];
    const message = reason
      ? (FAILURE_REASON_LABELS[reason] || reason)
      : (data.message || data.error || "Invalid credentials");
    throw new Error(message);
  }
}

// Called on AuthProvider mount to silently restore the session from the HttpOnly cookie.
// Returns { token } on success, throws on failure (user must log in again).
export async function refreshRequest() {
  const { data } = await axios.post(
    `${BASE_URL}/auth/refresh`,
    {},
    { withCredentials: true }
  );
  return data;
}

// Clears the HttpOnly cookie server-side and revokes the stored refresh token.
export async function logoutRequest() {
  try {
    await axios.post(`${BASE_URL}/auth/logout`, {}, { withCredentials: true });
  } catch {
    // Best-effort: proceed with client-side cleanup even if the server call fails
  }
}
