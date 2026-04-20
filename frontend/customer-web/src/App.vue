<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import AiMentorWidget from "./components/AiMentorWidget.vue";
import OnboardingWizard from "./components/OnboardingWizard.vue";
import { useAiChatStore } from "./stores/aiChatStore";
import { useAuthStore } from "./stores/authStore";
import { useCartStore } from "./stores/cartStore";
import { useProfileStore } from "./stores/profileStore";

const authStore = useAuthStore();
const cartStore = useCartStore();
const profileStore = useProfileStore();
const aiChatStore = useAiChatStore();

const online = ref(navigator.onLine);
const appBootReady = ref(false);

const onlineLabel = computed(() => (online.value ? "온라인" : "오프라인"));
const currentUserId = computed(() => {
  const sub = authStore.userClaims?.sub;
  return sub != null ? String(sub) : null;
});

watch(
  currentUserId,
  (userId) => {
    profileStore.setActiveUser(userId);
    cartStore.setActiveUser(userId);
    aiChatStore.setActiveUser(userId);
  },
  { immediate: true }
);

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
  try {
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
  } finally {
    appBootReady.value = true;
    requestAnimationFrame(() => {
      document.documentElement.classList.add("app-ready");
      setTimeout(() => {
        document.documentElement.classList.remove("app-preload");
        document.body.classList.remove("app-booting");
      }, 60);
    });
  }
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
      <div v-if="!appBootReady" class="panel">
        <div class="skeleton h-20 w-full rounded-xl" />
        <div class="mt-3 grid gap-3 md:grid-cols-2">
          <div class="skeleton h-56 rounded-xl" />
          <div class="skeleton h-56 rounded-xl" />
        </div>
      </div>
      <div v-if="authStore.isSessionLoading" class="panel">
        <div class="skeleton h-16 w-full rounded-xl" />
      </div>
      <router-view v-else-if="appBootReady" />
    </main>

    <OnboardingWizard />
    <AiMentorWidget />
  </div>
</template>
