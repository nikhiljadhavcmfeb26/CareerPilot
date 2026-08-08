import axios from 'axios';

/**
 * Every frontend request goes through the Spring Cloud API Gateway (port 8080),
 * never directly to a microservice. The old value here was
 * http://localhost:5000/api - the retired ASP.NET Core Kestrel port - which is
 * why nothing in the app could reach the backend after the migration.
 *
 * Override per environment with VITE_API_BASE_URL in client/.env
 * (see client/.env.example).
 */
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const AUTH_KEYS = ['accessToken', 'refreshToken'];

const clearSession = () => AUTH_KEYS.forEach((k) => localStorage.removeItem(k));

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' }
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Never try to refresh using a call that is itself part of the auth
    // handshake - that recurses and masks the real error.
    const isAuthCall =
      originalRequest?.url?.includes('/auth/refresh-token') ||
      originalRequest?.url?.includes('/auth/login') ||
      originalRequest?.url?.includes('/auth/register');

    if (error.response?.status !== 401 || originalRequest?._retry || isAuthCall) {
      return Promise.reject(error);
    }

    originalRequest._retry = true;
    const refreshToken = localStorage.getItem('refreshToken');

    if (!refreshToken) {
      clearSession();
      return Promise.reject(error);
    }

    try {
      const { data } = await axios.post(`${API_BASE_URL}/auth/refresh-token`, { refreshToken });

      if (!data?.success) {
        clearSession();
        window.location.href = '/login';
        return Promise.reject(error);
      }

      localStorage.setItem('accessToken', data.data.accessToken);
      localStorage.setItem('refreshToken', data.data.refreshToken);
      originalRequest.headers.Authorization = `Bearer ${data.data.accessToken}`;
      return api(originalRequest);
    } catch (refreshError) {
      clearSession();
      window.location.href = '/login';
      return Promise.reject(refreshError);
    }
  }
);

export default api;
