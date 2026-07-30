import api from "./client";

export async function getMyServices() {
  try {
    const response = await api.get("/therapist/therapist-services");
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to fetch services.";
    throw new Error(message);
  }
}

/**
 * Creates the service and its first delivery mode in one transactional call.
 * A service with no mode cannot be booked — AppointmentService resolves
 * duration and fee from the mode — so the two must not be able to half-succeed.
 */
export async function createServiceWithMode(service, mode) {
  try {
    const response = await api.post("/therapist/create-service-with-mode", { service, mode });
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to create service.";
    throw new Error(message);
  }
}

export async function createService(serviceData) {
  try {
    const response = await api.post("/therapist/create-service", serviceData);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to create service.";
    throw new Error(message);
  }
}

export async function updateService(serviceId, serviceData) {
  try {
    const response = await api.put(`/therapist/update-service/${serviceId}`, serviceData);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to update service.";
    throw new Error(message);
  }
}

export async function deleteService(serviceId) {
  try {
    const response = await api.delete(`/therapist/delete-service/${serviceId}`);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to delete service.";
    throw new Error(message);
  }
}
