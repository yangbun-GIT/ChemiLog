<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../stores/authStore";
import { useCartStore } from "../stores/cartStore";
import { useProfileStore } from "../stores/profileStore";

const router = useRouter();
const authStore = useAuthStore();
const cartStore = useCartStore();
const profileStore = useProfileStore();

const tab = ref("login");
const isLoading = ref(false);
const errorMessage = ref("");

const loginForm = reactive({
  username: "",
  password: "",
});

const registerForm = reactive({
  email: "",
  password: "",
  confirmPassword: "",
  goal: "MAINTAIN",
  strictness: "MEDIUM",
  allergiesText: "",
});

function parseAllergies(text) {
  return String(text || "")
    .split(",")
    .map((v) => v.trim())
    .filter(Boolean);
}

async function afterLoginCommonFlow() {
  try {
    await profileStore.fetchMe();
  } catch {
    // ignore
  }

  if (cartStore.items.length > 0) {
    try {
      const serverItems = await cartStore.fetchTodayRemote();
      if (serverItems.length > 0) {
        const merge = window.confirm("로컬 장바구니를 오늘 서버 기록과 병합할까요?");
        if (merge) {
          cartStore.mergeWithServerItems(serverItems);
        }
      }
    } catch {
      // ignore
    }
  }

  router.push("/");
}

async function submitLogin() {
  errorMessage.value = "";
  isLoading.value = true;

  try {
    await authStore.login(loginForm.username, loginForm.password);
    await afterLoginCommonFlow();
  } catch (error) {
    errorMessage.value = error?.response?.data?.message ?? "로그인에 실패했습니다.";
  } finally {
    isLoading.value = false;
  }
}

async function submitRegister() {
  errorMessage.value = "";
  if (registerForm.password !== registerForm.confirmPassword) {
    errorMessage.value = "비밀번호 확인이 일치하지 않습니다.";
    return;
  }

  isLoading.value = true;
  try {
    await authStore.register({
      email: registerForm.email,
      password: registerForm.password,
      goal: registerForm.goal,
      strictness: registerForm.strictness,
      allergies: parseAllergies(registerForm.allergiesText),
    });
    await afterLoginCommonFlow();
  } catch (error) {
    errorMessage.value = error?.response?.data?.message ?? "회원가입에 실패했습니다.";
  } finally {
    isLoading.value = false;
  }
}
</script>

<template>
  <section class="mx-auto max-w-2xl">
    <article class="bento-card">
      <p class="eyebrow">Account</p>
      <h2 class="title-lg">ChemiLog 계정</h2>
      <p class="mt-2 text-sm text-slate-600">
        로그인 또는 회원가입 후 식단 동기화, 마이페이지, AI 멘토링 기능을 사용할 수 있습니다.
      </p>

      <div class="mt-4 flex gap-2">
        <button
          class="btn-ghost"
          :class="tab === 'login' ? 'border-cyan-400 bg-cyan-50 text-cyan-700' : ''"
          @click="tab = 'login'"
        >
          로그인
        </button>
        <button
          class="btn-ghost"
          :class="tab === 'register' ? 'border-cyan-400 bg-cyan-50 text-cyan-700' : ''"
          @click="tab = 'register'"
        >
          회원가입
        </button>
      </div>

      <form v-if="tab === 'login'" class="mt-5 grid gap-4" @submit.prevent="submitLogin">
        <label>
          <span class="field-label">이메일</span>
          <input
            v-model="loginForm.username"
            type="email"
            required
            class="field-input"
            placeholder="user@chemilog.com"
          />
        </label>
        <label>
          <span class="field-label">비밀번호</span>
          <input
            v-model="loginForm.password"
            type="password"
            required
            class="field-input"
            placeholder="8자 이상 입력"
          />
        </label>
        <button :disabled="isLoading" class="btn-primary w-full justify-center">
          {{ isLoading ? "처리 중..." : "로그인" }}
        </button>
      </form>

      <form v-else class="mt-5 grid gap-4" @submit.prevent="submitRegister">
        <label>
          <span class="field-label">이메일</span>
          <input
            v-model="registerForm.email"
            type="email"
            required
            class="field-input"
            placeholder="new@chemilog.com"
          />
        </label>
        <div class="grid gap-3 md:grid-cols-2">
          <label>
            <span class="field-label">비밀번호</span>
            <input v-model="registerForm.password" type="password" required minlength="8" class="field-input" />
          </label>
          <label>
            <span class="field-label">비밀번호 확인</span>
            <input v-model="registerForm.confirmPassword" type="password" required minlength="8" class="field-input" />
          </label>
        </div>
        <div class="grid gap-3 md:grid-cols-2">
          <label>
            <span class="field-label">목표</span>
            <select v-model="registerForm.goal" class="field-input">
              <option value="MAINTAIN">건강 유지</option>
              <option value="FAT_LOSS">체지방 감량</option>
              <option value="BULK_UP">근육량 증가</option>
            </select>
          </label>
          <label>
            <span class="field-label">관리 강도</span>
            <select v-model="registerForm.strictness" class="field-input">
              <option value="LOW">낮음</option>
              <option value="MEDIUM">중간</option>
              <option value="HIGH">높음</option>
            </select>
          </label>
        </div>
        <label>
          <span class="field-label">알레르기(선택, 쉼표 구분)</span>
          <input
            v-model="registerForm.allergiesText"
            class="field-input"
            placeholder="예: SHRIMP, PEANUT"
          />
        </label>
        <button :disabled="isLoading" class="btn-primary w-full justify-center">
          {{ isLoading ? "처리 중..." : "회원가입 후 시작하기" }}
        </button>
      </form>

      <p
        v-if="errorMessage"
        class="mt-3 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700"
      >
        {{ errorMessage }}
      </p>

      <div class="mt-4 rounded-xl border border-slate-200 bg-slate-50 p-3 text-sm text-slate-600">
        <p class="font-semibold text-slate-800">개발용 계정</p>
        <p class="mt-1">- 관리자: admin@chemilog.com / Admin1234!</p>
        <p>- 사용자: user@chemilog.com / User1234!</p>
      </div>
    </article>
  </section>
</template>
