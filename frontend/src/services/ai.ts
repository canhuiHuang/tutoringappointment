import api from "./index";

export interface PendingAction {
  confirmationId: string;
  action: "BOOK" | "CANCEL";
  targetId: number;
  summary: string;
  expiresAt: string;
}

export interface ChatReply {
  answer: string;
  pending: PendingAction | null;
  sources: { source: string; text: string }[];
}

export async function aiStatus() {
  return (await api.get<{ enabled: boolean }>("/api/customer/ai/status")).data;
}

export async function sendChat(message: string, policyOnly: boolean) {
  const endpoint = policyOnly ? "/api/customer/ai/ask" : "/api/customer/ai/chat";
  return (await api.post<ChatReply>(endpoint, { message })).data;
}

export async function confirmAction(confirmationId: string) {
  return (await api.post<ChatReply>("/api/customer/ai/confirm", { confirmationId })).data;
}

export async function clearChat() {
  await api.delete("/api/customer/ai/chat");
}

export async function runReminders() {
  return (await api.post<{ scanned: number; sent: number; failed: number; skipped: number }>(
    "/api/provider/ai/reminders/run")).data;
}
