<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import api from "../api/client";
import { useAdminAuthStore } from "../stores/adminAuthStore";

const route = useRoute();
const router = useRouter();
const authStore = useAdminAuthStore();

const loading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");
const usersPage = ref({ items: [] });

const keyword = ref("");
const role = ref("");
const status = ref("");

const sortBy = ref("userId");
const sortOrder = ref("asc");

const editingUserId = ref(null);
const editForm = ref({ role: "USER", status: "ACTIVE", goal: "MAINTAIN", strictness: "MEDIUM", allergies: "" });

const ROLE_ORDER = { GUEST: 0, USER: 1, PREMIUM: 2, ADMIN: 3 };
const STATUS_ORDER = { ACTIVE: 0, SUSPENDED: 1, WITHDRAWN: 2 };
const GOAL_ORDER = { FAT_LOSS: 0, MAINTAIN: 1, BULK_UP: 2 };
const STRICTNESS_ORDER = { LOW: 0, MEDIUM: 1, HIGH: 2 };

const sortedUsers = computed(() => {
  const rows = [...(usersPage.value?.items || [])];
  const factor = sortOrder.value === "asc" ? 1 : -1;

  rows.sort((a, b) => {
    const key = sortBy.value;
    if (key === "userId") {
      return (Number(a.userId || 0) - Number(b.userId || 0)) * factor;
    }
    if (key === "role") {
      return ((ROLE_ORDER[a.role] ?? 99) - (ROLE_ORDER[b.role] ?? 99)) * factor;
    }
    if (key === "status") {
      return ((STATUS_ORDER[a.status] ?? 99) - (STATUS_ORDER[b.status] ?? 99)) * factor;
    }
    if (key === "goal") {
      return ((GOAL_ORDER[a.goal] ?? 99) - (GOAL_ORDER[b.goal] ?? 99)) * factor;
    }
    if (key === "strictness") {
      return ((STRICTNESS_ORDER[a.strictness] ?? 99) - (STRICTNESS_ORDER[b.strictness] ?? 99)) * factor;
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

function applyRouteQuery() {
  keyword.value = route.query.keyword ? String(route.query.keyword) : "";
  role.value = route.query.role ? String(route.query.role) : "";
  status.value = route.query.status ? String(route.query.status) : "";
}

async function loadUsers() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const response = await withAuthGuard(() =>
      api.get("/admin/data/users", {
        params: {
          keyword: keyword.value || undefined,
          role: role.value || undefined,
          status: status.value || undefined,
          page: 0,
          size: 100,
        },
      })
    );
    if (!response) return;
    usersPage.value = response.data?.data ?? { items: [] };
  } catch (error) {
    errorMessage.value = error?.response?.data?.message ?? "사용자 목록을 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}

function beginEdit(user) {
  editingUserId.value = user.userId;
  editForm.value = {
    role: user.role || "USER",
    status: user.status || "ACTIVE",
    goal: user.goal || "MAINTAIN",
    strictness: user.strictness || "MEDIUM",
    allergies: (user.allergies || []).join(", "),
  };
}

function cancelEdit() {
  editingUserId.value = null;
}

async function saveEdit(userId) {
  successMessage.value = "";
  errorMessage.value = "";

  try {
    await withAuthGuard(() =>
      api.patch(`/admin/users/${userId}`, {
        role: editForm.value.role,
        status: editForm.value.status,
        goal: editForm.value.goal,
        strictness: editForm.value.strictness,
        allergies: String(editForm.value.allergies || "")
          .split(",")
          .map((v) => v.trim())
          .filter(Boolean),
      })
    );
    successMessage.value = `사용자(${userId}) 정보를 수정했습니다.`;
    cancelEdit();
    await loadUsers();
  } catch (error) {
    errorMessage.value = error?.response?.data?.message ?? "사용자 수정에 실패했습니다.";
  }
}

async function quickSetStatus(user, nextStatus) {
  successMessage.value = "";
  errorMessage.value = "";
  try {
    await withAuthGuard(() =>
      api.patch(`/admin/users/${user.userId}`, {
        role: user.role,
        status: nextStatus,
        goal: user.goal,
        strictness: user.strictness,
        allergies: user.allergies || [],
      })
    );
    successMessage.value = `사용자(${user.userId}) 상태를 ${statusLabel(nextStatus)}(으)로 변경했습니다.`;
    await loadUsers();
  } catch (error) {
    errorMessage.value = error?.response?.data?.message ?? "상태 변경에 실패했습니다.";
  }
}

function updateRouteQuery() {
  router.replace({
    name: "admin-users",
    query: {
      keyword: keyword.value || undefined,
      role: role.value || undefined,
      status: status.value || undefined,
    },
  });
}

function roleLabel(value) {
  const map = {
    ADMIN: "관리자",
    PREMIUM: "프리미엄",
    USER: "일반회원",
    GUEST: "비회원",
  };
  return map[value] ?? value;
}

function statusLabel(value) {
  const map = {
    ACTIVE: "정상",
    SUSPENDED: "정지",
    WITHDRAWN: "탈퇴",
  };
  return map[value] ?? value;
}

function goalLabel(value) {
  const map = {
    MAINTAIN: "건강 유지",
    FAT_LOSS: "체지방 감량",
    BULK_UP: "근육량 증가",
  };
  return map[value] ?? value;
}

function strictnessLabel(value) {
  const map = {
    LOW: "낮음",
    MEDIUM: "중간",
    HIGH: "높음",
  };
  return map[value] ?? value;
}

watch(
  () => route.query,
  async () => {
    applyRouteQuery();
    await loadUsers();
  }
);

onMounted(async () => {
  applyRouteQuery();
  await loadUsers();
});
</script>

<template>
  <main class="admin-dashboard-wrap">
    <header class="admin-top">
      <div>
        <p class="admin-eyebrow">Users</p>
        <h1>사용자 관리</h1>
      </div>
      <div class="admin-top-actions">
        <button class="admin-button secondary" @click="loadUsers">새로고침</button>
      </div>
    </header>

    <article class="admin-card mt-4">
      <div class="admin-search-row">
        <input v-model="keyword" class="admin-input" placeholder="이메일 검색" />
        <select v-model="role" class="admin-input">
          <option value="">권한 전체</option>
          <option value="USER">일반회원</option>
          <option value="PREMIUM">프리미엄</option>
          <option value="ADMIN">관리자</option>
          <option value="GUEST">비회원</option>
        </select>
        <select v-model="status" class="admin-input">
          <option value="">상태 전체</option>
          <option value="ACTIVE">정상</option>
          <option value="SUSPENDED">정지</option>
          <option value="WITHDRAWN">탈퇴</option>
        </select>
        <button class="admin-button secondary" @click="updateRouteQuery">조회</button>
      </div>

      <p v-if="errorMessage" class="admin-error mt-3">{{ errorMessage }}</p>
      <p v-if="successMessage" class="admin-info mt-3">{{ successMessage }}</p>

      <div class="mt-3 overflow-x-auto">
        <table class="admin-table">
          <thead>
            <tr>
              <th><button class="sort-head-button" @click="toggleSort('userId')">ID <span>{{ sortIndicator("userId") }}</span></button></th>
              <th><button class="sort-head-button" @click="toggleSort('email')">이메일 <span>{{ sortIndicator("email") }}</span></button></th>
              <th><button class="sort-head-button" @click="toggleSort('role')">권한 <span>{{ sortIndicator("role") }}</span></button></th>
              <th><button class="sort-head-button" @click="toggleSort('status')">상태 <span>{{ sortIndicator("status") }}</span></button></th>
              <th><button class="sort-head-button" @click="toggleSort('goal')">목표 <span>{{ sortIndicator("goal") }}</span></button></th>
              <th><button class="sort-head-button" @click="toggleSort('strictness')">강도 <span>{{ sortIndicator("strictness") }}</span></button></th>
              <th>알레르기</th>
              <th>액션</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in sortedUsers" :key="user.userId">
              <td>{{ user.userId }}</td>
              <td>{{ user.email }}</td>
              <td>{{ roleLabel(user.role) }} ({{ user.role }})</td>
              <td>{{ statusLabel(user.status) }} ({{ user.status }})</td>
              <td>{{ goalLabel(user.goal) }} ({{ user.goal }})</td>
              <td>{{ strictnessLabel(user.strictness) }} ({{ user.strictness }})</td>
              <td>{{ (user.allergies || []).join(", ") || "-" }}</td>
              <td>
                <div class="inline-flex gap-1">
                  <button class="admin-button secondary" @click="beginEdit(user)">수정</button>
                  <button
                    v-if="user.status !== 'SUSPENDED'"
                    class="admin-button secondary"
                    @click="quickSetStatus(user, 'SUSPENDED')"
                  >
                    정지
                  </button>
                  <button
                    v-else
                    class="admin-button secondary"
                    @click="quickSetStatus(user, 'ACTIVE')"
                  >
                    해제
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <article v-if="editingUserId" class="admin-card mt-4">
        <h2>사용자 수정: {{ editingUserId }}</h2>
        <div class="mt-3 grid gap-3 md:grid-cols-2">
          <label>
            <span class="admin-muted">권한</span>
            <select v-model="editForm.role" class="admin-input">
              <option>USER</option>
              <option>PREMIUM</option>
              <option>ADMIN</option>
              <option>GUEST</option>
            </select>
          </label>
          <label>
            <span class="admin-muted">상태</span>
            <select v-model="editForm.status" class="admin-input">
              <option>ACTIVE</option>
              <option>SUSPENDED</option>
              <option>WITHDRAWN</option>
            </select>
          </label>
          <label>
            <span class="admin-muted">목표</span>
            <select v-model="editForm.goal" class="admin-input">
              <option>MAINTAIN</option>
              <option>FAT_LOSS</option>
              <option>BULK_UP</option>
            </select>
          </label>
          <label>
            <span class="admin-muted">강도</span>
            <select v-model="editForm.strictness" class="admin-input">
              <option>LOW</option>
              <option>MEDIUM</option>
              <option>HIGH</option>
            </select>
          </label>
          <label class="md:col-span-2">
            <span class="admin-muted">알레르기(쉼표 구분)</span>
            <input v-model="editForm.allergies" class="admin-input" />
          </label>
        </div>

        <div class="mt-3 flex gap-2">
          <button class="admin-button" @click="saveEdit(editingUserId)">저장</button>
          <button class="admin-button secondary" @click="cancelEdit">취소</button>
        </div>
      </article>
    </article>
  </main>
</template>
