import axios from "axios";
import { useAuthStore } from "../stores/authStore";

const api = axios.create({
  baseURL: "/api/v1",
  timeout: 8000,
  withCredentials: true,
});

const refreshClient = axios.create({
  baseURL: "/api/v1",
  timeout: 8000,
  withCredentials: true,
});

let isRefreshing = false;
let requestQueue = [];
let piniaRef = null;

function drainQueue(error, token = null) {
  requestQueue.forEach((pending) => {
    if (error) {
      pending.reject(error);
      return;
    }
    pending.resolve(token);
  });
  requestQueue = [];
}

function authStore() {
  if (!piniaRef) {
    throw new Error("API client has not been initialized with Pinia.");
  }
  return useAuthStore(piniaRef);
}

export function setupApiClient(pinia) {
  piniaRef = pinia;

  api.interceptors.request.use((config) => {
    const token = authStore().accessToken;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  api.interceptors.response.use(
    (response) => response,
    async (error) => {
      const originalRequest = error.config;
      const status = error.response?.status;

      if (!originalRequest || status !== 401 || originalRequest.__isRetryRequest) {
        return Promise.reject(error);
      }

      if (originalRequest.url?.includes("/auth/refresh")) {
        authStore().clearSession();
        return Promise.reject(error);
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          requestQueue.push({ resolve, reject });
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          originalRequest.__isRetryRequest = true;
          return api(originalRequest);
        });
      }

      isRefreshing = true;
      originalRequest.__isRetryRequest = true;

      try {
        const refreshResponse = await refreshClient.post("/auth/refresh");
        const nextToken = refreshResponse.data?.data?.accessToken;
        if (!nextToken) {
          throw new Error("No access token returned by refresh endpoint.");
        }
        authStore().setAccessToken(nextToken);
        drainQueue(null, nextToken);
        originalRequest.headers.Authorization = `Bearer ${nextToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        drainQueue(refreshError, null);
        authStore().clearSession();
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }
  );
}

export default api;
