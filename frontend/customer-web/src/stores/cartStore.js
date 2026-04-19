import { defineStore } from "pinia";
import api from "../api/client";

const CART_STORAGE_KEY = "chemilog.customer.cart.v4";
const CART_META_KEY = "chemilog.customer.cart.meta.v2";
let dateTickerId = null;

function todayDateString() {
  const now = new Date();
  const year = now.getFullYear();
  const month = `${now.getMonth() + 1}`.padStart(2, "0");
  const day = `${now.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function toNumber(value, fallback = 0) {
  const num = Number(value);
  return Number.isFinite(num) ? num : fallback;
}

function randomIdempotencyKey() {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function normalizeWarningLabel(value) {
  return String(value || "").replace(/^주의:\s*/u, "").trim();
}

function scoreFromCalories(totalCalories) {
  const value = toNumber(totalCalories);
  if (value <= 0) return 0;
  if (value <= 550) return 4;
  if (value <= 800) return 3;
  if (value <= 1100) return 2;
  return 1;
}

function normalizeItem(raw) {
  return {
    foodId: toNumber(raw.foodId ?? raw.food_id),
    name: String(raw.name ?? "식품"),
    category: String(raw.category ?? "기타"),
    mealTypeHint: String(raw.mealTypeHint ?? raw.meal_type_hint ?? "").toUpperCase(),
    calories: toNumber(raw.calories),
    additiveIds: Array.isArray(raw.additiveIds ?? raw.additive_ids)
      ? (raw.additiveIds ?? raw.additive_ids).map((id) => toNumber(id)).filter((id) => id > 0)
      : [],
    quantity: Math.max(0.1, toNumber(raw.quantity, 1)),
  };
}

function normalizeHistoryDay(raw) {
  const date = String(raw?.date || "").trim();
  if (!date) return null;
  const topAdditives = Array.isArray(raw?.topAdditives)
    ? [...new Set(raw.topAdditives.map(normalizeWarningLabel).filter(Boolean))]
    : [];
  return {
    date,
    totalCalories: toNumber(raw?.totalCalories),
    itemCount: toNumber(raw?.itemCount),
    topAdditives,
  };
}

export const useCartStore = defineStore("cartStore", {
  state: () => ({
    items: [],
    todayKey: todayDateString(),
    preferredMealType: "LUNCH",
    syncStatus: "LOCAL_DRAFT",
    isSyncing: false,
    syncError: null,
    lastSyncResult: null,
    history: [],
  }),
  getters: {
    totalItemCount: (state) =>
      state.items.reduce((sum, item) => sum + Number(item.quantity ?? 0), 0),
    totalCalories: (state) =>
      state.items.reduce(
        (sum, item) => sum + Number(item.calories ?? 0) * Number(item.quantity ?? 0),
        0
      ),
    syncStatusMeta: (state) => {
      const map = {
        LOCAL_DRAFT: {
          label: "로컬 초안",
          description: "브라우저에 임시 저장된 상태입니다.",
        },
        SYNCING: {
          label: "동기화 중",
          description: "서버에 식단을 저장하는 중입니다.",
        },
        COMMITTED: {
          label: "서버 저장 완료",
          description: "서버 기록까지 반영된 상태입니다.",
        },
        SYNC_FAILED: {
          label: "동기화 실패",
          description: "네트워크 또는 입력 오류입니다. 다시 시도해 주세요.",
        },
      };
      return map[state.syncStatus] ?? map.LOCAL_DRAFT;
    },
    weeklyRhythm: (state) => {
      const [year, month, day] = String(state.todayKey || todayDateString())
        .split("-")
        .map((value) => Number(value));
      const today = Number.isFinite(year) && Number.isFinite(month) && Number.isFinite(day)
        ? new Date(year, month - 1, day)
        : new Date();
      const todayKey = state.todayKey || todayDateString();
      const historyMap = new Map((state.history || []).map((day) => [day.date, day]));
      const cells = [];

      for (let offset = -14; offset <= 14; offset += 1) {
        const date = new Date(today);
        date.setDate(today.getDate() + offset);
        const key = `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, "0")}-${`${date.getDate()}`.padStart(2, "0")}`;
        const synced = historyMap.get(key);

        if (synced) {
          cells.push({
            date: key,
            offset,
            level: scoreFromCalories(synced.totalCalories),
            status: "SYNCED",
            totalCalories: toNumber(synced.totalCalories),
            itemCount: toNumber(synced.itemCount),
            topAdditives: synced.topAdditives || [],
          });
          continue;
        }

        if (key === todayKey && state.items.length > 0) {
          cells.push({
            date: key,
            offset,
            level: Math.max(1, scoreFromCalories(state.totalCalories) - 1),
            status: "DRAFT",
            totalCalories: state.totalCalories,
            itemCount: state.items.length,
            topAdditives: [],
          });
          continue;
        }

        cells.push({
          date: key,
          offset,
          level: 0,
          status: "NONE",
          totalCalories: 0,
          itemCount: 0,
          topAdditives: [],
        });
      }

      return cells;
    },
    topConsumedAdditives: (state) => {
      const [year, month, day] = String(state.todayKey || todayDateString())
        .split("-")
        .map((value) => Number(value));
      const today = Number.isFinite(year) && Number.isFinite(month) && Number.isFinite(day)
        ? new Date(year, month - 1, day)
        : new Date();
      const start = new Date(today);
      start.setDate(today.getDate() - 27);
      const startKey = `${start.getFullYear()}-${`${start.getMonth() + 1}`.padStart(2, "0")}-${`${start.getDate()}`.padStart(2, "0")}`;
      const endKey = state.todayKey || todayDateString();

      const score = new Map();
      for (const day of state.history || []) {
        if (!day?.date || day.date < startKey || day.date > endKey) continue;
        const dayWeight = Math.max(1, scoreFromCalories(day.totalCalories));
        for (const additive of day.topAdditives || []) {
          const normalized = normalizeWarningLabel(additive);
          score.set(normalized, (score.get(normalized) || 0) + dayWeight);
        }
      }

      return [...score.entries()]
        .sort((a, b) => b[1] - a[1])
        .map(([name]) => name)
        .slice(0, 4);
    },
    syncedDaysLast28: (state) => {
      const [year, month, day] = String(state.todayKey || todayDateString())
        .split("-")
        .map((value) => Number(value));
      const today = Number.isFinite(year) && Number.isFinite(month) && Number.isFinite(day)
        ? new Date(year, month - 1, day)
        : new Date();
      const start = new Date(today);
      start.setDate(today.getDate() - 27);
      const startKey = `${start.getFullYear()}-${`${start.getMonth() + 1}`.padStart(2, "0")}-${`${start.getDate()}`.padStart(2, "0")}`;
      const endKey = state.todayKey || todayDateString();
      return (state.history || []).filter(
        (day) => day.date >= startKey && day.date <= endKey && toNumber(day.totalCalories) > 0
      ).length;
    },
  },
  actions: {
    tickTodayKey() {
      const next = todayDateString();
      if (this.todayKey !== next) {
        this.todayKey = next;
      }
    },
    startDateTicker() {
      this.tickTodayKey();
      if (dateTickerId) return;
      dateTickerId = window.setInterval(() => {
        this.tickTodayKey();
      }, 30 * 1000);
    },
    stopDateTicker() {
      if (dateTickerId) {
        window.clearInterval(dateTickerId);
        dateTickerId = null;
      }
    },
    hydrate() {
      this.todayKey = todayDateString();
      const rawItems = localStorage.getItem(CART_STORAGE_KEY);
      if (rawItems) {
        try {
          const parsed = JSON.parse(rawItems);
          if (Array.isArray(parsed)) {
            this.items = parsed.map(normalizeItem).filter((item) => item.foodId > 0);
          }
        } catch {
          this.items = [];
        }
      }

      const rawMeta = localStorage.getItem(CART_META_KEY);
      if (rawMeta) {
        try {
          const parsed = JSON.parse(rawMeta);
          this.syncStatus = parsed.syncStatus ?? "LOCAL_DRAFT";
          this.lastSyncResult = parsed.lastSyncResult ?? null;
          this.syncError = parsed.syncError ?? null;
          this.preferredMealType = String(parsed.preferredMealType || "LUNCH").toUpperCase();
          this.history = Array.isArray(parsed.history)
            ? parsed.history.map(normalizeHistoryDay).filter(Boolean).slice(-365)
            : [];
        } catch {
          this.syncStatus = "LOCAL_DRAFT";
          this.lastSyncResult = null;
          this.syncError = null;
          this.preferredMealType = "LUNCH";
          this.history = [];
        }
      }
    },
    persistItems() {
      localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(this.items));
    },
    persistMeta() {
      localStorage.setItem(
        CART_META_KEY,
        JSON.stringify({
          syncStatus: this.syncStatus,
          syncError: this.syncError,
          lastSyncResult: this.lastSyncResult,
          preferredMealType: this.preferredMealType,
          history: this.history.slice(-365),
        })
      );
    },
    setPreferredMealType(next) {
      const candidate = String(next || "").toUpperCase();
      if (!["BREAKFAST", "LUNCH", "DINNER", "SNACK"].includes(candidate)) return;
      this.preferredMealType = candidate;
      this.persistMeta();
    },
    addFood(food) {
      const normalized = normalizeItem(food);
      const existing = this.items.find((item) => item.foodId === normalized.foodId);
      if (normalized.mealTypeHint) {
        this.preferredMealType = normalized.mealTypeHint;
      }
      if (existing) {
        existing.quantity = Number(existing.quantity) + Number(normalized.quantity || 1);
        existing.category = normalized.category || existing.category;
        if (normalized.mealTypeHint) {
          existing.mealTypeHint = normalized.mealTypeHint;
        }
        if (!existing.additiveIds?.length && normalized.additiveIds?.length) {
          existing.additiveIds = normalized.additiveIds;
        }
      } else {
        this.items.push(normalized);
      }
      this.syncStatus = "LOCAL_DRAFT";
      this.syncError = null;
      this.persistItems();
      this.persistMeta();
    },
    updateQuantity(foodId, quantity) {
      const nextQty = Number(quantity);
      const target = this.items.find((item) => item.foodId === foodId);
      if (!target) return;
      if (!Number.isFinite(nextQty) || nextQty <= 0) {
        this.remove(foodId);
        return;
      }
      target.quantity = nextQty;
      this.syncStatus = "LOCAL_DRAFT";
      this.syncError = null;
      this.persistItems();
      this.persistMeta();
    },
    remove(foodId) {
      this.items = this.items.filter((item) => item.foodId !== foodId);
      this.syncStatus = "LOCAL_DRAFT";
      this.syncError = null;
      this.persistItems();
      this.persistMeta();
    },
    clearItems() {
      this.items = [];
      localStorage.removeItem(CART_STORAGE_KEY);
      this.syncStatus = "LOCAL_DRAFT";
      this.syncError = null;
      this.persistMeta();
    },
    clearAll() {
      this.items = [];
      this.todayKey = todayDateString();
      this.preferredMealType = "LUNCH";
      this.syncStatus = "LOCAL_DRAFT";
      this.syncError = null;
      this.lastSyncResult = null;
      this.history = [];
      localStorage.removeItem(CART_STORAGE_KEY);
      localStorage.removeItem(CART_META_KEY);
    },
    incrementQuantity(foodId, step = 1) {
      const target = this.items.find((item) => item.foodId === foodId);
      if (!target) return;
      target.quantity = Math.max(0.1, Number(target.quantity || 0) + Number(step || 0));
      this.syncStatus = "LOCAL_DRAFT";
      this.syncError = null;
      this.persistItems();
      this.persistMeta();
    },
    decrementQuantity(foodId, step = 1) {
      const target = this.items.find((item) => item.foodId === foodId);
      if (!target) return;
      const next = Number(target.quantity || 0) - Number(step || 0);
      if (next <= 0) {
        this.remove(foodId);
        return;
      }
      target.quantity = Math.max(0.1, next);
      this.syncStatus = "LOCAL_DRAFT";
      this.syncError = null;
      this.persistItems();
      this.persistMeta();
    },
    async fetchTodayRemote(date = todayDateString()) {
      const response = await api.get("/meals/today", { params: { date } });
      return response.data?.data?.items ?? [];
    },
    async fetchHistory(from, to) {
      const response = await api.get("/meals/history", {
        params: {
          from,
          to,
        },
      });
      return response.data?.data?.days ?? [];
    },
    async fetchDayMeals(date = todayDateString()) {
      const response = await api.get("/meals/day", { params: { date } });
      return response.data?.data?.meals ?? [];
    },
    async updateMeal(mealId, payload) {
      const response = await api.patch(`/meals/${mealId}`, payload);
      const data = response.data?.data ?? null;
      this.lastSyncResult = data;
      this.syncStatus = "COMMITTED";
      this.syncError = null;
      this.persistMeta();
      return data;
    },
    mergeWithServerItems(serverItems) {
      const merged = new Map();
      for (const item of this.items) {
        merged.set(item.foodId, {
          ...item,
          quantity: Number(item.quantity),
        });
      }
      for (const remote of serverItems) {
        const remoteFoodId = toNumber(remote.foodId ?? remote.food_id);
        if (!remoteFoodId) continue;
        const remoteQuantity = toNumber(remote.quantity, 1);
        const remoteName = String(remote.name ?? remote.foodName ?? `식품 ID ${remoteFoodId}`);
        const remoteCategory = String(remote.category ?? "기타");
        const remoteCalories = toNumber(remote.calories ?? remote.kcal, 0);

        const existing = merged.get(remoteFoodId);
        if (existing) {
          existing.quantity = Number(existing.quantity) + remoteQuantity;
          if (!existing.name && remoteName) {
            existing.name = remoteName;
          }
          if ((!existing.category || existing.category === "기타") && remoteCategory) {
            existing.category = remoteCategory;
          }
          if (!Number(existing.calories) && Number.isFinite(remoteCalories)) {
            existing.calories = remoteCalories;
          }
        } else {
          merged.set(remoteFoodId, {
            foodId: remoteFoodId,
            name: remoteName,
            category: remoteCategory,
            mealTypeHint: this.preferredMealType,
            calories: remoteCalories,
            additiveIds: [],
            quantity: remoteQuantity,
          });
        }
      }
      this.items = [...merged.values()];
      this.syncStatus = "LOCAL_DRAFT";
      this.syncError = null;
      this.persistItems();
      this.persistMeta();
    },
    async syncToServer(mealType = "LUNCH", loggedDate = todayDateString()) {
      if (this.items.length === 0) {
        throw new Error("?λ컮援щ땲媛 鍮꾩뼱 ?덉뒿?덈떎.");
      }
      if (!navigator.onLine) {
        this.syncStatus = "SYNC_FAILED";
        this.syncError = "?ㅽ봽?쇱씤 ?곹깭?낅땲?? ?⑤씪?몄쑝濡??꾪솚 ???ㅼ떆 ?쒕룄?댁＜?몄슂.";
        this.persistMeta();
        throw new Error(this.syncError);
      }

      this.syncStatus = "SYNCING";
      this.syncError = null;
      this.isSyncing = true;
      this.persistMeta();

      try {
        const payload = {
          meal_type: mealType,
          logged_date: loggedDate,
          items: this.items.map((item) => ({
            food_id: item.foodId,
            quantity: Number(item.quantity),
          })),
        };

        const response = await api.post("/meals/sync", payload, {
          headers: {
            "Idempotency-Key": randomIdempotencyKey(),
          },
        });

        const data = response.data?.data ?? null;
        this.lastSyncResult = data;
        this.applyHistory([
          {
            date: loggedDate,
            totalCalories: toNumber(data?.totalCalories),
            itemCount: this.items.length,
            topAdditives: [],
          },
        ]);
        this.syncStatus = "COMMITTED";
        this.syncError = null;
        this.clearItems();
        this.persistMeta();
        return data;
      } catch (error) {
        this.syncStatus = "SYNC_FAILED";
        this.syncError =
          error?.response?.data?.message ??
          error?.message ??
          "?앸떒 ?숆린?붿뿉 ?ㅽ뙣?덉뒿?덈떎. ?좎떆 ???ㅼ떆 ?쒕룄?댁＜?몄슂.";
        this.persistMeta();
        throw error;
      } finally {
        this.isSyncing = false;
      }
    },
    applyHistory(days) {
      if (!Array.isArray(days)) return;
      const map = new Map((this.history || []).map((day) => [day.date, day]));
      for (const raw of days) {
        const normalized = normalizeHistoryDay(raw);
        if (!normalized) continue;
        map.set(normalized.date, normalized);
      }
      this.history = [...map.values()].sort((a, b) => a.date.localeCompare(b.date)).slice(-365);
      this.persistMeta();
    },
  },
});


