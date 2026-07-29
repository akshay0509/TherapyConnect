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

/**
 * Edit an existing profile. Email is intentionally stripped — it mirrors the
 * account login email and is owned by Account Settings (updateTherapistEmail),
 * which updates both records together. Sending it here would be ignored by the
 * backend anyway; dropping it makes that explicit at the call site.
 */
export async function updateTherapistProfile(profileData) {
  try {
    const { email, ...rest } = profileData;
    const response = await api.put("/therapist/update-therapist", rest);
    return response.data;
  } catch (err) {
    throw new Error(
      err.response?.data?.message || err.response?.data?.error || "Failed to update profile."
    );
  }
}

export async function updatePaymentSettings(paymentEnabled) {
  try {
    const response = await api.put("/therapist/payment-settings", { paymentEnabled });
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to update payment settings.";
    throw new Error(message);
  }
}

// keeps the therapist's invite/contact email in sync with the account email
export async function updateTherapistEmail(email) {
  try {
    const response = await api.put("/therapist/update-email", { email });
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to update therapist email.";
    throw new Error(message);
  }
}
