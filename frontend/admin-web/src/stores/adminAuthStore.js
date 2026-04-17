import { defineStore } from "pinia";
import api from "../api/client";

const STORAGE_KEY = "chemilog.admin.session.v1";

function parseJwtPayload(token) {
  try {
    const payload = token.split(".")[1];
    return JSON.parse(atob(payload));
  } catch {
    return null;
  }
}

export const useAdminAuthStore = defineStore("adminAuthStore", {
  state: () => ({
    accessToken: null,
    claims: null,
    loading: false,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken),
    isAdmin: (state) => (state.claims?.role || "") === "ADMIN",
  },
  actions: {
    hydrate() {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return;
      try {
        const parsed = JSON.parse(raw);
        this.accessToken = parsed.accessToken ?? null;
        this.claims = this.accessToken ? parseJwtPayload(this.accessToken) : null;
      } catch {
        this.accessToken = null;
        this.claims = null;
      }
    },
    persist() {
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({
          accessToken: this.accessToken,
        })
      );
    },
    clear() {
      this.accessToken = null;
      this.claims = null;
      localStorage.removeItem(STORAGE_KEY);
    },
    async login(username, password) {
      this.loading = true;
      try {
        const response = await api.post("/auth/login", { username, password });
        const token = response.data?.data?.accessToken;
        if (!token) {
          throw new Error("accessToken not found");
        }
        this.accessToken = token;
        this.claims = parseJwtPayload(token);
        this.persist();

        if (!this.isAdmin) {
          this.clear();
          throw new Error("관리자 권한 계정이 아닙니다.");
        }
      } finally {
        this.loading = false;
      }
    },
    async logout() {
      try {
        await api.post("/auth/logout");
      } catch {
        // no-op
      } finally {
        this.clear();
      }
    },
  },
});
