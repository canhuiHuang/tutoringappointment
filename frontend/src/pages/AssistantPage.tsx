import { useEffect, useState, type FormEvent } from "react";
import {
  aiStatus,
  clearChat,
  confirmAction,
  sendChat,
  type ChatReply,
} from "@/services/ai";

export default function AssistantPage() {
  const [enabled, setEnabled] = useState(false);
  const [message, setMessage] = useState("");
  const [policyOnly, setPolicyOnly] = useState(false);
  const [messages, setMessages] = useState<{ role: string; text: string }[]>(
    [],
  );
  const [reply, setReply] = useState<ChatReply | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    aiStatus()
      .then((status) => {
        if (active) setEnabled(status.enabled);
      })
      .catch((cause: Error) => {
        if (active) setError(cause.message);
      });
    return () => {
      active = false;
    };
  }, []);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!message.trim() || busy) return;
    const question = message.trim();
    setMessage("");
    setError("");
    setBusy(true);
    setReply(null);
    setMessages((previous) => [...previous, { role: "You", text: question }]);
    try {
      const result = await sendChat(question, policyOnly);
      setReply(result);
      setMessages((previous) => [
        ...previous,
        { role: "Assistant", text: result.answer },
      ]);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Request failed.");
    } finally {
      setBusy(false);
    }
  }

  async function confirm() {
    if (!reply?.pending || busy) return;
    setBusy(true);
    setError("");
    try {
      const result = await confirmAction(reply.pending.confirmationId);
      setReply(result);
      setMessages((previous) => [
        ...previous,
        { role: "Assistant", text: result.answer },
      ]);
    } catch (cause) {
      setReply(null);
      setError(
        cause instanceof Error
          ? cause.message
          : "Confirmation failed. Ask for another slot.",
      );
    } finally {
      setBusy(false);
    }
  }

  async function reset() {
    setBusy(true);
    setError("");
    try {
      await clearChat();
      setMessages([]);
      setReply(null);
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : "Could not clear conversation.",
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <section>
      <h2>Tutoring assistant</h2>
      <p>
        Find slots, view your appointments, or ask about tutoring policies.
        Booking and cancellation always require the Confirm button below.
      </p>
      {!enabled && (
        <p>
          AI is not enabled on this server. You can still use Available slots
          and My appointments.
        </p>
      )}
      <p>
        Try: “Find the earliest Algebra slot in the next seven days and book
        it.”
      </p>
      <div className="chat-history" aria-live="polite">
        {messages.map((item, index) => (
          <p key={index}>
            <strong>{item.role}:</strong> {item.text}
          </p>
        ))}
      </div>
      <form className="assistant-form" onSubmit={(event) => void submit(event)}>
        <label>
          Message
          <textarea
            rows={3}
            maxLength={2000}
            required
            value={message}
            onChange={(event) => setMessage(event.target.value)}
            disabled={!enabled || busy}
          />
        </label>
        <div className="form-actions">
          <label>
            <span>
              <input
                type="checkbox"
                checked={policyOnly}
                onChange={(event) => setPolicyOnly(event.target.checked)}
                disabled={busy}
              />{" "}
              Policy Q&amp;A only
            </span>
          </label>
          <button disabled={!enabled || busy}>Send</button>
          <button type="button" onClick={() => void reset()} disabled={busy}>
            Clear conversation
          </button>
        </div>
      </form>
      {busy && <p role="status">Working…</p>}
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {reply?.pending && (
        <section>
          <h3>Please confirm</h3>
          <p>{reply.pending.summary}</p>
          <button onClick={() => void confirm()} disabled={busy}>
            Confirm{" "}
            {reply.pending.action === "BOOK" ? "booking" : "cancellation"}
          </button>
          <button onClick={() => void reset()} disabled={busy}>
            Discard
          </button>
        </section>
      )}
      {!!reply?.sources.length && (
        <section>
          <h3>Policy sources</h3>
          {reply.sources.map((source) => (
            <details key={source.source}>
              <summary>{source.source}</summary>
              <p>{source.text}</p>
            </details>
          ))}
        </section>
      )}
    </section>
  );
}
