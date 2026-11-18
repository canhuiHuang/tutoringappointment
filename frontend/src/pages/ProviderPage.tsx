import AppointmentList from "@/components/AppointmentList";
import { useState } from "react";
import { runReminders } from "@/services/ai";
export default function ProviderPage() {
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState("");
  async function remind() {
    setBusy(true); setResult("");
    try {
      const response = await runReminders();
      setResult(`Scanned ${response.scanned}; sent ${response.sent}; failed ${response.failed}; skipped ${response.skipped}.`);
    } catch (cause) {
      setResult(cause instanceof Error ? cause.message : "Reminder run failed.");
    } finally { setBusy(false); }
  }
  return (
    <section>
      <h2>Provider portal</h2>
      <h3>Appointments booked with me</h3>
      <AppointmentList provider />
      <h3>AI reminders</h3>
      <p>Generate reminders for your booked appointments starting in the next 24 hours.</p>
      <button disabled={busy} onClick={() => void remind()}>{busy ? "Running…" : "Run reminders now"}</button>
      {result && <p role="status">{result}</p>}
    </section>
  );
}
