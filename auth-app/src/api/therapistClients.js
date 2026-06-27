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

export async function getClientNotes(clientId) {
  try {
    const response = await api.get(`/client/${clientId}/notes`);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to fetch notes.";
    throw new Error(message);
  }
}

export async function createClientNote(clientId, content) {
  try {
    const response = await api.post(`/client/${clientId}/notes`, { content });
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to save note.";
    throw new Error(message);
  }
}
