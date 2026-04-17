<script setup>
import { computed, nextTick, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import api from "../api/client";
import { useAdminAuthStore } from "../stores/adminAuthStore";

const router = useRouter();
const authStore = useAdminAuthStore();

const loading = ref(false);
const errorMessage = ref("");
const infoMessage = ref("");
const summary = ref(null);
const usersPage = ref({ items: [] });
const foodsPage = ref({ items: [] });

const userKeyword = ref("");
const foodKeyword = ref("");
const selectedMetric = ref("totalUsers");
const userRoleFilter = ref("");
const userStatusFilter = ref("");

const usersSectionRef = ref(null);
const foodsSectionRef = ref(null);

const cards = computed(() => {
  const s = summary.value;
  if (!s) return [];

  return [
    { key: "totalUsers", title: "전체 사용자", value: s.totalUsers, type: "user", role: "", status: "" },
    { key: "activeUsers", title: "활성 사용자", value: s.activeUsers, type: "user", role: "", status: "ACTIVE" },
    { key: "premiumUsers", title: "프리미엄", value: s.premiumUsers, type: "user", role: "PREMIUM", status: "" },
    { key: "suspendedUsers", title: "정지 사용자", value: s.suspendedUsers, type: "user", role: "", status: "SUSPENDED" },
    { key: "foodCount", title: "식품 마스터", value: s.foodCount, type: "food" },
    { key: "additiveCount", title: "첨가물 마스터", value: s.additiveCount, type: "summary" },
    { key: "todayMealLogs", title: "오늘 식단 기록", value: s.todayMealLogs, type: "summary" },
    { key: "weeklyMealLogs", title: "최근 7일 식단 기록", value: s.weeklyMealLogs, type: "summary" },
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
  if (!response) return;
  summary.value = response.data?.data ?? null;
}

async function loadUsers() {
  const response = await withAuthGuard(() =>
    api.get("/admin/data/users", {
      params: {
        keyword: userKeyword.value || undefined,
        role: userRoleFilter.value || undefined,
        status: userStatusFilter.value || undefined,
        page: 0,
        size: 30,
      },
    })
  );
  if (!response) return;
  usersPage.value = response.data?.data ?? { items: [] };
}

async function loadFoods() {
  const response = await withAuthGuard(() =>
    api.get("/admin/data/foods", {
      params: {
        keyword: foodKeyword.value || undefined,
        page: 0,
        size: 30,
      },
    })
  );
  if (!response) return;
  foodsPage.value = response.data?.data ?? { items: [] };
}

async function loadAll() {
  loading.value = true;
  errorMessage.value = "";
  infoMessage.value = "";
  try {
    await Promise.all([loadSummary(), loadUsers(), loadFoods()]);
  } catch (error) {
    errorMessage.value = error?.response?.data?.message ?? "관리자 데이터를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}

async function onMetricClick(card) {
  selectedMetric.value = card.key;
  infoMessage.value = "";

  if (card.type === "user") {
    userRoleFilter.value = card.role || "";
    userStatusFilter.value = card.status || "";
    await loadUsers();
    await nextTick();
    usersSectionRef.value?.scrollIntoView({ behavior: "smooth", block: "start" });
    return;
  }

  if (card.type === "food") {
    await loadFoods();
    await nextTick();
    foodsSectionRef.value?.scrollIntoView({ behavior: "smooth", block: "start" });
    return;
  }

  infoMessage.value = `${card.title}는 상단 요약 지표입니다. 상세 데이터는 사용자/식품 섹션에서 확인 가능합니다.`;
}

async function resetUserFilter() {
  userRoleFilter.value = "";
  userStatusFilter.value = "";
  selectedMetric.value = "totalUsers";
  await loadUsers();
}

async function logout() {
  await authStore.logout();
  router.push("/login");
}

onMounted(async () => {
  await loadAll();
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
        <button class="admin-button secondary" :disabled="loading" @click="loadAll">
          {{ loading ? "새로고침 중..." : "새로고침" }}
        </button>
        <button class="admin-button" @click="logout">로그아웃</button>
      </div>
    </header>

    <p v-if="errorMessage" class="admin-error mt-3">{{ errorMessage }}</p>
    <p v-if="infoMessage" class="admin-info mt-3">{{ infoMessage }}</p>

    <section v-if="loading && !summary" class="admin-grid mt-4">
      <article v-for="n in 8" :key="`skeleton-${n}`" class="admin-card skeleton h-24" />
    </section>

    <template v-else>
      <section class="admin-grid mt-4">
        <button
          v-for="item in cards"
          :key="item.key"
          class="admin-card metric-card metric-button"
          :class="selectedMetric === item.key ? 'is-active' : ''"
          @click="onMetricClick(item)"
        >
          <p class="metric-title">{{ item.title }}</p>
          <p class="metric-value">{{ item.value }}</p>
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
            <div class="stat-box">
              <p>Policy 위반 로그</p>
              <strong>{{ summary?.policyViolationLogs ?? 0 }}</strong>
            </div>
            <div class="stat-box">
              <p>Hallucination 로그</p>
              <strong>{{ summary?.hallucinationLogs ?? 0 }}</strong>
            </div>
          </div>
        </article>
      </section>

      <article ref="usersSectionRef" class="admin-card mt-4">
        <div class="admin-table-head">
          <div>
            <h2>사용자 정보</h2>
            <p class="admin-muted">상단 지표를 클릭하면 조건에 맞는 사용자 목록으로 필터링됩니다.</p>
          </div>
          <div class="admin-search-row">
            <input v-model="userKeyword" class="admin-input" placeholder="이메일 검색" />
            <button class="admin-button secondary" @click="loadUsers">조회</button>
            <button class="admin-button secondary" @click="resetUserFilter">필터 해제</button>
          </div>
        </div>

        <div class="mt-3 flex flex-wrap gap-2 text-xs">
          <span class="admin-chip">role: {{ userRoleFilter || 'ALL' }}</span>
          <span class="admin-chip">status: {{ userStatusFilter || 'ALL' }}</span>
          <span class="admin-chip">rows: {{ usersPage.items?.length || 0 }}</span>
        </div>

        <div class="mt-3 overflow-x-auto">
          <table class="admin-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>이메일</th>
                <th>권한</th>
                <th>상태</th>
                <th>목표</th>
                <th>강도</th>
                <th>알레르기</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in usersPage.items || []" :key="user.userId">
                <td>{{ user.userId }}</td>
                <td>{{ user.email }}</td>
                <td>{{ user.role }}</td>
                <td>{{ user.status }}</td>
                <td>{{ user.goal }}</td>
                <td>{{ user.strictness }}</td>
                <td>{{ (user.allergies || []).join(', ') || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <article ref="foodsSectionRef" class="admin-card mt-4">
        <div class="admin-table-head">
          <div>
            <h2>식품 정보</h2>
            <p class="admin-muted">식품명/카테고리/제조사/칼로리를 조회합니다.</p>
          </div>
          <div class="admin-search-row">
            <input v-model="foodKeyword" class="admin-input" placeholder="식품명 검색" />
            <button class="admin-button secondary" @click="loadFoods">조회</button>
          </div>
        </div>

        <div class="mt-3 overflow-x-auto">
          <table class="admin-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>식품명</th>
                <th>카테고리</th>
                <th>제조사</th>
                <th>칼로리</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="food in foodsPage.items || []" :key="food.foodId">
                <td>{{ food.foodId }}</td>
                <td>{{ food.name }}</td>
                <td>{{ food.category }}</td>
                <td>{{ food.manufacturer || '-' }}</td>
                <td>{{ Number(food.calories || 0).toFixed(0) }} kcal</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>
    </template>
  </main>
</template>
