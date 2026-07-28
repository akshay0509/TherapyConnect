import { useEffect, useState, useMemo, useRef, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { getAvailability, createAppointment, generateSlots, updateAppointmentStatus, rescheduleAppointment, createAvailabilityOverride, deleteAvailabilityOverride, bulkAvailabilityOverrides, getPaymentInfo, ensurePaymentLink } from "../api/appointments";
import { getTherapistClients, createSessionNotes } from "../api/therapistClients";
import api from "../api/client";
import { useModeMap, useAllModes } from "../context/DeliveryModesContext";
import SessionTimer from "../components/SessionTimer";
import Icon from "../components/icons";
import styles from "./AppointmentsPage.module.css";

// ─── Constants ────────────────────────────────────────────────────────────────
const DAY_SHORT = ["Sun","Mon","Tue","Wed","Thu","Fri","Sat"];
const MONTHS = ["January","February","March","April","May","June","July","August","September","October","November","December"];
const MONTHS_SHORT = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];
const STATUS_ICON = { CONFIRMED:"check", COMPLETED:"flag", CANCELLED:"x", ABANDONED:"alert", SCHEDULED:"calendar", RESCHEDULED:"refresh" };

// Status rides on the chip, the way the prototype does it — the timeline block
// itself only distinguishes booked (cyan) from live (green), instead of being
// recoloured six different ways.
const STATUS_CHIP = {
  CONFIRMED:   "online",
  SCHEDULED:   "warn",
  RESCHEDULED: "warn",
  COMPLETED:   "ok",
  CANCELLED:   "bad",
  ABANDONED:   "warn",
};

const CONFLICT_ERRORS = {
  SLOT_ALREADY_BOOKED:                  "This slot has already been booked.",
  SLOT_NOT_AVAILABLE:                   "This slot is no longer available.",
  APPOINTMENT_NOT_FOUND:                "Appointment could not be found.",
  INVALID_APPOINTMENT_STATUS_TRANSITION:"This status change is not allowed.",
  INVALID_APPOINTMENT_STATE:            "The appointment is in an invalid state for this action.",
  SCHEDULE_CONFLICT:                    "There is a scheduling conflict. Please choose a different slot.",
  INVALID_REQUEST:                      "The request contains invalid data. Please check your inputs.",
};

function friendlyError(message) {
  if (!message) return "Something went wrong. Please try again.";
  const key = Object.keys(CONFLICT_ERRORS).find(k => message.includes(k));
  return key ? CONFLICT_ERRORS[key] : message;
}
function titleCase(s) {
  return s ? s.charAt(0) + s.slice(1).toLowerCase() : "";
}

// ─── Time helpers ─────────────────────────────────────────────────────────────
const HOUR_START = 6;
const HOUR_END   = 22;
const TOTAL_HOURS = HOUR_END - HOUR_START;
const PX_PER_HOUR = 80;
const CANVAS_HEIGHT = TOTAL_HOURS * PX_PER_HOUR;
// visual separation between consecutive blocks, taken off the bottom only
const BLOCK_GAP = 4;
// availability grid size; a session rounds up to whole blocks
const BLOCK_MINUTES = 30;

function toMinutes(dt) {
  const d = new Date(dt);
  return d.getHours() * 60 + d.getMinutes();
}
function minutesToPx(minutes) {
  return ((minutes - HOUR_START * 60) / 60) * PX_PER_HOUR;
}
function pxToMinutes(px) {
  return Math.round((px / PX_PER_HOUR) * 60) + HOUR_START * 60;
}
function formatTime(dt) {
  if (!dt) return "—";
  return new Date(dt).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit", hour12: true });
}
function formatTimeFromMinutes(mins) {
  const h = Math.floor(mins / 60);
  const m = mins % 60;
  const ampm = h >= 12 ? "PM" : "AM";
  return `${h % 12 || 12}:${m.toString().padStart(2,"0")} ${ampm}`;
}
function toDateKey(dt) {
  const d = new Date(dt);
  return `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;
}
function getWeekStart(date) {
  const d = new Date(date); d.setHours(0,0,0,0);
  d.setDate(d.getDate() - d.getDay()); return d;
}
function addDays(date, n) {
  const d = new Date(date); d.setDate(d.getDate() + n); return d;
}
function toISODate(date) {
  const d = new Date(date);
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,"0")}-${String(d.getDate()).padStart(2,"0")}`;
}
function snapToSlot(minutes) { return Math.round(minutes / 30) * 30; }

// ─── Sub-components ───────────────────────────────────────────────────────────

function ClientDropdown({ clients, value, onChange }) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const ref = useRef(null);
  const selected = clients.find(c => c.clientId === value);
  const filtered = clients.filter(c => c.clientName?.toLowerCase().includes(search.toLowerCase()));
  useEffect(() => {
    const h = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener("mousedown", h); return () => document.removeEventListener("mousedown", h);
  }, []);
  return (
    <div className={styles.customDropdown} ref={ref}>
      <button type="button" className={`${styles.dropdownTrigger} ${open ? styles.dropdownTriggerOpen : ""}`} onClick={() => setOpen(o => !o)}>
        <span className={selected ? styles.dropdownValueSet : styles.dropdownPlaceholder}>{selected ? selected.clientName : "Select a client"}</span>
        <Icon name="chevron" size={16} className={`${styles.dropdownChevron} ${open ? styles.dropdownChevronOpen : ""}`} />
      </button>
      {open && (
        <div className={styles.dropdownMenu}>
          <div className={styles.dropdownSearch}>
            <span className={styles.dropdownSearchIcon}><Icon name="search" size={15} /></span>
            <input className={styles.dropdownSearchInput} type="text" placeholder="Search clients…" value={search} onChange={e => setSearch(e.target.value)} autoFocus />
          </div>
          <div className={styles.dropdownList}>
            {filtered.length === 0 && <div className={styles.dropdownEmpty}>No clients found</div>}
            {filtered.map(c => (
              <div key={c.clientId} className={`${styles.dropdownItem} ${c.clientId === value ? styles.dropdownItemActive : ""}`} onClick={() => { onChange(c.clientId, c.clientName); setOpen(false); setSearch(""); }}>
                <span className={styles.dropdownItemAvatar}>{c.clientName?.[0]?.toUpperCase() ?? "?"}</span>{c.clientName}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function serviceLabel(svc) {
  const name = (svc.serviceType || "").toLowerCase().split("_")
    .filter(Boolean)
    .map(w => w[0].toUpperCase() + w.slice(1))
    .join(" ");
  return name || svc.serviceId;
}

function ServiceDropdown({ services, value, onChange }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  const selected = services.find(s => s.serviceId === value);
  useEffect(() => {
    const h = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener("mousedown", h); return () => document.removeEventListener("mousedown", h);
  }, []);
  return (
    <div className={styles.customDropdown} ref={ref}>
      <button type="button" className={`${styles.dropdownTrigger} ${open ? styles.dropdownTriggerOpen : ""}`} onClick={() => setOpen(o => !o)}>
        <span className={selected ? styles.dropdownValueSet : styles.dropdownPlaceholder}>
          {selected ? `${serviceLabel(selected)} · ${selected.duration} min` : "Select a service"}
        </span>
        <Icon name="chevron" size={16} className={`${styles.dropdownChevron} ${open ? styles.dropdownChevronOpen : ""}`} />
      </button>
      {open && (
        <div className={styles.dropdownMenu}>
          <div className={styles.dropdownList}>
            {services.length === 0 && <div className={styles.dropdownEmpty}>No active services</div>}
            {services.map(svc => (
              <div key={svc.serviceId} className={`${styles.dropdownItem} ${svc.serviceId === value ? styles.dropdownItemActive : ""}`}
                onClick={() => { onChange(svc.serviceId); setOpen(false); }}>
                <span className={styles.dropdownItemAvatar}><Icon name="clipboard" size={15} /></span>
                <span>{serviceLabel(svc)}</span>
                <span className={styles.modePrice}> · {svc.duration} min</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function ModeDropdown({ modes, value, onChange }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  const selected = modes.find(m => m.modeId === value);
  useEffect(() => {
    const h = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener("mousedown", h); return () => document.removeEventListener("mousedown", h);
  }, []);
  const modeIcon = { ONLINE: "video", OFFLINE_AT_HALUSURU: "pin", OFFLINE_AT_SESHADRIPURAM: "pin" };
  return (
    <div className={styles.customDropdown} ref={ref}>
      <button type="button" className={`${styles.dropdownTrigger} ${open ? styles.dropdownTriggerOpen : ""}`} onClick={() => setOpen(o => !o)}>
        <span className={selected ? styles.dropdownValueSet : styles.dropdownPlaceholder}>
          {selected ? (
            <><Icon name={modeIcon[selected.modeType] ?? "chat"} size={15} /> {selected.displayName}</>
          ) : "Select delivery mode"}
        </span>
        <Icon name="chevron" size={16} className={`${styles.dropdownChevron} ${open ? styles.dropdownChevronOpen : ""}`} />
      </button>
      {open && (
        <div className={styles.dropdownMenu}>
          <div className={styles.dropdownList}>
            {modes.length === 0 && <div className={styles.dropdownEmpty}>No modes available for this service</div>}
            {modes.map(m => (
              <div key={m.modeId} className={`${styles.dropdownItem} ${m.modeId === value ? styles.dropdownItemActive : ""}`} onClick={() => { onChange(m.modeId); setOpen(false); }}>
                <span className={styles.dropdownItemAvatar}><Icon name={modeIcon[m.modeType] ?? "chat"} size={15} /></span>
                <span>{m.displayName}</span>
                {m.price != null && <span className={styles.modePrice}> · ₹{parseFloat(m.price).toFixed(0)}</span>}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Main component ───────────────────────────────────────────────────────────
export default function AppointmentsPage() {
  const navigate = useNavigate();
  const now = new Date();
  const canvasRef = useRef(null);
  const timelineRef = useRef(null);
  const panelRef = useRef(null);
  // The timeline matches the booking panel's height rather than the panel
  // scrolling: the panel sizes to its content and the canvas scrolls inside.
  const [timelineHeight, setTimelineHeight] = useState(null);
  const modeMap = useModeMap();
  const allModes = useAllModes();

  // Week / day selection
  const [weekStart, setWeekStart] = useState(() => getWeekStart(new Date()));
  const [selectedDate, setSelectedDate] = useState(() => { const d = new Date(); d.setHours(0,0,0,0); return d; });

  // Data
  const [slots, setSlots] = useState([]);
  const [appointments, setAppointments] = useState([]);
  const [overrides, setOverrides] = useState([]);
  const [loadingSlots, setLoadingSlots] = useState(true);
  const [slotsError, setSlotsError] = useState(null);
  const [clients, setClients] = useState([]);
  // Availability blocks are a fixed 30-minute grid now, so the appointment
  // length comes from the service chosen at booking time.
  const [services, setServices] = useState([]);

  // Today's appointments back the session timer. They're loaded separately from
  // the week view so the timer survives the therapist browsing another week —
  // which is exactly what happens when they book a follow-up mid-session.
  const [todayAppointments, setTodayAppointments] = useState([]);

  // Coarse re-render so the red "now" line drifts down the timeline.
  // The timer keeps its own 1s clock, so this doesn't need to be frequent.
  const [, setNowTick] = useState(0);

  useEffect(() => {
    api.get("/therapist/therapist-services")
      .then(res => setServices((res.data ?? []).filter(svc => svc.isActive !== false)))
      .catch(() => setServices([]));
  }, []);

  // Drag to create override
  const [dragging, setDragging] = useState(false);
  const [dragStart, setDragStart] = useState(null);
  const [dragEnd, setDragEnd] = useState(null);
  const dragRef = useRef({ active: false, startY: 0 });

  const [panel, setPanel] = useState(null);
  const [panelSlot, setPanelSlot] = useState(null);

  // Booking form
  const [booking, setBooking] = useState({ clientId: "", clientName: "", serviceId: "", modeId: "", useCustomPrice: false, customPrice: "" });
  const [bookingModes, setBookingModes] = useState([]); // modes for the slot's service
  const [bookingLoading, setBookingLoading] = useState(false);
  const [bookingError, setBookingError] = useState(null);
  const [bookingSuccess, setBookingSuccess] = useState(false);
  // { appointmentId, status, url } — set when the booking response includes payment info
  const [bookingPayment, setBookingPayment] = useState(null);
  const [linkCopied, setLinkCopied] = useState(false);

  // Update status
  const [updateStatus, setUpdateStatus] = useState("");
  const [updateReason, setUpdateReason] = useState("");
  const [updateLoading, setUpdateLoading] = useState(false);
  const [updateError, setUpdateError] = useState(null);
  const [panelPayment, setPanelPayment] = useState(null); // payment info for the selected appointment
  const [paymentRetryLoading, setPaymentRetryLoading] = useState(false);
  // Session notes, offered at the moment a session is marked COMPLETED —
  // the one point where the therapist has just finished and has it fresh.
  const [sessionNotes, setSessionNotes] = useState("");
  const [notesWarning, setNotesWarning] = useState(null);

  // Reschedule
  const [reschedWeekStart, setReschedWeekStart] = useState(() => getWeekStart(new Date()));
  const [reschedSelectedDate, setReschedSelectedDate] = useState(null);
  const [reschedNewSlot, setReschedNewSlot] = useState(null);
  const [reschedModeId, setReschedModeId] = useState("");
  const [reschedReason, setReschedReason] = useState("");
  const [reschedLoading, setReschedLoading] = useState(false);
  const [reschedError, setReschedError] = useState(null);

  // Override (drag result)
  const [overrideRange, setOverrideRange] = useState(null);
  const [overrideNote, setOverrideNote] = useState("");
  const [overrideIsAvailable, setOverrideIsAvailable] = useState(false);
  const [overrideSyncGcal, setOverrideSyncGcal] = useState(false);
  const [overrideLoading, setOverrideLoading] = useState(false);
  const [overrideError, setOverrideError] = useState(null);
  const [overrideDeleteLoading, setOverrideDeleteLoading] = useState(false);

  // Generate slots
  const [genStartDate, setGenStartDate] = useState("");
  const [genEndDate, setGenEndDate] = useState("");
  const [genLoading, setGenLoading] = useState(false);
  const [genError, setGenError] = useState(null);
  const [genSuccess, setGenSuccess] = useState(false);

  // Bulk holiday blocking
  const [holidayStartDate, setHolidayStartDate] = useState("");
  const [holidayEndDate, setHolidayEndDate]     = useState("");
  const [holidayReason, setHolidayReason]       = useState("");
  const [holidaySyncGcal, setHolidaySyncGcal]   = useState(true);
  const [holidayLoading, setHolidayLoading]     = useState(false);
  const [holidayError, setHolidayError]         = useState(null);
  const [holidaySuccess, setHolidaySuccess]     = useState(false);

  // Search / filter

  const fetchWeekData = useCallback((wStart) => {
    const fromDate = toISODate(wStart);
    const toDate = toISODate(addDays(wStart, 6));
    setLoadingSlots(true);
    setSlotsError(null);
    getAvailability(fromDate, toDate)
      .then(data => {
        setSlots(data.slots || []);
        setAppointments(data.appointments || []);
        setOverrides(data.overrides || []);
      })
      .catch(e => setSlotsError(e.message))
      .finally(() => setLoadingSlots(false));
  }, []);

  useEffect(() => {
    getTherapistClients().then(setClients).catch(() => {});
  }, []);

  useEffect(() => {
    fetchWeekData(weekStart);
  }, [weekStart]);

  const refreshToday = useCallback(() => {
    const today = toISODate(new Date());
    getAvailability(today, today)
      .then(data => setTodayAppointments(data.appointments || []))
      .catch(() => {}); // the timer is ambient — a failed poll shouldn't shout
  }, []);

  useEffect(() => {
    refreshToday();
    const id = setInterval(refreshToday, 60000);
    return () => clearInterval(id);
  }, [refreshToday]);

  useEffect(() => {
    const id = setInterval(() => setNowTick(t => t + 1), 30000);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    const h = (e) => { if (e.key === "Escape") setPanel(null); };
    window.addEventListener("keydown", h); return () => window.removeEventListener("keydown", h);
  }, []);

  const slotsByDay = useMemo(() => {
    const map = {};
    slots.forEach(s => { const k = toDateKey(s.startTime); if (!map[k]) map[k] = []; map[k].push(s); });
    return map;
  }, [slots]);

  const appointmentsByDay = useMemo(() => {
    const map = {};
    appointments.forEach(a => { const k = toDateKey(a.startTime); if (!map[k]) map[k] = []; map[k].push(a); });
    return map;
  }, [appointments]);

  const overridesByDay = useMemo(() => {
    const map = {};
    overrides.forEach(o => { const k = toDateKey(o.startTime); if (!map[k]) map[k] = []; map[k].push(o); });
    return map;
  }, [overrides]);

  const getSlotsForDate = (date) => slotsByDay[toDateKey(date)] || [];
  const getAppointmentsForDate = (date) => appointmentsByDay[toDateKey(date)] || [];
  const getOverridesForDate = (date) => overridesByDay[toDateKey(date)] || [];

  const daySlots = useMemo(() => getSlotsForDate(selectedDate), [selectedDate, slotsByDay]);
  const dayAppointments = useMemo(() => getAppointmentsForDate(selectedDate), [selectedDate, appointmentsByDay]);
  const dayOverrides = useMemo(() => getOverridesForDate(selectedDate), [selectedDate, overridesByDay]);

  // Cancelled/abandoned sessions stay in dayAppointments (the update panel and
  // history still need them) but are never drawn on the canvas. Everything that
  // describes the visible day derives from this list instead.
  const activeDayAppointments = useMemo(
    () => dayAppointments.filter(a => a.status !== "CANCELLED" && a.status !== "ABANDONED"),
    [dayAppointments]
  );

  const availableSlots = useMemo(() => {
    const activeAppts = activeDayAppointments;
    return daySlots.filter(s => {
      if (s.slotStatus !== "AVAILABLE") return false;
      if (new Date(s.startTime) <= now) return false;
      const ss = new Date(s.startTime).getTime(), se = new Date(s.endTime).getTime();
      return !activeAppts.some(a => ss < new Date(a.endTime).getTime() && se > new Date(a.startTime).getTime());
    });
  }, [daySlots, activeDayAppointments]);

  // Slots are generated per service, so two services can offer the same time.
  // Without lane assignment those blocks sit exactly on top of each other and
  // only the last one is clickable. Greedy sweep: each item takes the first lane
  // that's free at its start, and every overlapping group shares the width.
  // Grouped by exact time range rather than by any overlap: a chain of
  // half-hour-offset 60-minute slots all overlap transitively, and treating that
  // as one group would squeeze every block on the day into a sliver. Blocks that
  // merely overlap stay full width and remain readable because they sit at
  // different heights — only exact duplicates need splitting.
  const laneOf = useMemo(() => {
    const items = [
      ...availableSlots.map(s => ({ id: s.slotId, key: `${s.startTime}|${s.endTime}` })),
      ...activeDayAppointments.map(a => ({ id: a.appointmentId, key: `${a.startTime}|${a.endTime}` })),
    ];
    const groups = {};
    items.forEach(it => { (groups[it.key] ??= []).push(it.id); });
    const map = {};
    Object.values(groups).forEach(ids => {
      ids.forEach((id, i) => { map[id] = { lane: i, of: ids.length }; });
    });
    return map;
  }, [availableSlots, activeDayAppointments]);

  // A booking runs for the service's duration, which spans several 30-minute
  // blocks. Every block it covers must exist and still be free, otherwise the
  // backend rejects the booking — so work that out here instead of letting the
  // therapist find out on submit.
  const bookingFit = useMemo(() => {
    if (!panelSlot?.startTime) return null;
    const svc = services.find(x => x.serviceId === booking.serviceId);
    const minutes = svc ? svc.duration : null;
    if (!minutes) return null;

    const startMin = toMinutes(panelSlot.startTime);
    // The session runs `minutes`, but it occupies whole 30-minute blocks rounded
    // up — a 50-minute session holds a 60-minute footprint, which is what keeps
    // the following session starting on the clock.
    const blocks = Math.ceil(minutes / BLOCK_MINUTES);
    const footprintEnd = startMin + blocks * BLOCK_MINUTES;
    const freeStarts = new Set(
      availableSlots.map(sl => toMinutes(sl.startTime)).concat(startMin)
    );
    const missing = [];
    for (let m = startMin; m < footprintEnd; m += BLOCK_MINUTES) {
      if (!freeStarts.has(m)) missing.push(m);
    }
    // Format via the same helper the rest of the panel uses, so the two ends of
    // the range don't render in different styles ("05:30 pm" vs "6:30 PM").
    const endDate = new Date(panelSlot.startTime);
    endDate.setMinutes(endDate.getMinutes() + minutes);
    const freeUntil = new Date(panelSlot.startTime);
    freeUntil.setMinutes(freeUntil.getMinutes() + blocks * BLOCK_MINUTES);
    return {
      minutes,
      endLabel: formatTime(endDate),
      // what the therapist must actually have free, which can exceed the session
      needsFreeUntilLabel: formatTime(freeUntil),
      fits: missing.length === 0,
    };
  }, [panelSlot, booking.serviceId, services, availableSlots]);

  useEffect(() => {
    const el = panelRef.current;
    if (!el || typeof ResizeObserver === "undefined") return;
    const sync = () => setTimelineHeight(Math.max(Math.round(el.getBoundingClientRect().height), 500));
    sync();
    const ro = new ResizeObserver(sync);
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  // The canvas starts at 06:00 but a working day rarely does, so opening on
  // empty early hours wastes the visible area. Scroll to the first thing on the
  // day (or to "now" when that's within the day's span).
  useEffect(() => {
    const el = timelineRef.current;
    if (!el || loadingSlots) return;
    const starts = [
      ...availableSlots.map(sl => toMinutes(sl.startTime)),
      ...activeDayAppointments.map(a => toMinutes(a.startTime)),
      ...dayOverrides.map(o => toMinutes(o.startTime)),
    ];
    if (!starts.length) return;
    let target = Math.min(...starts);
    if (nowMinutes !== null && nowMinutes > target && nowMinutes < Math.max(...starts)) {
      target = nowMinutes;
    }
    // keep a little context above the first block
    el.scrollTop = Math.max(minutesToPx(target) - PX_PER_HOUR / 2, 0);
  }, [selectedDate, loadingSlots, availableSlots, activeDayAppointments, dayOverrides]);

  // Rescheduling keeps the appointment's own length, so a slot button should
  // show the session it would create, not the 30-minute block it starts in.
  const reschedEndFor = (slot) => {
    const mins = panelSlot?.startTime && panelSlot?.endTime
      ? Math.round((new Date(panelSlot.endTime) - new Date(panelSlot.startTime)) / 60000)
      : null;
    if (!mins) return slot.endTime;
    const end = new Date(slot.startTime);
    end.setMinutes(end.getMinutes() + mins);
    return end;
  };

  // Horizontal geometry for a block, given its lane within its overlap group.
  const laneStyle = (id) => {
    const l = laneOf[id];
    if (!l || l.of < 2) return {};
    const w = 100 / l.of;
    return { left: `${l.lane * w}%`, width: `calc(${w}% - 6px)` };
  };

  const weekDays = Array.from({ length: 7 }, (_, i) => addDays(weekStart, i));
  const weekEnd = addDays(weekStart, 6);
  const weekLabel = weekStart.getMonth() === weekEnd.getMonth()
    ? `${weekStart.getDate()}–${weekEnd.getDate()} ${MONTHS_SHORT[weekStart.getMonth()]} ${weekStart.getFullYear()}`
    : `${weekStart.getDate()} ${MONTHS_SHORT[weekStart.getMonth()]} – ${weekEnd.getDate()} ${MONTHS_SHORT[weekEnd.getMonth()]} ${weekEnd.getFullYear()}`;

  const prevWeek = () => { setWeekStart(d => addDays(d, -7)); };
  const nextWeek = () => { setWeekStart(d => addDays(d, 7)); };
  const goToday = () => { setWeekStart(getWeekStart(new Date())); setSelectedDate(new Date()); };

  const hasAvailable = (date) => getSlotsForDate(date).some(s => s.slotStatus === "AVAILABLE");
  const hasBooked = (date) => getSlotsForDate(date).some(s => s.slotStatus === "BOOKED") || getAppointmentsForDate(date).length > 0;

  // ─── Drag to create override ───────────────────────────────────────────────
  const getCanvasY = useCallback((clientY) => {
    if (!canvasRef.current) return 0;
    return clientY - canvasRef.current.getBoundingClientRect().top;
  }, []);

  const dragMoveRef = useRef(null);
  const dragUpRef = useRef(null);

  const onCanvasMouseDown = (e) => {
    if (e.button !== 0) return;
    const y = getCanvasY(e.clientY);
    const mins = snapToSlot(pxToMinutes(y));
    dragRef.current = { active: true, startY: y, startMins: mins, endMins: mins + 30, moved: false };
    setDragStart(mins);
    setDragEnd(mins + 30);

    dragMoveRef.current = (ev) => {
      if (!dragRef.current.active) return;
      const cy = getCanvasY(ev.clientY);
      const delta = Math.abs(cy - dragRef.current.startY);
      if (delta > 12) { dragRef.current.moved = true; setDragging(true); }
      if (!dragRef.current.moved) return;
      const m = snapToSlot(pxToMinutes(Math.max(cy, 0)));
      const endM = Math.max(m, dragRef.current.startMins + 30);
      dragRef.current.endMins = endM;
      setDragEnd(endM);
    };

    dragUpRef.current = () => {
      window.removeEventListener("mousemove", dragMoveRef.current);
      window.removeEventListener("mouseup", dragUpRef.current);
      if (!dragRef.current.active) return;
      const { moved, startMins, endMins } = dragRef.current;
      dragRef.current = { active: false, startY: 0, startMins: 0, endMins: 0, moved: false };
      setDragging(false);
      setDragStart(null);
      setDragEnd(null);
      if (moved && endMins > startMins) {
        setOverrideRange({ startMin: startMins, endMin: endMins });
        setOverrideNote("");
        setOverrideIsAvailable(false);
        setOverrideSyncGcal(true);
        setOverrideError(null);
        setPanel("override");
      }
    };

    window.addEventListener("mousemove", dragMoveRef.current);
    window.addEventListener("mouseup", dragUpRef.current);
  };

  // ─── Actions ───────────────────────────────────────────────────────────────

  // The panels speak "slot"; the timeline and the session timer both hand over
  // appointments. therapistId/slotId can be absent when the appointment came
  // from today's feed while another week is loaded — neither is required, the
  // backend re-derives the therapist from the JWT.
  const toApptSlot = useCallback((appt) => {
    const slot = slots.find(s => s.appointmentId === appt.appointmentId);
    return {
      appointmentId: appt.appointmentId,
      therapistId: appt.therapistId || slot?.therapistId,
      clientId: appt.clientId,
      clientName: appt.clientName,
      startTime: appt.startTime,
      endTime: appt.endTime,
      appointmentStatus: appt.status,
      modeId: appt.modeId,
      slotId: slot?.slotId,
    };
  }, [slots]);

  const openBook = (slot) => {
    setPanelSlot(slot);
    // Blocks are service-agnostic now, so the service is picked here and the
    // mode list follows from it. With a single active service there's nothing
    // to choose — preselect it so the common case stays one step.
    const only = services.length === 1 ? services[0].serviceId : "";
    setBookingModes(only ? allModes.filter(m => m.serviceId === only && m.isActive) : []);
    setBooking({ clientId: "", clientName: "", serviceId: only, modeId: "", useCustomPrice: false, customPrice: "" });
    setBookingError(null); setBookingSuccess(false);
    setBookingPayment(null); setLinkCopied(false);
    setPanel("book");
  };

  const openUpdate = (slot) => {
    setPanelSlot(slot);
    setUpdateStatus(slot.appointmentStatus || "");
    setUpdateReason(""); setUpdateError(null);
    setPanelPayment(null); setLinkCopied(false);
    setSessionNotes(""); setNotesWarning(null);
    setPanel("update");
    if (slot.appointmentId) {
      getPaymentInfo(slot.appointmentId).then(setPanelPayment).catch(() => setPanelPayment(null));
    }
  };

  const copyPaymentLink = async (url) => {
    try {
      await navigator.clipboard.writeText(url);
      setLinkCopied(true);
      setTimeout(() => setLinkCopied(false), 2000);
    } catch {
      // clipboard unavailable (http origin) — leave the link selectable
    }
  };

  const handlePaymentRetry = async (appointmentId, fromBookingPanel) => {
    setPaymentRetryLoading(true);
    try {
      const payment = await ensurePaymentLink(appointmentId);
      if (fromBookingPanel) {
        setBookingPayment({ appointmentId, status: payment?.status, url: payment?.paymentLinkUrl, clientNotified: payment?.clientNotified });
      } else {
        setPanelPayment(payment);
      }
    } catch (err) {
      if (fromBookingPanel) setBookingError(friendlyError(err.message));
      else setUpdateError(friendlyError(err.message));
    } finally {
      setPaymentRetryLoading(false);
    }
  };

  const openReschedule = (slot) => {
    // terminal appointments can't be rescheduled (backend rejects them too)
    if (["COMPLETED", "CANCELLED", "ABANDONED"].includes(slot.appointmentStatus)) return;
    setPanelSlot(slot);
    setReschedWeekStart(getWeekStart(new Date()));
    setReschedSelectedDate(null); setReschedNewSlot(null);
    setReschedModeId("");
    setReschedReason(""); setReschedError(null);
    setPanel("reschedule");
  };

  const handleBook = async (e) => {
    e.preventDefault();
    if (!booking.clientId) { setBookingError("Please select a client."); return; }
    if (!booking.modeId) { setBookingError("Please select a delivery mode."); return; }
    if (booking.useCustomPrice && (!booking.customPrice || parseFloat(booking.customPrice) <= 0)) {
      setBookingError("Custom session fee must be greater than zero."); return;
    }
    setBookingLoading(true); setBookingError(null);
    try {
      const result = await createAppointment({
        slotId: panelSlot.slotId,
        therapistId: panelSlot.therapistId,
        clientId: booking.clientId,
        clientName: booking.clientName,
        modeId: booking.modeId,
        customPrice: booking.useCustomPrice ? parseFloat(booking.customPrice) : undefined,
      });
      // A booking now spans however many 30-minute blocks the service needs, so
      // the client cannot predict which blocks were consumed or when the session
      // actually ends — patching local state from the clicked block drew a
      // 30-minute appointment and left the following blocks looking free.
      // The server is authoritative: refetch instead of guessing.
      await reloadAll();
      setBookingSuccess(true);
      if (result?.paymentStatus) {
        // keep the panel open so the therapist can copy/share the payment link
        setBookingPayment({
          appointmentId: result.appointmentId,
          status: result.paymentStatus,
          url: result.paymentLinkUrl,
          clientNotified: result.clientNotified,
        });
      } else {
        setTimeout(() => setPanel(null), 1200);
      }
    } catch (err) { setBookingError(friendlyError(err.message)); }
    finally { setBookingLoading(false); }
  };

  const handleUpdateStatus = async () => {
    if (!updateStatus) { setUpdateError("Please select a status."); return; }
    setUpdateLoading(true); setUpdateError(null); setNotesWarning(null);
    const notes = sessionNotes.trim();
    // Re-sending the current status would be rejected as an invalid transition,
    // so a notes-only save (adding notes to an already-completed session) skips
    // the status call entirely.
    const statusChanged = updateStatus !== panelSlot.appointmentStatus;
    try {
      if (statusChanged) {
        await updateAppointmentStatus({ appointmentId: panelSlot.appointmentId, therapistId: panelSlot.therapistId, status: updateStatus, reason: updateReason || undefined });
        setAppointments(prev => prev.map(a => a.appointmentId === panelSlot.appointmentId ? { ...a, status: updateStatus } : a));
        // keep the timer honest: completing a session should retire the card at once
        setTodayAppointments(prev => prev.map(a => a.appointmentId === panelSlot.appointmentId ? { ...a, status: updateStatus } : a));
        // Cancelling releases every block the appointment owned, which the client
        // can't enumerate — refetch so the freed time reappears as bookable.
        if (updateStatus === "CANCELLED") {
          await reloadAll();
        }
      }

      // Notes are saved after the status call, never before: the status change
      // emits the appointment event and is the action that must not be lost.
      // If the note fails, the completion still stands and the panel stays open
      // so the text can be retried or copied out rather than silently binned.
      if (notes) {
        try {
          await createSessionNotes(panelSlot.clientId, panelSlot.appointmentId, notes);
        } catch (noteErr) {
          setNotesWarning(statusChanged
            ? `Session marked ${updateStatus.toLowerCase()}, but the note didn't save: ${noteErr.message}`
            : `The note didn't save: ${noteErr.message}`);
          return;
        }
      }
      setPanel(null);
    } catch (err) { setUpdateError(friendlyError(err.message)); }
    finally { setUpdateLoading(false); }
  };

  const reschedDaySlots = useMemo(() => {
    if (!reschedSelectedDate) return [];
    const allDay = getSlotsForDate(reschedSelectedDate);
    const apptOnDay = getAppointmentsForDate(reschedSelectedDate)
      .filter(a => a.appointmentId !== panelSlot?.appointmentId &&
        a.status !== "CANCELLED" && a.status !== "ABANDONED"
      );
    return allDay.filter(s => {
      if (s.slotStatus !== "AVAILABLE") return false;
      if (new Date(s.startTime) <= now) return false;
      const ss = new Date(s.startTime).getTime(), se = new Date(s.endTime).getTime();
      return !apptOnDay.some(a => ss < new Date(a.endTime).getTime() && se > new Date(a.startTime).getTime());
    }).sort((a,b) => new Date(a.startTime) - new Date(b.startTime));
  }, [reschedSelectedDate, slotsByDay, appointmentsByDay, panelSlot]);

  const modesForSlot = (slot) => slot?.serviceId
    ? allModes.filter(m => m.serviceId === slot.serviceId && m.isActive)
    : allModes.filter(m => m.isActive);

  const reschedModes = useMemo(() => reschedNewSlot ? modesForSlot(reschedNewSlot) : [], [reschedNewSlot, allModes]);

  const selectReschedSlot = (s) => {
    setReschedNewSlot(s);
    // keep the appointment's current mode when the new slot's service still
    // offers it; otherwise fall back to the slot's own default mode
    const modes = modesForSlot(s);
    const keep = modes.find(m => m.modeId === panelSlot?.modeId) || modes.find(m => m.modeId === s.modeId);
    setReschedModeId(keep ? keep.modeId : (modes.length === 1 ? modes[0].modeId : ""));
  };

  const handleOverrideDelete = async () => {
    if (!overrideRange?.overrideId) return;
    setOverrideDeleteLoading(true); setOverrideError(null);
    try {
      await deleteAvailabilityOverride(overrideRange.overrideId);
      await reloadAll();
      setPanel(null);
    } catch (err) {
      setOverrideError(err.message);
    } finally {
      setOverrideDeleteLoading(false);
    }
  };

  const handleOverrideSave = async () => {
    setOverrideLoading(true); setOverrideError(null);
    try {
      const base = new Date(selectedDate);
      const startTime = new Date(base);
      startTime.setHours(Math.floor(overrideRange.startMin / 60), overrideRange.startMin % 60, 0, 0);
      const endTime = new Date(base);
      endTime.setHours(Math.floor(overrideRange.endMin / 60), overrideRange.endMin % 60, 0, 0);

      await createAvailabilityOverride({
        overrideId: overrideRange.overrideId || undefined,
        startTime: `${startTime.getFullYear()}-${String(startTime.getMonth()+1).padStart(2,"0")}-${String(startTime.getDate()).padStart(2,"0")}T${String(startTime.getHours()).padStart(2,"0")}:${String(startTime.getMinutes()).padStart(2,"0")}:00`,
        endTime: `${endTime.getFullYear()}-${String(endTime.getMonth()+1).padStart(2,"0")}-${String(endTime.getDate()).padStart(2,"0")}T${String(endTime.getHours()).padStart(2,"0")}:${String(endTime.getMinutes()).padStart(2,"0")}:00`,
        isAvailable: overrideIsAvailable,
        reason: overrideNote || undefined,
        syncToGoogleCalendar: overrideSyncGcal,
      });
      await reloadAll();
      setPanel(null);
    } catch (err) {
      setOverrideError(err.message);
    } finally {
      setOverrideLoading(false);
    }
  };

  const reloadAll = async () => {
    const data = await getAvailability(toISODate(weekStart), toISODate(addDays(weekStart, 6)));
    setSlots(data.slots || []);
    setAppointments(data.appointments || []);
    setOverrides(data.overrides || []);
    refreshToday();
  };

  const handleReschedule = async () => {
    if (!reschedNewSlot) { setReschedError("Please select a new slot."); return; }
    if (!reschedModeId) { setReschedError("Please select a delivery mode."); return; }
    setReschedLoading(true); setReschedError(null);
    try {
      await rescheduleAppointment({ appointmentId: panelSlot.appointmentId, therapistId: panelSlot.therapistId, newSlotId: reschedNewSlot.slotId, modeId: reschedModeId, reason: reschedReason || undefined });
      await reloadAll();
      setPanel(null);
    } catch (err) { setReschedError(err.message); }
    finally { setReschedLoading(false); }
  };

  const handleGenerate = async () => {
    if (!genStartDate || !genEndDate) { setGenError("Please select both dates."); return; }
    if (genStartDate > genEndDate) { setGenError("Start date must be before end date."); return; }
    setGenLoading(true); setGenError(null); setGenSuccess(false);
    try {
      await generateSlots(genStartDate, genEndDate);
      setGenSuccess(true);
      // Wait for Kafka event propagation before reloading so new slots are visible
      await new Promise(r => setTimeout(r, 3000));
      await reloadAll();
      setPanel(null);
    } catch (err) { setGenError(err.message); }
    finally { setGenLoading(false); }
  };

  const handleHolidayBlock = async () => {
    if (!holidayStartDate || !holidayEndDate) { setHolidayError("Please select both dates."); return; }
    if (holidayStartDate > holidayEndDate) { setHolidayError("Start date must be before end date."); return; }
    setHolidayLoading(true); setHolidayError(null); setHolidaySuccess(false);
    try {
      await bulkAvailabilityOverrides({
        startDate: holidayStartDate,
        endDate: holidayEndDate,
        isAvailable: false,
        reason: holidayReason || undefined,
        syncToGoogleCalendar: holidaySyncGcal,
      });
      setHolidaySuccess(true);
      await reloadAll();
    } catch (err) { setHolidayError(err.message); }
    finally { setHolidayLoading(false); }
  };



  // ─── Render helpers ────────────────────────────────────────────────────────────
  const hourLabels = Array.from({ length: TOTAL_HOURS + 1 }, (_, i) => {
    const h = HOUR_START + i;
    return h === 12 ? "12 PM" : h < 12 ? `${h} AM` : `${h-12} PM`;
  });

  const nowMinutes = now.toDateString() === selectedDate.toDateString()
    ? now.getHours() * 60 + now.getMinutes() : null;

  return (
    <div className="page-wrap">
      <div className="page-head">
        <div>
          <div className="eyebrow">Calendar</div>
          <h1>Schedule</h1>
          <div className="sub">Book, reschedule and block time. Drag on the timeline to mark unavailable.</div>
        </div>
        <div className="head-actions">
          <button className="btn" onClick={() => { setPanel("holiday"); setHolidaySuccess(false); setHolidayError(null); setHolidayStartDate(""); setHolidayEndDate(""); setHolidayReason(""); }}>
            <Icon name="umbrella" size={17} /> Block holiday
          </button>
          <button className="btn btn-primary" onClick={() => { setPanel("generate"); setGenSuccess(false); setGenError(null); }}>
            <Icon name="zap" size={17} /> Generate slots
          </button>
        </div>
      </div>

      <SessionTimer
        appointments={todayAppointments}
        onOpen={(appt) => openUpdate(toApptSlot(appt))}
        sticky
      />

      {/* Week navigation — prototype puts this on one row above the strip */}
      <div className={styles.weekNavRow}>
        <div className={styles.legend}>
          <span className={styles.legendItem}><span className={styles.legendDot} style={{ background: "var(--primary)" }} />Booked</span>
          <span className={styles.legendItem}><span className={styles.legendDot} style={{ background: "var(--ok-mid)" }} />Available</span>
          <span className={styles.legendItem}><span className={styles.legendDot} style={{ background: "var(--danger-mid)" }} />Unavailable</span>
        </div>
        <div className={styles.weekNavRight}>
          <button className="iconbtn" onClick={prevWeek} title="Previous week"><Icon name="back" size={17} /></button>
          <b className={styles.weekLabel}>{weekLabel}</b>
          <button className="iconbtn" onClick={nextWeek} title="Next week"><Icon name="chevron" size={17} /></button>
          <button className="btn btn-sm" onClick={goToday}>Today</button>
        </div>
      </div>

      {slotsError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{slotsError}</div>}

      {/* Week strip */}
      <div className="weekstrip">
        {weekDays.map((date, i) => {
          const isToday = date.toDateString() === new Date().toDateString();
          const isSel = date.toDateString() === selectedDate.toDateString();
          const avail = hasAvailable(date), booked = hasBooked(date);
          return (
            <div
              key={i}
              className={`day ${isSel ? "sel" : ""} ${isToday ? styles.dayToday : ""}`}
              onClick={() => setSelectedDate(new Date(date))}
            >
              <div className="dn">{DAY_SHORT[date.getDay()]}</div>
              <div className="dd">{date.getDate()}</div>
              {/* One dot per signal: cyan = bookable slots, green = has bookings */}
              <div className={styles.dotRow}>
                {avail && <span className="dot" />}
                {booked && <span className={`dot ${styles.dotBooked}`} />}
              </div>
            </div>
          );
        })}
      </div>

          <div className={styles.dayLabel}>
            <h2 className={styles.dayLabelText}>
              {DAY_SHORT[selectedDate.getDay()]}, {selectedDate.getDate()} {MONTHS[selectedDate.getMonth()]} {selectedDate.getFullYear()}
            </h2>
            <div className={styles.dayLabelRight}>
              <div className={styles.dayStats}>
                {/* Count only what the timeline actually draws — cancelled and
                    abandoned sessions are filtered out of the canvas, so
                    including them here reported "1 booked" against an empty day. */}
                <span>{activeDayAppointments.length} booked</span>
                <span>·</span>
                <span>{availableSlots.length} available</span>
                {dayOverrides.length > 0 && <><span>·</span><span>{dayOverrides.length} override{dayOverrides.length !== 1 ? "s" : ""}</span></>}
              </div>
            </div>
          </div>

      <div className={`grid-2 ${styles.scheduleGrid}`}>
        {/* ── Left: timeline canvas ── */}
        <div className={styles.canvasArea}>



          {/* First-run guidance: a therapist with no slots anywhere in the week
              sees an empty grid with no hint that Generate Slots is the missing
              step. Only shown when the week is genuinely bare — not merely when
              today happens to be free. */}
          {!loadingSlots && !slotsError && slots.length === 0 && appointments.length === 0 && (
            <div className={styles.setupHint}>
              <span className={styles.setupHintIcon}><Icon name="calendar" size={24} /></span>
              <div className={styles.setupHintText}>
                <h3 className={styles.setupHintTitle}>No slots this week</h3>
                <p className={styles.setupHintBody}>
                  Bookable slots are generated from your availability rules. Set your weekly hours
                  first, then generate slots for the dates you want to open up.
                </p>
              </div>
              <div className={styles.setupHintActions}>
                <button className={styles.setupHintGhost} onClick={() => navigate("/therapist/availability-rules")}>
                  Availability rules
                </button>
                <button className={styles.generateBtn} onClick={() => { setPanel("generate"); setGenSuccess(false); setGenError(null); }}>
                  <Icon name="zap" size={15} /> Generate slots
                </button>
              </div>
            </div>
          )}

          {/* Timeline canvas */}
          {loadingSlots ? (
            <div className={styles.canvasLoading}><div className={styles.spinner}/></div>
          ) : (
            <div className={`card ${styles.timelineCard}`} style={timelineHeight ? { height: timelineHeight } : undefined}>
             <div className={styles.timelineScroll} ref={timelineRef}>
              <div className={styles.timeGutter}>
                {hourLabels.map((label, i) => (
                  <div key={i} className={styles.hourLabel} style={{ top: i * PX_PER_HOUR }}>{label}</div>
                ))}
              </div>

              <div
                className={styles.canvas}
                style={{ height: CANVAS_HEIGHT }}
                ref={canvasRef}
                onMouseDown={onCanvasMouseDown}
              >
                {hourLabels.map((_, i) => (
                  <div key={i} className={styles.hourLine} style={{ top: i * PX_PER_HOUR }} />
                ))}
                {Array.from({ length: TOTAL_HOURS }, (_, i) => (
                  <div key={i} className={styles.halfHourLine} style={{ top: i * PX_PER_HOUR + PX_PER_HOUR / 2 }} />
                ))}

                {nowMinutes !== null && nowMinutes >= HOUR_START*60 && nowMinutes <= HOUR_END*60 && (
                  <div className={styles.nowLine} style={{ top: minutesToPx(nowMinutes) }}>
                    <div className={styles.nowDot}/>
                  </div>
                )}

                {dragging && dragStart !== null && dragEnd !== null && (
                  <div className={styles.dragPreview} style={{ top: minutesToPx(dragStart), height: Math.max(minutesToPx(dragEnd) - minutesToPx(dragStart), 24) }}>
                    <span className={styles.dragLabel}>{formatTimeFromMinutes(dragStart)} – {formatTimeFromMinutes(dragEnd)}</span>
                    <span className={styles.dragLabelSub}>{dragEnd - dragStart} min · Mark unavailable</span>
                  </div>
                )}

                {dayOverrides.map(override => {
                  const top = minutesToPx(toMinutes(override.startTime));
                  const height = Math.max(minutesToPx(toMinutes(override.endTime)) - top - BLOCK_GAP, 20);
                  return (
                    <div
                      key={override.overrideId}
                      className={styles.overrideBlock}
                      style={{ top, height }}
                      onClick={e => { e.stopPropagation(); setOverrideRange({ startMin: toMinutes(override.startTime), endMin: toMinutes(override.endTime), overrideId: override.overrideId, reason: override.reason }); setOverrideNote(override.reason || ""); setOverrideIsAvailable(override.available ?? false); setOverrideSyncGcal(false); setOverrideError(null); setPanel("override"); }}
                      title={`Unavailable: ${formatTime(override.startTime)} – ${formatTime(override.endTime)}${override.reason ? ` · ${override.reason}` : ""}`}
                    >
                      <span className={styles.overrideBlockLabel}>
                        <Icon name="ban" size={13} /> {formatTime(override.startTime)}{override.reason ? ` · ${override.reason}` : ""}
                      </span>
                    </div>
                  );
                })}

                {availableSlots.map(slot => {
                  const top = minutesToPx(toMinutes(slot.startTime));
                  // trim the height (never the top) so consecutive blocks read as
                  // separate without shifting either off its start time
                  const height = Math.max(minutesToPx(toMinutes(slot.endTime)) - top - BLOCK_GAP, 18);
                  const slotStartMin = toMinutes(slot.startTime);
                  const slotEndMin = toMinutes(slot.endTime);
                  const overlapsD = dragging && dragStart !== null && dragEnd !== null && slotStartMin < dragEnd && slotEndMin > dragStart;
                  return (
                    <div
                      key={slot.slotId}
                      className={`tl-block avail ${styles.availableBlock} ${overlapsD ? styles.availableBlockDimmed : ""}`}
                      style={{ top, height, ...laneStyle(slot.slotId) }}
                      onClick={e => { e.stopPropagation(); openBook(slot); }}
                      title={`Available: ${formatTime(slot.startTime)} – ${formatTime(slot.endTime)}`}
                    >
                      <span className={styles.availableBlockLabel}>
                        <Icon name="plus" size={14} />
                        <span className={styles.availableBlockTime}>{formatTime(slot.startTime)}</span>
                        {/* the prototype's full wording, only where it fits */}
                        {height > 30 && <span className={styles.availableBlockHint}>Available · click to book</span>}
                      </span>
                    </div>
                  );
                })}

                {activeDayAppointments.map(appt => {
                  const top = minutesToPx(toMinutes(appt.startTime));
                  const height = Math.max(minutesToPx(toMinutes(appt.endTime)) - top - BLOCK_GAP, 28);
                  const apptAsSlot = toApptSlot(appt);
                  const modeName = modeMap[appt.modeId]?.displayName;
                  // "Live" is the prototype's green treatment — only while the
                  // session is actually running right now.
                  const startMin = toMinutes(appt.startTime), endMin = toMinutes(appt.endTime);
                  const isLive = nowMinutes !== null && nowMinutes >= startMin && nowMinutes < endMin
                    && appt.status !== "COMPLETED";
                  return (
                    <div
                      key={appt.appointmentId}
                      className={`tl-block ${isLive ? "live" : "booked"} ${styles.bookedBlock}`}
                      style={{ top, height, ...laneStyle(appt.appointmentId) }}
                      onClick={e => { e.stopPropagation(); openUpdate(apptAsSlot); }}
                      title={`${appt.clientName} · ${formatTime(appt.startTime)} – ${formatTime(appt.endTime)}`}
                    >
                      <div className={styles.bookedBlockContent}>
                        <span className={styles.bookedBlockTime}>{formatTime(appt.startTime)} – {formatTime(appt.endTime)}</span>
                        {height > 36 && <span className={styles.bookedBlockClient}>{appt.clientName}</span>}
                        {height > 52 && (
                          <span className={styles.bookedBlockMeta}>
                            {modeName ? modeName : titleCase(appt.status)}
                          </span>
                        )}
                      </div>
                      <div className={styles.bookedBlockRight}>
                        {height > 30 && (
                          isLive ? (
                            <span className={`chip chip-ok ${styles.liveChip}`}><i className={styles.livePulse} /> Live</span>
                          ) : (
                            <span className={`chip chip-${STATUS_CHIP[appt.status] || "mut"}`}>
                              <Icon name={STATUS_ICON[appt.status] || "calendar"} size={11} />
                              {titleCase(appt.status)}
                            </span>
                          )
                        )}
                        {height > 40 && appt.status !== "COMPLETED" && (
                          <button className={styles.reschedBadge} onClick={e => { e.stopPropagation(); openReschedule(apptAsSlot); }} title="Reschedule">
                            <Icon name="refresh" size={13} />
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
             </div>
            </div>
          )}
        </div>

        {/* ── Right: action panel ── */}
        <div className={`card ${styles.panel} ${panel ? styles.panelOpen : ""}`} ref={panelRef}>
          {!panel && (
            <div className={styles.panelEmpty}>
              <span className={styles.panelEmptyIcon}><Icon name="cursor" size={26} /></span>
              <p>Click a slot or appointment<br/>to take action</p>
              <p className={styles.panelEmptyHint}>Drag on the timeline to<br/>mark unavailable time</p>
            </div>
          )}

          {/* Book panel */}
          {panel === "book" && panelSlot && (
            <div className={styles.panelBody}>
              <div className={styles.panelHeader}>
                <div>
                  <h2 className={styles.panelTitle}>Book Appointment</h2>
                  {/* The clicked block is only where the session starts — showing its
                      30-minute span here contradicted the real time below it. */}
                  <p className={styles.panelSub}>
                    {formatTime(panelSlot.startTime)} – {bookingFit ? bookingFit.endLabel : formatTime(panelSlot.endTime)}
                  </p>
                </div>
                <button className={styles.closeBtn} onClick={() => setPanel(null)} title="Close"><Icon name="x" size={16} /></button>
              </div>
              {bookingSuccess ? (
                <div className={styles.panelForm}>
                  <div className={styles.successBox}><span className={styles.successIcon}><Icon name="check" size={14} /></span> Booked!</div>
                  {bookingPayment && bookingPayment.status === "LINK_CREATED" && (
                    <div className={styles.slotSummary}>
                      <div className={styles.summaryRow}>
                        <span className={styles.summaryLabel}>Payment link</span>
                        <span className={styles.summaryValue} style={{ wordBreak: "break-all" }}>{bookingPayment.url}</span>
                      </div>
                      <div className={styles.formActions}>
                        <button type="button" className={styles.submitBtn} onClick={() => copyPaymentLink(bookingPayment.url)}>
                          {linkCopied ? "Copied!" : "Copy link"}
                        </button>
                      </div>
                      <p className={styles.customFeeHint}>
                        {bookingPayment.clientNotified
                          ? <><Icon name="check" size={13} /> Link sent to the client by SMS/email. The appointment confirms automatically once paid.</>
                          : "Client has no contact details on file — share this link manually. The appointment confirms automatically once paid."}
                      </p>
                    </div>
                  )}
                  {bookingPayment && bookingPayment.status === "LINK_FAILED" && (
                    <div className={styles.slotSummary}>
                      <div className={styles.errorBox}>
                        <span className={styles.errorIcon}>!</span>
                        Booking saved, but the payment link could not be created.
                      </div>
                      <div className={styles.formActions}>
                        <button type="button" className={styles.submitBtn} disabled={paymentRetryLoading}
                          onClick={() => handlePaymentRetry(bookingPayment.appointmentId, true)}>
                          {paymentRetryLoading ? <span className={styles.btnSpinner}/> : "Retry payment link"}
                        </button>
                      </div>
                    </div>
                  )}
                  {bookingError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{bookingError}</div>}
                  {bookingPayment && (
                    <div className={styles.formActions}>
                      <button type="button" className={styles.cancelBtn} onClick={() => setPanel(null)}>Close</button>
                    </div>
                  )}
                </div>
              ) : (
                <form onSubmit={handleBook} className={styles.panelForm}>
                  <div className={styles.field}><label className={styles.label}>Client</label>
                    <ClientDropdown clients={clients} value={booking.clientId} onChange={(id,name) => setBooking(p => ({...p, clientId:id, clientName:name}))} />
                  </div>
                  <div className={styles.field}><label className={styles.label}>Service</label>
                    <ServiceDropdown
                      services={services}
                      value={booking.serviceId}
                      onChange={id => {
                        setBookingModes(allModes.filter(m => m.serviceId === id && m.isActive));
                        setBooking(p => ({ ...p, serviceId: id, modeId: "", useCustomPrice: false, customPrice: "" }));
                      }}
                    />
                  </div>
                  <div className={styles.field}><label className={styles.label}>Delivery Mode</label>
                    <ModeDropdown modes={bookingModes} value={booking.modeId} onChange={v => setBooking(p => ({...p, modeId:v, useCustomPrice: false, customPrice: ""}))} />
                  </div>
                  {booking.modeId && (() => {
                    const selectedMode = bookingModes.find(m => m.modeId === booking.modeId);
                    if (!selectedMode || selectedMode.price == null) return null;
                    return (
                      <div className={styles.customFeeBox}>
                        <label className={styles.customFeeLabel}>
                          <input
                            type="checkbox"
                            className={styles.customFeeCheck}
                            checked={booking.useCustomPrice}
                            onChange={e => setBooking(p => ({ ...p, useCustomPrice: e.target.checked, customPrice: "" }))}
                          />
                          Custom session fee
                        </label>
                        <div className={styles.customFeeInputRow}>
                          <span className={styles.customFeeCurrency}>₹</span>
                          <input
                            type="number"
                            min="0"
                            step="1"
                            className={`${styles.customFeeInput} ${!booking.useCustomPrice ? styles.customFeeInputOff : ""}`}
                            disabled={!booking.useCustomPrice}
                            value={booking.useCustomPrice ? booking.customPrice : parseFloat(selectedMode.price).toFixed(0)}
                            onChange={e => setBooking(p => ({ ...p, customPrice: e.target.value }))}
                            placeholder="0"
                          />
                        </div>
                        {!booking.useCustomPrice && (
                          <p className={styles.customFeeHint}>Default: ₹{parseFloat(selectedMode.price).toFixed(0)}</p>
                        )}
                      </div>
                    );
                  })()}
                  <div className={styles.slotSummary}>
                    <div className={styles.summaryRow}>
                      <span className={styles.summaryLabel}>Time</span>
                      <span className={styles.summaryValue}>
                        {formatTime(panelSlot.startTime)} – {bookingFit ? bookingFit.endLabel : formatTime(panelSlot.endTime)}
                        {bookingFit && <span className={styles.durationNote}> · {bookingFit.minutes} min</span>}
                      </span>
                    </div>
                    <div className={styles.summaryRow}><span className={styles.summaryLabel}>Slot</span><span className={styles.summaryValue}>{panelSlot.slotId}</span></div>
                  </div>
                  {bookingFit && !bookingFit.fits && (
                    <div className={styles.errorBox}>
                      <span className={styles.errorIcon}>!</span>
                      This {bookingFit.minutes}-minute session holds the calendar until {bookingFit.needsFreeUntilLabel}, and that time isn't free. Pick an earlier slot or a shorter service.
                    </div>
                  )}
                  {bookingError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{bookingError}</div>}
                  <div className={styles.formActions}>
                    <button type="button" className={styles.cancelBtn} onClick={() => setPanel(null)}>Cancel</button>
                    <button type="submit" className={styles.submitBtn} disabled={bookingLoading || (bookingFit && !bookingFit.fits)}>{bookingLoading ? <span className={styles.btnSpinner}/> : "Confirm"}</button>
                  </div>
                </form>
              )}
            </div>
          )}

          {/* Update status panel */}
          {panel === "update" && panelSlot && (
            <div className={styles.panelBody}>
              <div className={styles.panelHeader}>
                <div>
                  <h2 className={styles.panelTitle}>Update Appointment</h2>
                  <p className={styles.panelSub}>
                    <span className={styles.clientLink} onClick={() => navigate(`/therapist/clients/${panelSlot.clientId}`)}>{panelSlot.clientName}</span>
                    {" · "}{formatTime(panelSlot.startTime)} – {formatTime(panelSlot.endTime)}
                  </p>
                </div>
                <button className={styles.closeBtn} onClick={() => setPanel(null)} title="Close"><Icon name="x" size={16} /></button>
              </div>
              <div className={styles.panelForm}>
                <div className={styles.slotSummary}>
                  <div className={styles.summaryRow}>
                    <span className={styles.summaryLabel}>Client</span>
                    <span className={styles.clientLink} onClick={() => navigate(`/therapist/clients/${panelSlot.clientId}`)}>{panelSlot.clientName}</span>
                  </div>
                  <div className={styles.summaryRow}><span className={styles.summaryLabel}>Current</span>
                    <span className={`chip chip-${STATUS_CHIP[panelSlot.appointmentStatus] || "mut"}`}>{titleCase(panelSlot.appointmentStatus)}</span>
                  </div>
                  {modeMap[panelSlot.modeId] && (
                    <div className={styles.summaryRow}>
                      <span className={styles.summaryLabel}>Mode</span>
                      <span className={styles.summaryValue}>{modeMap[panelSlot.modeId].displayName}</span>
                    </div>
                  )}
                  {panelSlot.reason && (
                    <div className={styles.summaryRow}>
                      <span className={styles.summaryLabel}>Reason</span>
                      <span className={styles.summaryValue}>{panelSlot.reason}</span>
                    </div>
                  )}
                  {panelPayment && (
                    <div className={styles.summaryRow}>
                      <span className={styles.summaryLabel}>Payment</span>
                      <span className={styles.summaryValue}>
                        {panelPayment.status === "PAID" && <><Icon name="check" size={13} /> Paid ₹{parseFloat(panelPayment.amount).toFixed(0)}</>}
                        {panelPayment.status === "LINK_CREATED" && (panelPayment.clientNotified ? "Awaiting payment (client notified)" : "Awaiting payment")}
                        {panelPayment.status === "LINK_FAILED" && "Link creation failed"}
                        {panelPayment.status === "EXPIRED" && "Link expired"}
                        {panelPayment.status === "CANCELLED" && "Link cancelled"}
                      </span>
                    </div>
                  )}
                  {panelPayment && panelPayment.status === "LINK_CREATED" && panelPayment.paymentLinkUrl && (
                    <div className={styles.formActions}>
                      <button type="button" className={styles.cancelBtn} onClick={() => copyPaymentLink(panelPayment.paymentLinkUrl)}>
                        {linkCopied ? "Copied!" : "Copy payment link"}
                      </button>
                    </div>
                  )}
                  {panelPayment && (panelPayment.status === "LINK_FAILED" || panelPayment.status === "EXPIRED") &&
                    !["CANCELLED", "ABANDONED", "COMPLETED"].includes(panelSlot.appointmentStatus) && (
                    <div className={styles.formActions}>
                      <button type="button" className={styles.cancelBtn} disabled={paymentRetryLoading}
                        onClick={() => handlePaymentRetry(panelSlot.appointmentId, false)}>
                        {paymentRetryLoading ? "Retrying…" : "Retry payment link"}
                      </button>
                    </div>
                  )}
                </div>
                <div className={styles.field}><label className={styles.label}>New Status</label>
                  <div className={styles.statusGrid}>
                    {["CONFIRMED", "COMPLETED", "CANCELLED", "ABANDONED"].map(s => (
                      <button key={s} type="button"
                        className={`${styles.statusOption} ${updateStatus === s ? styles.statusOptionActive : ""} ${styles[`statusOption_${s}`] || ""}`}
                        onClick={() => setUpdateStatus(s)}>
                        <Icon name={STATUS_ICON[s]} size={15} /> {titleCase(s)}
                      </button>
                    ))}
                  </div>
                </div>
                {updateStatus === "COMPLETED" && (
                  <div className={styles.field}>
                    <label className={styles.label}>
                      Session notes <span className={styles.optionalTag}>(optional)</span>
                    </label>
                    <textarea className={styles.reasonTextarea} rows={5}
                      placeholder="What came up, what to pick up next time…"
                      value={sessionNotes} onChange={e => setSessionNotes(e.target.value)}/>
                    <span className={styles.notesHint}>
                      Saved against this session and visible on{" "}
                      <span className={styles.clientLink} onClick={() => navigate(`/therapist/clients/${panelSlot.clientId}`)}>
                        {panelSlot.clientName}
                      </span>'s history.
                    </span>
                  </div>
                )}
                <div className={styles.field}><label className={styles.label}>Reason <span className={styles.optionalTag}>(optional)</span></label>
                  <textarea className={styles.reasonTextarea} rows={3} placeholder="Reason…" value={updateReason} onChange={e => setUpdateReason(e.target.value)}/>
                </div>
                {updateError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{updateError}</div>}
                {notesWarning && <div className={styles.warnBox}><span className={styles.warnIcon}>!</span>{notesWarning}</div>}
                <div className={styles.formActions}>
                  <button className={styles.cancelBtn} onClick={() => setPanel(null)}>Cancel</button>
                  {!["COMPLETED", "CANCELLED", "ABANDONED"].includes(panelSlot.appointmentStatus) && (
                    <button className={styles.rescheduleActionBtn} onClick={() => openReschedule(panelSlot)}><Icon name="refresh" size={15} /> Reschedule</button>
                  )}
                  <button className={styles.submitBtn} onClick={handleUpdateStatus}
                    disabled={updateLoading || !updateStatus ||
                      (updateStatus === panelSlot.appointmentStatus && !sessionNotes.trim())}>
                    {updateLoading ? <span className={styles.btnSpinner}/> : "Save"}
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Reschedule panel */}
          {panel === "reschedule" && panelSlot && (
            <div className={styles.panelBody}>
              <div className={styles.panelHeader}>
                <div>
                  <h2 className={styles.panelTitle}>Reschedule</h2>
                  <p className={styles.panelSub}>
                    <span className={styles.clientLink} onClick={() => navigate(`/therapist/clients/${panelSlot.clientId}`)}>{panelSlot.clientName}</span>
                    {" · "}{formatTime(panelSlot.startTime)} – {formatTime(panelSlot.endTime)}
                  </p>
                </div>
                <button className={styles.closeBtn} onClick={() => setPanel(null)} title="Close"><Icon name="x" size={16} /></button>
              </div>
              <div className={styles.panelForm}>
                <div className={styles.field}><label className={styles.label}>Select New Date</label>
                  <div className={styles.reschedWeekCard}>
                    <div className={styles.reschedWeekNav}>
                      <button className="iconbtn" onClick={() => { setReschedWeekStart(d => addDays(d,-7)); setReschedSelectedDate(null); setReschedNewSlot(null); }} title="Previous week"><Icon name="back" size={16} /></button>
                      <span className={styles.reschedWeekLabel}>{(() => { const e = addDays(reschedWeekStart,6); return reschedWeekStart.getMonth()===e.getMonth()?`${reschedWeekStart.getDate()}–${e.getDate()} ${MONTHS_SHORT[reschedWeekStart.getMonth()]}`: `${reschedWeekStart.getDate()} ${MONTHS_SHORT[reschedWeekStart.getMonth()]} – ${e.getDate()} ${MONTHS_SHORT[e.getMonth()]}`; })()}</span>
                      <button className="iconbtn" onClick={() => { setReschedWeekStart(d => addDays(d,7)); setReschedSelectedDate(null); setReschedNewSlot(null); }} title="Next week"><Icon name="chevron" size={16} /></button>
                    </div>
                    <div className={styles.reschedDayRow}>
                      {Array.from({length:7},(_,i)=>addDays(reschedWeekStart,i)).map((date,i)=>{
                        const currentNow=new Date(); const allDay=getSlotsForDate(date);
                        const bookedOnDay=allDay.filter(s=>s.slotStatus==="BOOKED"&&s.slotId!==panelSlot?.slotId);
                        const hasSlots=allDay.some(s=>{ if(s.slotId===panelSlot?.slotId||s.slotStatus!=="AVAILABLE"||new Date(s.startTime)<=currentNow) return false; const ss=new Date(s.startTime).getTime(),se=new Date(s.endTime).getTime(); return !bookedOnDay.some(b=>ss<new Date(b.endTime).getTime()&&se>new Date(b.startTime).getTime()); });
                        const isSel=reschedSelectedDate&&date.toDateString()===reschedSelectedDate.toDateString();
                        const isTod=date.toDateString()===new Date().toDateString();
                        return (<div key={i} className={`${styles.reschedDay} ${isTod?styles.reschedDayToday:""} ${isSel?styles.reschedDaySelected:""} ${!hasSlots?styles.reschedDayNoSlots:""}`} onClick={()=>{ if(hasSlots){setReschedSelectedDate(new Date(date));setReschedNewSlot(null);} }}>
                          <span className={styles.reschedDayName}>{DAY_SHORT[date.getDay()]}</span>
                          <span className={styles.reschedDayNum}>{date.getDate()}</span>
                          {hasSlots&&<span className={styles.reschedDot}/>}
                        </div>);
                      })}
                    </div>
                  </div>
                </div>
                {reschedSelectedDate && (
                  <div className={styles.field}><label className={styles.label}>Available Slots · {reschedSelectedDate.getDate()} {MONTHS_SHORT[reschedSelectedDate.getMonth()]}</label>
                    {reschedDaySlots.length===0 ? <p className={styles.reschedNoSlots}>No available slots.</p> : (
                      <div className={styles.reschedSlotList}>
                        {reschedDaySlots.map(s=>{
                          const modeName = modeMap[s.modeId]?.displayName ?? s.modeId ?? "";
                          return (
                            <button key={s.slotId} type="button" className={`${styles.reschedSlotBtn} ${reschedNewSlot?.slotId===s.slotId?styles.reschedSlotBtnActive:""}`} onClick={()=>selectReschedSlot(s)}>
                              {formatTime(s.startTime)} – {formatTime(reschedEndFor(s))}
                              {modeName && <span className={styles.reschedSlotType}>{modeName}</span>}
                            </button>
                          );
                        })}
                      </div>
                    )}
                  </div>
                )}
                {reschedNewSlot && (
                  <div className={styles.field}><label className={styles.label}>Delivery Mode</label>
                    <ModeDropdown modes={reschedModes} value={reschedModeId} onChange={setReschedModeId} />
                    {reschedModeId && reschedModeId !== panelSlot.modeId && modeMap[panelSlot.modeId] && (() => {
                      const m = reschedModes.find(x => x.modeId === reschedModeId);
                      return (
                        <p className={styles.reschedNoSlots}>
                          Switching from {modeMap[panelSlot.modeId].displayName}{m?.price != null ? ` · new fee ₹${parseFloat(m.price).toFixed(0)}` : ""}
                        </p>
                      );
                    })()}
                  </div>
                )}
                <div className={styles.field}><label className={styles.label}>Reason <span className={styles.optionalTag}>(optional)</span></label>
                  <textarea className={styles.reasonTextarea} rows={3} placeholder="Reason…" value={reschedReason} onChange={e=>setReschedReason(e.target.value)}/>
                </div>
                {reschedError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{reschedError}</div>}
                <div className={styles.formActions}>
                  <button className={styles.cancelBtn} onClick={() => setPanel(null)}>Cancel</button>
                  <button className={styles.submitBtn} onClick={handleReschedule} disabled={reschedLoading||!reschedNewSlot||!reschedModeId}>{reschedLoading?<span className={styles.btnSpinner}/>:"Confirm"}</button>
                </div>
              </div>
            </div>
          )}

          {/* Override panel */}
          {panel === "override" && overrideRange && (
            <div className={styles.panelBody}>
              <div className={styles.panelHeader}>
                <div>
                  <h2 className={styles.panelTitle}>{overrideRange.overrideId ? "Override Details" : "Time Override"}</h2>
                  <p className={styles.panelSub}>{formatTimeFromMinutes(overrideRange.startMin)} – {formatTimeFromMinutes(overrideRange.endMin)}</p>
                </div>
                <button className={styles.closeBtn} onClick={() => setPanel(null)} title="Close"><Icon name="x" size={16} /></button>
              </div>
              <div className={styles.panelForm}>
                <div className={styles.overridePreview}>
                  <span className={styles.overrideTime}>{formatTimeFromMinutes(overrideRange.startMin)} – {formatTimeFromMinutes(overrideRange.endMin)}</span>
                  <span className={styles.overrideDuration}>{overrideRange.endMin - overrideRange.startMin} min</span>
                </div>

                {overrideRange.overrideId ? (
                  <>
                    {overrideRange.reason && (
                      <div className={styles.slotSummary}>
                        <div className={styles.summaryRow}>
                          <span className={styles.summaryLabel}>Reason</span>
                          <span className={styles.summaryValue}>{overrideRange.reason}</span>
                        </div>
                      </div>
                    )}
                    {overrideError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{overrideError}</div>}
                    <div className={styles.formActions}>
                      <button className={styles.cancelBtn} onClick={() => setPanel(null)}>Cancel</button>
                      <button className={styles.deleteBtn} onClick={handleOverrideDelete} disabled={overrideDeleteLoading}>
                        {overrideDeleteLoading ? <span className={styles.btnSpinner}/> : "Delete Override"}
                      </button>
                    </div>
                  </>
                ) : (
                  <>
                    <div className={styles.field}>
                      <label className={styles.label}>Mark as</label>
                      <div className={styles.overrideToggle}>
                        <button type="button"
                          className={`${styles.overrideToggleBtn} ${!overrideIsAvailable ? styles.overrideToggleBtnUnavailable : ""}`}
                          onClick={() => { setOverrideIsAvailable(false); setOverrideSyncGcal(true); }}>
                          <Icon name="ban" size={14} /> Unavailable
                        </button>
                        <button type="button"
                          className={`${styles.overrideToggleBtn} ${overrideIsAvailable ? styles.overrideToggleBtnAvailable : ""}`}
                          onClick={() => { setOverrideIsAvailable(true); setOverrideSyncGcal(false); }}>
                          Available
                        </button>
                      </div>
                    </div>
                    <div className={styles.field}>
                      <label className={styles.label}>Reason <span className={styles.optionalTag}>(optional)</span></label>
                      <textarea className={styles.reasonTextarea} rows={3} placeholder="e.g. Lunch break, personal appointment…" value={overrideNote} onChange={e => setOverrideNote(e.target.value)}/>
                    </div>
                    <div className={styles.syncRow}>
                      <span className={styles.syncLabel}>Sync to Google Calendar</span>
                      <button type="button" className={`${styles.syncToggle} ${overrideSyncGcal ? styles.syncToggleOn : ""}`} onClick={() => setOverrideSyncGcal(v => !v)}>
                        <span className={styles.syncKnob}/>
                      </button>
                    </div>
                    {overrideError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{overrideError}</div>}
                    <div className={styles.formActions}>
                      <button className={styles.cancelBtn} onClick={() => setPanel(null)}>Cancel</button>
                      <button className={styles.submitBtn} onClick={handleOverrideSave} disabled={overrideLoading}>
                        {overrideLoading ? <span className={styles.btnSpinner}/> : "Save"}
                      </button>
                    </div>
                  </>
                )}
              </div>
            </div>
          )}

          {/* Generate slots panel */}
          {panel === "generate" && (
            <div className={styles.panelBody}>
              <div className={styles.panelHeader}>
                <div>
                  <h2 className={styles.panelTitle}>Generate Slots</h2>
                  <p className={styles.panelSub}>Create availability for a date range</p>
                </div>
                <button className={styles.closeBtn} onClick={() => setPanel(null)} title="Close"><Icon name="x" size={16} /></button>
              </div>
              <div className={styles.panelForm}>
                <div className={styles.dateRow}>
                  <div className={styles.field}><label className={styles.label}>From</label>
                    <input type="date" className={styles.dateInput} value={genStartDate} onChange={e=>{setGenStartDate(e.target.value);setGenSuccess(false);setGenError(null);}}/>
                  </div>
                  <div className={styles.dateSep}>to</div>
                  <div className={styles.field}><label className={styles.label}>To</label>
                    <input type="date" className={styles.dateInput} value={genEndDate} onChange={e=>{setGenEndDate(e.target.value);setGenSuccess(false);setGenError(null);}}/>
                  </div>
                </div>
                {genError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{genError}</div>}
                {genSuccess && <div className={styles.successBox}><span className={styles.successIcon}><Icon name="check" size={14} /></span> Slots generated! Refreshing calendar…</div>}
                <div className={styles.formActions}>
                  <button className={styles.cancelBtn} onClick={() => setPanel(null)}>Cancel</button>
                  <button className={styles.generateSubmitBtn} onClick={handleGenerate} disabled={genLoading||!genStartDate||!genEndDate}>{genLoading?<span className={styles.btnSpinner}/>:<><Icon name="zap" size={15} /> Generate</>}</button>
                </div>
              </div>
            </div>
          )}

          {/* Holiday block panel */}
          {panel === "holiday" && (
            <div className={styles.panelBody}>
              <div className={styles.panelHeader}>
                <div>
                  <h2 className={styles.panelTitle}>Block Holiday</h2>
                  <p className={styles.panelSub}>Mark a date range as unavailable</p>
                </div>
                <button className={styles.closeBtn} onClick={() => setPanel(null)} title="Close"><Icon name="x" size={16} /></button>
              </div>
              <div className={styles.panelForm}>
                <div className={styles.dateRow}>
                  <div className={styles.field}><label className={styles.label}>From</label>
                    <input type="date" className={styles.dateInput} value={holidayStartDate} onChange={e=>{setHolidayStartDate(e.target.value);setHolidaySuccess(false);setHolidayError(null);}}/>
                  </div>
                  <div className={styles.dateSep}>to</div>
                  <div className={styles.field}><label className={styles.label}>To</label>
                    <input type="date" className={styles.dateInput} value={holidayEndDate} onChange={e=>{setHolidayEndDate(e.target.value);setHolidaySuccess(false);setHolidayError(null);}}/>
                  </div>
                </div>
                <div className={styles.field}>
                  <label className={styles.label}>Reason <span className={styles.optionalTag}>(optional)</span></label>
                  <input className={styles.dateInput} type="text" placeholder="e.g. Annual leave, Conference…" value={holidayReason} onChange={e => setHolidayReason(e.target.value)}/>
                </div>
                <div className={styles.syncRow}>
                  <span className={styles.syncLabel}>Sync to Google Calendar</span>
                  <button type="button" className={`${styles.syncToggle} ${holidaySyncGcal ? styles.syncToggleOn : ""}`} onClick={() => setHolidaySyncGcal(v => !v)}>
                    <span className={styles.syncKnob}/>
                  </button>
                </div>
                {holidayError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{holidayError}</div>}
                {holidaySuccess && <div className={styles.successBox}><span className={styles.successIcon}><Icon name="check" size={14} /></span> Holiday blocked successfully!</div>}
                <div className={styles.formActions}>
                  <button className={styles.cancelBtn} onClick={() => setPanel(null)}>Cancel</button>
                  <button className={styles.generateSubmitBtn} onClick={handleHolidayBlock} disabled={holidayLoading||!holidayStartDate||!holidayEndDate}>
                    {holidayLoading ? <span className={styles.btnSpinner}/> : <><Icon name="umbrella" size={15} /> Block</>}
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
