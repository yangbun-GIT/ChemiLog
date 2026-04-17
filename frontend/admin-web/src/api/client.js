import axios from "axios";

const api = axios.create({
  baseURL: "/api/v1",
  timeout: 8000,
  withCredentials: true,
});

let tokenGetter = () => null;

export function setupAdminApi(getToken) {
  tokenGetter = getToken;
}

api.interceptors.request.use((config) => {
  const token = tokenGetter?.();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;
