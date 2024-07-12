// Formats a date (without time) similar to RFC 3339 but in the local time zone.
export function standardDateString(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth()).padStart(
    2,
    "0"
  )}-${String(date.getDate()).padStart(2, "0")}`;
}
