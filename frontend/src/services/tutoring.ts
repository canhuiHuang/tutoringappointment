import { addDays } from "@/utils/tools";
import api from "./index";
import type {
  Appointment,
  Session,
  Slot,
  SlotFilters,
  TutoringService,
} from "@/utils/interfaces";

export async function getSession() {
  return (await api.get<Session>("/api/auth/session")).data;
}
export async function login(username: string, password: string) {
  return (await api.post<Session>("/api/auth/login", { username, password }))
    .data;
}
export async function logout() {
  return (await api.post<Session>("/api/auth/logout")).data;
}
export async function getServices() {
  return (await api.get<TutoringService[]>("/api/services")).data;
}
export async function getSlots(filters: SlotFilters, week: string) {
  const slots: Slot[] = [];
  let page = 0;
  let batch: Slot[];
  // Fetch the whole week, including weeks with more than 100 matching slots.
  // The API still uses SQL LIMIT/OFFSET internally.
  do {
    batch = (await api.get<Slot[]>("/slots", {
      params: {
        providerId: filters.providerId || undefined,
        serviceId: filters.serviceId || undefined,
        date: week,
        endDate: addDays(week, 7),
        size: 100,
        page,
      },
    })).data;
    slots.push(...batch);
    page++;
  } while (batch.length === 100);
  return slots;
}
export async function bookSlot(slot: Slot) {
  return (
    await api.post<{ appointmentId: number }>("/api/customer/appointments", {
      slotId: slot.id,
    })
  ).data;
}
export async function getAppointments(provider: boolean) {
  const role = provider ? "provider" : "customer";
  return (await api.get<Appointment[]>("/api/" + role + "/appointments")).data;
}
export async function cancelAppointment(id: number) {
  await api.post("/api/customer/appointments/" + id + "/cancel");
}
export async function createSlot(
  serviceId: number,
  startTime: string,
  endTime: string,
) {
  await api.post("/api/provider/slots", { serviceId, startTime, endTime });
}
export async function removeSlot(id: number) {
  await api.delete("/api/provider/slots/" + id);
}
