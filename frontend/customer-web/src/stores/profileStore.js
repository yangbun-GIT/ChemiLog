import { defineStore } from "pinia";
import api from "../api/client";

const PROFILE_STORAGE_PREFIX = "chemilog.customer.profile.v2.";

function parseAllergies(raw) {
  if (!raw) {
    return [];
  }
  if (Array.isArray(raw)) {
    return raw.filter(Boolean).map((value) => String(value).trim()).filter(Boolean);
  }
  return String(raw)
    .split(",")
    .map((value) => value.trim())
    .filter(Boolean);
}

export const useProfileStore = defineStore("profileStore", {
  state: () => ({
    activeUserKey: "guest",
    onboardingCompleted: false,
    goal: "MAINTAIN",
    strictness: "MEDIUM",
    allergies: [],
    email: "",
    role: "USER",
    status: "ACTIVE",
    loadedFromServer: false,
  }),
  getters: {
    goalLabel: (state) => {
      const map = {
        FAT_LOSS: "체지방 감량",
        BULK_UP: "근육량 증가",
        MAINTAIN: "건강 유지",
      };
      return map[state.goal] ?? state.goal;
    },
    strictnessLabel: (state) => {
      const map = {
        LOW: "낮음",
        MEDIUM: "중간",
        HIGH: "높음",
      };
      return map[state.strictness] ?? state.strictness;
    },
    adaptiveTone: (state) => {
      const map = {
        FAT_LOSS: {
          title: "Cut Mode",
          description: "칼로리 밀도와 고위험 첨가물 비중을 우선 관리합니다.",
          accent: "var(--brand-green)",
        },
        BULK_UP: {
          title: "Bulk Mode",
          description: "충분한 섭취량과 단백질 목표 달성을 우선 안내합니다.",
          accent: "var(--brand-orange)",
        },
        MAINTAIN: {
          title: "Balance Mode",
          description: "균형 식단과 과도한 첨가물 회피를 함께 관리합니다.",
          accent: "var(--brand-teal)",
        },
      };
      return map[state.goal] ?? map.MAINTAIN;
    },
  },
  actions: {
    storageKey() {
      return `${PROFILE_STORAGE_PREFIX}${this.activeUserKey}`;
    },
    setActiveUser(userId) {
      const nextKey = userId ? `user:${userId}` : "guest";
      if (nextKey === this.activeUserKey) return;
      this.persist();
      this.activeUserKey = nextKey;
      this.loadedFromServer = false;
      this.hydrate();
    },
    hydrate() {
      const raw = localStorage.getItem(this.storageKey());
      if (!raw) {
        this.onboardingCompleted = false;
        this.goal = "MAINTAIN";
        this.strictness = "MEDIUM";
        this.allergies = [];
        this.email = "";
        this.role = "USER";
        this.status = "ACTIVE";
        this.loadedFromServer = false;
        return;
      }
      try {
        const parsed = JSON.parse(raw);
        this.onboardingCompleted = Boolean(parsed.onboardingCompleted);
        this.goal = parsed.goal ?? "MAINTAIN";
        this.strictness = parsed.strictness ?? "MEDIUM";
        this.allergies = parseAllergies(parsed.allergies);
        this.email = parsed.email ?? "";
        this.role = parsed.role ?? "USER";
        this.status = parsed.status ?? "ACTIVE";
      } catch {
        this.onboardingCompleted = false;
        this.goal = "MAINTAIN";
        this.strictness = "MEDIUM";
        this.allergies = [];
        this.email = "";
        this.role = "USER";
        this.status = "ACTIVE";
        this.loadedFromServer = false;
      }
    },
    persist() {
      localStorage.setItem(
        this.storageKey(),
        JSON.stringify({
          onboardingCompleted: this.onboardingCompleted,
          goal: this.goal,
          strictness: this.strictness,
          allergies: this.allergies,
          email: this.email,
          role: this.role,
          status: this.status,
        })
      );
    },
    async completeProfile(payload, { syncRemote = false } = {}) {
      this.goal = payload.goal;
      this.strictness = payload.strictness;
      this.allergies = parseAllergies(payload.allergies);
      this.onboardingCompleted = true;
      this.persist();

      if (syncRemote) {
        await api.post("/users/onboarding", {
          goal: this.goal,
          strictness: this.strictness,
          allergies: this.allergies,
        });
      }
    },
    async fetchMe() {
      const response = await api.get("/users/me");
      const data = response.data?.data;
      if (!data) {
        return null;
      }
      this.email = data.email ?? "";
      this.role = data.role ?? "USER";
      this.status = data.status ?? "ACTIVE";
      this.goal = data.goal ?? this.goal;
      this.strictness = data.strictness ?? this.strictness;
      this.allergies = parseAllergies(data.allergies);
      this.onboardingCompleted = true;
      this.loadedFromServer = true;
      this.persist();
      return data;
    },
    async updateProfile(payload) {
      await api.patch("/users/me/profile", {
        goal: payload.goal,
        strictness: payload.strictness,
        allergies: parseAllergies(payload.allergies),
      });
      this.goal = payload.goal;
      this.strictness = payload.strictness;
      this.allergies = parseAllergies(payload.allergies);
      this.onboardingCompleted = true;
      this.persist();
    },
  },
});
