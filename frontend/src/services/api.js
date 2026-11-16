import axios from 'axios';

export const TOKEN_KEY = 'jat_token';
export const USER_KEY = 'jat_user';

// localStorage can throw (private mode, blocked storage), so every access is guarded.
export function readSession() {
  try {
    const token = localStorage.getItem(TOKEN_KEY);
    const user = JSON.parse(localStorage.getItem(USER_KEY) || 'null');
    return token && user ? { token, user } : { token: null, user: null };
  } catch {
    return { token: null, user: null };
  }
}

export function saveSession(token, user) {
  try {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  } catch {
    /* session lives in memory only */
  }
}

export function clearSession() {
  try {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  } catch {
    /* nothing to clear */
  }
}

export const AUTH_EXPIRED_EVENT = 'auth:expired';

const api = axios.create({
  // "/api" is proxied to Spring Boot by Vite (dev) and nginx (Docker).
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 20000,
});

// Attach the JWT to every request.
api.interceptors.request.use((config) => {
  const { token } = readSession();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// An expired or invalid token on a protected call logs the user out.
// Login/register failures (also 401/400) must NOT trigger this, so /auth/* is skipped.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url ?? '';
    if (error.response?.status === 401 && !url.startsWith('/auth/')) {
      clearSession();
      window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
    }
    return Promise.reject(error);
  },
);

/** Human-readable message from the backend's ErrorResponse, with sensible fallbacks. */
export function getErrorMessage(error, fallback = 'Something went wrong. Please try again.') {
  if (error?.response?.data?.message) return error.response.data.message;
  if (error?.code === 'ECONNABORTED') return 'The request timed out. Please try again.';
  if (error?.request && !error.response) return 'Cannot reach the server. Check your connection and try again.';
  return fallback;
}

/** Field-level validation errors ({ fieldName: message }) from a 400 response, or {}. */
export function getFieldErrors(error) {
  return error?.response?.data?.fieldErrors ?? {};
}

export default api;
