import api from "./client";

export async function getAvailability(fromDate, toDate) {
  try {
    const response = await api.get("/appointment/editor-view", {
      params: { fromDate, toDate }
    });
    return response.data; // { slots, appointments, overrides }
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to fetch availability.";
    throw new Error(message);
  }
}

export async function getAppointments() {
  try {
    const response = await api.get("/appointment/get-appointments");
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to fetch appointments.";
    throw new Error(message);
  }
}

export async function createAppointment(data) {
  try {
    const response = await api.post("/appointment/create-appointment", data);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to book appointment.";
    throw new Error(message);
  }
}

export async function generateSlots(startDate, endDate) {
  try {
    const response = await api.post(`/therapist/generate-slots?startDate=${startDate}&endDate=${endDate}`);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to generate slots.";
    throw new Error(message);
  }
}

export async function updateAppointmentStatus(data) {
  try {
    const response = await api.patch("/appointment/update-appointment", data);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to update appointment.";
    throw new Error(message);
  }
}

export async function rescheduleAppointment(data) {
  try {
    const response = await api.patch("/appointment/reschedule-appointment", data);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to reschedule appointment.";
    throw new Error(message);
  }
}

export async function getDashboardStats() {
  try {
    const response = await api.get("/therapist/dashboard/stats");
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to fetch stats.";
    throw new Error(message);
  }
}

export async function createAvailabilityOverride(data) {
  try {
    const response = await api.post("/therapist/create-availability-overrides", data);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to save override.";
    throw new Error(message);
  }
}

export async function deleteAvailabilityOverride(overrideId) {
  try {
    const response = await api.delete(`/therapist/availability-overrides/${overrideId}`);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to delete override.";
    throw new Error(message);
  }
}

export async function bulkAvailabilityOverrides(data) {
  try {
    const response = await api.post("/therapist/bulk-availability-overrides", data);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to save overrides.";
    throw new Error(message);
  }
}

export async function searchAppointments({ clientName, status, fromDate, toDate }) {
  try {
    const params = {};
    if (clientName) params.clientName = clientName;
    if (status && status.length > 0) params.status = status;
    if (fromDate) params.fromDate = fromDate;
    if (toDate) params.toDate = toDate;
    const response = await api.get("/appointment/search", { params });
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to search appointments.";
    throw new Error(message);
  }
}
