// Lightweight inline SVG icon set (Lucide-style), replacing emoji across the
// redesigned UI. One component, name-keyed — <Icon name="calendar" size={18} />.
// Purely presentational: no state, no side effects.

const PATHS = {
  grid: <><rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/></>,
  calendar: <><rect x="3" y="4.5" width="18" height="16" rx="2.5"/><path d="M3 9h18M8 2.5v4M16 2.5v4"/></>,
  users: <><circle cx="9" cy="8" r="3.2"/><path d="M3.5 20c0-3.3 2.5-5.2 5.5-5.2s5.5 1.9 5.5 5.2"/><path d="M16 5.2a3 3 0 0 1 0 5.8M17.5 20c0-2.4-1-4-2.5-4.9"/></>,
  clipboard: <><rect x="5" y="4" width="14" height="17" rx="2.5"/><path d="M9 4V3.2A1.2 1.2 0 0 1 10.2 2h3.6A1.2 1.2 0 0 1 15 3.2V4M9 10h6M9 14h4"/></>,
  clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7.5V12l3 2"/></>,
  dollar: <path d="M12 2.5v19M16.5 6.5c-.8-1.4-2.5-2.2-4.5-2.2-2.8 0-4.5 1.4-4.5 3.4 0 4.6 9 2.4 9 7 0 2-1.9 3.5-4.5 3.5-2.2 0-3.9-.9-4.7-2.4"/>,
  bar: <path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/>,
  trend: <path d="M3 17l6-6 4 4 8-8M15 7h6v6"/>,
  settings: <><circle cx="12" cy="12" r="3.2"/><path d="M19.4 13a7.9 7.9 0 0 0 0-2l2-1.5-2-3.4-2.3 1a7.6 7.6 0 0 0-1.7-1l-.3-2.6h-4l-.3 2.6a7.6 7.6 0 0 0-1.7 1l-2.3-1-2 3.4 2 1.5a7.9 7.9 0 0 0 0 2l-2 1.5 2 3.4 2.3-1a7.6 7.6 0 0 0 1.7 1l.3 2.6h4l.3-2.6a7.6 7.6 0 0 0 1.7-1l2.3 1 2-3.4z"/></>,
  logout: <path d="M15 4h3.5A1.5 1.5 0 0 1 20 5.5v13a1.5 1.5 0 0 1-1.5 1.5H15M10 8l-4 4 4 4M6 12h11"/>,
  search: <><circle cx="11" cy="11" r="7"/><path d="M20 20l-3.2-3.2"/></>,
  bell: <path d="M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6M10 20a2 2 0 0 0 4 0"/>,
  video: <><rect x="3" y="6" width="12" height="12" rx="2.5"/><path d="M15 10l6-3v10l-6-3"/></>,
  pin: <><path d="M12 21s7-6 7-11a7 7 0 0 0-14 0c0 5 7 11 7 11z"/><circle cx="12" cy="10" r="2.6"/></>,
  chevron: <path d="M9 6l6 6-6 6"/>,
  back: <path d="M15 6l-6 6 6 6"/>,
  heart: <path d="M12 20s-7-4.6-9.3-9C1 7.5 3 4 6.5 4 9 4 10.5 5.7 12 7.5 13.5 5.7 15 4 17.5 4 21 4 23 7.5 21.3 11 19 15.4 12 20 12 20z"/>,
  check: <><circle cx="12" cy="12" r="9"/><path d="M8.5 12.5l2.3 2.3 4.7-5"/></>,
  plus: <path d="M12 5v14M5 12h14"/>,
  edit: <><path d="M4 20h4L18.5 9.5a2 2 0 0 0-2.8-2.8L5 17.2V20z"/><path d="M14 7l3 3"/></>,
  download: <path d="M12 3v12M7 11l5 5 5-5M4 20h16"/>,
  mail: <><rect x="3" y="5" width="18" height="14" rx="2.5"/><path d="M4 7l8 6 8-6"/></>,
  phone: <path d="M5 4h3l2 5-2.5 1.5a11 11 0 0 0 5 5L14 14l5 2v3a2 2 0 0 1-2 2A15 15 0 0 1 3 6a2 2 0 0 1 2-2z"/>,
  shield: <><path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z"/><path d="M9 12l2 2 4-4"/></>,
  activity: <path d="M3 12h4l2.5-7 5 14 2.5-7H21"/>,
  sun: <><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M2 12h2M20 12h2M5 5l1.5 1.5M17.5 17.5L19 19M19 5l-1.5 1.5M6.5 17.5L5 19"/></>,
  moon: <path d="M20 14.5A8.5 8.5 0 0 1 9.5 4a8.5 8.5 0 1 0 10.5 10.5z"/>,
  star: <path d="M12 3l2.6 5.6L21 9.3l-4.5 4.3L17.6 20 12 16.8 6.4 20l1.1-6.4L3 9.3l6.4-.7z"/>,
  x: <path d="M6 6l12 12M18 6L6 18"/>,
  alert: <><path d="M12 3l9.5 16.5H2.5L12 3z"/><path d="M12 9v5M12 17.5h.01"/></>,
  server: <><rect x="3" y="4" width="18" height="7" rx="2"/><rect x="3" y="13" width="18" height="7" rx="2"/><path d="M7 7.5h.01M7 16.5h.01"/></>,
  // Schedule screen
  refresh: <><path d="M20.5 12a8.5 8.5 0 1 1-2.6-6.1"/><path d="M20.5 4.2V10h-5.6"/></>,
  zap: <path d="M13 2.5L5.5 13.5H11l-1 8 7.5-11H12z"/>,
  umbrella: <><path d="M12 3.2a8.8 8.8 0 0 1 8.8 8.8H3.2A8.8 8.8 0 0 1 12 3.2z"/><path d="M12 12v6.4a2.4 2.4 0 0 0 4.8 0"/><path d="M12 3.2V1.8"/></>,
  ban: <><circle cx="12" cy="12" r="9"/><path d="M5.6 5.6l12.8 12.8"/></>,
  flag: <><path d="M5.5 21V3.5"/><path d="M5.5 3.5h9.8l-1.5 4 1.5 4H5.5"/></>,
  chat: <path d="M20 11.8a7.4 7.4 0 0 1-7.4 7.4H9.2L4.6 22v-5.1A7.4 7.4 0 0 1 12.6 4.4a7.4 7.4 0 0 1 7.4 7.4z"/>,
  cursor: <path d="M6.5 3.2l12.4 7.2-5.2 1.5-1.6 5.2z"/>,
};

export default function Icon({ name, size = 20, className = "", strokeWidth = 1.9, style }) {
  const p = PATHS[name];
  if (!p) return null;
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor"
      strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round"
      className={className} style={style} aria-hidden="true" focusable="false">
      {p}
    </svg>
  );
}
