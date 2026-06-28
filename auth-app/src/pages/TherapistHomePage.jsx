import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { getAvailability, getDashboardStats } from "../api/appointments";
import { useModeMap } from "../context/DeliveryModesContext";
import styles from "./TherapistHomePage.module.css";

const MODE_TYPE_ICON = {
  ONLINE: "💻",
  OFFLINE_AT_HALUSURU: "📍",
  OFFLINE_AT_SESHADRIPURAM: "📍",
};

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

function getGreeting() {
  const h = new Date().getHours();
  if (h < 12) return "Good morning";
  if (h < 17) return "Good afternoon";
  return "Good evening";
}

export default function TherapistHomePage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const modeMap = useModeMap();

  const [appointments, setAppointments] = useState([]);
  const [apptLoading, setApptLoading] = useState(true);
  const [apptError, setApptError] = useState(null);
  const [stats, setStats] = useState(null);
  const [statsLoading, setStatsLoading] = useState(true);

  const handleLogout = () => { logout(); navigate("/login"); };
  const displayName = user?.username || user?.name || "Therapist";

  useEffect(() => {
    getDashboardStats()
      .then(setStats)
      .catch(() => {})
      .finally(() => setStatsLoading(false));

    // Fetch upcoming sessions for the next 30 days via editor-view
    // (get-appointments only returns today; editor-view supports a date range)
    const today = toISODate(new Date());
    const next30 = toISODate(addDays(new Date(), 30));
    getAvailability(today, next30)
      .then((data) => setAppointments(data.appointments || []))
      .catch((e) => setApptError(e.message))
      .finally(() => setApptLoading(false));
  }, []);

  const upcoming = appointments
    .filter((a) => isUpcoming(a.startTime))
    .sort((a, b) => new Date(a.startTime) - new Date(b.startTime));

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <span className={styles.logo}>🧠 Therapy Connect</span>
          <div className={styles.headerRight}>
            <span className={styles.rolePill}>Therapist</span>
            <span className={styles.username}>{displayName}</span>
            <button className={styles.settingsBtn} onClick={() => navigate("/account-settings")} title="Account Settings">⚙️</button>
            <button onClick={handleLogout} className={styles.logoutBtn}>Sign out</button>
          </div>
        </div>
      </header>

      <main className={styles.main}>
        <div className={styles.hero}>
          <p className={styles.eyebrow}>Therapist Portal ✦</p>
          <h1 className={styles.heading}>
            {getGreeting()}, <span className={styles.accent}>{displayName}</span>
          </h1>
        </div>

        <div className={styles.layout}>
          {/* ── Left column ── */}
          <div className={styles.leftCol}>
            <div className={styles.statsRow}>
              <div className={styles.statCard}>
                <span className={styles.statIcon}>🧠</span>
                <span className={styles.statLabel}>Sessions today</span>
                <span className={styles.statValue}>{statsLoading ? "—" : (stats?.sessionsToday ?? 0)}</span>
              </div>
              <div className={styles.statCard}>
                <span className={styles.statIcon}>⭐</span>
                <span className={styles.statLabel}>Active clients</span>
                <span className={styles.statValue}>{statsLoading ? "—" : (stats?.activeClients ?? 0)}</span>
              </div>
              <div className={styles.statCard}>
                <span className={styles.statIcon}>✅</span>
                <span className={styles.statLabel}>Completed this week</span>
                <span className={styles.statValue}>{statsLoading ? "—" : (stats?.completedThisWeek ?? 0)}</span>
              </div>
              <div className={`${styles.statCard} ${styles.earningsCard}`}>
                <div className={styles.earningsHeader}>
                  <span className={styles.statIcon}>💰</span>
                  <span className={styles.earningsTitle}>Earnings</span>
                </div>
                <div className={styles.earningsGrid}>
                  <div className={styles.earningsItem}>
                    <span className={styles.earningsLabel}>Today</span>
                    <span className={styles.earningsValue}>{statsLoading ? "—" : `₹${Number(stats?.dayEarnings ?? 0).toLocaleString("en-IN")}`}</span>
                  </div>
                  <div className={styles.earningsItem}>
                    <span className={styles.earningsLabel}>This week</span>
                    <span className={styles.earningsValue}>{statsLoading ? "—" : `₹${Number(stats?.weekEarnings ?? 0).toLocaleString("en-IN")}`}</span>
                  </div>
                  <div className={styles.earningsItem}>
                    <span className={styles.earningsLabel}>This month</span>
                    <span className={styles.earningsValue}>{statsLoading ? "—" : `₹${Number(stats?.monthEarnings ?? 0).toLocaleString("en-IN")}`}</span>
                  </div>
                  <div className={styles.earningsItem}>
                    <span className={styles.earningsLabel}>Lifetime</span>
                    <span className={`${styles.earningsValue} ${styles.earningsLifetime}`}>{statsLoading ? "—" : `₹${Number(stats?.lifetimeEarnings ?? 0).toLocaleString("en-IN")}`}</span>
                  </div>
                </div>
              </div>
            </div>

            <div className={styles.actions}>
              <button className={styles.actionBtn} onClick={() => navigate("/therapist/profile")}>
                <span className={styles.actionIcon}>🧑‍⚕️</span>
                <div><div className={styles.actionTitle}>My Profile</div><div className={styles.actionSub}>View or create your therapist profile</div></div>
                <span className={styles.actionArrow}>→</span>
              </button>
              <button className={styles.actionBtn} onClick={() => navigate("/therapist/services")}>
                <span className={styles.actionIcon}>📋</span>
                <div><div className={styles.actionTitle}>My Services</div><div className={styles.actionSub}>Manage your therapy offerings</div></div>
                <span className={styles.actionArrow}>→</span>
              </button>
              <button className={styles.actionBtn} onClick={() => navigate("/therapist/availability-rules")}>
                <span className={styles.actionIcon}>📅</span>
                <div><div className={styles.actionTitle}>Availability Rules</div><div className={styles.actionSub}>Set your weekly schedule</div></div>
                <span className={styles.actionArrow}>→</span>
              </button>
              <button className={styles.actionBtn} onClick={() => navigate("/therapist/appointments")}>
                <span className={styles.actionIcon}>🗓️</span>
                <div><div className={styles.actionTitle}>Appointments</div><div className={styles.actionSub}>View calendar and book sessions</div></div>
                <span className={styles.actionArrow}>→</span>
              </button>
              <button className={styles.actionBtn} onClick={() => navigate("/therapist/clients")}>
                <span className={styles.actionIcon}>👥</span>
                <div><div className={styles.actionTitle}>My Clients</div><div className={styles.actionSub}>Browse your client list</div></div>
                <span className={styles.actionArrow}>→</span>
              </button>
              <button className={styles.actionBtn} onClick={() => navigate("/therapist/earnings")}>
                <span className={styles.actionIcon}>📊</span>
                <div><div className={styles.actionTitle}>Earnings Report</div><div className={styles.actionSub}>View and export earnings by date range</div></div>
                <span className={styles.actionArrow}>→</span>
              </button>
            </div>
          </div>

          {/* ── Right column — Upcoming sessions ── */}
          <div className={styles.rightCol}>
            <div className={styles.sessionsPanel}>
              <div className={styles.sessionsPanelHeader}>
                <div>
                  <h2 className={styles.sessionsPanelTitle}>Upcoming Sessions</h2>
                  <p className={styles.sessionsPanelSub}>
                    {apptLoading ? "Loading…" : `${upcoming.length} session${upcoming.length !== 1 ? "s" : ""} scheduled`}
                  </p>
                </div>
                <button className={styles.viewAllBtn} onClick={() => navigate("/therapist/appointments")}>
                  View calendar →
                </button>
              </div>

              <div className={styles.sessionsList}>
                {apptLoading && (
                  <div className={styles.sessionsEmpty}>
                    <div className={styles.spinner} />
                    <p className={styles.loadingText}>Loading…</p>
                  </div>
                )}

                {!apptLoading && apptError && (
                  <div className={styles.errorBox}>
                    <span className={styles.errorIcon}>!</span>{apptError}
                  </div>
                )}

                {!apptLoading && !apptError && upcoming.length === 0 && (
                  <div className={styles.sessionsEmpty}>
                    <span className={styles.emptyIcon}>📭</span>
                    <p className={styles.emptyText}>No upcoming sessions</p>
                  </div>
                )}

                {!apptLoading && !apptError && upcoming.map((a, i) => {
                  const mode = modeMap[a.modeId];
                  const modeIcon = MODE_TYPE_ICON[mode?.modeType] ?? "💬";
                  const modeLabel = mode?.displayName ?? "—";
                  return (
                    <div key={a.appointmentId} className={styles.sessionItem} style={{ animationDelay: `${i * 0.05}s` }}>
                      <div className={styles.sessionTimeBlock}>
                        <span className={styles.sessionDate}>{formatDate(a.startTime)}</span>
                        <span className={styles.sessionTime}>{formatTime(a.startTime)}</span>
                      </div>
                      <div className={styles.sessionDivider} />
                      <div className={styles.sessionInfo}>
                        <div className={styles.sessionClient}>
                          <span className={styles.sessionAvatar}>{a.clientName?.[0]?.toUpperCase() ?? "?"}</span>
                          <span className={styles.sessionClientName}>{a.clientName || "—"}</span>
                        </div>
                        <span className={styles.sessionType}>{modeIcon} {modeLabel}</span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
