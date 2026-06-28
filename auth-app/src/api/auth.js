import api from "./client";

const FAILURE_REASON_LABELS = {
  BAD_CREDENTIALS:  "Incorrect username or password.",
  ACCOUNT_LOCKED:   "Your account has been locked. Please contact support.",
  ACCOUNT_DISABLED: "Your account has been disabled. Please contact support.",
};

export async function loginRequest(username, password) {
  try {
    const response = await api.post("/auth/login", { username, password });
    return response.data;
  } catch (err) {
    const data = err.response?.data || {};
    const reason = data["Failure Reason"];
    const message = reason
      ? (FAILURE_REASON_LABELS[reason] || reason)
      : (data.message || data.error || "Invalid credentials");
    throw new Error(message);
  }
}
