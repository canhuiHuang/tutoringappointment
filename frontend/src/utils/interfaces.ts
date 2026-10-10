export interface User {
  id: number;
  name: string;
  role: "STUDENT" | "TUTOR";
}
export interface Session {
  user: User | null;
  providerId: number | null;
}
export interface TutoringService {
  id: number;
  providerId: number;
  name: string;
  description: string;
  durationMinutes: number;
  providerName: string;
}
export interface Slot {
  id: number;
  serviceId: number;
  providerName: string;
  serviceName: string;
  startTime: string;
  endTime: string;
  status: string;
}
export interface Appointment {
  id: number;
  studentName: string;
  providerName: string;
  serviceName: string;
  startTime: string;
  endTime: string;
  status: "BOOKED" | "CANCELLED" | "COMPLETED";
}
export interface SlotFilters {
  providerId: string;
  serviceId: string;
  date: string;
}
