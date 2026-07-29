import { useEffect, useState } from "react";
import { useTherapistProfile } from "../context/therapistProfileStore";
import { useNavigate } from "react-router-dom";
import { getAvailability, getDashboardStats } from "../api/appointments";
import { useModeMap } from "../context/DeliveryModesContext";
import SessionTimer from "../components/SessionTimer";
import Icon from "../components/icons";
import styles from "./TherapistHomePage.module.css";

function toISODate(date) {
  const d = new Date(date);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function addDays(date, n) {
  const d = new Date(date);
  d.setDate(d.getDate() + n);
  return d;
}

function formatDate(dt) {
  if (!dt) return "—";
  const d = new Date(dt);
  const today = new Date();
  const tomorrow = new Date(today);
  tomorrow.setDate(today.getDate() + 1);
  if (d.toDateString() === today.toDateString()) return "Today";
  if (d.toDateString() === tomorrow.toDateString()) return "Tomorrow";
  return d.toLocaleDateString("en-IN", { day: "2-digit", month: "short" });
}

function formatTime(dt) {
  if (!dt) return "—";
  return new Date(dt).toLocaleTimeString("en-IN", {
    hour: "2-digit", minute: "2-digit", hour12: true,
  });
}

function isUpcoming(dt) {
  return new Date(dt) >= new Date();
}

function isSameDay(a, b) {
  const x = new Date(a), y = new Date(b);
  return x.toDateString() === y.toDateString();
}

// Monday-first start of the current week (chart runs Mon→Sun, like the prototype)
function startOfWeek(d = new Date()) {
  const x = new Date(d);
  x.setHours(0, 0, 0, 0);
  const day = (x.getDay() + 6) % 7; // Mon=0 … Sun=6
  x.setDate(x.getDate() - day);
  return x;
}

const DAY_LETTERS = ["M", "T", "W", "T", "F", "S", "S"];
const LIVE_STATUSES = ["SCHEDULED", "CONFIRMED", "RESCHEDULED", "COMPLETED"];

function getGreeting() {
  const h = new Date().getHours();
  if (h < 12) return "Good morning";
  if (h < 17) return "Good afternoon";
  return "Good evening";
}

function fullDate() {
  return new Date().toLocaleDateString("en-IN", { weekday: "long", day: "numeric", month: "long", year: "numeric" });
}

const ACTIONS = [
  { title: "My Profile", sub: "View or create your therapist profile", to: "/therapist/profile", icon: "heart" },
  { title: "My Services", sub: "Manage your therapy offerings", to: "/therapist/services", icon: "clipboard" },
  { title: "Availability Rules", sub: "Set your weekly schedule", to: "/therapist/availability-rules", icon: "clock" },
  { title: "Appointments", sub: "View calendar and book sessions", to: "/therapist/appointments", icon: "calendar" },
  { title: "My Clients", sub: "Browse your client list", to: "/therapist/clients", icon: "users" },
  { title: "Earnings Report", sub: "View and export earnings by date range", to: "/therapist/earnings", icon: "dollar" },
  { title: "Analytics", sub: "Session trends, outcomes, and service insights", to: "/therapist/analytics", icon: "bar" },
];

export default function TherapistHomePage() {
  const navigate = useNavigate();
  const modeMap = useModeMap();

  const [appointments, setAppointments] = useState([]);
  const [apptLoading, setApptLoading] = useState(true);
  const [apptError, setApptError] = useState(null);
  const [stats, setStats] = useState(null);
  const [statsLoading, setStatsLoading] = useState(true);

  // Greet by first name rather than login username — the shell already
  // fetched the profile, so this reuses it instead of refetching.
  const { firstName } = useTherapistProfile();

  useEffect(() => {
    getDashboardStats()
      .then(setStats)
      .catch(() => {})
      .finally(() => setStatsLoading(false));

    // fetch from the start of this week so the weekly chart has real past data
    const from = toISODate(startOfWeek());
    const next30 = toISODate(addDays(new Date(), 30));
    getAvailability(from, next30)
      .then((data) => setAppointments(data.appointments || []))
      .catch((e) => setApptError(e.message))
      .finally(() => setApptLoading(false));
  }, []);

  const active = appointments.filter((a) => a.status !== "CANCELLED" && a.status !== "ABANDONED");

  const upcoming = active
    .filter((a) => isUpcoming(a.startTime))
    .sort((a, b) => new Date(a.startTime) - new Date(b.startTime));

  const todaySessions = active
    .filter((a) => isSameDay(a.startTime, new Date()))
    .sort((a, b) => new Date(a.startTime) - new Date(b.startTime));

  // "Next up" = upcoming sessions that are NOT today
  const nextUp = upcoming.filter((a) => !isSameDay(a.startTime, new Date())).slice(0, 3);

  // Sessions per day for the current week (Mon→Sun)
  const wkStart = startOfWeek();
  const weekCounts = DAY_LETTERS.map((_, i) => {
    const day = addDays(wkStart, i);
    return active.filter((a) => isSameDay(a.startTime, day) && LIVE_STATUSES.includes(a.status)).length;
  });
  const weekMax = Math.max(1, ...weekCounts);
  const todayIdx = (new Date().getDay() + 6) % 7;

  const kpiValue = (v) => (statsLoading ? "—" : (v ?? 0));

  return (
    <div className={styles.content}>
      <div className={`page-head ${styles.reveal}`}>
        <div>
          <div className="eyebrow">Therapist workspace</div>
          <h1>{getGreeting()}, <span className="g">{firstName}</span></h1>
          <div className="sub">{fullDate()}</div>
        </div>
        <button className="btn btn-primary" onClick={() => navigate("/therapist/appointments")}>
          <Icon name="plus" size={18} /> New appointment
        </button>
      </div>

      <SessionTimer
        appointments={appointments}
        onOpen={() => navigate("/therapist/appointments")}
        className={styles.homeTimer}
      />

      <div className={`${styles.kpis} ${stats?.pendingNotes > 0 ? styles.kpis4 : ""} ${styles.reveal}`}>
        <div className="card kpi">
          <div className="kpi-top"><span className="kpi-ic ic-c"><Icon name="calendar" size={20} /></span></div>
          <div className="kpi-val">{kpiValue(stats?.sessionsToday)}</div>
          <div className="kpi-lbl">Sessions today</div>
        </div>
        <div className="card kpi">
          <div className="kpi-top"><span className="kpi-ic ic-g"><Icon name="users" size={20} /></span></div>
          <div className="kpi-val">{kpiValue(stats?.activeClients)}</div>
          <div className="kpi-lbl">Active clients</div>
        </div>
        <div className="card kpi">
          <div className="kpi-top"><span className="kpi-ic ic-a"><Icon name="check" size={20} /></span></div>
          <div className="kpi-val">{kpiValue(stats?.completedThisWeek)}</div>
          <div className="kpi-lbl">Completed this week</div>
        </div>
        {/* Only shown when there is something to act on — a permanent zero
            would be noise on every visit. */}
        {stats?.pendingNotes > 0 && (
          <button className={`card kpi ${styles.kpiBtn}`} onClick={() => navigate("/therapist/clients")}>
            <div className="kpi-top"><span className="kpi-ic ic-v"><Icon name="clipboard" size={20} /></span></div>
            <div className="kpi-val">{kpiValue(stats?.pendingNotes)}</div>
            <div className="kpi-lbl">Notes due</div>
          </button>
        )}
      </div>

      <div className={`grid-2 ${styles.reveal}`}>
        {/* Today's schedule */}
        <div className="card">
          <div className="panel-h">
            <div>
              <h2>Today's schedule</h2>
              <p>{apptLoading ? "Loading…" : `${todaySessions.length} session${todaySessions.length !== 1 ? "s" : ""}`}</p>
            </div>
            <button className="link" onClick={() => navigate("/therapist/appointments")}>
              View calendar <Icon name="chevron" size={15} />
            </button>
          </div>

          {apptLoading && (
            <div className={styles.sessionsEmpty}><div className={styles.spinner} /><p className={styles.loadingText}>Loading…</p></div>
          )}
          {!apptLoading && apptError && (
            <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{apptError}</div>
          )}
          {!apptLoading && !apptError && todaySessions.length === 0 && (
            <div className={styles.sessionsEmpty}>
              <span className={styles.emptyIcon}><Icon name="calendar" size={26} /></span>
              <p className={styles.emptyText}>Nothing scheduled today</p>
            </div>
          )}
          {!apptLoading && !apptError && todaySessions.map((a) => {
            const mode = modeMap[a.modeId];
            const isOnline = mode?.modeType === "ONLINE";
            return (
              <div key={a.appointmentId} className="sess">
                <div className="time"><b>{formatTime(a.startTime)}</b></div>
                <span className={`avatar avatar-s ${styles.sessAvatar}`}>{a.clientName?.[0]?.toUpperCase() ?? "?"}</span>
                <div className="info">
                  <b>{a.clientName || "—"}</b>
                  <div className="s"><Icon name={isOnline ? "video" : "pin"} size={13} /> {mode?.displayName ?? "—"}</div>
                </div>
                <span className={`chip ${isOnline ? "chip-online" : "chip-clinic"}`}>{isOnline ? "Online" : "Clinic"}</span>
              </div>
            );
          })}
        </div>

        {/* Right column: weekly chart + next up */}
        <div className="col">
          <div className="card" style={{ padding: "18px 20px 20px" }}>
            <div className="panel-h" style={{ padding: "0 0 4px" }}>
              <div><h2>This week</h2><p>Sessions per day</p></div>
            </div>
            <div className="bars">
              {weekCounts.map((c, i) => (
                <div key={i} className={`b ${i === todayIdx ? "today" : ""}`}>
                  <div className="bar" style={{ height: `${Math.round((c / weekMax) * 100)}%` }} title={`${c} session${c !== 1 ? "s" : ""}`} />
                  <div className="d">{DAY_LETTERS[i]}</div>
                </div>
              ))}
            </div>
          </div>

          <div className="card">
            <div className="panel-h"><div><h2>Next up</h2><p>After today</p></div></div>
            {nextUp.length === 0 && (
              <div className={styles.sessionsEmpty}><p className={styles.emptyText}>No upcoming sessions</p></div>
            )}
            {nextUp.map((a) => {
              const mode = modeMap[a.modeId];
              const isOnline = mode?.modeType === "ONLINE";
              return (
                <div key={a.appointmentId} className="sess" onClick={() => navigate("/therapist/appointments")} style={{ cursor: "pointer" }}>
                  <div className="time"><b>{formatDate(a.startTime)}</b><span>{formatTime(a.startTime)}</span></div>
                  <span className={`avatar avatar-s ${styles.sessAvatar}`}>{a.clientName?.[0]?.toUpperCase() ?? "?"}</span>
                  <div className="info">
                    <b>{a.clientName || "—"}</b>
                    <div className="s"><Icon name={isOnline ? "video" : "pin"} size={13} /> {mode?.displayName ?? "—"}</div>
                  </div>
                  <Icon name="chevron" size={18} className={styles.actionArrow} />
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Quick actions — full width, 4 columns (prototype) */}
      <h2 className={`${styles.sectionTitle} ${styles.reveal}`} style={{ margin: "26px 0 14px" }}>Quick actions</h2>
      <div className={`qa ${styles.reveal}`}>
        {ACTIONS.map((a) => (
          <button key={a.to} className="qa-item" onClick={() => navigate(a.to)}>
            <span className="qa-ic"><Icon name={a.icon} size={20} /></span>
            <b>{a.title}</b>
            <span>{a.sub}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
