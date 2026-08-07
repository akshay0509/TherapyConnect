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
//
// Deliberately deduplicated. The refresh token ROTATES: the first call consumes
// it and issues a replacement, so a second call carrying the same cookie value
// presents an already-revoked token and gets a 401. Two callers overlapping
// therefore means one of them fails and the session is torn down.
//
// That is not hypothetical — React StrictMode double-invokes the mount effect in
// development, so every page load fired two refreshes: the first succeeded but
// its result was discarded by the effect cleanup, and the second (the one whose
// result would have been used) got the 401. The user was logged out on every
// reload. In production the same collision happens whenever two tabs start at
// once, or a request is retried.
//
// Sharing one in-flight promise means concurrent callers all receive the single
// rotation, and the token is spent exactly once.
let inFlightRefresh = null;

export function refreshRequest() {
  if (inFlightRefresh) return inFlightRefresh;

  inFlightRefresh = axios
    .post(`${BASE_URL}/auth/refresh`, {}, { withCredentials: true })
    .then(({ data }) => data)
    .finally(() => {
      // Cleared only once settled, so a later refresh still performs a real call.
      inFlightRefresh = null;
    });

  return inFlightRefresh;
}

// Clears the HttpOnly cookie server-side and revokes the stored refresh token.
export async function logoutRequest() {
  try {
    await axios.post(`${BASE_URL}/auth/logout`, {}, { withCredentials: true });
  } catch {
    // Best-effort: proceed with client-side cleanup even if the server call fails
  }
}
