<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import api from "../api/client";
import { useAdminAuthStore } from "../stores/adminAuthStore";

const router = useRouter();
const authStore = useAdminAuthStore();

const loading = ref(false);
const errorMessage = ref("");
const summary = ref(null);
let summaryRequestId = 0;

const cards = computed(() => {
  const s = summary.value;
  if (!s) return [];
  return [
    { key: "totalUsers", title: "전체 사용자", value: s.totalUsers, route: { name: "admin-users" } },
    {
      key: "activeUsers",
      title: "활성 사용자",
      value: s.activeUsers,
      route: { name: "admin-users", query: { status: "ACTIVE" } },
    },
    {
      key: "premiumUsers",
      title: "프리미엄",
      value: s.premiumUsers,
      route: { name: "admin-users", query: { role: "PREMIUM" } },
    },
    {
      key: "suspendedUsers",
      title: "정지 사용자",
      value: s.suspendedUsers,
      route: { name: "admin-users", query: { status: "SUSPENDED" } },
    },
    { key: "foodCount", title: "식품 마스터", value: s.foodCount, route: { name: "admin-foods" } },
    { key: "additiveCount", title: "첨가물 마스터", value: s.additiveCount, route: { name: "admin-additives" } },
    { key: "todayMealLogs", title: "오늘 식단 기록", value: s.todayMealLogs, route: null },
    { key: "weeklyMealLogs", title: "최근 7일 식단 기록", value: s.weeklyMealLogs, route: null },
  ];
});

async function withAuthGuard(fn) {
  try {
    return await fn();
  } catch (error) {
    if (error?.response?.status === 401 || error?.response?.status === 403) {
      authStore.clear();
      router.push("/login");
      return null;
    }
    throw error;
  }
}

async function loadSummary() {
  const response = await withAuthGuard(() => api.get("/admin/dashboard/summary"));
  if (!response) return null;
  return response.data?.data ?? null;
}

async function refresh() {
  const requestId = ++summaryRequestId;
  loading.value = true;
  errorMessage.value = "";
  try {
    const nextSummary = await loadSummary();
    if (requestId !== summaryRequestId) return;
    summary.value = nextSummary;
  } catch (error) {
    if (requestId !== summaryRequestId) return;
    errorMessage.value = error?.response?.data?.message ?? "대시보드 데이터를 불러오지 못했습니다.";
  } finally {
    if (requestId !== summaryRequestId) return;
    loading.value = false;
  }
}

function openRoute(card) {
  if (card.route) {
    router.push(card.route);
  }
}

onMounted(async () => {
  await refresh();
});
</script>

<template>
  <main class="admin-dashboard-wrap">
    <header class="admin-top">
      <div>
        <p class="admin-eyebrow">ChemiLog CMS</p>
        <h1>운영 대시보드</h1>
      </div>
      <div class="admin-top-actions">
        <button class="admin-button secondary" :disabled="loading" @click="refresh">
          {{ loading ? "새로고침 중..." : "새로고침" }}
        </button>
      </div>
    </header>

    <p v-if="errorMessage" class="admin-error mt-3">{{ errorMessage }}</p>

    <section v-if="loading && !summary" class="admin-grid mt-4">
      <article v-for="n in 8" :key="`skeleton-${n}`" class="admin-card skeleton h-24" />
    </section>

    <template v-else>
      <section class="admin-grid mt-4">
        <button
          v-for="item in cards"
          :key="item.key"
          class="admin-card metric-card metric-button"
          :class="item.route ? '' : 'is-disabled'"
          @click="openRoute(item)"
        >
          <p class="metric-title">{{ item.title }}</p>
          <p class="metric-value">{{ item.value }}</p>
          <p class="metric-hint">{{ item.route ? "클릭해서 상세 보기" : "요약 지표" }}</p>
        </button>
      </section>

      <section class="admin-grid-2 mt-4">
        <article class="admin-card">
          <h2>카테고리 분포</h2>
          <p class="admin-muted">카테고리별 식품 등록 수</p>

          <div v-if="summary?.categoryStats?.length" class="mt-3 space-y-2">
            <div v-for="item in summary.categoryStats" :key="item.category" class="category-row">
              <div class="row-head">
                <span>{{ item.category }}</span>
                <strong>{{ item.itemCount }}</strong>
              </div>
              <div class="bar-bg">
                <div
                  class="bar-fill"
                  :style="{
                    width: `${Math.max(6, (item.itemCount / (summary.categoryStats[0]?.itemCount || 1)) * 100)}%`,
                  }"
                />
              </div>
            </div>
          </div>

          <p v-else class="admin-muted mt-3">표시할 카테고리 데이터가 없습니다.</p>
        </article>

        <article class="admin-card">
          <h2>AI 거버넌스 로그 수</h2>
          <p class="admin-muted">정책 위반/환각 로그 누적</p>

          <div class="mt-4 grid gap-3 md:grid-cols-2">
            <button
              class="stat-box metric-button"
              @click="openRoute({ route: { name: 'admin-logs', query: { tab: 'policy' } } })"
            >
              <p>Policy 위반 로그</p>
              <strong>{{ summary?.policyViolationLogs ?? 0 }}</strong>
              <p class="metric-hint">클릭해서 상세 보기</p>
            </button>
            <button
              class="stat-box metric-button"
              @click="openRoute({ route: { name: 'admin-logs', query: { tab: 'hallucination' } } })"
            >
              <p>Hallucination 로그</p>
              <strong>{{ summary?.hallucinationLogs ?? 0 }}</strong>
              <p class="metric-hint">클릭해서 상세 보기</p>
            </button>
          </div>
        </article>
      </section>
    </template>
  </main>
</template>
