import api from "./client";

// Response shape: { modeId, therapistId, serviceId, modeType, displayName, address, price, isActive }

export async function getDeliveryModes(serviceId) {
  try {
    const params = serviceId ? { serviceId } : {};
    const response = await api.get("/therapist/delivery-modes", { params });
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to fetch delivery modes.";
    throw new Error(message);
  }
}

export async function createDeliveryMode(dto) {
  try {
    const response = await api.post("/therapist/delivery-modes", dto);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to create delivery mode.";
    throw new Error(message);
  }
}

export async function updateDeliveryMode(modeId, dto) {
  try {
    const response = await api.put(`/therapist/delivery-modes/${modeId}`, dto);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to update delivery mode.";
    throw new Error(message);
  }
}

export async function deleteDeliveryMode(modeId) {
  try {
    const response = await api.delete(`/therapist/delivery-modes/${modeId}`);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to delete delivery mode.";
    throw new Error(message);
  }
}
