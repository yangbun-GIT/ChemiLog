import { defineStore } from "pinia";
import api from "../api/client";

const PROFILE_STORAGE_KEY = "chemilog.customer.profile.v2";

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
    hydrate() {
      const raw = localStorage.getItem(PROFILE_STORAGE_KEY);
      if (!raw) {
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
      }
    },
    persist() {
      localStorage.setItem(
        PROFILE_STORAGE_KEY,
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
  },
});
