import { useEffect, useState } from "react";
import * as tutoring from "@/services/tutoring";
import type { Appointment } from "@/utils/interfaces";
import { formatLocalDate } from "@/utils/tools";

export default function AppointmentList({ provider }: { provider: boolean }) {
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    let active = true;
    tutoring.getAppointments(provider).then((items) => { if (active) setAppointments(items); })
      .catch((cause: Error) => { if (active) setError(cause.message); });
    return () => { active = false; };
  }, [provider]);

  async function refresh(id?: number) {
    setBusy(true); setError("");
    try {
      if (id !== undefined) await tutoring.cancelAppointment(id);
      setAppointments(await tutoring.getAppointments(provider));
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Request failed."); }
    finally { setBusy(false); }
  }

  const now = new Date();
  const upcoming = appointments.filter((item) => item.status === "BOOKED" && new Date(item.endTime) > now);
  const history = appointments.filter((item) => !upcoming.includes(item));
  function table(items: Appointment[]) {
    if (!items.length) return <p>No appointments.</p>;
    return <div className="table-scroll"><table>
      <thead><tr><th>Service</th><th>{provider ? "Student" : "Tutor"}</th><th>Start</th><th>Status</th><th>Action</th></tr></thead>
      <tbody>{items.map((item) => <tr key={item.id}>
        <td>{item.serviceName}</td><td>{provider ? item.studentName : item.providerName}</td>
        <td>{formatLocalDate(item.startTime)}</td><td>{item.status}</td>
        <td>{!provider && item.status === "BOOKED" && new Date(item.startTime) > now
          && <button disabled={busy} onClick={() => void refresh(item.id)}>Cancel</button>}</td>
      </tr>)}</tbody>
    </table></div>;
  }
  return <>{error && <p className="error" role="alert">{error}</p>}
    <button disabled={busy} onClick={() => void refresh()}>Refresh</button>
    <h3>Upcoming</h3>{table(upcoming)}<h3>History</h3>{table(history)}
  </>;
}