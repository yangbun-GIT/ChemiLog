import { defineStore } from "pinia";
import api from "../api/client";

const CART_STORAGE_KEY = "chemilog.customer.cart.v3";
const CART_META_KEY = "chemilog.customer.cart.meta.v1";

function todayDateString() {
  const now = new Date();
  const year = now.getFullYear();
  const month = `${now.getMonth() + 1}`.padStart(2, "0");
  const day = `${now.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function randomIdempotencyKey() {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function toNumber(value, fallback = 0) {
  const num = Number(value);
  return Number.isFinite(num) ? num : fallback;
}

function normalizeItem(raw) {
  return {
    foodId: toNumber(raw.foodId ?? raw.food_id),
    name: String(raw.name ?? "식품"),
    category: String(raw.category ?? "기타"),
    calories: toNumber(raw.calories),
    additiveIds: Array.isArray(raw.additiveIds ?? raw.additive_ids)
      ? (raw.additiveIds ?? raw.additive_ids).map((id) => toNumber(id)).filter((id) => id > 0)
      : [],
    quantity: Math.max(0.1, toNumber(raw.quantity, 1)),
  };
}

function scoreFromCalories(totalCalories) {
  const value = toNumber(totalCalories);
  if (value <= 0) return 0;
  if (value <= 550) return 4;
  if (value <= 800) return 3;
  if (value <= 1100) return 2;
  return 1;
}

export const useCartStore = defineStore("cartStore", {
  state: () => ({
    items: [],
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
          description: "브라우저에만 저장된 상태입니다. 기록 완료를 누르면 서버에 저장됩니다.",
          tone: "slate",
        },
        SYNCING: {
          label: "동기화 중",
          description: "서버에 식단을 저장하고 있습니다.",
          tone: "amber",
        },
        COMMITTED: {
          label: "서버 저장 완료",
          description: "서버 기록까지 완료된 상태입니다.",
          tone: "emerald",
        },
        SYNC_FAILED: {
          label: "동기화 실패",
          description: "네트워크 또는 인증 문제로 저장에 실패했습니다. 재시도해주세요.",
          tone: "rose",
        },
      };
      return map[state.syncStatus] ?? map.LOCAL_DRAFT;
    },
    weeklyRhythm: (state) => {
      const dayMap = new Map();
      for (const entry of state.history) {
        if (!entry?.date) continue;
        const previous = dayMap.get(entry.date);
        const nextScore = scoreFromCalories(entry.totalCalories);
        if (!previous || nextScore > previous.level) {
          dayMap.set(entry.date, {
            date: entry.date,
            level: nextScore,
            totalCalories: toNumber(entry.totalCalories),
            itemCount: toNumber(entry.itemCount),
          });
        }
      }

      const result = [];
      const today = new Date();
      for (let i = 27; i >= 0; i -= 1) {
        const date = new Date(today);
        date.setDate(today.getDate() - i);
        const key = `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, "0")}-${`${date.getDate()}`.padStart(2, "0")}`;
        const synced = dayMap.get(key);

        if (synced) {
          result.push({
            date: key,
            level: synced.level,
            status: "SYNCED",
            totalCalories: synced.totalCalories,
            itemCount: synced.itemCount,
          });
          continue;
        }

        if (key === todayDateString() && state.items.length > 0) {
          result.push({
            date: key,
            level: Math.max(1, scoreFromCalories(state.totalCalories) - 1),
            status: "DRAFT",
            totalCalories: state.totalCalories,
            itemCount: state.items.length,
          });
          continue;
        }

        result.push({ date: key, level: 0, status: "NONE", totalCalories: 0, itemCount: 0 });
      }
      return result;
    },
  },
  actions: {
    hydrate() {
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
          this.history = Array.isArray(parsed.history) ? parsed.history.slice(-180) : [];
          this.syncError = parsed.syncError ?? null;
        } catch {
          this.syncStatus = "LOCAL_DRAFT";
          this.lastSyncResult = null;
          this.history = [];
          this.syncError = null;
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
          history: this.history.slice(-180),
        })
      );
    },
    addFood(food) {
      const normalized = normalizeItem(food);
      const existing = this.items.find((item) => item.foodId === normalized.foodId);
      if (existing) {
        existing.quantity = Number(existing.quantity) + Number(normalized.quantity || 1);
        existing.category = normalized.category || existing.category;
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
    },
    clearAll() {
      this.items = [];
      this.syncStatus = "LOCAL_DRAFT";
      this.syncError = null;
      this.lastSyncResult = null;
      this.history = [];
      localStorage.removeItem(CART_STORAGE_KEY);
      localStorage.removeItem(CART_META_KEY);
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
        const existing = merged.get(remoteFoodId);
        if (existing) {
          existing.quantity = Number(existing.quantity) + remoteQuantity;
        } else {
          merged.set(remoteFoodId, {
            foodId: remoteFoodId,
            name: `Food #${remoteFoodId}`,
            category: "기타",
            calories: 0,
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
        throw new Error("장바구니가 비어 있습니다.");
      }
      if (!navigator.onLine) {
        this.syncStatus = "SYNC_FAILED";
        this.syncError = "오프라인 상태입니다. 온라인으로 전환 후 다시 시도해주세요.";
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
        this.history.push({
          date: loggedDate,
          totalCalories: toNumber(data?.totalCalories),
          itemCount: this.items.length,
          mealId: data?.mealId ?? null,
        });
        this.history = this.history.slice(-180);
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
          "식단 동기화에 실패했습니다. 잠시 후 다시 시도해주세요.";
        this.persistMeta();
        throw error;
      } finally {
        this.isSyncing = false;
      }
    },
    applyHistory(days) {
      if (!Array.isArray(days)) return;
      for (const day of days) {
        if (!day?.date) continue;
        this.history.push({
          date: day.date,
          totalCalories: toNumber(day.totalCalories),
          itemCount: toNumber(day.itemCount),
          mealId: null,
        });
      }
      this.history = this.history
        .reduce((acc, item) => {
          const existing = acc.find((v) => v.date === item.date);
          if (!existing) {
            acc.push(item);
            return acc;
          }
          if (toNumber(item.totalCalories) > toNumber(existing.totalCalories)) {
            existing.totalCalories = item.totalCalories;
            existing.itemCount = item.itemCount;
          }
          return acc;
        }, [])
        .sort((a, b) => a.date.localeCompare(b.date))
        .slice(-180);
      this.persistMeta();
    },
  },
});
