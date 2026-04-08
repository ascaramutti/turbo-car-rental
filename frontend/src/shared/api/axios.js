import axios from 'axios';

const TOKEN_KEY = 'token';
const USER_KEY = 'user';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

/** Attaches JWT token to every outgoing request. */
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/** Auth endpoints where 401 should NOT trigger redirect (handled by the component). */
const AUTH_ENDPOINTS = ['/auth/login', '/auth/verify-otp'];

/** Clears session and redirects to login on 401 Unauthorized (except auth endpoints). */
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const requestUrl = error.config?.url;
    const isAuthRequest = AUTH_ENDPOINTS.some((endpoint) => requestUrl?.includes(endpoint));

    if (error.response?.status === 401 && !isAuthRequest) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
