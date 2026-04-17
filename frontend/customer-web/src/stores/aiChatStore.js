import { defineStore } from "pinia";

let sequence = 0;

function nextId() {
  sequence += 1;
  return `chat-${Date.now()}-${sequence}`;
}

export const useAiChatStore = defineStore("aiChatStore", {
  state: () => ({
    isOpen: false,
    isStreaming: false,
    draftAssistantId: null,
    messages: [
      {
        id: nextId(),
        role: "assistant",
        content: "ChemiLog AI 영양사입니다. 현재 식단을 기반으로 개선 포인트를 알려드릴게요.",
        action: null,
      },
    ],
  }),
  actions: {
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
    },
    setStreaming(value) {
      this.isStreaming = value;
    },
  },
});
