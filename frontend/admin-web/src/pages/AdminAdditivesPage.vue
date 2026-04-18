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
    const response = await api.get("/admin/data/additives", {
      params: {
        keyword: keyword.value || undefined,
        page: 0,
        size: 100,
      },
    });
    additivesPage.value = response.data?.data ?? { items: [] };
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
      <div class="mt-3 grid gap-3 md:grid-cols-2">
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

    <section class="admin-grid-2 mt-4">
      <article class="admin-card">
        <div class="admin-table-head">
          <h2>첨가물 목록</h2>
          <div class="admin-search-row">
            <input v-model="keyword" class="admin-input" placeholder="첨가물명/용도 검색" />
            <button class="admin-button secondary" :disabled="loading" @click="loadAdditives">조회</button>
          </div>
        </div>

        <div class="admin-search-row mt-3">
          <select v-model="sortBy" class="admin-input">
            <option value="additiveId">정렬: ID</option>
            <option value="name">정렬: 첨가물명</option>
            <option value="purpose">정렬: 용도</option>
            <option value="dangerLevel">정렬: 위험도</option>
            <option value="mappedFoodCount">정렬: 매핑 식품 수</option>
          </select>
          <select v-model="sortOrder" class="admin-input">
            <option value="asc">오름차순</option>
            <option value="desc">내림차순</option>
          </select>
        </div>

        <div class="mt-3 overflow-x-auto">
          <table class="admin-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>첨가물명</th>
                <th>용도</th>
                <th>위험도</th>
                <th>ADI</th>
                <th>매핑 식품</th>
                <th>작업</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in sortedAdditives" :key="row.additiveId">
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
      </article>

      <article class="admin-card">
        <div class="admin-table-head">
          <h2>미등록 요청(첨가물)</h2>
          <div class="admin-search-row">
            <input v-model="missKeyword" class="admin-input" placeholder="키워드 검색" />
            <button class="admin-button secondary" @click="loadMissLogs">조회</button>
          </div>
        </div>

        <div class="mt-3 overflow-x-auto">
          <table class="admin-table">
            <thead>
              <tr>
                <th>키워드</th>
                <th>요청 수</th>
                <th>최근 시각</th>
                <th>작업</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="miss in missPage.items || []" :key="miss.missId">
                <td>{{ miss.keyword }}</td>
                <td>{{ miss.hitCount }}</td>
                <td>{{ miss.lastSeenAt }}</td>
                <td>
                  <div class="inline-flex gap-1">
                    <button class="admin-button secondary" @click="applyMissKeywordToForm(miss)">등록폼 채우기</button>
                    <button class="admin-button secondary" @click="resolveMiss(miss.missId, true)">처리완료</button>
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
