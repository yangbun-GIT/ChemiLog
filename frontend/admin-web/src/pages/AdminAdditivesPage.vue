<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import api from "../api/client";

const route = useRoute();

const loading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");

const additivesPage = ref({ items: [] });
const keyword = ref("");
const sortBy = ref("additiveId");
const sortOrder = ref("asc");
const PAGE_SIZE = 100;
const DEFAULT_VISIBLE_COUNT = 30;
const visibleCount = ref(DEFAULT_VISIBLE_COUNT);
const showAllRows = ref(false);

const missPage = ref({ items: [] });
const missKeyword = ref("");

const editingAdditiveId = ref(null);
const form = ref({
  name: "",
  purpose: "",
  dangerLevel: 3,
  dailyAcceptableIntake: "",
});

const sortedAdditives = computed(() => {
  const rows = [...(additivesPage.value?.items || [])];
  const factor = sortOrder.value === "asc" ? 1 : -1;

  rows.sort((a, b) => {
    const left = a?.[sortBy.value];
    const right = b?.[sortBy.value];

    if (["additiveId", "dangerLevel", "mappedFoodCount"].includes(sortBy.value)) {
      return (Number(left || 0) - Number(right || 0)) * factor;
    }

    return String(left || "").localeCompare(String(right || ""), "ko") * factor;
  });

  return rows;
});

const visibleAdditives = computed(() => {
  if (showAllRows.value) return sortedAdditives.value;
  return sortedAdditives.value.slice(0, visibleCount.value);
});

const canShowMore = computed(() => !showAllRows.value && visibleCount.value < sortedAdditives.value.length);
const canShowAll = computed(() => !showAllRows.value && sortedAdditives.value.length > DEFAULT_VISIBLE_COUNT);
const canCollapse = computed(() => showAllRows.value || visibleCount.value > DEFAULT_VISIBLE_COUNT);

function resetVisibleRows() {
  visibleCount.value = DEFAULT_VISIBLE_COUNT;
  showAllRows.value = false;
}

function showMoreRows() {
  visibleCount.value = Math.min(sortedAdditives.value.length, visibleCount.value + DEFAULT_VISIBLE_COUNT);
}

function showAll() {
  showAllRows.value = true;
}

function collapseRows() {
  resetVisibleRows();
}

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

function resetForm() {
  editingAdditiveId.value = null;
  form.value = {
    name: "",
    purpose: "",
    dangerLevel: 3,
    dailyAcceptableIntake: "",
  };
}

function beginEdit(row) {
  editingAdditiveId.value = row.additiveId;
  form.value = {
    name: row.name || "",
    purpose: row.purpose || "",
    dangerLevel: Number(row.dangerLevel || 3),
    dailyAcceptableIntake: row.dailyAcceptableIntake || "",
  };
}

function applyMissKeywordToForm(row) {
  const text = String(row?.keyword || "").trim();
  if (!text) return;
  form.value.name = text;
  editingAdditiveId.value = null;
}

async function loadAdditives() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const baseParams = {
      keyword: keyword.value || undefined,
      size: PAGE_SIZE,
    };
    let page = 0;
    let totalPages = 1;
    let totalElements = 0;
    const mergedItems = [];

    while (page < totalPages) {
      const response = await api.get("/admin/data/additives", {
        params: {
          ...baseParams,
          page,
        },
      });
      const data = response.data?.data ?? {};
      const pageInfo = data.pageInfo ?? data.page_info ?? {};
      mergedItems.push(...(data.items ?? []));
      totalPages = Number(pageInfo.totalPages ?? pageInfo.total_pages ?? 1);
      totalElements = Number(pageInfo.totalElements ?? pageInfo.total_elements ?? mergedItems.length);
      page += 1;
    }

    additivesPage.value = {
      items: mergedItems,
      pageInfo: {
        currentPage: 0,
        totalPages,
        totalElements,
        hasNext: false,
      },
    };
    resetVisibleRows();
  } catch (error) {
    errorMessage.value = error?.response?.data?.message ?? "첨가물 목록을 불러오지 못했습니다.";
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

async function saveAdditive() {
  errorMessage.value = "";
  successMessage.value = "";

  const payload = {
    name: form.value.name,
    purpose: form.value.purpose || null,
    dangerLevel: Number(form.value.dangerLevel || 3),
    dailyAcceptableIntake: form.value.dailyAcceptableIntake || null,
  };

  try {
    if (editingAdditiveId.value) {
      await api.patch(`/admin/data/additives/${editingAdditiveId.value}`, payload);
      successMessage.value = `첨가물(${editingAdditiveId.value})을 수정했습니다.`;
    } else {
      await api.post("/admin/data/additives", payload);
      successMessage.value = "첨가물을 등록했습니다.";
    }
    resetForm();
    await Promise.all([loadAdditives(), loadMissLogs()]);
  } catch (error) {
    errorMessage.value = error?.response?.data?.message ?? "첨가물 저장에 실패했습니다.";
  }
}

function dangerClass(level) {
  if (level >= 5) return "is-red";
  if (level >= 4) return "is-orange";
  if (level >= 3) return "is-yellow";
  return "is-green";
}

onMounted(async () => {
  if (route.query.prefill) {
    form.value.name = String(route.query.prefill);
  }
  await Promise.all([loadAdditives(), loadMissLogs()]);
});
</script>

<template>
  <main class="admin-dashboard-wrap">
    <header class="admin-top">
      <div>
        <p class="admin-eyebrow">Additives</p>
        <h1>첨가물 관리</h1>
      </div>
      <div class="admin-top-actions">
        <button class="admin-button secondary" @click="loadAdditives">새로고침</button>
      </div>
    </header>

    <article class="admin-card mt-4">
      <h2>{{ editingAdditiveId ? `첨가물 수정 #${editingAdditiveId}` : "첨가물 신규 등록" }}</h2>
      <div class="admin-form-grid mt-4 md:grid-cols-2">
        <label>
          <span class="admin-muted">첨가물명</span>
          <input v-model="form.name" class="admin-input" placeholder="예: 아스파탐" />
        </label>
        <label>
          <span class="admin-muted">용도</span>
          <input v-model="form.purpose" class="admin-input" placeholder="예: 감미료" />
        </label>
        <label>
          <span class="admin-muted">위험도(1~5)</span>
          <input v-model="form.dangerLevel" type="number" min="1" max="5" class="admin-input" />
        </label>
        <label>
          <span class="admin-muted">ADI</span>
          <input v-model="form.dailyAcceptableIntake" class="admin-input" placeholder="예: 40mg/kg/day" />
        </label>
      </div>
      <div class="mt-3 flex flex-wrap gap-2">
        <button class="admin-button" @click="saveAdditive">저장</button>
        <button class="admin-button secondary" @click="resetForm">초기화</button>
      </div>

      <p v-if="errorMessage" class="admin-error mt-3">{{ errorMessage }}</p>
      <p v-if="successMessage" class="admin-info mt-3">{{ successMessage }}</p>
    </article>

    <section class="mt-4 space-y-4">
      <article class="admin-card">
        <div class="admin-table-head">
          <h2>첨가물 목록</h2>
          <div class="admin-search-row">
            <input v-model="keyword" class="admin-input" placeholder="첨가물명/용도 검색" />
            <button class="admin-button secondary" :disabled="loading" @click="loadAdditives">조회</button>
          </div>
        </div>
        <p class="admin-muted mt-2">
          총 {{ additivesPage.pageInfo?.totalElements ?? sortedAdditives.length }}건 · 표시 {{ visibleAdditives.length }}건
        </p>

        <div class="mt-3">
          <table class="admin-table is-fixed">
            <thead>
              <tr>
                <th><button class="sort-head-button" @click="toggleSort('additiveId')">ID <span>{{ sortIndicator("additiveId") }}</span></button></th>
                <th><button class="sort-head-button" @click="toggleSort('name')">첨가물명 <span>{{ sortIndicator("name") }}</span></button></th>
                <th><button class="sort-head-button" @click="toggleSort('purpose')">용도 <span>{{ sortIndicator("purpose") }}</span></button></th>
                <th><button class="sort-head-button" @click="toggleSort('dangerLevel')">위험도 <span>{{ sortIndicator("dangerLevel") }}</span></button></th>
                <th>ADI</th>
                <th><button class="sort-head-button" @click="toggleSort('mappedFoodCount')">매핑 식품 <span>{{ sortIndicator("mappedFoodCount") }}</span></button></th>
                <th>작업</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in visibleAdditives" :key="row.additiveId">
                <td>{{ row.additiveId }}</td>
                <td>{{ row.name }}</td>
                <td>{{ row.purpose || "-" }}</td>
                <td>
                  <span class="admin-chip danger-chip" :class="dangerClass(Number(row.dangerLevel || 0))">
                    Lv.{{ row.dangerLevel }}
                  </span>
                </td>
                <td>{{ row.dailyAcceptableIntake || "-" }}</td>
                <td>{{ row.mappedFoodCount }}</td>
                <td>
                  <button class="admin-button secondary" @click="beginEdit(row)">수정</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="sortedAdditives.length > DEFAULT_VISIBLE_COUNT" class="mt-3 flex flex-wrap items-center gap-2">
          <button v-if="canShowMore" class="admin-button secondary" @click="showMoreRows">30개 더보기</button>
          <button v-if="canShowAll" class="admin-button secondary" @click="showAll">전체보기</button>
          <button v-if="canCollapse" class="admin-button secondary" @click="collapseRows">접기</button>
        </div>
      </article>

      <article class="admin-card">
        <div class="admin-table-head">
          <h2>미등록 요청(첨가물)</h2>
          <div class="admin-search-row">
            <input v-model="missKeyword" class="admin-input" placeholder="키워드 검색" />
            <button class="admin-button secondary" @click="loadMissLogs">조회</button>
          </div>
        </div>

        <div class="mt-3">
          <table class="admin-table is-fixed">
            <thead>
              <tr>
                <th style="width: 34%">키워드</th>
                <th style="width: 12%">요청 수</th>
                <th style="width: 24%">최근 시각</th>
                <th style="width: 30%">작업</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="miss in missPage.items || []" :key="miss.missId">
                <td>{{ miss.keyword }}</td>
                <td>{{ miss.hitCount }}</td>
                <td>{{ miss.lastSeenAt }}</td>
                <td>
                  <div class="admin-action-stack">
                    <button class="admin-button secondary" @click="applyMissKeywordToForm(miss)">등록폼 채우기</button>
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
