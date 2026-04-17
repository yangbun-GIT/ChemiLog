<script setup>
import { computed, nextTick, ref } from "vue";
import { useRouter } from "vue-router";
import { useAiChatStore } from "../stores/aiChatStore";
import { useAuthStore } from "../stores/authStore";
import { useCartStore } from "../stores/cartStore";
import { useProfileStore } from "../stores/profileStore";

const router = useRouter();
const aiChatStore = useAiChatStore();
const authStore = useAuthStore();
const cartStore = useCartStore();
const profileStore = useProfileStore();

const text = ref("");
const listRef = ref(null);

const canSend = computed(() => text.value.trim().length > 0 && !aiChatStore.isStreaming);

function scrollToBottom() {
  nextTick(() => {
    if (!listRef.value) return;
    listRef.value.scrollTop = listRef.value.scrollHeight;
  });
}

function applyAction(action) {
  if (!action) return;
  if (action.type === "ADD_TO_CART" || action.type === "ADD_CART") {
    cartStore.addFood({
      foodId: action.food_id,
      name: action.name ?? `추천 식품 #${action.food_id}`,
      category: "추천",
      calories: action.calories ?? 0,
    });
  }
}

async function sendMessage() {
  if (!canSend.value) return;

  if (!authStore.isAuthenticated) {
    aiChatStore.open();
    aiChatStore.pushUserMessage(text.value.trim());
    aiChatStore.failAssistantDraft("AI 멘토링은 로그인 후 사용할 수 있습니다.");
    text.value = "";
    router.push("/login");
    return;
  }

  const userText = text.value.trim();
  text.value = "";

  aiChatStore.open();
  aiChatStore.pushUserMessage(userText);
  aiChatStore.setStreaming(true);
  aiChatStore.startAssistantDraft();
  scrollToBottom();

  try {
    const payload = {
      chat_history: aiChatStore.messages
        .slice(-10)
        .map((msg) => ({ role: msg.role === "assistant" ? "assistant" : "user", content: msg.content })),
      current_cart: cartStore.items.map((item) => ({
        food_id: item.foodId,
        name: item.name,
        quantity: Number(item.quantity),
        additive_ids: item.additiveIds ?? [],
      })),
      profile_context: {
        goal: profileStore.goal,
        allergies: profileStore.allergies,
        strictness: profileStore.strictness,
      },
    };

    const response = await fetch("/api/v1/ai/mentoring", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${authStore.accessToken}`,
      },
      credentials: "include",
      body: JSON.stringify(payload),
    });

    if (!response.ok || !response.body) {
      throw new Error("AI 서비스 응답을 가져오지 못했습니다.");
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");
    let buffer = "";
    let currentEvent = "message";
    let completedAction = null;

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const blocks = buffer.split("\n\n");
      buffer = blocks.pop() ?? "";

      for (const block of blocks) {
        const lines = block.split("\n");
        let dataPayload = "";
        for (const line of lines) {
          if (line.startsWith("event:")) currentEvent = line.slice(6).trim();
          if (line.startsWith("data:")) dataPayload += line.slice(5).trim();
        }

        if (!dataPayload) continue;

        const parsed = JSON.parse(dataPayload);
        if (currentEvent === "error") {
          aiChatStore.failAssistantDraft(parsed.message ?? "AI 응답이 중단되었습니다.");
          scrollToBottom();
          return;
        }
        if (parsed.chunk) {
          aiChatStore.appendAssistantChunk(parsed.chunk);
          scrollToBottom();
        }
        if (parsed.status === "completed") {
          completedAction = parsed.action ?? null;
        }
      }
    }

    aiChatStore.completeAssistantDraft(completedAction);
    scrollToBottom();
  } catch (error) {
    aiChatStore.failAssistantDraft(error?.message ?? "AI 서버 지연 중입니다. 잠시 후 다시 시도해주세요.");
    scrollToBottom();
  }
}
</script>

<template>
  <div class="fixed bottom-6 right-6 z-40">
    <transition name="fade-slide">
      <section
        v-if="aiChatStore.isOpen"
        class="glass-card mb-4 flex h-[560px] w-[min(92vw,420px)] flex-col overflow-hidden"
      >
        <header class="flex items-center justify-between border-b border-slate-200 px-4 py-3">
          <div>
            <p class="text-xs uppercase tracking-[0.22em] text-slate-500">AI Mentoring</p>
            <h2 class="text-sm font-semibold text-slate-900">ChemiLog Nutrition Agent</h2>
          </div>
          <button class="btn-ghost !px-2 !py-1 text-xs" @click="aiChatStore.close">닫기</button>
        </header>

        <div ref="listRef" class="chat-scroll flex-1 space-y-3 overflow-y-auto px-4 py-4 bg-slate-50/70">
          <article
            v-for="message in aiChatStore.messages"
            :key="message.id"
            :class="[
              'max-w-[92%] rounded-2xl px-3 py-2 text-sm leading-relaxed',
              message.role === 'user'
                ? 'ml-auto bg-cyan-500 text-white'
                : 'bg-white border border-slate-200 text-slate-800',
            ]"
          >
            <p>{{ message.content }}</p>
            <button
              v-if="message.action && (message.action.type === 'ADD_TO_CART' || message.action.type === 'ADD_CART')"
              class="mt-2 rounded-lg bg-emerald-500 px-3 py-1 text-xs font-semibold text-white"
              @click="applyAction(message.action)"
            >
              장바구니에 즉시 담기
            </button>
          </article>

          <div
            v-if="aiChatStore.isStreaming"
            class="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-3 py-1 text-xs text-slate-500"
          >
            <span class="typing-dot" />
            <span class="typing-dot" />
            <span class="typing-dot" />
            분석 중...
          </div>
        </div>

        <footer class="border-t border-slate-200 px-4 py-3 bg-white">
          <div class="flex items-end gap-2">
            <textarea
              v-model="text"
              rows="2"
              class="field-input min-h-[54px] flex-1 resize-none"
              placeholder="식단 질문을 입력하세요"
              @keydown.enter.exact.prevent="sendMessage"
            />
            <button class="btn-primary !px-4 !py-2" :disabled="!canSend" @click="sendMessage">
              전송
            </button>
          </div>
        </footer>
      </section>
    </transition>

    <button class="fab-button" :aria-expanded="aiChatStore.isOpen ? 'true' : 'false'" @click="aiChatStore.toggle">
      <span v-if="!aiChatStore.isOpen">AI</span>
      <span v-else>×</span>
    </button>
  </div>
</template>
