export function dateKey(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

export function weekStart(value = dateKey(new Date())) {
  const date = new Date(value + "T00:00:00");
  date.setDate(date.getDate() - (date.getDay() + 6) % 7);
  return dateKey(date);
}

export function addDays(value: string, days: number) {
  const date = new Date(value + "T00:00:00");
  date.setDate(date.getDate() + days);
  return dateKey(date);
}

export function formatLocalDate(iso: string) {
  const d = new Date(iso);

  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");

  let h = d.getHours();
  const ampm = h >= 12 ? "pm" : "am";
  h = h % 12 || 12;

  const min = String(d.getMinutes()).padStart(2, "0");

  return `${yyyy}/${mm}/${dd} ${h}:${min}${ampm}`;
}
