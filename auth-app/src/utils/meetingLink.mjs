export function safeMeetUrl(value) {
  try {
    const url = new URL(value);
    return url.protocol === "https:" && url.hostname === "meet.google.com"
      && !url.username && !url.password && !url.port
      && /^\/[a-z]{3}-[a-z]{4}-[a-z]{3}$/.test(url.pathname) ? url.href : null;
  } catch { return null; }
}

export function meetingView(appointment, modeType) {
  const status = appointment?.status ?? appointment?.appointmentStatus;
  if (modeType !== "ONLINE" || !["CONFIRMED", "RESCHEDULED"].includes(status)) return null;
  // An older production backend has no meeting fields; keep its UI unchanged.
  if (!appointment?.meetingStatus) return null;
  if (appointment.meetingStatus === "NOT_APPLICABLE") return null;
  const url = appointment.meetingStatus === "READY" ? safeMeetUrl(appointment.meetingUrl) : null;
  return { url, failed: appointment.meetingStatus === "FAILED" || appointment.meetingStatus === "READY" && !url };
}
