import type { Appointment, Slot } from "@/utils/interfaces";
import { addDays } from "@/utils/tools";

function time(value: string) {
  return new Date(value).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

interface Props {
  slots: Slot[];
  appointments: Appointment[];
  week: string;
  busy: boolean;
  onSelect: (slot: Slot) => void;
}

export default function SlotCalendar({ slots, appointments, week, busy, onSelect }: Props) {
  const end = addDays(week, 7);
  const mine = appointments.filter((item) => item.status === "BOOKED"
    && item.startTime.slice(0, 10) >= week && item.startTime.slice(0, 10) < end);

  return <>
    <p><span className="legend available">Available</span> <span className="legend booked">Your bookings</span></p>
    <h3>Week of {new Date(week + "T00:00:00").toLocaleDateString()}</h3>
    {!slots.length && !mine.length && <p>No available slots or bookings this week.</p>}
    <div className="calendar-scroll"><div className="calendar">
      {Array.from({ length: 7 }, (_, day) => {
        const key = addDays(week, day);
        const current = new Date(key + "T00:00:00");
        return <div className="calendar-day" key={key}>
          <h4>{current.toLocaleDateString([], { weekday: "short", month: "short", day: "numeric" })}</h4>
          {slots.filter((slot) => slot.startTime.slice(0, 10) === key).map((slot) =>
            <button className="available calendar-slot" key={slot.id} disabled={busy} onClick={() => onSelect(slot)}>
              {time(slot.startTime)}–{time(slot.endTime)}<br />{slot.serviceName}<br />{slot.providerName}
            </button>)}
          {mine.filter((item) => item.startTime.slice(0, 10) === key).map((item) =>
            <div className="booked calendar-slot" key={item.id}>{time(item.startTime)}–{time(item.endTime)}<br />{item.serviceName}<br />Booked by you</div>)}
        </div>;
      })}
    </div></div>
  </>;
}