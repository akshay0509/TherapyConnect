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

// Kept for backwards compatibility — note the backend scopes this to today only.
// Prefer getAvailability with explicit dates for any range-aware UI.
export async function getAppointments() {
  try {
    const response = await api.get("/appointment/get-appointments");
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to fetch appointments.";
    throw new Error(message);
  }
}

// modeId is required (@NotNull on backend). customPrice is optional — overrides mode default fee.
// Returns { appointmentId, paymentStatus, paymentLinkUrl } — payment fields are
// null when payments are disabled for the therapist.
export async function createAppointment({ slotId, therapistId, clientId, clientName, modeId, customPrice }) {
  try {
    const body = { slotId, therapistId, clientId, clientName, modeId };
    if (customPrice != null) body.customPrice = customPrice;
    const response = await api.post("/appointment/create-appointment", body);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to book appointment.";
    throw new Error(message);
  }
}

export async function getPaymentInfo(appointmentId) {
  try {
    const response = await api.get(`/appointment/payments/${appointmentId}`);
    return response.data;
  } catch (err) {
    if (err.response?.status === 404) return null;
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to fetch payment info.";
    throw new Error(message);
  }
}

// Creates the payment link if missing, or retries after a failure.
export async function ensurePaymentLink(appointmentId) {
  try {
    const response = await api.post(`/appointment/payments/${appointmentId}/link`);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to create payment link.";
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
