import api from "./client";

export async function getTherapists() {
  try {
    const response = await api.get("/therapist/therapists");
    return response.data;
  } catch (err) {
    const message =
      err.response?.data?.message ||
      err.response?.data?.error ||
      "Failed to fetch therapists.";
    throw new Error(message);
  }
}

export async function getTherapistServices(therapistId) {
  try {
    const response = await api.get(`/therapist/${therapistId}/therapist-services`);
    return response.data;
  } catch (err) {
    const message =
      err.response?.data?.message ||
      err.response?.data?.error ||
      "Failed to fetch services.";
    throw new Error(message);
  }
}
