import { useEffect, useState, useMemo, useRef, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { getAvailability, createAppointment, generateSlots, updateAppointmentStatus, rescheduleAppointment, createAvailabilityOverride, deleteAvailabilityOverride, bulkAvailabilityOverrides, searchAppointments } from "../api/appointments";
import { getTherapistClients } from "../api/therapistClients";
import { useModeMap, useAllModes } from "../context/DeliveryModesContext";
import styles from "./AppointmentsPage.module.css";

// ─── Constants ────────────────────────────────────────────────────────────────
const DAY_SHORT = ["Sun","Mon","Tue","Wed","Thu","Fri","Sat"];
const MONTHS = ["January","February","March","April","May","June","July","August","September","October","November","December"];
const MONTHS_SHORT = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];
const STATUS_ICON = { CONFIRMED:"✅", COMPLETED:"🏁", CANCELLED:"✕", ABANDONED:"⚠️", SCHEDULED:"🗓️", RESCHEDULED:"🔄" };

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
const APPT_STATUS_COLORS = {
  CONFIRMED:   { bg: "rgba(99,102,241,0.18)",  border: "#6366f1", text: "#a5b4fc" },
  SCHEDULED:   { bg: "rgba(16,185,129,0.15)",  border: "#10b981", text: "#34d399" },
  RESCHEDULED: { bg: "rgba(245,158,11,0.12)",  border: "#f59e0b", text: "#fbbf24" },
  COMPLETED:   { bg: "rgba(16,185,129,0.15)",  border: "#10b981", text: "#34d399" },
  CANCELLED:   { bg: "rgba(239,68,68,0.12)",   border: "#ef4444", text: "#f87171" },
  ABANDONED:   { bg: "rgba(245,158,11,0.12)",  border: "#f59e0b", text: "#fbbf24" },
};

// ─── Time helpers ─────────────────────────────────────────────────────────────
const HOUR_START = 6;
const HOUR_END   = 22;
const TOTAL_HOURS = HOUR_END - HOUR_START;
const PX_PER_HOUR = 80;
const CANVAS_HEIGHT = TOTAL_HOURS * PX_PER_HOUR;

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
        <span className={`${styles.dropdownChevron} ${open ? styles.dropdownChevronOpen : ""}`}>▾</span>
      </button>
      {open && (
        <div className={styles.dropdownMenu}>
          <div className={styles.dropdownSearch}>
            <span className={styles.dropdownSearchIcon}>⌕</span>
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

function ModeDropdown({ modes, value, onChange }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  const selected = modes.find(m => m.modeId === value);
  useEffect(() => {
    const h = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener("mousedown", h); return () => document.removeEventListener("mousedown", h);
  }, []);
  const modeIcon = { ONLINE: "💻", OFFLINE_AT_HALUSURU: "📍", OFFLINE_AT_SESHADRIPURAM: "📍" };
  return (
    <div className={styles.customDropdown} ref={ref}>
      <button type="button" className={`${styles.dropdownTrigger} ${open ? styles.dropdownTriggerOpen : ""}`} onClick={() => setOpen(o => !o)}>
        <span className={selected ? styles.dropdownValueSet : styles.dropdownPlaceholder}>
          {selected ? `${modeIcon[selected.modeType] ?? "💬"} ${selected.displayName}` : "Select delivery mode"}
        </span>
        <span className={`${styles.dropdownChevron} ${open ? styles.dropdownChevronOpen : ""}`}>▾</span>
      </button>
      {open && (
        <div className={styles.dropdownMenu}>
          <div className={styles.dropdownList}>
            {modes.length === 0 && <div className={styles.dropdownEmpty}>No modes available for this service</div>}
            {modes.map(m => (
              <div key={m.modeId} className={`${styles.dropdownItem} ${m.modeId === value ? styles.dropdownItemActive : ""}`} onClick={() => { onChange(m.modeId); setOpen(false); }}>
                <span className={styles.dropdownItemAvatar}>{modeIcon[m.modeType] ?? "💬"}</span>
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

  // Drag to create override
  const [dragging, setDragging] = useState(false);
  const [dragStart, setDragStart] = useState(null);
  const [dragEnd, setDragEnd] = useState(null);
  const dragRef = useRef({ active: false, startY: 0 });

  const [panel, setPanel] = useState(null);
  const [panelSlot, setPanelSlot] = useState(null);

  // Booking form
  const [booking, setBooking] = useState({ clientId: "", clientName: "", modeId: "", useCustomPrice: false, customPrice: "" });
  const [bookingModes, setBookingModes] = useState([]); // modes for the slot's service
  const [bookingLoading, setBookingLoading] = useState(false);
  const [bookingError, setBookingError] = useState(null);
  const [bookingSuccess, setBookingSuccess] = useState(false);

  // Update status
  const [updateStatus, setUpdateStatus] = useState("");
  const [updateReason, setUpdateReason] = useState("");
  const [updateLoading, setUpdateLoading] = useState(false);
  const [updateError, setUpdateError] = useState(null);

  // Reschedule
  const [reschedWeekStart, setReschedWeekStart] = useState(() => getWeekStart(new Date()));
  const [reschedSelectedDate, setReschedSelectedDate] = useState(null);
  const [reschedNewSlot, setReschedNewSlot] = useState(null);
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
  const [searchClient, setSearchClient]         = useState("");
  const [filterStatuses, setFilterStatuses]     = useState([]);
  const [searchResults, setSearchResults]       = useState(null);
  const [searchLoading, setSearchLoading]       = useState(false);
  const [searchError, setSearchError]           = useState(null);

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

  const availableSlots = useMemo(() => {
    const activeAppts = dayAppointments.filter(a =>
      a.status !== "CANCELLED" && a.status !== "ABANDONED"
    );
    return daySlots.filter(s => {
      if (s.slotStatus !== "AVAILABLE") return false;
      if (new Date(s.startTime) <= now) return false;
      const ss = new Date(s.startTime).getTime(), se = new Date(s.endTime).getTime();
      return !activeAppts.some(a => ss < new Date(a.endTime).getTime() && se > new Date(a.startTime).getTime());
    });
  }, [daySlots, dayAppointments]);

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
  const openBook = (slot) => {
    setPanelSlot(slot);
    // Pre-select the slot's own modeId if available; filter modes for the slot's service
    const slotModes = slot.serviceId
      ? allModes.filter(m => m.serviceId === slot.serviceId && m.isActive)
      : allModes.filter(m => m.isActive);
    setBookingModes(slotModes);
    setBooking({ clientId: "", clientName: "", modeId: slot.modeId || "", useCustomPrice: false, customPrice: "" });
    setBookingError(null); setBookingSuccess(false);
    setPanel("book");
  };

  const openUpdate = (slot) => {
    setPanelSlot(slot);
    setUpdateStatus(slot.appointmentStatus || "");
    setUpdateReason(""); setUpdateError(null);
    setPanel("update");
  };

  const openReschedule = (slot) => {
    setPanelSlot(slot);
    setReschedWeekStart(getWeekStart(new Date()));
    setReschedSelectedDate(null); setReschedNewSlot(null);
    setReschedReason(""); setReschedError(null);
    setPanel("reschedule");
  };

  const handleBook = async (e) => {
    e.preventDefault();
    if (!booking.clientId) { setBookingError("Please select a client."); return; }
    if (!booking.modeId) { setBookingError("Please select a delivery mode."); return; }
    if (booking.useCustomPrice && (!booking.customPrice || parseFloat(booking.customPrice) < 0)) {
      setBookingError("Please enter a valid custom session fee."); return;
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
      setSlots(prev => prev.map(s => s.slotId === panelSlot.slotId ? { ...s, slotStatus: "BOOKED", clientId: booking.clientId, clientName: booking.clientName } : s));
      setAppointments(prev => [...prev, {
        appointmentId: result?.appointmentId || `tmp-${Date.now()}`,
        clientId: booking.clientId,
        clientName: booking.clientName,
        startTime: panelSlot.startTime,
        endTime: panelSlot.endTime,
        status: "SCHEDULED",
        modeId: booking.modeId,
      }]);
      setBookingSuccess(true);
      setTimeout(() => setPanel(null), 1200);
    } catch (err) { setBookingError(friendlyError(err.message)); }
    finally { setBookingLoading(false); }
  };

  const handleUpdateStatus = async () => {
    if (!updateStatus) { setUpdateError("Please select a status."); return; }
    setUpdateLoading(true); setUpdateError(null);
    try {
      await updateAppointmentStatus({ appointmentId: panelSlot.appointmentId, therapistId: panelSlot.therapistId, status: updateStatus, reason: updateReason || undefined });
      setSlots(prev => prev.map(s => s.slotId === panelSlot.slotId ? { ...s, appointmentStatus: updateStatus } : s));
      setAppointments(prev => prev.map(a => a.appointmentId === panelSlot.appointmentId ? { ...a, status: updateStatus } : a));
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
  };

  const handleReschedule = async () => {
    if (!reschedNewSlot) { setReschedError("Please select a new slot."); return; }
    setReschedLoading(true); setReschedError(null);
    try {
      await rescheduleAppointment({ appointmentId: panelSlot.appointmentId, therapistId: panelSlot.therapistId, newSlotId: reschedNewSlot.slotId, reason: reschedReason || undefined });
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

  const handleSearch = async () => {
    if (!searchClient && filterStatuses.length === 0) { clearSearch(); return; }
    setSearchLoading(true); setSearchError(null);
    try {
      const results = await searchAppointments({
        clientName: searchClient || undefined,
        status: filterStatuses.length > 0 ? filterStatuses : undefined,
        fromDate: toISODate(weekStart),
        toDate: toISODate(addDays(weekStart, 6)),
      });
      setSearchResults(results);
    } catch (err) { setSearchError(err.message); }
    finally { setSearchLoading(false); }
  };

  const clearSearch = () => {
    setSearchClient("");
    setFilterStatuses([]);
    setSearchResults(null);
    setSearchError(null);
  };

  // ─── Render helpers ────────────────────────────────────────────────────────────
  const hourLabels = Array.from({ length: TOTAL_HOURS + 1 }, (_, i) => {
    const h = HOUR_START + i;
    return h === 12 ? "12 PM" : h < 12 ? `${h} AM` : `${h-12} PM`;
  });

  const nowMinutes = now.toDateString() === selectedDate.toDateString()
    ? now.getHours() * 60 + now.getMinutes() : null;

  return (
    <div className={styles.page}>
      {/* Header */}
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <button className={styles.back} onClick={() => navigate("/therapist-home")}>← Back</button>
          <span className={styles.logo}>🧠 Therapy Connect</span>
          <div className={styles.headerActions}>
            <span className={styles.rolePill}>Therapist</span>
          </div>
        </div>
      </header>

      <div className={styles.scheduleLayout}>
        {/* ── Left: week strip + timeline canvas ── */}
        <div className={styles.canvasArea}>
          <div className={styles.canvasTop}>
            <div>
              <h1 className={styles.heading}>Schedule</h1>
              <p className={styles.sub}>Drag on the timeline to mark unavailable time</p>
            </div>
            <div className={styles.legend}>
              <span className={styles.legendItem}><span className={styles.legendDot} style={{background:"#6366f1"}} />Booked</span>
              <span className={styles.legendItem}><span className={styles.legendDot} style={{background:"#10b981"}} />Available</span>
              <span className={styles.legendItem}><span className={styles.legendDot} style={{background:"rgba(239,68,68,0.6)"}} />Unavailable</span>
            </div>
          </div>

          {slotsError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{slotsError}</div>}

          {/* Week strip */}
          <div className={styles.weekCard}>
            <div className={styles.weekNav}>
              <button className={styles.navBtn} onClick={prevWeek}>‹</button>
              <div className={styles.weekNavCenter}>
                <span className={styles.weekLabel}>{weekLabel}</span>
                <button className={styles.todayBtn} onClick={goToday}>Today</button>
              </div>
              <button className={styles.navBtn} onClick={nextWeek}>›</button>
            </div>
            <div className={styles.weekGrid}>
              {weekDays.map((date, i) => {
                const isToday = date.toDateString() === new Date().toDateString();
                const isSel = date.toDateString() === selectedDate.toDateString();
                const avail = hasAvailable(date), booked = hasBooked(date);
                return (
                  <div key={i} className={`${styles.weekDay} ${isToday ? styles.weekDayToday:""} ${isSel ? styles.weekDaySelected:""}`} onClick={() => setSelectedDate(new Date(date))}>
                    <span className={styles.weekDayName}>{DAY_SHORT[date.getDay()]}</span>
                    <span className={styles.weekDayNum}>{date.getDate()}</span>
                    <div className={styles.weekDotRow}>
                      {avail && <span className={styles.dotAvailable}/>}
                      {booked && <span className={styles.dotBooked}/>}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Search / filter bar */}
          <div className={styles.searchBar}>
            <input
              className={styles.searchInput}
              placeholder="Search by client name…"
              value={searchClient}
              onChange={e => setSearchClient(e.target.value)}
              onKeyDown={e => e.key === "Enter" && handleSearch()}
            />
            <div className={styles.statusFilterChips}>
              {["SCHEDULED","CONFIRMED","COMPLETED","CANCELLED","ABANDONED","RESCHEDULED"].map(s => (
                <button
                  key={s}
                  className={`${styles.filterChip} ${filterStatuses.includes(s) ? styles.filterChipActive : ""}`}
                  onClick={() => setFilterStatuses(prev => prev.includes(s) ? prev.filter(x => x !== s) : [...prev, s])}
                >{s}</button>
              ))}
            </div>
            <div className={styles.searchActions}>
              <button className={styles.searchBtn} onClick={handleSearch} disabled={searchLoading}>
                {searchLoading ? <span className={styles.btnSpinner}/> : "🔍 Search"}
              </button>
              {searchResults !== null && (
                <button className={styles.clearSearchBtn} onClick={clearSearch}>✕ Clear</button>
              )}
            </div>
            {searchError && <p className={styles.searchError}>{searchError}</p>}
          </div>

          <div className={styles.dayLabel}>
            <h2 className={styles.dayLabelText}>
              {DAY_SHORT[selectedDate.getDay()]}, {selectedDate.getDate()} {MONTHS[selectedDate.getMonth()]} {selectedDate.getFullYear()}
            </h2>
            <div className={styles.dayLabelRight}>
              <div className={styles.dayStats}>
                <span>{dayAppointments.length} booked</span>
                <span>·</span>
                <span>{availableSlots.length} available</span>
                {dayOverrides.length > 0 && <><span>·</span><span>{dayOverrides.length} override{dayOverrides.length !== 1 ? "s" : ""}</span></>}
              </div>
              <button className={styles.holidayBtn} onClick={() => { setPanel("holiday"); setHolidaySuccess(false); setHolidayError(null); setHolidayStartDate(""); setHolidayEndDate(""); setHolidayReason(""); }}>🏖 Block Holiday</button>
              <button className={styles.generateBtn} onClick={() => { setPanel("generate"); setGenSuccess(false); setGenError(null); }}>⚡ Generate Slots</button>
            </div>
          </div>

          {/* Search results list */}
          {searchResults !== null && (
            <div className={styles.searchResultsPanel}>
              <h3 className={styles.searchResultsTitle}>
                {searchResults.length} result{searchResults.length !== 1 ? "s" : ""}
                {searchClient ? ` for "${searchClient}"` : ""}
                {filterStatuses.length > 0 ? ` · ${filterStatuses.join(", ")}` : ""}
              </h3>
              {searchResults.length === 0 && (
                <p className={styles.searchEmpty}>No appointments match your search.</p>
              )}
              {searchResults.map(a => (
                <div key={a.appointmentId} className={styles.searchResultItem}>
                  <div className={styles.searchResultLeft}>
                    <span className={styles.searchResultClient}>{a.clientName}</span>
                    <span className={styles.searchResultTime}>{formatTime(a.startTime)} – {formatTime(a.endTime)}</span>
                  </div>
                  <span className={`${styles.slotBadge} ${styles[`apptStatus_${a.status}`] || styles.apptStatusDefault}`}>{a.status}</span>
                </div>
              ))}
            </div>
          )}

          {/* Timeline canvas */}
          {loadingSlots ? (
            <div className={styles.canvasLoading}><div className={styles.spinner}/></div>
          ) : (
            <div className={styles.timelineWrapper}>
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
                  const height = Math.max(minutesToPx(toMinutes(override.endTime)) - top, 20);
                  return (
                    <div
                      key={override.overrideId}
                      className={styles.overrideBlock}
                      style={{ top, height }}
                      onClick={e => { e.stopPropagation(); setOverrideRange({ startMin: toMinutes(override.startTime), endMin: toMinutes(override.endTime), overrideId: override.overrideId, reason: override.reason }); setOverrideNote(override.reason || ""); setOverrideIsAvailable(override.available ?? false); setOverrideSyncGcal(false); setOverrideError(null); setPanel("override"); }}
                      title={`Unavailable: ${formatTime(override.startTime)} – ${formatTime(override.endTime)}${override.reason ? ` · ${override.reason}` : ""}`}
                    >
                      <span className={styles.overrideBlockLabel}>⛔ {formatTime(override.startTime)}{override.reason ? ` · ${override.reason}` : ""}</span>
                    </div>
                  );
                })}

                {availableSlots.map(slot => {
                  const top = minutesToPx(toMinutes(slot.startTime));
                  const height = minutesToPx(toMinutes(slot.endTime)) - top;
                  const slotStartMin = toMinutes(slot.startTime);
                  const slotEndMin = toMinutes(slot.endTime);
                  const overlapsD = dragging && dragStart !== null && dragEnd !== null && slotStartMin < dragEnd && slotEndMin > dragStart;
                  return (
                    <div
                      key={slot.slotId}
                      className={`${styles.availableBlock} ${overlapsD ? styles.availableBlockDimmed : ""}`}
                      style={{ top, height }}
                      onClick={e => { e.stopPropagation(); openBook(slot); }}
                      title={`Available: ${formatTime(slot.startTime)} – ${formatTime(slot.endTime)}`}
                    >
                      <span className={styles.availableBlockLabel}>{formatTime(slot.startTime)}</span>
                    </div>
                  );
                })}

                {dayAppointments
                  .filter(appt => appt.status !== "CANCELLED" && appt.status !== "ABANDONED")
                  .map(appt => {
                  const top = minutesToPx(toMinutes(appt.startTime));
                  const height = Math.max(minutesToPx(toMinutes(appt.endTime)) - top, 28);
                  const colors = APPT_STATUS_COLORS[appt.status] || APPT_STATUS_COLORS.CONFIRMED;
                  const apptAsSlot = {
                    appointmentId: appt.appointmentId,
                    therapistId: appt.therapistId || slots.find(s => s.appointmentId === appt.appointmentId)?.therapistId,
                    clientId: appt.clientId,
                    clientName: appt.clientName,
                    startTime: appt.startTime,
                    endTime: appt.endTime,
                    appointmentStatus: appt.status,
                    modeId: appt.modeId,
                    slotId: slots.find(s => s.appointmentId === appt.appointmentId)?.slotId,
                  };
                  const modeName = modeMap[appt.modeId]?.displayName;
                  return (
                    <div
                      key={appt.appointmentId}
                      className={styles.bookedBlock}
                      style={{ top, height, background: colors.bg, borderColor: colors.border }}
                      onClick={e => { e.stopPropagation(); openUpdate(apptAsSlot); }}
                      title={`${appt.clientName} · ${formatTime(appt.startTime)} – ${formatTime(appt.endTime)}`}
                    >
                      <div className={styles.bookedBlockAccent} style={{ background: colors.border }}/>
                      <div className={styles.bookedBlockContent}>
                        <span className={styles.bookedBlockTime} style={{ color: colors.text }}>{formatTime(appt.startTime)} – {formatTime(appt.endTime)}</span>
                        {height > 36 && <span className={styles.bookedBlockClient}>{appt.clientName}</span>}
                        {height > 52 && <span className={styles.bookedBlockStatus} style={{ color: colors.text }}>{STATUS_ICON[appt.status]} {appt.status}{modeName ? ` · ${modeName}` : ""}</span>}
                      </div>
                      {height > 40 && (
                        <button className={styles.reschedBadge} onClick={e => { e.stopPropagation(); openReschedule(apptAsSlot); }} title="Reschedule">↺</button>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>

        {/* ── Right: action panel ── */}
        <div className={`${styles.panel} ${panel ? styles.panelOpen : ""}`}>
          {!panel && (
            <div className={styles.panelEmpty}>
              <span className={styles.panelEmptyIcon}>👆</span>
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
                  <p className={styles.panelSub}>{formatTime(panelSlot.startTime)} – {formatTime(panelSlot.endTime)}</p>
                </div>
                <button className={styles.closeBtn} onClick={() => setPanel(null)}>✕</button>
              </div>
              {bookingSuccess ? (
                <div className={styles.successBox}><span className={styles.successIcon}>✓</span> Booked!</div>
              ) : (
                <form onSubmit={handleBook} className={styles.panelForm}>
                  <div className={styles.field}><label className={styles.label}>Client</label>
                    <ClientDropdown clients={clients} value={booking.clientId} onChange={(id,name) => setBooking(p => ({...p, clientId:id, clientName:name}))} />
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
                    <div className={styles.summaryRow}><span className={styles.summaryLabel}>Time</span><span className={styles.summaryValue}>{formatTime(panelSlot.startTime)} – {formatTime(panelSlot.endTime)}</span></div>
                    <div className={styles.summaryRow}><span className={styles.summaryLabel}>Slot</span><span className={styles.summaryValue}>{panelSlot.slotId}</span></div>
                  </div>
                  {bookingError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{bookingError}</div>}
                  <div className={styles.formActions}>
                    <button type="button" className={styles.cancelBtn} onClick={() => setPanel(null)}>Cancel</button>
                    <button type="submit" className={styles.submitBtn} disabled={bookingLoading}>{bookingLoading ? <span className={styles.btnSpinner}/> : "Confirm"}</button>
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
                <button className={styles.closeBtn} onClick={() => setPanel(null)}>✕</button>
              </div>
              <div className={styles.panelForm}>
                <div className={styles.slotSummary}>
                  <div className={styles.summaryRow}>
                    <span className={styles.summaryLabel}>Client</span>
                    <span className={styles.clientLink} onClick={() => navigate(`/therapist/clients/${panelSlot.clientId}`)}>{panelSlot.clientName}</span>
                  </div>
                  <div className={styles.summaryRow}><span className={styles.summaryLabel}>Current</span>
                    <span className={`${styles.slotBadge} ${styles[`apptStatus_${panelSlot.appointmentStatus}`] || styles.apptStatusDefault}`}>{panelSlot.appointmentStatus}</span>
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
                </div>
                <div className={styles.field}><label className={styles.label}>New Status</label>
                  <div className={styles.statusGrid}>
                    {["CONFIRMED", "COMPLETED", "CANCELLED", "ABANDONED"].map(s => (
                      <button key={s} type="button"
                        className={`${styles.statusOption} ${updateStatus === s ? styles.statusOptionActive : ""} ${styles[`statusOption_${s}`] || ""}`}
                        onClick={() => setUpdateStatus(s)}>
                        {STATUS_ICON[s]} {s}
                      </button>
                    ))}
                  </div>
                </div>
                <div className={styles.field}><label className={styles.label}>Reason <span className={styles.optionalTag}>(optional)</span></label>
                  <textarea className={styles.reasonTextarea} rows={3} placeholder="Reason…" value={updateReason} onChange={e => setUpdateReason(e.target.value)}/>
                </div>
                {updateError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{updateError}</div>}
                <div className={styles.formActions}>
                  <button className={styles.cancelBtn} onClick={() => setPanel(null)}>Cancel</button>
                  <button className={styles.rescheduleActionBtn} onClick={() => openReschedule(panelSlot)}>↺ Reschedule</button>
                  <button className={styles.submitBtn} onClick={handleUpdateStatus} disabled={updateLoading || !updateStatus || updateStatus === panelSlot.appointmentStatus}>
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
                <button className={styles.closeBtn} onClick={() => setPanel(null)}>✕</button>
              </div>
              <div className={styles.panelForm}>
                <div className={styles.field}><label className={styles.label}>Select New Date</label>
                  <div className={styles.reschedWeekCard}>
                    <div className={styles.reschedWeekNav}>
                      <button className={styles.navBtn} onClick={() => { setReschedWeekStart(d => addDays(d,-7)); setReschedSelectedDate(null); setReschedNewSlot(null); }}>‹</button>
                      <span className={styles.reschedWeekLabel}>{(() => { const e = addDays(reschedWeekStart,6); return reschedWeekStart.getMonth()===e.getMonth()?`${reschedWeekStart.getDate()}–${e.getDate()} ${MONTHS_SHORT[reschedWeekStart.getMonth()]}`: `${reschedWeekStart.getDate()} ${MONTHS_SHORT[reschedWeekStart.getMonth()]} – ${e.getDate()} ${MONTHS_SHORT[e.getMonth()]}`; })()}</span>
                      <button className={styles.navBtn} onClick={() => { setReschedWeekStart(d => addDays(d,7)); setReschedSelectedDate(null); setReschedNewSlot(null); }}>›</button>
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
                            <button key={s.slotId} type="button" className={`${styles.reschedSlotBtn} ${reschedNewSlot?.slotId===s.slotId?styles.reschedSlotBtnActive:""}`} onClick={()=>setReschedNewSlot(s)}>
                              {formatTime(s.startTime)} – {formatTime(s.endTime)}
                              {modeName && <span className={styles.reschedSlotType}>{modeName}</span>}
                            </button>
                          );
                        })}
                      </div>
                    )}
                  </div>
                )}
                <div className={styles.field}><label className={styles.label}>Reason <span className={styles.optionalTag}>(optional)</span></label>
                  <textarea className={styles.reasonTextarea} rows={3} placeholder="Reason…" value={reschedReason} onChange={e=>setReschedReason(e.target.value)}/>
                </div>
                {reschedError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{reschedError}</div>}
                <div className={styles.formActions}>
                  <button className={styles.cancelBtn} onClick={() => setPanel(null)}>Cancel</button>
                  <button className={styles.submitBtn} onClick={handleReschedule} disabled={reschedLoading||!reschedNewSlot}>{reschedLoading?<span className={styles.btnSpinner}/>:"Confirm"}</button>
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
                <button className={styles.closeBtn} onClick={() => setPanel(null)}>✕</button>
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
                        {overrideDeleteLoading ? <span className={styles.btnSpinner}/> : "🗑 Delete Override"}
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
                          ⛔ Unavailable
                        </button>
                        <button type="button"
                          className={`${styles.overrideToggleBtn} ${overrideIsAvailable ? styles.overrideToggleBtnAvailable : ""}`}
                          onClick={() => { setOverrideIsAvailable(true); setOverrideSyncGcal(false); }}>
                          ✅ Available
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
                <button className={styles.closeBtn} onClick={() => setPanel(null)}>✕</button>
              </div>
              <div className={styles.panelForm}>
                <div className={styles.dateRow}>
                  <div className={styles.field}><label className={styles.label}>From</label>
                    <input type="date" className={styles.dateInput} value={genStartDate} onChange={e=>{setGenStartDate(e.target.value);setGenSuccess(false);setGenError(null);}}/>
                  </div>
                  <div className={styles.dateSep}>→</div>
                  <div className={styles.field}><label className={styles.label}>To</label>
                    <input type="date" className={styles.dateInput} value={genEndDate} onChange={e=>{setGenEndDate(e.target.value);setGenSuccess(false);setGenError(null);}}/>
                  </div>
                </div>
                {genError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{genError}</div>}
                {genSuccess && <div className={styles.successBox}><span className={styles.successIcon}>✓</span> Slots generated! Refreshing calendar…</div>}
                <div className={styles.formActions}>
                  <button className={styles.cancelBtn} onClick={() => setPanel(null)}>Cancel</button>
                  <button className={styles.generateSubmitBtn} onClick={handleGenerate} disabled={genLoading||!genStartDate||!genEndDate}>{genLoading?<span className={styles.btnSpinner}/>:"⚡ Generate"}</button>
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
                <button className={styles.closeBtn} onClick={() => setPanel(null)}>✕</button>
              </div>
              <div className={styles.panelForm}>
                <div className={styles.dateRow}>
                  <div className={styles.field}><label className={styles.label}>From</label>
                    <input type="date" className={styles.dateInput} value={holidayStartDate} onChange={e=>{setHolidayStartDate(e.target.value);setHolidaySuccess(false);setHolidayError(null);}}/>
                  </div>
                  <div className={styles.dateSep}>→</div>
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
                {holidaySuccess && <div className={styles.successBox}><span className={styles.successIcon}>✓</span> Holiday blocked successfully!</div>}
                <div className={styles.formActions}>
                  <button className={styles.cancelBtn} onClick={() => setPanel(null)}>Cancel</button>
                  <button className={styles.generateSubmitBtn} onClick={handleHolidayBlock} disabled={holidayLoading||!holidayStartDate||!holidayEndDate}>
                    {holidayLoading ? <span className={styles.btnSpinner}/> : "🏖 Block"}
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
