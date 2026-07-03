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
