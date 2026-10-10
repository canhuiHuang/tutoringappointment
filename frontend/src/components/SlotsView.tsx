import { useEffect, useState, type FormEvent } from "react";
import { useSelector } from "react-redux";
import type { RootState } from "@/store/store";
import * as tutoring from "@/services/tutoring";
import type { Appointment, Slot, SlotFilters, TutoringService } from "@/utils/interfaces";
import SlotCalendar from "./SlotCalendar";
import BookingPopup from "./BookingPopup";
import { formatLocalDate, weekStart, addDays } from "@/utils/tools";

export default function SlotsView({ provider = false }: { provider?: boolean }) {
  const { providerId } = useSelector((state: RootState) => state.auth);
  const [services, setServices] = useState<TutoringService[]>([]);
  const [slots, setSlots] = useState<Slot[]>([]);
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [filters, setFilters] = useState<SlotFilters>({
    providerId: provider ? String(providerId) : "", serviceId: "", date: weekStart(),
  });
  const [week, setWeek] = useState(() => weekStart());
  const [selected, setSelected] = useState<Slot | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    let active = true;
    const initial = { providerId: provider ? String(providerId) : "", serviceId: "", date: weekStart() };
    Promise.all([tutoring.getServices(), tutoring.getSlots(initial, initial.date),
      provider ? Promise.resolve([]) : tutoring.getAppointments(false)])
      .then(([availableServices, availableSlots, mine]) => {
        if (active) { setServices(availableServices); setSlots(availableSlots); setAppointments(mine); }
      }).catch((cause: Error) => { if (active) setError(cause.message); });
    return () => { active = false; };
  }, [provider, providerId]);

  async function run(action: () => Promise<void>) {
    setBusy(true); setError(""); setMessage("");
    try { await action(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Request failed."); }
    finally { setBusy(false); }
  }

  async function refresh() {
    setSlots(await tutoring.getSlots(filters, week));
    if (!provider) setAppointments(await tutoring.getAppointments(false));
  }

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const next = { providerId: String(data.get("providerId")), serviceId: String(data.get("serviceId")),
      date: weekStart(String(data.get("date")) || week) };
    void run(async () => { setSlots(await tutoring.getSlots(next, next.date)); setFilters(next); setWeek(next.date); });
  }

  function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    void run(async () => {
      await tutoring.createSlot(Number(data.get("serviceId")), String(data.get("startTime")), String(data.get("endTime")));
      form.reset(); await refresh(); setMessage("Slot created.");
    });
  }

  function changeWeek(next: string) {
    void run(async () => { setSlots(await tutoring.getSlots(filters, next)); setWeek(next); });
  }

  const ownServices = services.filter((service) => service.providerId === providerId);
  const providers = [...new Map(services.map((service) => [service.providerId, service.providerName])).entries()];
  return <>
    {error && <p className="error" role="alert">{error}</p>}
    {message && <p className="message" role="status">{message}</p>}
    {provider && <section><h3>Create availability</h3><form onSubmit={create}>
      <label>Service<select name="serviceId" required defaultValue=""><option value="" disabled>Choose a service</option>
        {ownServices.map((service) => <option key={service.id} value={service.id}>{service.name} ({service.durationMinutes} minutes)</option>)}</select></label>
      <label>Start<input name="startTime" type="datetime-local" required /></label>
      <label>End<input name="endTime" type="datetime-local" required /></label><button disabled={busy}>Create slot</button>
    </form><p>Slot length must match the service duration.</p></section>}
    <form onSubmit={applyFilters} key={week}>
      {provider ? <input name="providerId" type="hidden" value={providerId ?? ""} />
        : <label>Provider<select name="providerId" defaultValue={filters.providerId}><option value="">All</option>{providers.map(([id, name]) => <option key={id} value={id}>{name}</option>)}</select></label>}
      <label>Service<select name="serviceId" defaultValue={filters.serviceId}><option value="">All</option>{(provider ? ownServices : services).map((service) =>
        <option key={service.id} value={service.id}>{service.name} — {service.providerName}</option>)}</select></label>
      <label>Week containing<input name="date" type="date" defaultValue={week} /></label>
      <button disabled={busy}>Apply filters</button>
    </form>
    {provider ? <div className="table-scroll"><table><thead><tr><th>Service</th><th>Start</th><th>End</th><th>Action</th></tr></thead>
      <tbody>{slots.map((slot) => <tr key={slot.id}><td>{slot.serviceName}</td><td>{formatLocalDate(slot.startTime)}</td><td>{formatLocalDate(slot.endTime)}</td>
        <td><button disabled={busy} onClick={() => void run(async () => { await tutoring.removeSlot(slot.id); await refresh(); setMessage("Slot removed."); })}>Remove</button></td>
      </tr>)}</tbody></table>{!slots.length && <p>No available slots.</p>}</div>
      : <SlotCalendar slots={slots} appointments={appointments} week={week} busy={busy} onSelect={setSelected} />}
    <div className="actions"><button disabled={busy} onClick={() => changeWeek(addDays(week, -7))}>Previous week</button>
      <span>{formatLocalDate(week + "T00:00:00").split(" ")[0]} – {formatLocalDate(addDays(week, 6) + "T00:00:00").split(" ")[0]}</span><button disabled={busy} onClick={() => changeWeek(addDays(week, 7))}>Next week</button>
      <button disabled={busy} onClick={() => void run(refresh)}>Refresh</button></div>
    {selected && <BookingPopup slot={selected} onClose={() => setSelected(null)} onBooked={refresh} />}
  </>;
}
