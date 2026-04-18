<script setup>
import { computed } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { useAdminAuthStore } from "./stores/adminAuthStore";

const route = useRoute();
const router = useRouter();
const authStore = useAdminAuthStore();

const showNav = computed(() => route.name !== "admin-login" && authStore.isAuthenticated);

async function logout() {
  await authStore.logout();
  router.push({ name: "admin-login" });
}
</script>

<template>
  <div class="admin-app-shell">
    <header class="admin-header">
      <div class="admin-container admin-header-row">
        <RouterLink to="/" class="admin-brand-link">
          <p class="admin-eyebrow">CHEMILOG</p>
          <h1>Admin CMS</h1>
        </RouterLink>

        <div v-if="showNav" class="admin-nav-wrap">
          <nav class="admin-nav">
            <RouterLink to="/" class="admin-nav-link">대시보드</RouterLink>
            <RouterLink to="/users" class="admin-nav-link">사용자 관리</RouterLink>
            <RouterLink to="/foods" class="admin-nav-link">식품 관리</RouterLink>
            <RouterLink to="/additives" class="admin-nav-link">첨가물</RouterLink>
            <RouterLink to="/logs" class="admin-nav-link">AI 로그</RouterLink>
          </nav>
          <button class="admin-button" @click="logout">로그아웃</button>
        </div>
      </div>
    </header>

    <section class="admin-container">
      <router-view />
    </section>
  </div>
</template>
