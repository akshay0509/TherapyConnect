import axios from "axios";

const BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8091";

// In-memory token — never written to localStorage
let _accessToken = null;
let _isRefreshing = false;
let _refreshQueue = [];

// AuthContext registers here so a silent refresh below also re-arms its expiry
// timers. Without it the context keeps counting down the token it last saw and
// signs the user out while a perfectly valid one is in play.
let _onTokenRefreshed = null;

export function setOnTokenRefreshed(cb) {
  _onTokenRefreshed = cb;
}

export function setAccessToken(token) {
  _accessToken = token;
}

export function clearAccessToken() {
  _accessToken = null;
}

const api = axios.create({
  baseURL: BASE_URL,
  headers: { "Content-Type": "application/json" },
  withCredentials: true, // Required to send/receive the HttpOnly refresh token cookie
});

// Attach in-memory access token to every request
api.interceptors.request.use((config) => {
  if (_accessToken) {
    config.headers.Authorization = `Bearer ${_accessToken}`;
  }
  return config;
});

// On 401: attempt one silent refresh, then retry the original request.
// Requests to /auth/* are never retried (avoids infinite loops).
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url.includes("/auth/")
    ) {
      if (_isRefreshing) {
        return new Promise((resolve, reject) => {
          _refreshQueue.push({ resolve, reject });
        }).then((newToken) => {
          originalRequest.headers.Authorization = `Bearer ${newToken}`;
          return api(originalRequest);
        });
      }

      originalRequest._retry = true;
      _isRefreshing = true;

      try {
        const { data } = await axios.post(
          `${BASE_URL}/auth/refresh`,
          {},
          { withCredentials: true }
        );
        const newToken = data.token;
        setAccessToken(newToken);
        _onTokenRefreshed?.(newToken);
        _refreshQueue.forEach(({ resolve }) => resolve(newToken));
        _refreshQueue = [];
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        _refreshQueue.forEach(({ reject }) => reject(refreshError));
        _refreshQueue = [];
        clearAccessToken();
        window.location.href = "/login";
        return Promise.reject(refreshError);
      } finally {
        _isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default api;
