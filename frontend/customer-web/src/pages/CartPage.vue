<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../stores/authStore";
import { useCartStore } from "../stores/cartStore";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const cartStore = useCartStore();

const VALID_MEAL_TYPES = ["BREAKFAST", "LUNCH", "DINNER", "SNACK"];

function normalizeMealType(value, fallback = "LUNCH") {
  const next = String(value || "").toUpperCase();
  return VALID_MEAL_TYPES.includes(next) ? next : fallback;
}

function normalizeDate(value, fallback) {
  const text = String(value || "");
  return /^\d{4}-\d{2}-\d{2}$/u.test(text) ? text : fallback;
}

const mealType = ref(normalizeMealType(route.query.mealType || cartStore.preferredMealType, "LUNCH"));
const loggedDate = ref(normalizeDate(route.query.date, cartStore.todayKey));
const followToday = ref(true);

const online = ref(typeof navigator !== "undefined" ? navigator.onLine : true);
const errorMessage = ref("");
const successMessage = ref("");
const mergeLoading = ref(false);
const remoteLoading = ref(false);

const remoteMeals = ref([]);
const editingMealId = ref(null);
const editingMealType = ref("LUNCH");
const editingItems = ref([]);

const syncMeta = computed(() => cartStore.syncStatusMeta);
const totalCalories = computed(() => Number(cartStore.totalCalories || 0).toFixed(1));
const unsyncedDraftDays = computed(() => cartStore.weeklyRhythm.filter((day) => day.status === "DRAFT").length);
const lastSync = computed(() => cartStore.lastSyncResult);

function mealTypeLabel(type) {
  const map = {
    BREAKFAST: "아침",
    LUNCH: "점심",
    DINNER: "저녁",
    SNACK: "간식",
  };
  return map[type] ?? type;
}

function handleOnline() {
  online.value = true;
}

function handleOffline() {
  online.value = false;
}

async function loadRemoteMeals() {
  if (!authStore.isAuthenticated) {
    remoteMeals.value = [];
    return;
  }

  remoteLoading.value = true;
  try {
    const rows = await cartStore.fetchDayMeals(loggedDate.value);
    remoteMeals.value = (rows || []).filter((meal) => (meal.items || []).length > 0);
  } catch {
    remoteMeals.value = [];
  } finally {
    remoteLoading.value = false;
  }
}

async function mergeWithDayRecord() {
  errorMessage.value = "";
  successMessage.value = "";

  if (!authStore.isAuthenticated) {
    errorMessage.value = "서버 기록과 병합하려면 먼저 로그인해 주세요.";
    return;
  }

  mergeLoading.value = true;
  try {
    const serverMeals = await cartStore.fetchDayMeals(loggedDate.value);
    const serverItems = (serverMeals || [])
      .flatMap((meal) => meal.items || [])
      .map((item) => ({
        foodId: item.foodId,
        quantity: Number(item.quantity || 0),
        name: item.foodName || `식품 ID ${item.foodId}`,
        category: item.category || "기타",
        calories: Number(item.calories || 0),
      }))
      .filter((item) => item.foodId && item.quantity > 0);

    if (!serverItems.length) {
      successMessage.value = "해당 날짜 서버 식단 기록이 없어 병합할 항목이 없습니다.";
      return;
    }
    cartStore.mergeWithServerItems(serverItems);
    successMessage.value = "서버 기록 식단 항목을 현재 로컬 초안에 합쳤습니다.";
    await loadRemoteMeals();
  } catch (error) {
    errorMessage.value = error?.response?.data?.message ?? "병합 중 오류가 발생했습니다.";
  } finally {
    mergeLoading.value = false;
  }
}

async function sync() {
  errorMessage.value = "";
  successMessage.value = "";

  if (!authStore.isAuthenticated) {
    errorMessage.value = "식단 동기화는 로그인 후 사용할 수 있습니다.";
    router.push("/login");
    return;
  }

  try {
    const result = await cartStore.syncToServer(mealType.value, loggedDate.value);
    if (result) {
      successMessage.value = `${loggedDate.value} ${mealTypeLabel(mealType.value)} 동기화 완료 · 총 ${Number(result.totalCalories).toFixed(1)} kcal`;
      await loadRemoteMeals();
      const dayHistory = await cartStore.fetchHistory(loggedDate.value, loggedDate.value);
      cartStore.applyHistory(dayHistory);
    }
  } catch (error) {
    errorMessage.value =
      cartStore.syncError ||
      error?.response?.data?.message ||
      error?.message ||
      "동기화 중 오류가 발생했습니다.";
  }
}

function startMealEdit(meal) {
  editingMealId.value = meal.mealId;
  editingMealType.value = meal.mealType;
  editingItems.value = (meal.items || []).map((item) => ({
    foodId: item.foodId,
    foodName: item.foodName || `Food #${item.foodId}`,
    category: item.category || "기타",
    quantity: Number(item.quantity || 1),
  }));
}

function cancelMealEdit() {
  editingMealId.value = null;
  editingItems.value = [];
}

function updateEditQuantity(foodId, value) {
  const next = Number(value);
  const target = editingItems.value.find((item) => item.foodId === foodId);
  if (!target) return;
  target.quantity = Number.isFinite(next) ? next : target.quantity;
}

function incrementEditItem(foodId) {
  const target = editingItems.value.find((item) => item.foodId === foodId);
  if (!target) return;
  target.quantity = Number(target.quantity || 0) + 1;
}

function decrementEditItem(foodId) {
  const target = editingItems.value.find((item) => item.foodId === foodId);
  if (!target) return;
  target.quantity = Math.max(0, Number(target.quantity || 0) - 1);
}

function removeEditItem(foodId) {
  editingItems.value = editingItems.value.filter((item) => item.foodId !== foodId);
}

function mergeLocalDraftIntoEdit() {
  for (const item of cartStore.items) {
    const found = editingItems.value.find((v) => v.foodId === item.foodId);
    if (found) {
      found.quantity = Number(found.quantity) + Number(item.quantity || 0);
    } else {
      editingItems.value.push({
        foodId: item.foodId,
        foodName: item.name,
        category: item.category || "기타",
        quantity: Number(item.quantity || 1),
      });
    }
  }
}

async function saveMealEdit() {
  if (!editingMealId.value) return;

  const items = editingItems.value
    .filter((item) => Number(item.quantity) > 0)
    .map((item) => ({ food_id: item.foodId, quantity: Number(item.quantity) }));

  try {
    await cartStore.updateMeal(editingMealId.value, {
      meal_type: editingMealType.value,
      logged_date: loggedDate.value,
      items,
    });

    successMessage.value = `${loggedDate.value} ${mealTypeLabel(editingMealType.value)} 식단 수정 완료`;
    cancelMealEdit();
    await loadRemoteMeals();
    const dayHistory = await cartStore.fetchHistory(loggedDate.value, loggedDate.value);
    cartStore.applyHistory(dayHistory);
  } catch (error) {
    errorMessage.value = error?.response?.data?.message ?? "식단 수정 중 오류가 발생했습니다.";
  }
}

async function clearMeal(meal) {
  errorMessage.value = "";
  successMessage.value = "";

  try {
    await cartStore.updateMeal(meal.mealId, {
      meal_type: meal.mealType,
      logged_date: loggedDate.value,
      items: [],
    });
    successMessage.value = `${loggedDate.value} ${mealTypeLabel(meal.mealType)} 식단을 삭제했습니다.`;
    await loadRemoteMeals();
    const dayHistory = await cartStore.fetchHistory(loggedDate.value, loggedDate.value);
    cartStore.applyHistory(dayHistory);
  } catch (error) {
    errorMessage.value = error?.response?.data?.message ?? "식단 삭제에 실패했습니다.";
  }
}

watch(loggedDate, async () => {
  followToday.value = loggedDate.value === cartStore.todayKey;
  cancelMealEdit();
  await loadRemoteMeals();
});

watch(mealType, (next) => {
  cartStore.setPreferredMealType(next);
});

watch(
  () => route.query,
  async (query) => {
    const nextMealType = normalizeMealType(query.mealType, mealType.value);
    const nextDate = normalizeDate(query.date, loggedDate.value);
    const changed = nextMealType !== mealType.value || nextDate !== loggedDate.value;
    mealType.value = nextMealType;
    loggedDate.value = nextDate;
    if (changed) {
      cancelMealEdit();
      await loadRemoteMeals();
    }
  },
  { deep: true }
);

watch(
  () => cartStore.todayKey,
  async (next, prev) => {
    if (followToday.value || loggedDate.value === prev) {
      loggedDate.value = next;
      await loadRemoteMeals();
    }
  }
);

onMounted(async () => {
  window.addEventListener("online", handleOnline);
  window.addEventListener("offline", handleOffline);
  await loadRemoteMeals();
});

onUnmounted(() => {
  window.removeEventListener("online", handleOnline);
  window.removeEventListener("offline", handleOffline);
});
</script>

<template>
  <section class="space-y-4">
    <article class="bento-card">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p class="eyebrow">Meal Sync</p>
          <h2 class="title-lg">식단 동기화 / 수정</h2>
        </div>
        <RouterLink to="/" class="btn-ghost">대시보드로 이동</RouterLink>
      </div>

      <div class="mt-4 rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
        <p class="font-semibold text-slate-900">사용 가이드</p>
        <p class="mt-2"><strong>1)</strong> 로컬 초안에서 음식 수량을 맞춥니다.</p>
        <p class="mt-2"><strong>2)</strong> 식사 타입과 날짜를 선택합니다.</p>
        <p class="mt-2"><strong>3)</strong> [식단 동기화]를 눌러 서버에 저장합니다.</p>
        <p class="mt-2 text-xs text-slate-600">
          서버 기록 병합: 선택 날짜의 서버 식단 항목을 로컬 초안에 가져와 함께 편집할 때 사용합니다.
        </p>
      </div>

      <div class="mt-4 grid gap-2 sm:grid-cols-4">
        <div class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-3">
          <p class="text-xs text-slate-500">동기화 상태</p>
          <p class="mt-1 text-sm font-semibold text-slate-900">{{ syncMeta.label }}</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-3">
          <p class="text-xs text-slate-500">총 수량</p>
          <p class="mt-1 text-sm font-semibold text-slate-900">{{ cartStore.totalItemCount.toFixed(1) }}</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-3">
          <p class="text-xs text-slate-500">추정 칼로리</p>
          <p class="mt-1 text-sm font-semibold text-slate-900">{{ totalCalories }} kcal</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-3">
          <p class="text-xs text-slate-500">미동기화 초안 일수</p>
          <p class="mt-1 text-sm font-semibold text-slate-900">{{ unsyncedDraftDays }}</p>
        </div>
      </div>

      <div class="mt-3 rounded-xl border border-slate-200 bg-white p-3 text-sm text-slate-600">
        네트워크:
        <strong class="text-slate-900">{{ online ? "온라인" : "오프라인" }}</strong>
        · {{ syncMeta.description }}
      </div>

      <div class="mt-3 grid gap-3 md:grid-cols-4">
        <label>
          <span class="field-label">식사 타입</span>
          <select v-model="mealType" class="field-input">
            <option value="BREAKFAST">아침</option>
            <option value="LUNCH">점심</option>
            <option value="DINNER">저녁</option>
            <option value="SNACK">간식</option>
          </select>
        </label>

        <label>
          <span class="field-label">기록 날짜</span>
          <input v-model="loggedDate" type="date" class="field-input" />
        </label>

        <div class="md:col-span-2 flex items-end gap-2">
          <button class="btn-ghost" :disabled="mergeLoading || cartStore.isSyncing" @click="mergeWithDayRecord">
            {{ mergeLoading ? "병합 중..." : "서버 기록 병합" }}
          </button>
          <button class="btn-primary" :disabled="cartStore.isSyncing || cartStore.items.length === 0" @click="sync">
            {{ cartStore.isSyncing ? "동기화 중..." : "식단 동기화" }}
          </button>
        </div>
      </div>

      <p v-if="successMessage" class="mt-3 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
        {{ successMessage }}
      </p>
      <p v-if="errorMessage" class="mt-3 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
        {{ errorMessage }}
      </p>
      <p v-if="lastSync" class="mt-3 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-700">
        최근 동기화 결과: 총 {{ Number(lastSync.totalCalories || 0).toFixed(1) }} kcal
      </p>
    </article>

    <div class="grid items-start gap-4 lg:grid-cols-2">
      <article class="bento-card">
        <div class="mb-3 flex items-center justify-between">
          <p class="eyebrow">Local Draft</p>
          <button class="btn-ghost" @click="cartStore.clearItems">로컬 초안 비우기</button>
        </div>

        <div v-if="cartStore.items.length === 0" class="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
          아직 장바구니가 비어 있습니다. 대시보드에서 식품을 담아주세요.
        </div>

        <div v-else class="space-y-2">
          <article
            v-for="item in cartStore.items"
            :key="item.foodId"
            class="rounded-xl border border-slate-200 bg-white p-3"
          >
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p class="font-semibold text-slate-900">{{ item.name }}</p>
                <p class="text-xs text-slate-500">{{ item.category || "기타" }} · food_id {{ item.foodId }}</p>
              </div>

              <div class="flex items-center gap-2">
                <button class="btn-ghost px-2 py-1" @click="cartStore.decrementQuantity(item.foodId, 1)">-</button>
                <input
                  :value="item.quantity"
                  type="number"
                  min="0.1"
                  step="0.1"
                  class="field-input w-24 text-right"
                  @input="cartStore.updateQuantity(item.foodId, $event.target.value)"
                />
                <button class="btn-ghost px-2 py-1" @click="cartStore.incrementQuantity(item.foodId, 1)">+</button>
                <button class="btn-ghost" @click="cartStore.remove(item.foodId)">제거</button>
              </div>
            </div>
          </article>
        </div>
      </article>

      <article class="bento-card">
        <div class="mb-3 flex items-center justify-between">
          <p class="eyebrow">Server Meals</p>
          <button class="btn-ghost" :disabled="remoteLoading" @click="loadRemoteMeals">
            {{ remoteLoading ? "조회 중..." : "식단 조회" }}
          </button>
        </div>

        <div v-if="!authStore.isAuthenticated" class="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
          서버 식단 수정 기능은 로그인 후 사용할 수 있습니다.
        </div>

        <div v-else-if="remoteLoading" class="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
          서버 식단을 조회하고 있습니다...
        </div>

        <div v-else-if="remoteMeals.length === 0" class="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
          {{ loggedDate }} 날짜의 서버 식단이 없습니다.
        </div>

        <div v-else class="space-y-3">
          <article
            v-for="meal in remoteMeals"
            :key="meal.mealId"
            class="rounded-xl border border-slate-200 bg-white p-3"
          >
            <div class="mb-2 flex items-center justify-between gap-2">
              <div>
                <p class="text-sm font-semibold text-slate-900">{{ loggedDate }} · {{ mealTypeLabel(meal.mealType) }}</p>
                <p class="text-xs text-slate-500">총 {{ Number(meal.totalCalories || 0).toFixed(1) }} kcal</p>
              </div>
              <div class="flex items-center gap-2">
                <button class="btn-ghost" @click="startMealEdit(meal)">수정</button>
                <button class="btn-ghost" @click="clearMeal(meal)">삭제</button>
              </div>
            </div>

            <div class="space-y-1">
              <p v-for="item in meal.items" :key="`${meal.mealId}-${item.detailId || item.foodId}`" class="text-xs text-slate-600">
                {{ item.foodName || `Food #${item.foodId}` }} · {{ Number(item.quantity || 0).toFixed(1) }}개
              </p>
            </div>
          </article>
        </div>

        <article v-if="editingMealId" class="mt-3 rounded-xl border border-cyan-200 bg-cyan-50 p-3">
          <div class="mb-2 flex items-center justify-between">
            <p class="text-sm font-semibold text-slate-900">{{ loggedDate }} 식단 수정</p>
            <button class="btn-ghost" @click="cancelMealEdit">닫기</button>
          </div>

          <label class="mb-2 block">
            <span class="field-label">식사 타입</span>
            <select v-model="editingMealType" class="field-input">
              <option value="BREAKFAST">아침</option>
              <option value="LUNCH">점심</option>
              <option value="DINNER">저녁</option>
              <option value="SNACK">간식</option>
            </select>
          </label>

          <div class="space-y-2">
            <div
              v-for="item in editingItems"
              :key="`edit-${item.foodId}`"
              class="rounded-lg border border-slate-200 bg-white p-2"
            >
              <div class="flex items-center justify-between gap-2">
                <div>
                  <p class="text-xs font-semibold text-slate-900">{{ item.foodName }}</p>
                  <p class="text-[11px] text-slate-500">food_id {{ item.foodId }} · {{ item.category }}</p>
                </div>
                <div class="flex items-center gap-2">
                  <button class="btn-ghost px-2 py-1" @click="decrementEditItem(item.foodId)">-</button>
                  <input
                    :value="item.quantity"
                    type="number"
                    min="0"
                    step="0.1"
                    class="field-input w-24 text-right"
                    @input="updateEditQuantity(item.foodId, $event.target.value)"
                  />
                  <button class="btn-ghost px-2 py-1" @click="incrementEditItem(item.foodId)">+</button>
                  <button class="btn-ghost" @click="removeEditItem(item.foodId)">제거</button>
                </div>
              </div>
            </div>
            <p v-if="editingItems.length === 0" class="text-xs text-slate-600">
              현재 식단 항목이 비어 있습니다. 저장하면 해당 식사는 삭제됩니다.
            </p>
          </div>

          <div class="mt-3 flex flex-wrap gap-2">
            <button class="btn-ghost" @click="mergeLocalDraftIntoEdit">로컬 초안 합치기</button>
            <button class="btn-primary" @click="saveMealEdit">수정 저장</button>
          </div>
        </article>
      </article>
    </div>
  </section>
</template>
