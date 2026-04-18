<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import api from "../api/client";
import { useAuthStore } from "../stores/authStore";
import { useCartStore } from "../stores/cartStore";
import { useProfileStore } from "../stores/profileStore";
import { resolveFoodImageUrl } from "../utils/foodImageMap";

const DEFAULT_LIMIT = 6;
const EXPANDED_LIMIT = 60;

const authStore = useAuthStore();
const cartStore = useCartStore();
const profileStore = useProfileStore();

const keyword = ref("");
const selectedCategory = ref("전체");
const categories = ref([{ category: "전체", itemCount: 0 }]);
const foods = ref([]);
const loading = ref(false);
const errorMessage = ref("");
const guideOpen = ref(false);
const expandedMap = ref({});

const todayMeals = ref([]);
const todayMealsLoading = ref(false);

const recommendation = ref(null);
const recommendationLoading = ref(false);
const recommendationError = ref("");
const recommendationSeed = ref(String(Date.now()));
const mealTypeOptions = ["BREAKFAST", "LUNCH", "DINNER", "SNACK"];
const selectedSummaryMealType = ref(inferDefaultMealType());

let searchTimer = null;

const weeklyRhythm = computed(() => cartStore.weeklyRhythm);
const weeklyRhythmWeek = computed(() => {
  const list = weeklyRhythm.value || [];
  const center = list.findIndex((day) => day.offset === 0);
  if (center < 0) return list.slice(0, 7);
  let start = Math.max(0, center - 3);
  let end = Math.min(list.length, start + 7);
  if (end - start < 7) start = Math.max(0, end - 7);
  return list.slice(start, end);
});

const unsyncedDraftDays = computed(() => weeklyRhythm.value.filter((day) => day.status === "DRAFT").length);
const topConsumedAdditives = computed(() => cartStore.topConsumedAdditives);
const todayDate = computed(() => cartStore.todayKey);
const mealTypes = ["BREAKFAST", "LUNCH", "DINNER"];

const recommendationByMealType = computed(() => {
  const rows = recommendation.value?.meals || [];
  const map = new Map();
  for (const row of rows) {
    if (row?.mealType) {
      map.set(row.mealType, row);
    }
  }
  return map;
});

const dailyMealSlots = computed(() => {
  const grouped = new Map();
  for (const meal of todayMeals.value || []) {
    const type = meal?.mealType;
    if (!type) continue;
    if (!grouped.has(type)) {
      grouped.set(type, { items: [], totalCalories: 0 });
    }
    const bucket = grouped.get(type);
    bucket.totalCalories += Number(meal?.totalCalories || 0);
    for (const item of meal?.items || []) {
      const found = bucket.items.find((row) => row.foodId === item.foodId);
      if (found) {
        found.quantity += Number(item.quantity || 0);
      } else {
        bucket.items.push({
          foodId: item.foodId,
          name: item.foodName || `Food #${item.foodId}`,
          quantity: Number(item.quantity || 0),
        });
      }
    }
  }

  return mealTypes.map((type) => {
    const fromServer = grouped.get(type) || { items: [], totalCalories: 0 };
    const recommendationRow = recommendationByMealType.value.get(type);
    const hasServer = fromServer.items.length > 0;
    const fallbackItems = recommendationRow?.food
      ? [{ name: `${recommendationRow.food.name} (추천)`, quantity: 1 }]
      : [];
    return {
      type,
      label: mealTypeLabel(type),
      status: hasServer ? "기록됨" : fallbackItems.length ? "예정(추천)" : "미기록",
      totalCalories: fromServer.totalCalories,
      items: hasServer ? fromServer.items : fallbackItems,
    };
  });
});

const snackBundle = computed(() => {
  const snacks = [];
  let totalCalories = 0;

  for (const meal of todayMeals.value || []) {
    if (meal?.mealType !== "SNACK") continue;
    totalCalories += Number(meal?.totalCalories || 0);
    for (const item of meal?.items || []) {
      const found = snacks.find((row) => row.foodId === item.foodId);
      if (found) {
        found.quantity += Number(item.quantity || 0);
      } else {
        snacks.push({
          foodId: item.foodId,
          name: item.foodName || `Food #${item.foodId}`,
          quantity: Number(item.quantity || 0),
        });
      }
    }
  }

  if (snacks.length === 0) {
    const recommendationRow = recommendationByMealType.value.get("SNACK");
    if (recommendationRow?.food) {
      snacks.push({ name: `${recommendationRow.food.name} (추천)`, quantity: 1 });
      totalCalories = Number(recommendationRow.food.calories || 0);
    }
  }

  return {
    status: snacks.length ? "간식 번들" : "간식 미기록",
    totalCalories,
    items: snacks,
  };
});

const draftPlannedFoods = computed(() =>
  (cartStore.items || []).map((item) => ({
    foodId: item.foodId,
    name: item.name,
    quantity: Number(item.quantity || 0),
  }))
);

const totalCategoryCount = computed(() =>
  categories.value.filter((item) => item.category !== "전체").reduce((sum, item) => sum + Number(item.itemCount || 0), 0)
);

const selectedCategoryCount = computed(() => {
  if (selectedCategory.value === "전체") return totalCategoryCount.value;
  const found = categories.value.find((item) => item.category === selectedCategory.value);
  return Number(found?.itemCount || 0);
});

const currentExpansionKey = computed(() => selectedCategory.value);
const isExpanded = computed(() => Boolean(expandedMap.value[currentExpansionKey.value]));
const showMoreButton = computed(
  () => !keyword.value.trim() && !isExpanded.value && selectedCategoryCount.value > DEFAULT_LIMIT
);
const showCollapseButton = computed(
  () => !keyword.value.trim() && isExpanded.value && selectedCategoryCount.value > DEFAULT_LIMIT
);
const isBarMode = computed(() => isExpanded.value);
const resultTitle = computed(() =>
  selectedCategory.value === "전체" ? "전체 Popular 6" : `${selectedCategory.value} Popular 6`
);
const recommendedCalories = computed(() =>
  (recommendation.value?.meals || []).reduce((sum, row) => sum + Number(row?.food?.calories || 0), 0)
);
const recommendationSignature = computed(() =>
  (recommendation.value?.meals || []).map((row) => row?.food?.foodId || "-").join("|")
);
const syncLinkTo = computed(() => ({
  path: "/cart",
  query: {
    mealType: selectedSummaryMealType.value,
    date: todayDate.value,
  },
}));
const selectedMealPanel = computed(() => {
  if (selectedSummaryMealType.value === "SNACK") {
    return {
      type: "SNACK",
      label: "간식",
      status: snackBundle.value.status,
      totalCalories: snackBundle.value.totalCalories,
      items: snackBundle.value.items || [],
    };
  }
  const slot = dailyMealSlots.value.find((item) => item.type === selectedSummaryMealType.value);
  if (slot) return slot;
  return {
    type: selectedSummaryMealType.value,
    label: mealTypeLabel(selectedSummaryMealType.value),
    status: "미기록",
    totalCalories: 0,
    items: [],
  };
});
const orderedRecommendationMeals = computed(() => {
  const rows = recommendation.value?.meals || [];
  return [...rows].sort((a, b) => {
    if (a?.mealType === selectedSummaryMealType.value) return -1;
    if (b?.mealType === selectedSummaryMealType.value) return 1;
    return 0;
  });
});

function inferDefaultMealType() {
  const hour = new Date().getHours();
  if (hour < 10) return "BREAKFAST";
  if (hour < 15) return "LUNCH";
  if (hour < 21) return "DINNER";
  return "SNACK";
}

function normalizedWarnings(labels) {
  return [
    ...new Set(
      (labels || [])
        .map((label) => String(label || "").replace(/^주의:\s*/u, "").trim())
        .filter(Boolean)
    ),
  ];
}

function compactWarning(labels) {
  const unique = normalizedWarnings(labels);
  if (!unique.length) return "";
  if (unique.length === 1) return unique[0];
  if (unique.length === 2) return `${unique[0]} · ${unique[1]}`;
  return `${unique[0]} +${unique.length - 1}`;
}

function formatCalories(value) {
  const num = Number(value || 0);
  return Number.isFinite(num) ? `${num.toFixed(0)} kcal` : "-";
}

function formatQuantity(value) {
  const num = Number(value || 0);
  if (!Number.isFinite(num)) return "1";
  return Number.isInteger(num) ? String(num) : num.toFixed(1);
}

function normalizePercent(calories) {
  const value = Number(calories || 0);
  return Math.max(8, Math.min(100, Math.round((value / 900) * 100)));
}

function mealTypeLabel(type) {
  const map = {
    BREAKFAST: "아침",
    LUNCH: "점심",
    DINNER: "저녁",
    SNACK: "간식",
  };
  return map[type] ?? type;
}

function addFoodToCart(food) {
  cartStore.addFood({
    foodId: food.foodId,
    name: food.name,
    category: food.category || "기타",
    calories: Number(food.calories || 0),
    additiveIds: food.additiveIds || [],
    quantity: 1,
  });
}

function increaseCartQuantity(foodId) {
  cartStore.incrementQuantity(foodId, 1);
}

function decreaseCartQuantity(foodId) {
  cartStore.decrementQuantity(foodId, 1);
}

function applyKeyword(nextKeyword) {
  keyword.value = nextKeyword;
}

function setCategory(category) {
  selectedCategory.value = category;
}

function expandCurrentCategory() {
  expandedMap.value = {
    ...expandedMap.value,
    [currentExpansionKey.value]: true,
  };
  loadFoods();
}

function collapseCurrentCategory() {
  expandedMap.value = {
    ...expandedMap.value,
    [currentExpansionKey.value]: false,
  };
  loadFoods();
}

async function loadCategories() {
  try {
    const response = await api.get("/foods/categories");
    const items = response.data?.data ?? [];
    const total = items.reduce((sum, row) => sum + Number(row.itemCount || 0), 0);
    categories.value = [{ category: "전체", itemCount: total }, ...items];
  } catch {
    categories.value = [{ category: "전체", itemCount: 0 }];
  }
}

async function loadFoods() {
  loading.value = true;
  errorMessage.value = "";

  try {
    const hasKeyword = keyword.value.trim().length > 0;
    if (!hasKeyword && !isExpanded.value) {
      const categoryParam = selectedCategory.value === "전체" ? undefined : selectedCategory.value;
      const response = await api.get("/foods/popular", {
        params: {
          category: categoryParam,
          limit: DEFAULT_LIMIT,
        },
      });
      foods.value = response.data?.data ?? [];
      return;
    }

    const response = await api.get("/foods/search", {
      params: {
        keyword: hasKeyword ? keyword.value.trim() : undefined,
        category: selectedCategory.value === "전체" ? undefined : selectedCategory.value,
        page: 0,
        size: hasKeyword ? EXPANDED_LIMIT : isExpanded.value ? EXPANDED_LIMIT : DEFAULT_LIMIT,
      },
    });

    foods.value = response.data?.data?.items ?? [];
  } catch (error) {
    foods.value = [];
    errorMessage.value =
      error?.response?.data?.message ?? "식품 목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.";
  } finally {
    loading.value = false;
  }
}

async function loadTodayMeals() {
  if (!authStore.isAuthenticated) {
    todayMeals.value = [];
    return;
  }

  todayMealsLoading.value = true;
  try {
    todayMeals.value = await cartStore.fetchDayMeals(todayDate.value);
  } catch {
    todayMeals.value = [];
  } finally {
    todayMealsLoading.value = false;
  }
}

async function refreshTodaySummary() {
  cartStore.clearItems();
  await loadTodayMeals();
}

async function loadRecommendation() {
  recommendationLoading.value = true;
  recommendationError.value = "";
  try {
    const response = await api.get("/foods/recommendation", {
      params: {
        goal: profileStore.goal,
        strictness: profileStore.strictness,
        seed: recommendationSeed.value,
      },
    });
    recommendation.value = response.data?.data ?? null;
  } catch (error) {
    recommendation.value = null;
    recommendationError.value = error?.response?.data?.message ?? "추천 식단을 불러오지 못했습니다.";
  } finally {
    recommendationLoading.value = false;
  }
}

async function rerollRecommendation() {
  const before = recommendationSignature.value;
  for (let i = 0; i < 3; i += 1) {
    recommendationSeed.value = String(Date.now() + Math.floor(Math.random() * 100000) + i);
    await loadRecommendation();
    if (recommendationSignature.value && recommendationSignature.value !== before) {
      break;
    }
  }
}

function addRecommendedFood(recommendationItem) {
  if (!recommendationItem?.food) return;
  addFoodToCart(recommendationItem.food);
}

function shortDate(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return `${date.getMonth() + 1}/${date.getDate()}`;
}

function weekLabel(day) {
  if (!day) return "";
  return shortDate(day.date);
}

watch([keyword, selectedCategory], () => {
  if (searchTimer) clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    loadFoods();
  }, 300);
});

watch(
  () => [profileStore.goal, profileStore.strictness],
  () => {
    loadRecommendation();
  }
);

watch(todayDate, () => {
  loadTodayMeals();
});

onMounted(async () => {
  await loadCategories();
  await Promise.all([loadFoods(), loadTodayMeals(), loadRecommendation()]);
});
</script>

<template>
  <section class="space-y-4">
    <article id="daily-meal-plan" class="bento-card">
      <p class="eyebrow">Personal Context</p>
      <div class="mt-2 grid gap-3 md:grid-cols-[1.2fr_1fr]">
        <div class="rounded-xl border border-slate-200 bg-slate-50 p-3">
          <div class="flex flex-wrap items-center gap-2">
            <span class="pill">현재 목표: {{ profileStore.goalLabel }}</span>
            <span class="pill">강도: {{ profileStore.strictnessLabel }}</span>
            <span class="pill">알레르기: {{ profileStore.allergies.length ? profileStore.allergies.join(", ") : "없음" }}</span>
          </div>
          <RouterLink to="/mypage" class="btn-ghost mt-3 inline-flex">마이페이지에서 목표/강도 수정</RouterLink>
        </div>
        <div class="rounded-xl border border-slate-200 bg-white p-3">
          <p class="text-xs font-semibold text-slate-500">최근 4주 상위 주의 첨가물</p>
          <div class="mt-2 flex flex-wrap gap-2">
            <span
              v-for="additive in topConsumedAdditives"
              :key="additive"
              class="rounded bg-rose-50 px-2 py-1 text-xs font-semibold text-rose-700"
            >
              {{ additive }}
            </span>
            <span v-if="!topConsumedAdditives.length" class="text-xs text-slate-500">데이터가 아직 없습니다.</span>
          </div>
        </div>
      </div>
    </article>

    <article class="bento-card">
      <div class="flex items-center justify-between gap-2">
        <div>
          <p class="eyebrow">Daily Meal Plan</p>
          <h3 class="mt-1 text-xl font-semibold text-slate-900">오늘 식단 확인</h3>
        </div>
        <span class="pill">{{ shortDate(todayDate) }}</span>
      </div>

      <div v-if="todayMealsLoading" class="mt-3 grid gap-3 md:grid-cols-3">
        <div v-for="n in 3" :key="`meal-slot-skeleton-${n}`" class="skeleton h-28 rounded-xl" />
      </div>

      <div v-else class="mt-3 space-y-3">
        <div class="meal-slot-grid">
          <article
            v-for="slot in dailyMealSlots"
            :key="slot.type"
            class="meal-slot-card"
            :class="{ 'is-selected': selectedSummaryMealType === slot.type }"
          >
            <div class="flex items-center justify-between gap-2">
              <p class="text-sm font-semibold text-slate-900">{{ slot.label }}</p>
              <span class="pill">{{ slot.status }}</span>
            </div>
            <p class="mt-1 text-xs text-slate-500">{{ formatCalories(slot.totalCalories) }}</p>
            <div v-if="slot.items.length" class="mt-2 space-y-1">
              <p v-for="(item, index) in slot.items.slice(0, 3)" :key="`${slot.type}-${index}`" class="text-xs text-slate-700">
                {{ item.name }} {{ formatQuantity(item.quantity) }}개
              </p>
            </div>
            <p v-else class="mt-2 text-xs text-slate-500">아직 기록된 식단이 없습니다.</p>
          </article>
        </div>

        <article class="snack-bundle-card" :class="{ 'is-selected': selectedSummaryMealType === 'SNACK' }">
          <div class="flex items-center justify-between gap-2">
            <div>
              <p class="text-sm font-semibold text-slate-900">간식 번들</p>
              <p class="text-xs text-slate-500">{{ snackBundle.status }} · {{ formatCalories(snackBundle.totalCalories) }}</p>
            </div>
            <span class="pill">Snack</span>
          </div>
          <div v-if="snackBundle.items.length" class="mt-2 flex flex-wrap gap-2">
            <span
              v-for="(item, index) in snackBundle.items.slice(0, 6)"
              :key="`snack-bundle-${index}`"
              class="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs text-slate-700"
            >
              {{ item.name }} {{ formatQuantity(item.quantity) }}개
            </span>
          </div>
          <p v-else class="mt-2 text-xs text-slate-500">간식 기록이 없습니다.</p>
        </article>

        <article class="rounded-xl border border-slate-200 bg-slate-50 p-3">
          <p class="text-xs font-semibold text-slate-500">예정 식단(미동기화 장바구니)</p>
          <div v-if="draftPlannedFoods.length" class="mt-2 flex flex-wrap gap-2">
            <span
              v-for="item in draftPlannedFoods.slice(0, 8)"
              :key="`draft-plan-${item.foodId}`"
              class="rounded-lg border border-cyan-100 bg-white px-2 py-1 text-xs text-slate-700"
            >
              {{ item.name }} {{ formatQuantity(item.quantity) }}개
            </span>
          </div>
          <p v-else class="mt-2 text-xs text-slate-500">장바구니에 담긴 예정 식단이 없습니다.</p>
        </article>
      </div>
    </article>

    <div class="grid items-start gap-4 lg:grid-cols-2">
      <article class="bento-card h-full">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <div>
            <p class="eyebrow">Today Summary</p>
            <h3 class="mt-1 text-xl font-semibold text-slate-900">장바구니</h3>
          </div>
          <label class="inline-flex items-center gap-2">
            <span class="text-xs font-semibold text-slate-600">기준 식사</span>
            <select v-model="selectedSummaryMealType" class="field-input !w-28 !py-2 !text-sm">
              <option v-for="type in mealTypeOptions" :key="`summary-meal-${type}`" :value="type">
                {{ mealTypeLabel(type) }}
              </option>
            </select>
          </label>
        </div>

        <div class="mt-3 grid gap-2 sm:grid-cols-4">
          <div class="rounded-xl border border-cyan-200 bg-cyan-50 px-3 py-3">
            <p class="text-xs text-slate-500">선택 식사</p>
            <p class="text-lg font-semibold text-slate-900">{{ selectedMealPanel.label }}</p>
          </div>
          <div class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-3">
            <p class="text-xs text-slate-500">총 수량</p>
            <p class="text-lg font-semibold text-slate-900">{{ cartStore.totalItemCount.toFixed(1) }}</p>
          </div>
          <div class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-3">
            <p class="text-xs text-slate-500">추정 칼로리</p>
            <p class="text-lg font-semibold text-slate-900">{{ formatCalories(cartStore.totalCalories) }}</p>
          </div>
          <div class="rounded-xl border border-slate-200 bg-slate-50 px-3 py-3">
            <p class="text-xs text-slate-500">미동기화 초안 일수</p>
            <p class="text-lg font-semibold text-slate-900">{{ unsyncedDraftDays }}</p>
          </div>
        </div>

        <div class="mt-3 rounded-xl border border-slate-200 bg-slate-50 p-3">
          <div class="flex items-center justify-between gap-2">
            <p class="text-xs font-semibold text-slate-500">선택 식사 서버/추천 요약</p>
            <span class="pill">{{ selectedMealPanel.status }}</span>
          </div>
          <p class="mt-1 text-sm font-semibold text-slate-900">{{ formatCalories(selectedMealPanel.totalCalories) }}</p>
          <div v-if="selectedMealPanel.items.length" class="mt-2 flex flex-wrap gap-2">
            <span
              v-for="(item, index) in selectedMealPanel.items.slice(0, 6)"
              :key="`selected-panel-item-${index}`"
              class="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs text-slate-700"
            >
              {{ item.name }} {{ formatQuantity(item.quantity) }}개
            </span>
          </div>
          <p v-else class="mt-2 text-xs text-slate-500">해당 식사 기록이 없습니다.</p>
        </div>

        <div class="mt-3 rounded-xl border border-slate-200 bg-white p-3">
          <p class="text-xs font-semibold text-slate-500">현재 담은 음식</p>
          <div v-if="cartStore.items.length" class="mt-2 space-y-1">
            <div
              v-for="item in cartStore.items.slice(0, 8)"
              :key="`summary-${item.foodId}`"
              class="flex items-center justify-between gap-2 text-sm"
            >
              <span class="truncate pr-2 text-slate-800">{{ item.name }}</span>
              <div class="flex items-center gap-2">
                <button class="btn-ghost px-2 py-1" @click="decreaseCartQuantity(item.foodId)">-</button>
                <span class="text-slate-500">{{ Number(item.quantity).toFixed(1) }}개</span>
                <button class="btn-ghost px-2 py-1" @click="increaseCartQuantity(item.foodId)">+</button>
                <button class="btn-ghost" @click="cartStore.remove(item.foodId)">제거</button>
              </div>
            </div>
          </div>
          <p v-else class="mt-2 text-sm text-slate-500">아직 장바구니가 비어 있습니다.</p>
        </div>

        <div class="mt-3 flex gap-2">
          <RouterLink :to="syncLinkTo" class="btn-primary inline-flex">식단 동기화</RouterLink>
          <button class="btn-ghost" @click="refreshTodaySummary">오늘 식단 새로고침</button>
        </div>
      </article>

      <article class="bento-card h-full">
        <div class="flex items-center justify-between">
          <div>
            <p class="eyebrow">Weekly Detox Rhythm</p>
            <h3 class="mt-1 text-xl font-semibold text-slate-900">주간 첨가물 디톡스 리듬</h3>
          </div>
          <RouterLink to="/mypage" class="btn-ghost">마이페이지 상세</RouterLink>
        </div>

        <div class="mt-3 rounded-xl border border-slate-200 bg-white p-3">
          <p class="text-xs font-semibold text-slate-500">오늘 하루 식단(서버 기록)</p>
          <div v-if="todayMealsLoading" class="mt-2 text-sm text-slate-500">불러오는 중...</div>
          <div v-else-if="todayMeals.length" class="mt-2 grid gap-2 sm:grid-cols-2">
            <div
              v-for="meal in todayMeals"
              :key="`today-${meal.mealId}`"
              class="rounded-lg border border-slate-100 bg-slate-50 px-2 py-2"
            >
              <p class="text-xs font-semibold text-slate-800">{{ mealTypeLabel(meal.mealType) }}</p>
              <p class="text-xs text-slate-600">{{ formatCalories(meal.totalCalories) }}</p>
              <p v-if="meal.items?.length" class="mt-1 text-xs text-slate-500 line-clamp-2">
                {{ meal.items.map((item) => `${item.foodName} ${Number(item.quantity || 0).toFixed(1)}개`).join(" · ") }}
              </p>
            </div>
          </div>
          <p v-else class="mt-2 text-sm text-slate-500">오늘 서버 기록 식단이 없습니다.</p>
        </div>

        <div class="mt-3 heat-label-grid-week">
          <span v-for="day in weeklyRhythmWeek" :key="`label-week-${day.date}`" class="heat-label-cell">
            <template v-if="day.offset === 0">
              <strong>오늘</strong>
              <small>{{ weekLabel(day) }}</small>
            </template>
            <template v-else>
              {{ weekLabel(day) }}
            </template>
          </span>
        </div>

        <div class="heat-grid-week heat-grid-week-compact mt-1">
          <span
            v-for="day in weeklyRhythmWeek"
            :key="`week-${day.date}`"
            :title="`${day.date} | ${day.status} | ${Math.round(day.totalCalories)}kcal`"
            class="heat-cell heat-cell-compact"
            :class="[`heat-${day.level}`, day.status === 'DRAFT' ? 'heat-draft' : '']"
          />
        </div>

        <div class="mt-2 flex flex-wrap gap-2 text-xs text-slate-600">
          <span class="pill"><span class="legend-dot heat-0" />기록 없음</span>
          <span class="pill"><span class="legend-dot heat-1" />고열량/주의</span>
          <span class="pill"><span class="legend-dot heat-2" />보통</span>
          <span class="pill"><span class="legend-dot heat-3" />좋음</span>
          <span class="pill"><span class="legend-dot heat-4" />매우 좋음</span>
        </div>
      </article>
    </div>

    <article class="bento-card">
      <p class="eyebrow">Discover</p>
      <h2 class="title-lg">식품 검색</h2>

        <div class="mt-4 grid gap-3 md:grid-cols-[1fr_auto]">
        <input
          v-model="keyword"
          type="text"
          class="field-input"
          placeholder="예: 삼겹살, 곱창, 된장찌개, 회, 라면"
        />
        <RouterLink :to="syncLinkTo" class="btn-ghost">장바구니/동기화 화면</RouterLink>
      </div>

      <div class="mt-3 flex flex-wrap gap-2">
        <button
          v-for="item in categories"
          :key="item.category"
          class="pill"
          :class="selectedCategory === item.category ? 'border-cyan-300 bg-cyan-50 text-cyan-700' : ''"
          @click="setCategory(item.category)"
        >
          {{ item.category }}
          <span v-if="item.itemCount" class="text-xs text-slate-500">({{ item.itemCount }})</span>
        </button>
      </div>
    </article>

    <div class="grid items-start gap-4 lg:grid-cols-2">
      <article class="bento-card h-full">
        <div class="mb-3 flex items-center justify-between">
          <p class="eyebrow">{{ isBarMode ? `${selectedCategory} 전체` : resultTitle }}</p>
          <div class="flex items-center gap-2">
            <button v-if="showMoreButton" class="btn-ghost" @click="expandCurrentCategory">더보기</button>
            <button v-if="showCollapseButton" class="btn-ghost" @click="collapseCurrentCategory">대표 6 보기</button>
            <button class="btn-ghost" @click="loadFoods">새로고침</button>
          </div>
        </div>

        <p v-if="errorMessage" class="mb-3 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
          {{ errorMessage }}
        </p>

        <div v-if="loading" class="grid gap-3 md:grid-cols-2">
          <div v-for="n in 6" :key="`skeleton-${n}`" class="skeleton h-44 rounded-xl" />
        </div>

        <div
          v-else-if="foods.length === 0"
          class="rounded-xl border border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-600"
        >
          <p>검색 결과가 없습니다. 다른 키워드나 카테고리를 선택해 주세요.</p>
          <div class="mt-3 flex flex-wrap gap-2">
            <button class="pill" @click="applyKeyword('삼겹살')">삼겹살</button>
            <button class="pill" @click="applyKeyword('라면')">라면</button>
            <button class="pill" @click="applyKeyword('된장찌개')">된장찌개</button>
            <button class="pill" @click="applyKeyword('곱창')">곱창</button>
            <button class="pill" @click="applyKeyword('회')">회</button>
          </div>
        </div>

        <div v-else-if="isBarMode" class="space-y-2">
          <article v-for="food in foods" :key="food.foodId" class="food-row">
            <img :src="resolveFoodImageUrl(food)" :alt="food.name" class="food-row-thumb" loading="lazy" />
            <div class="food-row-main">
              <div class="food-row-head">
                <div>
                  <h3 class="text-sm font-semibold text-slate-900">{{ food.name }}</h3>
                  <p class="text-xs text-slate-500">{{ food.category || "기타" }} · {{ food.manufacturer || "제조사 정보 없음" }}</p>
                </div>
                <span class="pill">{{ formatCalories(food.calories) }}</span>
              </div>
              <div class="calorie-track">
                <span class="calorie-fill" :style="{ width: `${normalizePercent(food.calories)}%` }" />
              </div>
              <div class="mt-2 flex flex-wrap gap-2">
                <span
                  v-if="normalizedWarnings(food.warningLabels).length"
                  class="rounded bg-rose-50 px-2 py-1 text-[11px] font-semibold text-rose-700"
                >
                  주의 첨가물: {{ compactWarning(food.warningLabels) }}
                </span>
              </div>
            </div>
            <button class="btn-primary food-row-action" @click="addFoodToCart(food)">즉시 담기</button>
          </article>
        </div>

        <div v-else class="grid gap-3 md:grid-cols-2">
          <article v-for="food in foods" :key="food.foodId" class="rounded-xl border border-slate-200 bg-white p-3">
            <img
              :src="resolveFoodImageUrl(food)"
              :alt="food.name"
              class="h-32 w-full rounded-lg border border-slate-200 object-cover"
              loading="lazy"
            />
            <div class="mt-3 flex items-start justify-between gap-3">
              <div>
                <h3 class="text-base font-semibold text-slate-900">{{ food.name }}</h3>
                <p class="text-xs text-slate-500">{{ food.category || "기타" }} · {{ food.manufacturer || "제조사 정보 없음" }}</p>
              </div>
              <span class="pill">{{ formatCalories(food.calories) }}</span>
            </div>
            <div class="mt-2 flex flex-wrap gap-2">
              <span
                v-if="normalizedWarnings(food.warningLabels).length"
                class="rounded bg-rose-50 px-2 py-1 text-[11px] font-semibold text-rose-700"
              >
                주의 첨가물: {{ compactWarning(food.warningLabels) }}
              </span>
            </div>
            <button class="btn-primary mt-3 w-full justify-center" @click="addFoodToCart(food)">즉시 담기</button>
          </article>
        </div>
      </article>

      <article class="bento-card h-full">
        <p class="eyebrow">AI Free Recommendation</p>
        <h3 class="mt-1 text-xl font-semibold text-slate-900">오늘의 추천 식단</h3>
        <p class="mt-1 text-sm text-slate-500">
          현재 목표({{ profileStore.goalLabel }})와 강도({{ profileStore.strictnessLabel }}) 기준 자동 추천입니다.
        </p>

        <div v-if="recommendationLoading" class="mt-3 space-y-2">
          <div class="skeleton h-16 rounded-xl" />
          <div class="skeleton h-16 rounded-xl" />
          <div class="skeleton h-16 rounded-xl" />
          <div class="skeleton h-16 rounded-xl" />
        </div>

        <div v-else-if="recommendation?.meals?.length" class="mt-3 space-y-2">
          <article
            v-for="item in orderedRecommendationMeals"
            :key="item.mealType"
            class="rounded-xl border border-slate-200 bg-white p-3"
            :class="item.mealType === selectedSummaryMealType ? 'border-cyan-300 bg-cyan-50/40' : ''"
          >
            <div class="flex items-center justify-between gap-2">
              <div>
                <p class="text-xs font-semibold text-slate-500">{{ mealTypeLabel(item.mealType) }}</p>
                <p class="text-sm font-semibold text-slate-900">{{ item.food?.name }}</p>
              </div>
              <span class="pill">{{ formatCalories(item.food?.calories) }}</span>
            </div>
            <p class="mt-1 text-xs text-slate-500">{{ item.reason }}</p>
            <button class="btn-ghost mt-2" @click="addRecommendedFood(item)">장바구니 담기</button>
          </article>
        </div>
        <p v-else class="mt-3 text-sm text-slate-500">{{ recommendationError || "추천 식단을 생성하지 못했습니다." }}</p>

        <div class="mt-3 rounded-xl border border-slate-200 bg-slate-50 p-3 text-xs text-slate-600">
          하루 추천 칼로리 합계:
          <strong class="text-slate-900">{{ recommendedCalories.toFixed(0) }} kcal</strong>
        </div>

        <div class="mt-3 rounded-xl border border-slate-200 bg-white p-3 text-xs text-slate-600">
          재추천을 눌러 조합을 바꾸고, 원하는 식단만 장바구니에 담아 기록하세요.
        </div>

        <div class="mt-4 flex justify-end">
          <button class="btn-ghost" :disabled="recommendationLoading" @click="rerollRecommendation">
            {{ recommendationLoading ? "재추천 중..." : "재추천" }}
          </button>
        </div>
      </article>
    </div>

    <button class="floating-guide-button" @click="guideOpen = true">가이드</button>

    <div v-if="guideOpen" class="overlay-shell" @click.self="guideOpen = false">
      <article class="bento-card w-full max-w-3xl">
        <div class="flex items-center justify-between gap-2">
          <div>
            <p class="eyebrow">Page Guide</p>
            <h3 class="mt-1 text-xl font-semibold text-slate-900">화면 안내</h3>
          </div>
          <button class="btn-ghost" @click="guideOpen = false">닫기</button>
        </div>
        <div class="mt-3 grid gap-4 md:grid-cols-2">
          <img
            src="https://images.pexels.com/photos/1640774/pexels-photo-1640774.jpeg?auto=compress&cs=tinysrgb&w=1200"
            alt="식단 가이드 이미지"
            class="h-44 w-full rounded-xl border border-slate-200 object-cover"
          />
          <img
            src="https://images.pexels.com/photos/884600/pexels-photo-884600.jpeg?auto=compress&cs=tinysrgb&w=1200"
            alt="식품 탐색 가이드 이미지"
            class="h-44 w-full rounded-xl border border-slate-200 object-cover"
          />
        </div>
        <div class="mt-3 space-y-2 text-sm text-slate-700">
          <p>1. 기본은 카테고리별 대표 6개만 보여주고, [더보기]를 누르면 전체 목록을 막대형으로 확인할 수 있습니다.</p>
          <p>2. Today Summary에서 담은 음식 수량(+, -)과 제거를 바로 조작할 수 있습니다.</p>
          <p>3. Weekly Detox Rhythm은 최근 7일 기준이며, 중앙 칸이 오늘입니다.</p>
          <p>4. 이미지가 맞지 않으면 관리자 &gt; 식품 관리에서 image_url을 수정해 정확한 사진으로 교체할 수 있습니다.</p>
        </div>
      </article>
    </div>
  </section>
</template>
