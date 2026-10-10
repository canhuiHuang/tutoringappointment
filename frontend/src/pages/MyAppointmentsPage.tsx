import AppointmentList from "@/components/AppointmentList";
export default function MyAppointmentsPage() {
  return (
    <section>
      <h2>My appointments</h2>
      <AppointmentList provider={false} />
    </section>
  );
}
