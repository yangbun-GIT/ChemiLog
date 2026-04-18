<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { RouterLink } from "vue-router";
import AiMentorWidget from "./components/AiMentorWidget.vue";
import OnboardingWizard from "./components/OnboardingWizard.vue";
import { useAuthStore } from "./stores/authStore";
import { useCartStore } from "./stores/cartStore";
import { useProfileStore } from "./stores/profileStore";

const authStore = useAuthStore();
const cartStore = useCartStore();
const profileStore = useProfileStore();

const online = ref(navigator.onLine);

const onlineLabel = computed(() => (online.value ? "온라인" : "오프라인"));

function onOnline() {
  online.value = true;
}

function onOffline() {
  online.value = false;
}

function onVisibilityChange() {
  if (document.visibilityState === "visible") {
    cartStore.tickTodayKey();
  }
}

onMounted(async () => {
  cartStore.hydrate();
  cartStore.startDateTicker();
  profileStore.hydrate();
  await authStore.trySilentRefresh();

  if (authStore.isAuthenticated) {
    try {
      await profileStore.fetchMe();
      const history = await cartStore.fetchHistory();
      cartStore.applyHistory(history);
    } catch {
      // ignore initial hydrate failures and keep local fallback
    }
  }

  window.addEventListener("online", onOnline);
  window.addEventListener("offline", onOffline);
  document.addEventListener("visibilitychange", onVisibilityChange);
});

onUnmounted(() => {
  cartStore.stopDateTicker();
  window.removeEventListener("online", onOnline);
  window.removeEventListener("offline", onOffline);
  document.removeEventListener("visibilitychange", onVisibilityChange);
});
</script>

<template>
  <div class="app-shell min-h-screen text-slate-900">
    <header class="site-header">
      <div class="container-row">
        <RouterLink to="/" class="brand-block">
          <h1>ChemiLog</h1>
        </RouterLink>

        <nav class="main-nav">
          <RouterLink to="/" class="nav-link">대시보드</RouterLink>
          <RouterLink to="/cart" class="nav-link">식단 동기화</RouterLink>
          <RouterLink to="/mypage" class="nav-link">마이페이지</RouterLink>
          <RouterLink v-if="!authStore.isAuthenticated" to="/login" class="nav-link">로그인</RouterLink>
          <button v-else class="nav-link nav-button" @click="authStore.logout">로그아웃</button>
        </nav>
      </div>
    </header>

    <div class="status-strip">
      <div class="container-row status-content">
        <div class="status-pill" :class="online ? 'is-online' : 'is-offline'">
          <span class="dot" />
          {{ onlineLabel }}
        </div>
        <p class="status-help">
          오프라인에서도 장바구니 편집은 유지되며, 온라인 전환 후 서버 동기화가 가능합니다.
        </p>
      </div>
    </div>

    <main class="container-main">
      <div v-if="authStore.isSessionLoading" class="panel">
        <div class="skeleton h-16 w-full rounded-xl" />
      </div>
      <router-view v-else />
    </main>

    <OnboardingWizard />
    <AiMentorWidget />
  </div>
</template>
