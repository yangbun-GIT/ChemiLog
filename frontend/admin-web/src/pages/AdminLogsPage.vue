<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import api from "../api/client";

const route = useRoute();
const router = useRouter();

const tab = ref("policy");
const loading = ref(false);
const errorMessage = ref("");

const policyFilters = ref({
  keyword: "",
  category: "",
});
const hallucinationKeyword = ref("");

const policyRows = ref([]);
const hallucinationRows = ref([]);

const policySortBy = ref("createdAt");
const policySortOrder = ref("desc");
const hallucinationSortBy = ref("createdAt");
const hallucinationSortOrder = ref("desc");

function togglePolicySort(column) {
  if (policySortBy.value === column) {
    policySortOrder.value = policySortOrder.value === "asc" ? "desc" : "asc";
    return;
  }
  policySortBy.value = column;
  policySortOrder.value = "asc";
}

function policySortIndicator(column) {
  if (policySortBy.value !== column) return "↕";
  return policySortOrder.value === "asc" ? "↑" : "↓";
}

function toggleHallucinationSort(column) {
  if (hallucinationSortBy.value === column) {
    hallucinationSortOrder.value = hallucinationSortOrder.value === "asc" ? "desc" : "asc";
    return;
  }
  hallucinationSortBy.value = column;
  hallucinationSortOrder.value = "asc";
}

function hallucinationSortIndicator(column) {
  if (hallucinationSortBy.value !== column) return "↕";
  return hallucinationSortOrder.value === "asc" ? "↑" : "↓";
}

function asDateNumber(value) {
  const time = new Date(value || "").getTime();
  return Number.isFinite(time) ? time : 0;
}

const sortedPolicyRows = computed(() => {
  const rows = [...policyRows.value];
  const factor = policySortOrder.value === "asc" ? 1 : -1;

  rows.sort((a, b) => {
    const key = policySortBy.value;
    const left = a?.[key];
    const right = b?.[key];

    if (key === "confidenceScore") {
      return (Number(left || 0) - Number(right || 0)) * factor;
    }
    if (key === "createdAt") {
      return (asDateNumber(left) - asDateNumber(right)) * factor;
    }
    return String(left || "").localeCompare(String(right || ""), "ko") * factor;
  });

  return rows;
});

const sortedHallucinationRows = computed(() => {
  const rows = [...hallucinationRows.value];
  const factor = hallucinationSortOrder.value === "asc" ? 1 : -1;

  rows.sort((a, b) => {
    const key = hallucinationSortBy.value;
    const left = a?.[key];
    const right = b?.[key];
    if (key === "createdAt") {
      return (asDateNumber(left) - asDateNumber(right)) * factor;
    }
    return String(left || "").localeCompare(String(right || ""), "ko") * factor;
  });

  return rows;
});

function applyRouteTab() {
  const candidate = String(route.query.tab || "policy");
  tab.value = candidate === "hallucination" ? "hallucination" : "policy";
}

async function loadPolicyLogs() {
  const response = await api.get("/admin/data/violation-logs", {
    params: {
      keyword: policyFilters.value.keyword || undefined,
      category: policyFilters.value.category || undefined,
      page: 0,
      size: 100,
    },
  });
  policyRows.value = response.data?.data?.items ?? [];
}

async function loadHallucinationLogs() {
  const response = await api.get("/admin/data/hallucination-logs", {
    params: {
      keyword: hallucinationKeyword.value || undefined,
      page: 0,
      size: 100,
    },
  });
  hallucinationRows.value = response.data?.data?.items ?? [];
}

async function refresh() {
  loading.value = true;
  errorMessage.value = "";
  try {
    if (tab.value === "policy") {
      await loadPolicyLogs();
    } else {
      await loadHallucinationLogs();
    }
  } catch (error) {
    errorMessage.value = error?.response?.data?.message ?? "로그 데이터를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}

function setTab(nextTab) {
  tab.value = nextTab;
  router.replace({ name: "admin-logs", query: { tab: nextTab } });
}

function violationLabel(value) {
  const map = {
    PRO_ANA: "섭식장애 미화",
    SELF_HARM: "자해 위험",
    JAILBREAK: "프롬프트 우회",
    OUT_OF_DOMAIN: "도메인 외 질문",
  };
  return map[value] ?? value;
}

watch(
  () => route.query.tab,
  async () => {
    applyRouteTab();
    await refresh();
  }
);

onMounted(async () => {
  applyRouteTab();
  await refresh();
});
</script>

<template>
  <main class="admin-dashboard-wrap">
    <header class="admin-top">
      <div>
        <p class="admin-eyebrow">AI Logs</p>
        <h1>Policy / Hallucination 로그</h1>
      </div>
      <div class="admin-top-actions">
        <button class="admin-button secondary" :disabled="loading" @click="refresh">새로고침</button>
      </div>
    </header>

    <article class="admin-card mt-4">
      <div class="flex flex-wrap gap-2">
        <button
          class="admin-button secondary"
          :class="tab === 'policy' ? 'is-selected' : ''"
          @click="setTab('policy')"
        >
          Policy 위반 로그
        </button>
        <button
          class="admin-button secondary"
          :class="tab === 'hallucination' ? 'is-selected' : ''"
          @click="setTab('hallucination')"
        >
          Hallucination 로그
        </button>
      </div>

      <p v-if="errorMessage" class="admin-error mt-3">{{ errorMessage }}</p>

      <template v-if="tab === 'policy'">
        <div class="admin-search-row mt-3">
          <input v-model="policyFilters.keyword" class="admin-input" placeholder="입력 원문 검색" />
          <select v-model="policyFilters.category" class="admin-input">
            <option value="">카테고리 전체</option>
            <option value="PRO_ANA">섭식장애 미화</option>
            <option value="SELF_HARM">자해 위험</option>
            <option value="JAILBREAK">프롬프트 우회</option>
            <option value="OUT_OF_DOMAIN">도메인 외 질문</option>
          </select>
          <button class="admin-button secondary" @click="refresh">조회</button>
        </div>

        <div class="mt-3 overflow-x-auto">
          <table class="admin-table">
            <thead>
              <tr>
                <th><button class="sort-head-button" @click="togglePolicySort('createdAt')">시각 <span>{{ policySortIndicator("createdAt") }}</span></button></th>
                <th><button class="sort-head-button" @click="togglePolicySort('userEmailMasked')">사용자 <span>{{ policySortIndicator("userEmailMasked") }}</span></button></th>
                <th><button class="sort-head-button" @click="togglePolicySort('violationCategory')">카테고리 <span>{{ policySortIndicator("violationCategory") }}</span></button></th>
                <th><button class="sort-head-button" @click="togglePolicySort('confidenceScore')">신뢰도 <span>{{ policySortIndicator("confidenceScore") }}</span></button></th>
                <th>입력 요약</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in sortedPolicyRows" :key="row.logId">
                <td>{{ row.createdAt }}</td>
                <td>{{ row.userEmailMasked || `USER#${row.userId ?? '-'}` }}</td>
                <td>{{ violationLabel(row.violationCategory) }} ({{ row.violationCategory }})</td>
                <td>{{ Number(row.confidenceScore || 0).toFixed(2) }}</td>
                <td>{{ row.inputPreview }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>

      <template v-else>
        <div class="admin-search-row mt-3">
          <input v-model="hallucinationKeyword" class="admin-input" placeholder="실패 사유/응답 검색" />
          <button class="admin-button secondary" @click="refresh">조회</button>
        </div>

        <div class="mt-3 overflow-x-auto">
          <table class="admin-table">
            <thead>
              <tr>
                <th><button class="sort-head-button" @click="toggleHallucinationSort('createdAt')">시각 <span>{{ hallucinationSortIndicator("createdAt") }}</span></button></th>
                <th><button class="sort-head-button" @click="toggleHallucinationSort('modelVersion')">모델 버전 <span>{{ hallucinationSortIndicator("modelVersion") }}</span></button></th>
                <th><button class="sort-head-button" @click="toggleHallucinationSort('failedReason')">실패 사유 <span>{{ hallucinationSortIndicator("failedReason") }}</span></button></th>
                <th>프롬프트 요약</th>
                <th>응답 요약</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in sortedHallucinationRows" :key="row.logId">
                <td>{{ row.createdAt }}</td>
                <td>{{ row.modelVersion }}</td>
                <td>{{ row.failedReason }}</td>
                <td>{{ row.promptPreview }}</td>
                <td>{{ row.responsePreview }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
    </article>
  </main>
</template>
