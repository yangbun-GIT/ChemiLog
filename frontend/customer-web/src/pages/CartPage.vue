<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../stores/authStore";
import { useCartStore } from "../stores/cartStore";

const router = useRouter();
const authStore = useAuthStore();
const cartStore = useCartStore();

const mealType = ref("LUNCH");
const loggedDate = ref(new Date().toISOString().slice(0, 10));
const online = ref(typeof navigator !== "undefined" ? navigator.onLine : true);
const errorMessage = ref("");
const successMessage = ref("");
const mergeLoading = ref(false);
const remoteItems = ref([]);
const remoteLoading = ref(false);

const syncMeta = computed(() => cartStore.syncStatusMeta);
const totalCalories = computed(() => Number(cartStore.totalCalories || 0).toFixed(1));
const unsyncedDraftDays = computed(() => cartStore.weeklyRhythm.filter((day) => day.status === "DRAFT").length);
const lastSync = computed(() => cartStore.lastSyncResult);

function handleOnline() {
  online.value = true;
}

function handleOffline() {
  online.value = false;
}

async function loadRemoteToday() {
  if (!authStore.isAuthenticated) {
    remoteItems.value = [];
    return;
  }

  remoteLoading.value = true;
  try {
    remoteItems.value = await cartStore.fetchTodayRemote(loggedDate.value);
  } catch {
    remoteItems.value = [];
  } finally {
    remoteLoading.value = false;
  }
}

async function mergeWithTodayRecord() {
  errorMessage.value = "";
  successMessage.value = "";

  if (!authStore.isAuthenticated) {
    errorMessage.value = "서버 기록과 병합하려면 먼저 로그인해주세요.";
    return;
  }

  mergeLoading.value = true;
  try {
    const serverItems = await cartStore.fetchTodayRemote(loggedDate.value);
    if (!serverItems.length) {
      successMessage.value = "오늘 서버 기록이 없어 병합할 항목이 없습니다.";
      return;
    }
    cartStore.mergeWithServerItems(serverItems);
    successMessage.value = "로컬 초안에 오늘 서버 기록을 병합했습니다.";
    await loadRemoteToday();
  } catch (error) {
    errorMessage.value = error?.response?.data?.message ?? "병합 중 오류가 발생했습니다.";
  } finally {
    mergeLoading.value = false;
  }
}

async function sync() {
  errorMessage.value = "";
  successMessage.value = "";

  if (!authStore.isAuthenticated) {
    errorMessage.value = "식단 동기화는 로그인 후 사용할 수 있습니다.";
    router.push("/login");
    return;
  }

  try {
    const result = await cartStore.syncToServer(mealType.value, loggedDate.value);
    if (result) {
      successMessage.value = `동기화 완료: meal_id ${result.mealId}, 총 ${Number(result.totalCalories).toFixed(1)} kcal`;
      await loadRemoteToday();
    }
  } catch (error) {
    errorMessage.value =
      cartStore.syncError ||
      error?.response?.data?.message ||
      error?.message ||
      "동기화 중 오류가 발생했습니다.";
  }
}

onMounted(async () => {
  window.addEventListener("online", handleOnline);
  window.addEventListener("offline", handleOffline);
  await loadRemoteToday();
});

onUnmounted(() => {
  window.removeEventListener("online", handleOnline);
  window.removeEventListener("offline", handleOffline);
});

watch(loggedDate, async () => {
  await loadRemoteToday();
});
</script>

<template>
  <section class="space-y-4">
    <article class="bento-card">
      <p class="eyebrow">Meal Sync</p>
      <h2 class="title-lg">식단 동기화</h2>

      <div class="mt-4 grid gap-3 md:grid-cols-4">
        <div class="rounded-xl border border-slate-200 bg-slate-50 p-3">
          <p class="text-xs text-slate-500">동기화 상태</p>
          <p class="mt-1 text-sm font-semibold text-slate-900">{{ syncMeta.label }}</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-slate-50 p-3">
          <p class="text-xs text-slate-500">총 수량</p>
          <p class="mt-1 text-sm font-semibold text-slate-900">{{ cartStore.totalItemCount.toFixed(1) }}</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-slate-50 p-3">
          <p class="text-xs text-slate-500">추정 칼로리</p>
          <p class="mt-1 text-sm font-semibold text-slate-900">{{ totalCalories }} kcal</p>
        </div>
        <div class="rounded-xl border border-slate-200 bg-slate-50 p-3">
          <p class="text-xs text-slate-500">미동기화 초안 일수</p>
          <p class="mt-1 text-sm font-semibold text-slate-900">{{ unsyncedDraftDays }}</p>
        </div>
      </div>

      <div class="mt-3 rounded-xl border border-slate-200 bg-white p-3 text-sm text-slate-600">
        네트워크: <strong class="text-slate-900">{{ online ? "온라인" : "오프라인" }}</strong> ·
        {{ syncMeta.description }}
      </div>
    </article>

    <article class="bento-card">
      <div class="grid gap-4 md:grid-cols-3">
        <label class="block">
          <span class="field-label">식사 타입</span>
          <select v-model="mealType" class="field-input">
            <option>BREAKFAST</option>
            <option>LUNCH</option>
            <option>DINNER</option>
            <option>SNACK</option>
          </select>
        </label>

        <label class="block">
          <span class="field-label">기록 날짜</span>
          <input v-model="loggedDate" type="date" class="field-input" />
        </label>

        <div class="flex items-end gap-2">
          <button
            class="btn-ghost"
            :disabled="mergeLoading || cartStore.isSyncing"
            @click="mergeWithTodayRecord"
          >
            {{ mergeLoading ? "병합 중..." : "오늘 기록 병합" }}
          </button>
          <button
            :disabled="cartStore.isSyncing || cartStore.items.length === 0"
            class="btn-primary"
            @click="sync"
          >
            {{ cartStore.isSyncing ? "동기화 중..." : "서버 저장" }}
          </button>
        </div>
      </div>

      <p
        v-if="successMessage"
        class="mt-3 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700"
      >
        {{ successMessage }}
      </p>
      <p
        v-if="errorMessage"
        class="mt-3 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700"
      >
        {{ errorMessage }}
      </p>
      <p
        v-if="lastSync"
        class="mt-3 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-700"
      >
        최근 동기화 결과: meal_id {{ lastSync.mealId }} · 총 {{ Number(lastSync.totalCalories || 0).toFixed(1) }} kcal
      </p>
    </article>

    <div class="grid gap-4 lg:grid-cols-2">
      <article class="bento-card">
        <div class="mb-3 flex items-center justify-between">
          <p class="eyebrow">Local Draft</p>
          <button class="btn-ghost" @click="cartStore.clearItems">로컬 비우기</button>
        </div>

        <div class="space-y-3">
          <div
            v-if="cartStore.items.length === 0"
            class="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600"
          >
            아직 담긴 항목이 없습니다. 대시보드에서 식품을 추가해주세요.
          </div>

          <article
            v-for="item in cartStore.items"
            :key="item.foodId"
            class="rounded-xl border border-slate-200 bg-white p-3"
          >
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p class="font-semibold text-slate-900">{{ item.name }}</p>
                <p class="text-xs text-slate-500">{{ item.category || "기타" }} · food_id {{ item.foodId }}</p>
              </div>

              <div class="flex items-center gap-2">
                <input
                  :value="item.quantity"
                  type="number"
                  min="0.1"
                  step="0.1"
                  class="field-input w-24 text-right"
                  @input="cartStore.updateQuantity(item.foodId, $event.target.value)"
                />
                <button class="btn-ghost" @click="cartStore.remove(item.foodId)">제거</button>
              </div>
            </div>
          </article>
        </div>
      </article>

      <article class="bento-card">
        <div class="mb-3 flex items-center justify-between">
          <p class="eyebrow">Server Record</p>
          <button class="btn-ghost" :disabled="remoteLoading" @click="loadRemoteToday">
            {{ remoteLoading ? "조회 중..." : "오늘 기록 조회" }}
          </button>
        </div>

        <div
          v-if="!authStore.isAuthenticated"
          class="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600"
        >
          서버 기록 조회는 로그인 후 사용할 수 있습니다.
        </div>

        <div
          v-else-if="remoteLoading"
          class="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600"
        >
          서버 기록을 조회하고 있습니다...
        </div>

        <div
          v-else-if="remoteItems.length === 0"
          class="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600"
        >
          선택한 날짜에 저장된 서버 기록이 없습니다.
        </div>

        <div v-else class="space-y-2">
          <article
            v-for="item in remoteItems"
            :key="`remote-${item.foodId}`"
            class="rounded-xl border border-slate-200 bg-white px-3 py-2"
          >
            <p class="text-sm font-semibold text-slate-900">food_id {{ item.foodId }}</p>
            <p class="text-xs text-slate-500">수량 {{ Number(item.quantity || 0).toFixed(1) }}</p>
          </article>
        </div>
      </article>
    </div>
  </section>
</template>
