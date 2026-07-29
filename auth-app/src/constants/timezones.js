/**
 * Timezones offered when a therapist creates or edits their profile.
 *
 * Shared between TherapistSetupPage (profile creation) and TherapistProfilePage
 * (edit) so the two cannot drift. A therapist whose timezone is null has every
 * calendar invite built in a fallback zone — NotificationService logs
 * "Invalid timezone 'null' ... using fallback" — so creation must collect it,
 * not just editing.
 */
export const TIMEZONES = [
  { value: "Asia/Kolkata", label: "Asia/Kolkata (IST, UTC+5:30)" },
  { value: "Asia/Dubai", label: "Asia/Dubai (GST, UTC+4)" },
  { value: "Asia/Singapore", label: "Asia/Singapore (SGT, UTC+8)" },
  { value: "Asia/Tokyo", label: "Asia/Tokyo (JST, UTC+9)" },
  { value: "Europe/London", label: "Europe/London (GMT/BST)" },
  { value: "Europe/Paris", label: "Europe/Paris (CET/CEST, UTC+1/+2)" },
  { value: "America/New_York", label: "America/New_York (EST/EDT)" },
  { value: "America/Chicago", label: "America/Chicago (CST/CDT)" },
  { value: "America/Denver", label: "America/Denver (MST/MDT)" },
  { value: "America/Los_Angeles", label: "America/Los_Angeles (PST/PDT)" },
  { value: "UTC", label: "UTC" },
];

/** The browser's zone when we offer it, otherwise the practice's home zone. */
export function detectTimezone() {
  try {
    const zone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    return TIMEZONES.some(t => t.value === zone) ? zone : "Asia/Kolkata";
  } catch {
    return "Asia/Kolkata";
  }
}
