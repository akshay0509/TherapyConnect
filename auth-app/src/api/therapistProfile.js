import api from "./client";

export async function getTherapistProfile() {
  try {
    const response = await api.get("/therapist/therapistProfile");
    return response.data;
  } catch (err) {
    if (err.response?.status === 404 || err.response?.status === 204) return null;
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to fetch profile.";
    throw new Error(message);
  }
}

export async function createTherapistProfile(profileData) {
  try {
    const response = await api.post("/therapist/create-therapist", profileData);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to create profile.";
    throw new Error(message);
  }
}
