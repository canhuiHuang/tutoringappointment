import AppointmentList from "@/components/AppointmentList";
export default function ProviderPage() {
  return (
    <section>
      <h2>Provider portal</h2>
      <h3>Appointments booked with me</h3>
      <AppointmentList provider />
    </section>
  );
}
