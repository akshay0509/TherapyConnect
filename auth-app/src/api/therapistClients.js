import api from "./client";

export async function getTherapistClients() {
  try {
    const response = await api.get("/therapist/clients");
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to fetch clients.";
    throw new Error(message);
  }
}

export async function createClient(clientData) {
  try {
    const response = await api.post("/therapist/create-client", clientData);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to create client.";
    throw new Error(message);
  }
}

export async function getClientById(clientId) {
  try {
    const response = await api.get(`/client/get/${clientId}`);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to fetch client details.";
    throw new Error(message);
  }
}

export async function getClientFeeHistory(clientId) {
  try {
    const response = await api.get(`/client/${clientId}/fee-history`);
    return response.data;
  } catch (err) {
    // The audit trail is supporting detail, never the reason a page fails to load.
    return [];
  }
}

export async function getClientRisk(clientId) {
  try {
    const response = await api.get(`/therapist/clients/${clientId}/risk`);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to load risk record.";
    throw new Error(message);
  }
}

// Saving is reviewing: the server stamps the review date and appends to the log.
export async function saveClientRisk(clientId, risk) {
  try {
    const response = await api.put(`/therapist/clients/${clientId}/risk`, risk);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to save risk record.";
    throw new Error(message);
  }
}

export async function getSessionDetails(clientId) {
  try {
    const response = await api.get(`/therapist/${clientId}/session-details`);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to fetch sessions.";
    throw new Error(message);
  }
}

export async function createSessionNotes(clientId, appointmentId, sessionNotes) {
  try {
    const response = await api.post(`/therapist/${clientId}/create-notes`, { appointmentId, sessionNotes });
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to save notes.";
    throw new Error(message);
  }
}

export async function updateSessionNotes(clientId, appointmentId, sessionNotes) {
  try {
    const response = await api.put(`/therapist/${clientId}/update-notes`, { appointmentId, sessionNotes });
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to update notes.";
    throw new Error(message);
  }
}

export async function updateClient(clientId, clientData) {
  try {
    const response = await api.put(`/client/update/${clientId}`, clientData);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to update client.";
    throw new Error(message);
  }
}

export async function updateClientStatus(clientId, status) {
  try {
    const response = await api.patch(`/client/update-status/${clientId}`, { status });
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to update client status.";
    throw new Error(message);
  }
}

// Backend always returns 200 with a single ClientNotesDto.
// When no note exists yet, returns { therapistId, clientId, content: "" } with no noteId.
// Wraps in array so callers can use .map()/.length; returns [] when no real note exists.
export async function getClientNotes(clientId) {
  try {
    const response = await api.get(`/therapist/${clientId}/note`);
    const data = response.data;
    return data && data.noteId ? [data] : [];
  } catch (err) {
    if (err.response?.status === 404) return [];
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to fetch notes.";
    throw new Error(message);
  }
}

// Backend is an upsert (PUT) — one note per therapist+client pair.
// Calling this creates or replaces the existing note.
export async function upsertClientNote(clientId, content) {
  try {
    const response = await api.put(`/therapist/${clientId}/note`, { content });
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to save note.";
    throw new Error(message);
  }
}
