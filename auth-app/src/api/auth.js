import api from "./client";

export async function loginRequest(username, password) {
  try {
    const response = await api.post("/auth/login", { username, password });
    return response.data;
  } catch (err) {
    const message =
      err.response?.data?.message ||
      err.response?.data?.error ||
      "Invalid credentials";
    throw new Error(message);
  }
}
