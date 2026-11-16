const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/** "2026-11-10" -> "10 Nov 2026" (parsed manually so the time zone can never shift the day). */
export function formatDate(isoDate) {
  if (!isoDate) return '—';
  const [year, month, day] = isoDate.split('T')[0].split('-').map(Number);
  if (!year || !month || !day) return isoDate;
  return `${day} ${MONTHS[month - 1]} ${year}`;
}

/** "2026-11-20T14:30:00" (no zone, as sent by the API) -> "20 Nov 2026, 14:30". */
export function formatDateTime(localDateTime) {
  if (!localDateTime) return '—';
  const [datePart, timePart = '00:00'] = localDateTime.split('T');
  return `${formatDate(datePart)}, ${timePart.slice(0, 5)}`;
}

/** Instant from the API ("2026-11-10T09:39:00.123Z") -> "10 Nov 2026, 15:09" in the viewer's local time zone. */
export function formatInstant(instant) {
  if (!instant) return '—';
  const date = new Date(instant);
  if (Number.isNaN(date.getTime())) return instant;
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getDate()} ${MONTHS[date.getMonth()]} ${date.getFullYear()}, ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/** "2026-11" -> "Nov 2026" */
export function formatMonth(yearMonth) {
  const [year, month] = yearMonth.split('-').map(Number);
  return `${MONTHS[month - 1]} ${year}`;
}

/** Value for <input type="datetime-local">: "2026-11-20T14:30:00" -> "2026-11-20T14:30". */
export function toDateTimeInput(localDateTime) {
  return localDateTime ? localDateTime.slice(0, 16) : '';
}

/** Today as yyyy-MM-dd in the user's local time zone. */
export function todayIso() {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

/** Current local time as "yyyy-MM-ddTHH:mm" (same format the API returns, minus seconds). */
export function nowLocalDateTime() {
  const now = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${todayIso()}T${pad(now.getHours())}:${pad(now.getMinutes())}`;
}

/** Empty / whitespace-only strings become null so optional fields are stored as NULL. */
export function blankToNull(value) {
  if (value === undefined || value === null) return null;
  const trimmed = String(value).trim();
  return trimmed === '' ? null : trimmed;
}
