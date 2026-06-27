import api from "./client";

export async function getAvailabilityRules() {
  try {
    const response = await api.get("/therapist/availability-rules");
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to fetch availability rules.";
    throw new Error(message);
  }
}

export async function createAvailabilityRules(rules) {
  try {
    const response = await api.post("/therapist/create-availability-rules", rules);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to save rules.";
    throw new Error(message);
  }
}

export async function deleteAvailabilityRule(ruleId) {
  try {
    const response = await api.delete(`/therapist/availability-rules/${ruleId}`);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to delete rule.";
    throw new Error(message);
  }
}

export async function updateAvailabilityRule(ruleId, ruleData) {
  try {
    const response = await api.put(`/therapist/availability-rules/${ruleId}`, ruleData);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to update rule.";
    throw new Error(message);
  }
}
