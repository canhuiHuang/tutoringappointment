import { useEffect, useRef, useState } from "react";
import { Link } from "react-router";
import * as tutoring from "@/services/tutoring";
import type { Slot } from "@/utils/interfaces";
import { formatLocalDate } from "@/utils/tools";

export default function BookingPopup({ slot, onClose, onBooked }: {
  slot: Slot; onClose: () => void; onBooked: () => Promise<void>;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const [appointmentId, setAppointmentId] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => { dialog.current?.showModal(); }, []);

  async function book() {
    setBusy(true); setError("");
    try {
      const result = await tutoring.bookSlot(slot);
      setAppointmentId(result.appointmentId);
      await onBooked();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Booking failed.");
      try { await onBooked(); } catch { /* Keep the original error. */ }
    } finally { setBusy(false); }
  }

  return <dialog ref={dialog} onCancel={(event) => { if (busy) event.preventDefault(); else onClose(); }}>
    <h2>{appointmentId === null ? "Book appointment" : "Booking confirmed!"}</h2>
    <dl><dt>Tutor</dt><dd>{slot.providerName}</dd><dt>Service</dt><dd>{slot.serviceName}</dd>
      <dt>Start</dt><dd>{formatLocalDate(slot.startTime)}</dd><dt>End</dt><dd>{formatLocalDate(slot.endTime)}</dd></dl>
    {error && <p className="error" role="alert">{error}</p>}
    {appointmentId === null ? <button disabled={busy} onClick={() => void book()}>Confirm booking</button>
      : <><p role="status">Appointment number: {appointmentId}</p><Link to="/customer/appointments">View details</Link></>}
    <button disabled={busy} onClick={onClose}>Close</button>
  </dialog>;
}