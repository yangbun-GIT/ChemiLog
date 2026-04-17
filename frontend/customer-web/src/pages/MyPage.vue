<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useAuthStore } from "../stores/authStore";
import { useCartStore } from "../stores/cartStore";
import { useProfileStore } from "../stores/profileStore";

const authStore = useAuthStore();
const cartStore = useCartStore();
const profileStore = useProfileStore();

const loading = ref(false);
const errorMessage = ref("");
const currentMonth = ref(new Date(new Date().getFullYear(), new Date().getMonth(), 1));
const monthlyDays = ref([]);

const rhythm = computed(() => cartStore.weeklyRhythm);
const syncedDays = computed(() => rhythm.value.filter((day) => day.status === "SYNCED").length);
const draftDays = computed(() => rhythm.value.filter((day) => day.status === "DRAFT").length);
const avgCalories = computed(() => {
  const synced = rhythm.value.filter((day) => day.status === "SYNCED" && day.totalCalories > 0);
  if (!synced.length) return 0;
  const total = synced.reduce((sum, day) => sum + Number(day.totalCalories || 0), 0);
  return total / synced.length;
});
const monthLabel = computed(() =>
  `${currentMonth.value.getFullYear()}년 ${String(currentMonth.value.getMonth() + 1).padStart(2, "0")}월`
);
const monthlySyncedCount = computed(
  () => monthlyDays.value.filter((day) => day.inMonth && Number(day.totalCalories || 0) > 0).length
);

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
    cells.push({ key: `prev-${i}`, inMonth: false, date: null, day: "", totalCalories: 0, scoreClass: "heat-0" });
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
    });
  }

  while (cells.length % 7 !== 0) {
    const nextIndex = cells.length;
    cells.push({ key: `next-${nextIndex}`, inMonth: false, date: null, day: "", totalCalories: 0, scoreClass: "heat-0" });
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
  } catch (error) {
    errorMessage.value = error?.response?.data?.message ?? "마이페이지 데이터를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
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
          {{ loading ? "새로고침 중..." : "새로고침" }}
        </button>
      </div>

      <p
        v-if="errorMessage"
        class="mt-3 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700"
      >
        {{ errorMessage }}
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
          <p class="text-xs text-slate-500">계정 상태</p>
          <p class="mt-1 text-sm font-semibold text-slate-900">{{ profileStore.role }} / {{ profileStore.status }}</p>
        </div>
      </div>

      <div class="mt-3 rounded-xl border border-slate-200 bg-white p-3">
        <p class="text-xs text-slate-500">알레르기/주의 식품</p>
        <p class="mt-1 text-sm text-slate-800">
          {{ profileStore.allergies.length ? profileStore.allergies.join(", ") : "등록된 알레르기 없음" }}
        </p>
      </div>
    </article>

    <article class="bento-card">
      <div class="flex items-center justify-between gap-2">
        <div>
          <p class="eyebrow">Monthly Calendar</p>
          <h3 class="mt-1 text-xl font-semibold text-slate-900">한달 식단 캘린더</h3>
        </div>
        <div class="flex items-center gap-2">
          <button class="btn-ghost" @click="prevMonth">이전달</button>
          <p class="text-sm font-semibold text-slate-900">{{ monthLabel }}</p>
          <button class="btn-ghost" @click="nextMonth">다음달</button>
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
          <p class="text-xs text-slate-500">최근 4주 평균 칼로리</p>
          <p class="mt-1 text-sm font-semibold text-slate-900">{{ avgCalories.toFixed(0) }} kcal</p>
        </div>
      </div>
    </article>

    <article class="bento-card">
      <p class="eyebrow">Detox Rhythm</p>
      <h3 class="mt-1 text-xl font-semibold text-slate-900">최근 4주 첨가물 디톡스 리듬</h3>

      <div class="mt-3 heat-grid">
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
          <p class="text-xs text-slate-500">평균 동기화 칼로리</p>
          <p class="mt-1 text-sm font-semibold text-slate-900">{{ avgCalories.toFixed(0) }} kcal</p>
        </div>
      </div>
    </article>
  </section>
</template>
