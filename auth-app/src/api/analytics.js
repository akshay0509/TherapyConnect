import api from "./client";

export async function getAnalyticsSummary(from, to) {
  try {
    const response = await api.get("/analytics/summary", { params: { from, to } });
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to fetch analytics summary.";
    throw new Error(message);
  }
}

export async function getAnalyticsDaily(from, to) {
  try {
    const response = await api.get("/analytics/daily", { params: { from, to } });
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to fetch daily analytics.";
    throw new Error(message);
  }
}

export async function getAnalyticsServices(from, to) {
  try {
    const response = await api.get("/analytics/services", { params: { from, to } });
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to fetch service breakdown.";
    throw new Error(message);
  }
}

export async function getAnalyticsRetention() {
  try {
    const response = await api.get("/analytics/retention");
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to fetch retention summary.";
    throw new Error(message);
  }
}

export async function getAnalyticsRetentionFrequency() {
  try {
    const response = await api.get("/analytics/retention/frequency");
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to fetch session frequency.";
    throw new Error(message);
  }
}
