import axios from "axios";

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8091";

const adminApi = axios.create({
  baseURL: BASE_URL,
  headers: { "Content-Type": "application/json" },
});

// Attach admin token from sessionStorage on every request
adminApi.interceptors.request.use((config) => {
  const token = sessionStorage.getItem("adminToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export async function adminLogin(username, password) {
  const { data } = await adminApi.post("/auth/admin-login", { username, password });
  sessionStorage.setItem("adminToken", data.token);
  return data;
}

export function adminLogout() {
  sessionStorage.removeItem("adminToken");
}

export function isAdminLoggedIn() {
  return !!sessionStorage.getItem("adminToken");
}

export async function getAppointmentHealth() {
  const { data } = await adminApi.get("/appointment/admin/health");
  return data;
}

export async function getAnalyticsHealth() {
  const { data } = await adminApi.get("/analytics/admin/health");
  return data;
}

export async function replayOutbox(from) {
  const body = from ? { from } : {};
  const { data } = await adminApi.post("/appointment/admin/outbox/replay", body);
  return data;
}

// ── Service health (gateway pings each service via Eureka) ──
export async function getServicesHealth() {
  const { data } = await adminApi.get("/admin/services");
  return data;
}

// ── User management ──
export async function getUsers() {
  const { data } = await adminApi.get("/user/admin/users");
  return data;
}

export async function updateUserStatus(userId, { enabled, locked }) {
  const body = {};
  if (enabled !== undefined) body.enabled = enabled;
  if (locked !== undefined) body.locked = locked;
  const { data } = await adminApi.put(`/user/admin/users/${userId}/status`, body);
  return data;
}

// ── Login audit log ──
export async function getLoginAudit() {
  const { data } = await adminApi.get("/user/admin/audit");
  return data;
}
