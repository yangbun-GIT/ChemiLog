<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useAdminAuthStore } from "../stores/adminAuthStore";

const router = useRouter();
const authStore = useAdminAuthStore();

const username = ref("admin@chemilog.com");
const password = ref("Admin1234!");
const errorMessage = ref("");

async function submit() {
  errorMessage.value = "";
  try {
    await authStore.login(username.value, password.value);
    router.push("/");
  } catch (error) {
    errorMessage.value =
      error?.response?.data?.message ?? error?.message ?? "관리자 로그인에 실패했습니다.";
  }
}
</script>

<template>
  <main class="admin-auth-wrap">
    <section class="admin-card">
      <p class="admin-eyebrow">Admin Access</p>
      <h1>ChemiLog CMS 로그인</h1>
      <p class="admin-muted">관리자 권한 계정으로 로그인하면 대시보드 데이터를 확인할 수 있습니다.</p>

      <form class="admin-form" @submit.prevent="submit">
        <label>
          <span>이메일</span>
          <input v-model="username" type="email" required />
        </label>

        <label>
          <span>비밀번호</span>
          <input v-model="password" type="password" required />
        </label>

        <p v-if="errorMessage" class="admin-error">{{ errorMessage }}</p>

        <button type="submit" :disabled="authStore.loading">
          {{ authStore.loading ? "로그인 중..." : "로그인" }}
        </button>
      </form>
    </section>
  </main>
</template>
