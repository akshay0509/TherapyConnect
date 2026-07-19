import api from "./client";

export async function createUserRequest(username, email, password, role) {
  try {
    const response = await api.post("/user/create-user", { username, email, password, userRole: role });
    return response.data;
  } catch (err) {
    const message =
      err.response?.data?.message ||
      err.response?.data?.error ||
      "Failed to create user. Please try again.";
    throw new Error(message);
  }
}

export async function forgotPassword(email) {
  try {
    const response = await api.post("/user/forgot-password", { email });
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to send reset email.";
    throw new Error(message);
  }
}

export async function forgotUsername(email) {
  try {
    const response = await api.post("/user/forgot-username", { email });
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to send username email.";
    throw new Error(message);
  }
}

export async function resetPassword(token, newPassword) {
  try {
    const response = await api.post("/user/reset-password", { token, newPassword });
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to reset password.";
    throw new Error(message);
  }
}

export async function updateAccount(data) {
  try {
    const response = await api.put("/user/update-account", data);
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to update account.";
    throw new Error(message);
  }
}

export async function getAccount() {
  try {
    const response = await api.get("/user/account");
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to load account details.";
    throw new Error(message);
  }
}

export async function changePassword(currentPassword, newPassword) {
  try {
    const response = await api.put("/user/change-password", { currentPassword, newPassword });
    return response.data;
  } catch (err) {
    const message = err.response?.data?.message || err.response?.data?.error || "Failed to change password.";
    throw new Error(message);
  }
}
