<script setup>
import { ref, watch } from "vue";
import { useAuthStore } from "../stores/authStore";
import { useProfileStore } from "../stores/profileStore";

const authStore = useAuthStore();
const profileStore = useProfileStore();

const open = ref(false);
const isSaving = ref(false);
const errorMessage = ref("");

const goal = ref(profileStore.goal || "MAINTAIN");
const strictness = ref(profileStore.strictness || "MEDIUM");
const allergiesText = ref((profileStore.allergies || []).join(", "));

watch(
  () => profileStore.onboardingCompleted,
  (completed) => {
    open.value = !completed;
  },
  { immediate: true }
);

async function submit() {
  errorMessage.value = "";
  isSaving.value = true;
  try {
    await profileStore.completeProfile(
      {
        goal: goal.value,
        strictness: strictness.value,
        allergies: allergiesText.value,
      },
      { syncRemote: authStore.isAuthenticated }
    );
    open.value = false;
  } catch (error) {
    errorMessage.value = error?.response?.data?.message ?? "프로필 저장 중 오류가 발생했습니다.";
  } finally {
    isSaving.value = false;
  }
}

function skip() {
  profileStore.completeProfile(
    {
      goal: "MAINTAIN",
      strictness: "MEDIUM",
      allergies: [],
    },
    { syncRemote: false }
  );
  open.value = false;
}
</script>

<template>
  <teleport to="body">
    <transition name="fade-slide">
      <div v-if="open" class="overlay-shell">
        <div class="glass-card mx-auto w-[min(92vw,700px)] p-6 md:p-8">
          <p class="eyebrow">Onboarding</p>
          <h2 class="mt-2 text-2xl font-semibold text-slate-900">개인 맞춤 식단 목표 설정</h2>
          <p class="mt-2 text-sm text-slate-600">
            설정한 정보는 식단 추천과 AI 멘토링에 반영됩니다.
          </p>

          <div class="mt-5 grid gap-4 md:grid-cols-2">
            <label>
              <span class="field-label">목표</span>
              <select v-model="goal" class="field-input">
                <option value="FAT_LOSS">체지방 감량</option>
                <option value="BULK_UP">근육량 증가</option>
                <option value="MAINTAIN">건강 유지</option>
              </select>
            </label>
            <label>
              <span class="field-label">관리 강도</span>
              <select v-model="strictness" class="field-input">
                <option value="LOW">낮음</option>
                <option value="MEDIUM">중간</option>
                <option value="HIGH">높음</option>
              </select>
            </label>
          </div>

          <label class="mt-4 block">
            <span class="field-label">알레르기/주의 식품 (쉼표 구분)</span>
            <input v-model="allergiesText" class="field-input" placeholder="예: SHRIMP, PEANUT" />
          </label>

          <p v-if="errorMessage" class="mt-3 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
            {{ errorMessage }}
          </p>

          <div class="mt-6 flex items-center justify-end gap-3">
            <button class="btn-ghost" type="button" @click="skip">기본값 사용</button>
            <button class="btn-primary" type="button" :disabled="isSaving" @click="submit">
              {{ isSaving ? "저장 중..." : "저장하고 시작" }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>
