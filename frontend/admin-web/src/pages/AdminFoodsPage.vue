<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import api from "../api/client";

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");
const foodsPage = ref({ items: [] });

const keyword = ref("");
const category = ref("");
const sortBy = ref("foodId");
const sortOrder = ref("asc");

const missPage = ref({ items: [] });
const missKeyword = ref("");

const editingFoodId = ref(null);
const form = ref({
  name: "",
  category: "한식",
  manufacturer: "",
  barcode: "",
  imageUrl: "",
  calories: 0,
  carbs: 0,
  protein: 0,
  fat: 0,
  sugars: 0,
  sodium: 0,
  additiveIdsText: "",
});

const sortedFoods = computed(() => {
  const rows = [...(foodsPage.value?.items || [])];
  const factor = sortOrder.value === "asc" ? 1 : -1;

  rows.sort((a, b) => {
    const key = sortBy.value;
    if (["foodId", "calories"].includes(key)) {
      return (Number(a?.[key] || 0) - Number(b?.[key] || 0)) * factor;
    }
    return String(a?.[key] || "").localeCompare(String(b?.[key] || ""), "ko") * factor;
  });

  return rows;
});

function toggleSort(column) {
  if (sortBy.value === column) {
    sortOrder.value = sortOrder.value === "asc" ? "desc" : "asc";
    return;
  }
  sortBy.value = column;
  sortOrder.value = "asc";
}

function sortIndicator(column) {
  if (sortBy.value !== column) return "˄˅";
  return sortOrder.value === "asc" ? "˄" : "˅";
}

function parseAdditiveIds(text) {
  return String(text || "")
    .split(",")
    .map((v) => Number(v.trim()))
    .filter((v) => Number.isInteger(v) && v > 0);
}

function resetForm() {
  editingFoodId.value = null;
  form.value = {
    name: route.query.prefill ? String(route.query.prefill) : "",
    category: "한식",
    manufacturer: "",
    barcode: "",
    imageUrl: "",
    calories: 0,
    carbs: 0,
    protein: 0,
    fat: 0,
    sugars: 0,
    sodium: 0,
    additiveIdsText: "",
  };
}

function beginEdit(food) {
  editingFoodId.value = food.foodId;
  form.value = {
    name: food.name || "",
    category: food.category || "기타",
    manufacturer: food.manufacturer || "",
    barcode: food.barcode || "",
    imageUrl: food.imageUrl || "",
    calories: Number(food.calories || 0),
    carbs: Number(food.carbs || 0),
    protein: Number(food.protein || 0),
    fat: Number(food.fat || 0),
    sugars: Number(food.sugars || 0),
    sodium: Number(food.sodium || 0),
    additiveIdsText: (food.additiveIds || []).join(","),
  };
}

async function loadFoods() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const response = await api.get("/admin/data/foods", {
      params: {
        keyword: keyword.value || undefined,
        category: category.value || undefined,
        page: 0,
        size: 100,
      },
    });
    foodsPage.value = response.data?.data ?? { items: [] };
  } catch (error) {
    errorMessage.value = error?.response?.data?.message ?? "식품 목록을 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}

async function loadMissLogs() {
  try {
    const response = await api.get("/admin/data/search-miss-logs", {
      params: {
        keyword: missKeyword.value || undefined,
        resolved: false,
        page: 0,
        size: 30,
      },
    });
    missPage.value = response.data?.data ?? { items: [] };
  } catch {
    missPage.value = { items: [] };
  }
}

async function resolveMiss(missId, resolved = true) {
  try {
    await api.patch(`/admin/data/search-miss-logs/${missId}`, null, { params: { resolved } });
    await loadMissLogs();
  } catch (error) {
    errorMessage.value = error?.response?.data?.message ?? "미등록 요청 상태 변경에 실패했습니다.";
  }
}

function applyMissKeywordToFoodForm(row) {
  const text = String(row?.keyword || "").trim();
  if (!text) return;
  editingFoodId.value = null;
  form.value.name = text;
}

function openAdditivePageWithKeyword(row) {
  const text = String(row?.keyword || "").trim();
  router.push({ name: "admin-additives", query: text ? { prefill: text } : {} });
}

async function saveFood() {
  successMessage.value = "";
  errorMessage.value = "";

  const payload = {
    name: form.value.name,
    category: form.value.category,
    manufacturer: form.value.manufacturer || null,
    barcode: form.value.barcode || null,
    imageUrl: form.value.imageUrl || null,
    calories: Number(form.value.calories || 0),
    carbs: Number(form.value.carbs || 0),
    protein: Number(form.value.protein || 0),
    fat: Number(form.value.fat || 0),
    sugars: Number(form.value.sugars || 0),
    sodium: Number(form.value.sodium || 0),
    additiveIds: parseAdditiveIds(form.value.additiveIdsText),
  };

  try {
    if (editingFoodId.value) {
      await api.patch(`/foods/${editingFoodId.value}`, payload);
      successMessage.value = `식품(${editingFoodId.value})을 수정했습니다.`;
    } else {
      await api.post("/foods", payload);
      successMessage.value = "식품을 등록했습니다.";
    }

    resetForm();
    await Promise.all([loadFoods(), loadMissLogs()]);
  } catch (error) {
    errorMessage.value = error?.response?.data?.message ?? "식품 저장에 실패했습니다.";
  }
}

onMounted(async () => {
  resetForm();
  await Promise.all([loadFoods(), loadMissLogs()]);
});
</script>

<template>
  <main class="admin-dashboard-wrap">
    <header class="admin-top">
      <div>
        <p class="admin-eyebrow">Foods</p>
        <h1>식품 관리</h1>
      </div>
      <div class="admin-top-actions">
        <button class="admin-button secondary" @click="loadFoods">새로고침</button>
      </div>
    </header>

    <article class="admin-card mt-4">
      <h2>{{ editingFoodId ? `식품 수정 #${editingFoodId}` : "식품 신규 등록" }}</h2>
      <div class="admin-form-grid mt-4 md:grid-cols-2">
        <label>
          <span class="admin-muted">식품명</span>
          <input v-model="form.name" class="admin-input" />
        </label>
        <label>
          <span class="admin-muted">카테고리</span>
          <input v-model="form.category" class="admin-input" placeholder="예: 한식" />
        </label>
        <label>
          <span class="admin-muted">제조사</span>
          <input v-model="form.manufacturer" class="admin-input" />
        </label>
        <label>
          <span class="admin-muted">바코드</span>
          <input v-model="form.barcode" class="admin-input" />
        </label>
        <label class="md:col-span-2">
          <span class="admin-muted">이미지 URL</span>
          <input v-model="form.imageUrl" class="admin-input" placeholder="https://..." />
        </label>
        <label>
          <span class="admin-muted">칼로리</span>
          <input v-model="form.calories" type="number" min="0" step="0.1" class="admin-input" />
        </label>
        <label>
          <span class="admin-muted">탄수화물</span>
          <input v-model="form.carbs" type="number" min="0" step="0.1" class="admin-input" />
        </label>
        <label>
          <span class="admin-muted">단백질</span>
          <input v-model="form.protein" type="number" min="0" step="0.1" class="admin-input" />
        </label>
        <label>
          <span class="admin-muted">지방</span>
          <input v-model="form.fat" type="number" min="0" step="0.1" class="admin-input" />
        </label>
        <label>
          <span class="admin-muted">당류</span>
          <input v-model="form.sugars" type="number" min="0" step="0.1" class="admin-input" />
        </label>
        <label>
          <span class="admin-muted">나트륨</span>
          <input v-model="form.sodium" type="number" min="0" step="0.1" class="admin-input" />
        </label>
        <label class="md:col-span-2">
          <span class="admin-muted">첨가물 ID 목록(쉼표 구분)</span>
          <input v-model="form.additiveIdsText" class="admin-input" placeholder="예: 1,5,9" />
        </label>
      </div>
      <div class="mt-3 flex flex-wrap gap-2">
        <button class="admin-button" @click="saveFood">저장</button>
        <button class="admin-button secondary" @click="resetForm">초기화</button>
      </div>

      <p v-if="errorMessage" class="admin-error mt-3">{{ errorMessage }}</p>
      <p v-if="successMessage" class="admin-info mt-3">{{ successMessage }}</p>
    </article>

    <section class="mt-4 space-y-4">
      <article class="admin-card">
        <div class="admin-table-head">
          <h2>식품 목록</h2>
          <div class="admin-search-row">
            <input v-model="keyword" class="admin-input" placeholder="식품명 검색" />
            <input v-model="category" class="admin-input" placeholder="카테고리" />
            <button class="admin-button secondary" @click="loadFoods">조회</button>
          </div>
        </div>

        <div class="mt-3">
          <table class="admin-table is-fixed">
            <thead>
              <tr>
                <th><button class="sort-head-button" @click="toggleSort('foodId')">ID <span>{{ sortIndicator("foodId") }}</span></button></th>
                <th><button class="sort-head-button" @click="toggleSort('name')">식품명 <span>{{ sortIndicator("name") }}</span></button></th>
                <th><button class="sort-head-button" @click="toggleSort('category')">카테고리 <span>{{ sortIndicator("category") }}</span></button></th>
                <th><button class="sort-head-button" @click="toggleSort('manufacturer')">제조사 <span>{{ sortIndicator("manufacturer") }}</span></button></th>
                <th><button class="sort-head-button" @click="toggleSort('calories')">칼로리 <span>{{ sortIndicator("calories") }}</span></button></th>
                <th>이미지</th>
                <th>작업</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="food in sortedFoods" :key="food.foodId">
                <td>{{ food.foodId }}</td>
                <td>{{ food.name }}</td>
                <td>{{ food.category }}</td>
                <td>{{ food.manufacturer || "-" }}</td>
                <td>{{ Number(food.calories || 0).toFixed(0) }} kcal</td>
                <td>
                  <a v-if="food.imageUrl" :href="food.imageUrl" target="_blank" rel="noreferrer">보기</a>
                  <span v-else>-</span>
                </td>
                <td>
                  <button class="admin-button secondary" @click="beginEdit(food)">수정</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <article class="admin-card">
        <div class="admin-table-head">
          <h2>미등록 요청(식품/첨가물)</h2>
          <div class="admin-search-row">
            <input v-model="missKeyword" class="admin-input" placeholder="키워드 검색" />
            <button class="admin-button secondary" @click="loadMissLogs">조회</button>
          </div>
        </div>

        <div class="mt-3">
          <table class="admin-table is-fixed">
            <thead>
              <tr>
                <th style="width: 32%">키워드</th>
                <th style="width: 10%">요청 수</th>
                <th style="width: 24%">최근 시각</th>
                <th style="width: 34%">작업</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="miss in missPage.items || []" :key="miss.missId">
                <td>{{ miss.keyword }}</td>
                <td>{{ miss.hitCount }}</td>
                <td>{{ miss.lastSeenAt }}</td>
                <td>
                  <div class="admin-action-stack">
                    <button class="admin-button secondary" @click="applyMissKeywordToFoodForm(miss)">식품 등록폼</button>
                    <button class="admin-button secondary" @click="openAdditivePageWithKeyword(miss)">첨가물 이동</button>
                    <button class="admin-button secondary" @click="resolveMiss(miss.missId, true)">완료</button>
                  </div>
                </td>
              </tr>
              <tr v-if="!(missPage.items || []).length">
                <td colspan="4" class="text-slate-500">처리할 미등록 요청이 없습니다.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>
    </section>
  </main>
</template>
