import { defineStore } from "pinia";

let sequence = 0;
const CHAT_STORAGE_PREFIX = "chemilog.customer.ai-chat.v1.";

function nextId() {
  sequence += 1;
  return `chat-${Date.now()}-${sequence}`;
}

function initialMessages() {
  return [
    {
      id: nextId(),
      role: "assistant",
      content: "ChemiLog AI 영양사입니다. 현재 식단을 기반으로 개선 포인트를 알려드릴게요.",
      action: null,
    },
  ];
}

export const useAiChatStore = defineStore("aiChatStore", {
  state: () => ({
    activeUserKey: "guest",
    isOpen: false,
    isStreaming: false,
    draftAssistantId: null,
    messages: initialMessages(),
  }),
  actions: {
    storageKey() {
      return `${CHAT_STORAGE_PREFIX}${this.activeUserKey}`;
    },
    hydrateMessages() {
      try {
        const raw = localStorage.getItem(this.storageKey());
        if (!raw) {
          this.messages = initialMessages();
          return;
        }
        const parsed = JSON.parse(raw);
        if (!Array.isArray(parsed) || parsed.length === 0) {
          this.messages = initialMessages();
          return;
        }
        const normalized = parsed
          .map((item) => ({
            id: item?.id || nextId(),
            role: item?.role === "assistant" ? "assistant" : "user",
            content: String(item?.content || ""),
            action: item?.action ?? null,
          }))
          .filter((item) => item.content.trim().length > 0);
        this.messages = normalized.length > 0 ? normalized : initialMessages();
      } catch {
        this.messages = initialMessages();
      }
    },
    persistMessages() {
      try {
        localStorage.setItem(this.storageKey(), JSON.stringify(this.messages));
      } catch {
        // ignore storage failures
      }
    },
    setActiveUser(userId) {
      const nextKey = userId ? `user:${userId}` : "guest";
      if (nextKey === this.activeUserKey) return;
      this.persistMessages();
      this.activeUserKey = nextKey;
      this.isStreaming = false;
      this.draftAssistantId = null;
      this.hydrateMessages();
      this.isOpen = false;
    },
    toggle() {
      this.isOpen = !this.isOpen;
    },
    open() {
      this.isOpen = true;
    },
    close() {
      this.isOpen = false;
    },
    pushUserMessage(content) {
      this.messages.push({ id: nextId(), role: "user", content, action: null });
      this.persistMessages();
    },
    startAssistantDraft() {
      const id = nextId();
      this.draftAssistantId = id;
      this.messages.push({ id, role: "assistant", content: "", action: null });
      return id;
    },
    appendAssistantChunk(chunk) {
      if (!this.draftAssistantId) return;
      const target = this.messages.find((msg) => msg.id === this.draftAssistantId);
      if (!target) return;
      target.content += chunk;
      this.persistMessages();
    },
    completeAssistantDraft(action = null) {
      if (!this.draftAssistantId) return;
      const target = this.messages.find((msg) => msg.id === this.draftAssistantId);
      if (target) {
        target.action = action;
        if (!target.content.trim()) {
          target.content = "요약 응답이 완료되었습니다.";
        }
      }
      this.draftAssistantId = null;
      this.isStreaming = false;
      this.persistMessages();
    },
    failAssistantDraft(message) {
      if (this.draftAssistantId) {
        const target = this.messages.find((msg) => msg.id === this.draftAssistantId);
        if (target) {
          target.content = message;
          target.action = null;
        }
        this.draftAssistantId = null;
      } else {
        this.messages.push({ id: nextId(), role: "assistant", content: message, action: null });
      }
      this.isStreaming = false;
      this.persistMessages();
    },
    setStreaming(value) {
      this.isStreaming = value;
    },
    resetCurrentUserChat() {
      this.messages = initialMessages();
      this.draftAssistantId = null;
      this.isStreaming = false;
      this.persistMessages();
    },
  },
});
