<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import api from "../api/client";
import { useCartStore } from "../stores/cartStore";
import { useProfileStore } from "../stores/profileStore";
import { resolveFoodImageUrl } from "../utils/foodImageMap";

const DEFAULT_LIMIT = 6;
const EXPANDED_LIMIT = 60;

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
let searchTimer = null;

const weeklyRhythm = computed(() => cartStore.weeklyRhythm);
const unsyncedDraftDays = computed(() => weeklyRhythm.value.filter((day) => day.status === "DRAFT").length);
const totalCategoryCount = computed(() =>
  categories.value
    .filter((item) => item.category !== "전체")
    .reduce((sum, item) => sum + Number(item.itemCount || 0), 0)
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

const topWarnings = computed(() =>
  foods.value
    .flatMap((food) => food.warningLabels || [])
    .slice(0, 3)
    .join(" · ")
);

const rhythmLegend = [
  { level: 0, label: "기록 없음", className: "heat-0" },
  { level: 1, label: "고열량/주의", className: "heat-1" },
  { level: 2, label: "보통", className: "heat-2" },
  { level: 3, label: "좋음", className: "heat-3" },
  { level: 4, label: "매우 좋음", className: "heat-4" },
];

function formatCalories(value) {
  const num = Number(value || 0);
  return Number.isFinite(num) ? `${num.toFixed(0)} kcal` : "-";
}

function normalizePercent(calories) {
  const value = Number(calories || 0);
  return Math.max(8, Math.min(100, Math.round((value / 900) * 100)));
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
    if (!hasKeyword && selectedCategory.value === "전체" && !isExpanded.value) {
      const response = await api.get("/foods/popular", { params: { limit: DEFAULT_LIMIT } });
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
      error?.response?.data?.message ??
      "식품 목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.";
  } finally {
    loading.value = false;
  }
}

watch([keyword, selectedCategory], () => {
  if (searchTimer) clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    loadFoods();
  }, 300);
});

onMounted(async () => {
  await loadCategories();
  await loadFoods();
});
</script>

<template>
  <section class="space-y-4">
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
        <RouterLink to="/cart" class="btn-ghost">장바구니/동기화</RouterLink>
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

      <div class="mt-3 flex flex-wrap gap-2 text-xs text-slate-500">
        <span class="pill">현재 목표: {{ profileStore.goalLabel }}</span>
        <span class="pill">강도: {{ profileStore.strictnessLabel }}</span>
        <span v-if="topWarnings" class="pill">주의 첨가물: {{ topWarnings }}</span>
      </div>
    </article>

    <div class="grid gap-4 lg:grid-cols-[1.45fr_1fr]">
      <article class="bento-card">
        <div class="mb-3 flex items-center justify-between">
          <p class="eyebrow">{{ isBarMode ? `${selectedCategory} 전체` : "Popular 6" }}</p>
          <div class="flex items-center gap-2">
            <button v-if="showMoreButton" class="btn-ghost" @click="expandCurrentCategory">더보기</button>
            <button v-if="showCollapseButton" class="btn-ghost" @click="collapseCurrentCategory">대표 6 보기</button>
            <button class="btn-ghost" @click="loadFoods">새로고침</button>
          </div>
        </div>

        <p
          v-if="errorMessage"
          class="mb-3 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700"
        >
          {{ errorMessage }}
        </p>

        <div v-if="loading" class="grid gap-3 md:grid-cols-2">
          <div v-for="n in 6" :key="`skeleton-${n}`" class="skeleton h-44 rounded-xl" />
        </div>

        <div
          v-else-if="foods.length === 0"
          class="rounded-xl border border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-600"
        >
          <p>검색 결과가 없습니다. 다른 키워드나 카테고리를 선택해주세요.</p>
          <div class="mt-3 flex flex-wrap gap-2">
            <button class="pill" @click="applyKeyword('삼겹살')">삼겹살</button>
            <button class="pill" @click="applyKeyword('라면')">라면</button>
            <button class="pill" @click="applyKeyword('된장찌개')">된장찌개</button>
            <button class="pill" @click="applyKeyword('곱창')">곱창</button>
            <button class="pill" @click="applyKeyword('회')">회</button>
          </div>
        </div>

        <div v-else-if="isBarMode" class="space-y-2">
          <article
            v-for="food in foods"
            :key="food.foodId"
            class="food-row"
          >
            <img
              :src="resolveFoodImageUrl(food)"
              :alt="food.name"
              class="food-row-thumb"
              loading="lazy"
            />
            <div class="food-row-main">
              <div class="food-row-head">
                <div>
                  <h3 class="text-sm font-semibold text-slate-900">{{ food.name }}</h3>
                  <p class="text-xs text-slate-500">
                    {{ food.category || "기타" }} · {{ food.manufacturer || "제조사 정보 없음" }}
                  </p>
                </div>
                <span class="pill">{{ formatCalories(food.calories) }}</span>
              </div>
              <div class="calorie-track">
                <span class="calorie-fill" :style="{ width: `${normalizePercent(food.calories)}%` }" />
              </div>
              <div class="mt-2 flex flex-wrap gap-2">
                <span
                  v-for="label in food.warningLabels || []"
                  :key="label"
                  class="rounded bg-rose-50 px-2 py-1 text-[11px] text-rose-700"
                >
                  {{ label }}
                </span>
              </div>
            </div>
            <button class="btn-primary food-row-action" @click="addFoodToCart(food)">즉시 담기</button>
          </article>
        </div>

        <div v-else class="grid gap-3 md:grid-cols-2">
          <article
            v-for="food in foods"
            :key="food.foodId"
            class="rounded-xl border border-slate-200 bg-white p-3"
          >
            <img
              :src="resolveFoodImageUrl(food)"
              :alt="food.name"
              class="h-32 w-full rounded-lg border border-slate-200 object-cover"
              loading="lazy"
            />
            <div class="mt-3 flex items-start justify-between gap-3">
              <div>
                <h3 class="text-base font-semibold text-slate-900">{{ food.name }}</h3>
                <p class="text-xs text-slate-500">
                  {{ food.category || "기타" }} · {{ food.manufacturer || "제조사 정보 없음" }}
                </p>
              </div>
              <span class="pill">{{ formatCalories(food.calories) }}</span>
            </div>
            <div class="mt-2 flex flex-wrap gap-2">
              <span
                v-for="label in food.warningLabels || []"
                :key="label"
                class="rounded bg-rose-50 px-2 py-1 text-[11px] text-rose-700"
              >
                {{ label }}
              </span>
            </div>
            <button class="btn-primary mt-3 w-full justify-center" @click="addFoodToCart(food)">
              즉시 담기
            </button>
          </article>
        </div>
      </article>

      <article class="bento-card">
        <p class="eyebrow">Today Summary</p>
        <h3 class="mt-1 text-xl font-semibold text-slate-900">오프라인 우선 장바구니</h3>

        <div class="mt-3 grid gap-2">
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

        <RouterLink to="/cart" class="btn-primary mt-3 inline-flex">식단 동기화 실행</RouterLink>
      </article>
    </div>

    <article class="bento-card">
      <div class="flex items-center justify-between">
        <div>
          <p class="eyebrow">Weekly Detox Rhythm</p>
          <h3 class="mt-1 text-xl font-semibold text-slate-900">주간 첨가물 디톡스 리듬</h3>
        </div>
        <RouterLink to="/mypage" class="btn-ghost">마이페이지 상세</RouterLink>
      </div>

      <div class="mt-3 heat-grid">
        <span
          v-for="day in weeklyRhythm"
          :key="day.date"
          :title="`${day.date} | ${day.status} | ${Math.round(day.totalCalories)}kcal`"
          class="heat-cell"
          :class="[`heat-${day.level}`, day.status === 'DRAFT' ? 'heat-draft' : '']"
        />
      </div>

      <div class="mt-3 flex flex-wrap gap-2 text-xs text-slate-600">
        <span v-for="item in rhythmLegend" :key="item.level" class="pill">
          <span class="legend-dot" :class="item.className" />
          {{ item.label }}
        </span>
      </div>
    </article>

    <button class="floating-guide-button" @click="guideOpen = true">가이드</button>

    <div v-if="guideOpen" class="overlay-shell" @click.self="guideOpen = false">
      <article class="bento-card w-full max-w-2xl">
        <div class="flex items-center justify-between gap-2">
          <div>
            <p class="eyebrow">Page Guide</p>
            <h3 class="mt-1 text-xl font-semibold text-slate-900">화면 안내</h3>
          </div>
          <button class="btn-ghost" @click="guideOpen = false">닫기</button>
        </div>
        <div class="mt-3 space-y-2 text-sm text-slate-700">
          <p>1. 대표 6개 음식이 먼저 표시되고, 카테고리에서 [더보기]를 누르면 전체 목록이 바형 리스트로 확장됩니다.</p>
          <p>2. 오프라인에서도 장바구니는 유지되며, 온라인 상태에서 [식단 동기화 실행]으로 서버 저장을 진행합니다.</p>
          <p>3. 디톡스 리듬 색상은 최근 동기화 기록 기반 점수입니다. 붉은색은 고열량/주의 상태를 의미합니다.</p>
          <p>4. 이미지가 음식과 완전히 일치하도록 하려면 관리자에서 식품별 `image_url`을 직접 등록하면 됩니다.</p>
        </div>
      </article>
    </div>
  </section>
</template>
