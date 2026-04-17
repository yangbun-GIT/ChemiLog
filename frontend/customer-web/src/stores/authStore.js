import { defineStore } from "pinia";
import api from "../api/client";

function parseJwtPayload(token) {
  try {
    const payload = token.split(".")[1];
    return JSON.parse(atob(payload));
  } catch {
    return null;
  }
}

export const useAuthStore = defineStore("authStore", {
  state: () => ({
    accessToken: null,
    userClaims: null,
    isSessionLoading: false,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken),
  },
  actions: {
    setAccessToken(token) {
      this.accessToken = token;
      this.userClaims = token ? parseJwtPayload(token) : null;
    },
    clearSession() {
      this.accessToken = null;
      this.userClaims = null;
    },
    async login(username, password) {
      const response = await api.post("/auth/login", { username, password });
      this.setAccessToken(response.data?.data?.accessToken ?? null);
      return response.data?.data;
    },
    async register(payload) {
      const response = await api.post("/auth/register", payload);
      this.setAccessToken(response.data?.data?.accessToken ?? null);
      return response.data?.data;
    },
    async trySilentRefresh() {
      this.isSessionLoading = true;
      try {
        const response = await api.post("/auth/refresh");
        this.setAccessToken(response.data?.data?.accessToken ?? null);
      } catch {
        this.clearSession();
      } finally {
        this.isSessionLoading = false;
      }
    },
    async logout() {
      try {
        await api.post("/auth/logout");
      } finally {
        this.clearSession();
      }
    },
  },
});
