import api from "./client";

function message(err, fallback) {
  return err.response?.data?.message || err.response?.data?.error || fallback;
}

export async function getClientIntakes(status = "PENDING") {
  try {
    return (await api.get("/client/intakes", { params: { status } })).data;
  } catch (err) {
    throw new Error(message(err, "Failed to fetch client intakes."));
  }
}

/**
 * action: "CREATE" builds a new client from `client`; "LINK" attaches the
 * intake to an existing `clientId`.
 */
export async function approveClientIntake(intakeId, payload) {
  try {
    return (await api.post(`/client/intakes/${intakeId}/approve`, payload)).data;
  } catch (err) {
    throw new Error(message(err, "Failed to approve this intake."));
  }
}

export async function rejectClientIntake(intakeId, reason) {
  try {
    return (await api.post(`/client/intakes/${intakeId}/reject`, { reason })).data;
  } catch (err) {
    throw new Error(message(err, "Failed to reject this intake."));
  }
}
