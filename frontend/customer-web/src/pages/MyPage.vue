<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useAuthStore } from "../stores/authStore";
import { useCartStore } from "../stores/cartStore";
import { useProfileStore } from "../stores/profileStore";

const authStore = useAuthStore();
const cartStore = useCartStore();
const profileStore = useProfileStore();

const loading = ref(false);
const saving = ref(false);
const errorMessage = ref("");
const successMessage = ref("");

const currentMonth = ref(new Date(new Date().getFullYear(), new Date().getMonth(), 1));
const monthlyDays = ref([]);

const editGoal = ref("MAINTAIN");
const editStrictness = ref("MEDIUM");
const editAllergiesText = ref("");

const rhythm = computed(() => cartStore.weeklyRhythm);
const syncedDays = computed(() => cartStore.syncedDaysLast28);
const draftDays = computed(() => rhythm.value.filter((day) => day.status === "DRAFT").length);
const avgCalories = computed(() => {
  const synced = (cartStore.history || []).filter((day) => Number(day.totalCalories || 0) > 0);
  if (!synced.length) return 0;
  const total = synced.reduce((sum, day) => sum + Number(day.totalCalories || 0), 0);
  return total / synced.length;
});
const monthLabel = computed(
  () => `${currentMonth.value.getFullYear()}년 ${String(currentMonth.value.getMonth() + 1).padStart(2, "0")}월`
);
const monthlySyncedCount = computed(
  () => monthlyDays.value.filter((day) => day.inMonth && Number(day.totalCalories || 0) > 0).length
);

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

function toDateString(date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function scoreClassByCalories(calories) {
  const value = Number(calories || 0);
  if (value <= 0) return "heat-0";
  if (value > 1100) return "heat-1";
  if (value > 800) return "heat-2";
  if (value > 550) return "heat-3";
  return "heat-4";
}

function compactAdditives(list) {
  const values = [...new Set((list || []).map((v) => String(v || "").replace(/^주의:\s*/u, "").trim()).filter(Boolean))];
  if (!values.length) return "";
  if (values.length === 1) return values[0];
  if (values.length === 2) return `${values[0]} · ${values[1]}`;
  return `${values[0]} +${values.length - 1}`;
}

function accountStatusHint(role, status) {
  const roleGuide = role === "ADMIN"
    ? "ADMIN: 관리자 권한 계정입니다."
    : role === "PREMIUM"
      ? "PREMIUM: 프리미엄 멘토링 이용 계정입니다."
      : role === "USER"
        ? "USER: 일반 회원 계정입니다."
        : "GUEST: 비회원 상태 계정입니다.";

  const statusGuide = status === "ACTIVE"
    ? "ACTIVE: 정상 이용 상태입니다."
    : status === "SUSPENDED"
      ? "SUSPENDED: 관리자에 의해 일시 정지된 상태입니다."
      : status === "WITHDRAWN"
        ? "WITHDRAWN: 탈퇴 처리된 상태입니다."
        : "상태 정보 없음";

  return `${roleGuide}\n${statusGuide}`;
}

async function loadMonthHistory() {
  if (!authStore.isAuthenticated) {
    monthlyDays.value = buildMonthGrid([], currentMonth.value);
    return;
  }

  loading.value = true;
  errorMessage.value = "";
  try {
    const monthStart = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth(), 1);
    const monthEnd = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() + 1, 0);
    const days = await cartStore.fetchHistory(toDateString(monthStart), toDateString(monthEnd));
    cartStore.applyHistory(days);
    monthlyDays.value = buildMonthGrid(days, currentMonth.value);
  } catch (error) {
    monthlyDays.value = buildMonthGrid([], currentMonth.value);
    errorMessage.value = error?.response?.data?.message ?? "월간 식단 기록을 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}

function buildMonthGrid(days, monthDate) {
  const map = new Map();
  for (const day of days || []) {
    if (!day?.date) continue;
    map.set(day.date, day);
  }

  const year = monthDate.getFullYear();
  const month = monthDate.getMonth();
  const first = new Date(year, month, 1);
  const last = new Date(year, month + 1, 0);

  const startOffset = first.getDay();
  const endDay = last.getDate();
  const cells = [];

  for (let i = 0; i < startOffset; i += 1) {
    cells.push({ key: `prev-${i}`, inMonth: false, date: null, day: "", totalCalories: 0, scoreClass: "heat-0", topAdditives: [] });
  }

  for (let day = 1; day <= endDay; day += 1) {
    const date = new Date(year, month, day);
    const dateStr = toDateString(date);
    const found = map.get(dateStr);
    const calories = Number(found?.totalCalories || 0);
    cells.push({
      key: dateStr,
      inMonth: true,
      date: dateStr,
      day,
      totalCalories: calories,
      itemCount: Number(found?.itemCount || 0),
      scoreClass: scoreClassByCalories(calories),
      topAdditives: found?.topAdditives || [],
    });
  }

  while (cells.length % 7 !== 0) {
    const nextIndex = cells.length;
    cells.push({ key: `next-${nextIndex}`, inMonth: false, date: null, day: "", totalCalories: 0, scoreClass: "heat-0", topAdditives: [] });
  }
  return cells;
}

function prevMonth() {
  currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() - 1, 1);
}

function nextMonth() {
  currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() + 1, 1);
}

async function refreshProfileData() {
  if (!authStore.isAuthenticated) {
    return;
  }

  loading.value = true;
  errorMessage.value = "";
  try {
    await profileStore.fetchMe();
    const days = await cartStore.fetchHistory();
    cartStore.applyHistory(days);
    editGoal.value = profileStore.goal;
    editStrictness.value = profileStore.strictness;
    editAllergiesText.value = profileStore.allergies.join(", ");
  } catch (error) {
    errorMessage.value = error?.response?.data?.message ?? "마이페이지 데이터를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
}

async function saveProfile() {
  saving.value = true;
  errorMessage.value = "";
  successMessage.value = "";
  try {
    await profileStore.updateProfile({
      goal: editGoal.value,
      strictness: editStrictness.value,
      allergies: editAllergiesText.value,
    });
    successMessage.value = "프로필을 저장했습니다.";
    await refreshProfileData();
  } catch (error) {
    errorMessage.value = error?.response?.data?.message ?? "프로필 저장에 실패했습니다.";
  } finally {
    saving.value = false;
  }
}

function shortDate(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return `${date.getMonth() + 1}/${date.getDate()}`;
}

function offsetLabel(offset) {
  if (offset === -2) return "그제";
  if (offset === -1) return "어제";
  if (offset === 0) return "오늘";
  if (offset === 1) return "내일";
  if (offset === 2) return "모레";
  return "";
}

watch(currentMonth, async () => {
  await loadMonthHistory();
});

onMounted(async () => {
  await refreshProfileData();
  await loadMonthHistory();
});
</script>

<template>
  <section class="space-y-4">
    <article class="bento-card">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p class="eyebrow">My Profile</p>
          <h2 class="title-lg">사용자 건강 프로필</h2>
        </div>
        <button class="btn-ghost" :disabled="loading" @click="refreshProfileData">
          {{ loading ? "불러오는 중..." : "새로고침" }}
        </button>
      </div>

      <p
        v-if="errorMessage"
        class="mt-3 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700"
      >
        {{ errorMessage }}
      </p>
      <p
        v-if="successMessage"
        class="mt-3 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700"
      >
        {{ successMessage }}
      </p>

      <div class="mt-4 grid gap-3 md:grid-cols-4">
        <div class="rounded-xl border border-slate-200 bg-slate-50 p-3">
          <p class="text-xs text-slate-500">이메일</p>
          <p class="mt-1 text-sm font-semibold text-slate-900">{{ profileStore.email || "비로그인" }}</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-slate-50 p-3">
          <p class="text-xs text-slate-500">목표</p>
          <p class="mt-1 text-sm font-semibold text-slate-900">{{ profileStore.goalLabel }}</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-slate-50 p-3">
          <p class="text-xs text-slate-500">관리 강도</p>
          <p class="mt-1 text-sm font-semibold text-slate-900">{{ profileStore.strictnessLabel }}</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-slate-50 p-3">
          <div class="flex items-center gap-1">
            <p class="text-xs text-slate-500">계정 상태</p>
            <span class="status-help-icon" :title="accountStatusHint(profileStore.role, profileStore.status)">!</span>
          </div>
          <p class="mt-1 text-sm font-semibold text-slate-900">
            {{ roleLabel(profileStore.role) }} / {{ statusLabel(profileStore.status) }}
          </p>
        </div>
      </div>

      <div class="mt-3 grid gap-3 md:grid-cols-3">
        <label>
          <span class="field-label">목표</span>
          <select v-model="editGoal" class="field-input">
            <option value="MAINTAIN">건강 유지</option>
            <option value="FAT_LOSS">체지방 감량</option>
            <option value="BULK_UP">근육량 증가</option>
          </select>
        </label>
        <label>
          <span class="field-label">강도</span>
          <select v-model="editStrictness" class="field-input">
            <option value="LOW">낮음</option>
            <option value="MEDIUM">중간</option>
            <option value="HIGH">높음</option>
          </select>
        </label>
        <label>
          <span class="field-label">알레르기(한국어/영어, 쉼표 구분)</span>
          <input v-model="editAllergiesText" class="field-input" placeholder="예: 갑각류, 땅콩" />
        </label>
      </div>
      <button class="btn-primary mt-3" :disabled="saving" @click="saveProfile">
        {{ saving ? "저장 중..." : "프로필 저장" }}
      </button>
    </article>

    <article class="bento-card">
      <div class="flex items-center justify-between gap-2">
        <div>
          <p class="eyebrow">Monthly Calendar</p>
          <h3 class="mt-1 text-xl font-semibold text-slate-900">한달 식단 캘린더</h3>
        </div>
        <div class="flex items-center gap-2">
          <button class="btn-ghost" @click="prevMonth">이전 달</button>
          <p class="text-sm font-semibold text-slate-900">{{ monthLabel }}</p>
          <button class="btn-ghost" @click="nextMonth">다음 달</button>
        </div>
      </div>

      <div class="mt-3 calendar-grid">
        <span class="calendar-weekday">일</span>
        <span class="calendar-weekday">월</span>
        <span class="calendar-weekday">화</span>
        <span class="calendar-weekday">수</span>
        <span class="calendar-weekday">목</span>
        <span class="calendar-weekday">금</span>
        <span class="calendar-weekday">토</span>

        <article
          v-for="day in monthlyDays"
          :key="day.key"
          class="calendar-day"
          :class="[day.inMonth ? 'is-month' : 'is-out', day.inMonth ? day.scoreClass : '']"
          :title="day.inMonth && day.date ? `${day.date} · ${Math.round(day.totalCalories)}kcal` : ''"
        >
          <p class="day-number">{{ day.day }}</p>
          <p v-if="day.inMonth && day.totalCalories > 0" class="day-kcal">{{ Math.round(day.totalCalories) }} kcal</p>
          <p v-if="day.inMonth && compactAdditives(day.topAdditives)" class="day-additive">
            {{ compactAdditives(day.topAdditives) }}
          </p>
        </article>
      </div>

      <div class="mt-3 grid gap-3 md:grid-cols-3">
        <div class="rounded-xl border border-slate-200 bg-slate-50 p-3">
          <p class="text-xs text-slate-500">월간 기록 일수</p>
          <p class="mt-1 text-sm font-semibold text-slate-900">{{ monthlySyncedCount }}일</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-slate-50 p-3">
          <p class="text-xs text-slate-500">최근 4주 동기화 일수</p>
          <p class="mt-1 text-sm font-semibold text-slate-900">{{ syncedDays }}일</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-slate-50 p-3">
          <p class="text-xs text-slate-500">평균 칼로리</p>
          <p class="mt-1 text-sm font-semibold text-slate-900">{{ avgCalories.toFixed(0) }} kcal</p>
        </div>
      </div>
    </article>

    <article class="bento-card">
      <p class="eyebrow">Detox Rhythm</p>
      <h3 class="mt-1 text-xl font-semibold text-slate-900">최근 4주 첨가물 디톡스 리듬</h3>

      <div class="mt-3 heat-label-grid">
        <span v-for="day in rhythm" :key="`label-${day.date}`" class="heat-label-cell">
          <template v-if="[-2, -1, 0, 1, 2].includes(day.offset)">
            <strong>{{ offsetLabel(day.offset) }}</strong>
            <small>{{ shortDate(day.date) }}</small>
          </template>
        </span>
      </div>

      <div class="heat-grid">
        <span
          v-for="day in rhythm"
          :key="day.date"
          class="heat-cell"
          :class="[`heat-${day.level}`, day.status === 'DRAFT' ? 'heat-draft' : '']"
          :title="`${day.date} | ${Math.round(day.totalCalories)}kcal | ${day.status}`"
        />
      </div>

      <div class="mt-4 grid gap-3 md:grid-cols-3">
        <div class="rounded-xl border border-slate-200 bg-slate-50 p-3">
          <p class="text-xs text-slate-500">동기화 완료 일수</p>
          <p class="mt-1 text-sm font-semibold text-slate-900">{{ syncedDays }}일</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-slate-50 p-3">
          <p class="text-xs text-slate-500">로컬 초안 일수</p>
          <p class="mt-1 text-sm font-semibold text-slate-900">{{ draftDays }}일</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-slate-50 p-3">
          <p class="text-xs text-slate-500">평균 칼로리</p>
          <p class="mt-1 text-sm font-semibold text-slate-900">{{ avgCalories.toFixed(0) }} kcal</p>
        </div>
      </div>
    </article>
  </section>
</template>
