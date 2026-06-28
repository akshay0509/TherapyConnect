import api from "./client";

// GET /therapist/earnings/summary
// Returns fixed-period aggregates: week/month/lifetime earnings + paid/DSF session counts.
// No params — periods are computed server-side relative to today.
export async function getEarningsSummary() {
  try {
    const response = await api.get("/therapist/earnings/summary");
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to fetch earnings summary.";
    throw new Error(message);
  }
}

// GET /therapist/earnings/sessions?fromDate=YYYY-MM-DD&toDate=YYYY-MM-DD[&serviceId=...][&modeId=...]
// fromDate and toDate are required. serviceId and modeId are optional filters.
export async function getEarningsSessions(fromDate, toDate, serviceId, modeId) {
  try {
    const params = { fromDate, toDate };
    if (serviceId) params.serviceId = serviceId;
    if (modeId) params.modeId = modeId;
    const response = await api.get("/therapist/earnings/sessions", { params });
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to fetch earnings sessions.";
    throw new Error(message);
  }
}

// GET /therapist/earnings/export?fromDate=YYYY-MM-DD&toDate=YYYY-MM-DD[&serviceId=...][&modeId=...]
// Triggers a CSV file download in the browser using the same filters as getEarningsSessions.
export async function exportEarningsCsv(fromDate, toDate, serviceId, modeId) {
  try {
    const params = { fromDate, toDate };
    if (serviceId) params.serviceId = serviceId;
    if (modeId) params.modeId = modeId;
    const response = await api.get("/therapist/earnings/export", {
      params,
      responseType: "blob",
    });
    const url = window.URL.createObjectURL(new Blob([response.data], { type: "text/csv" }));
    const link = document.createElement("a");
    link.href = url;
    const disposition = response.headers["content-disposition"] || "";
    const match = disposition.match(/filename="?([^";\n]+)"?/);
    link.download = match ? match[1] : `earnings-${fromDate}-to-${toDate}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to export earnings.";
    throw new Error(message);
  }
}
