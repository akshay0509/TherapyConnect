import { useCallback, useEffect, useRef, useState } from "react";
import { NavLink, useNavigate, useLocation, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { TherapistProfileProvider } from "../context/TherapistProfileContext";
import { useTherapistProfile } from "../context/therapistProfileStore";
import { getClientIntakes } from "../api/clientIntakes";
import { getDashboardStats } from "../api/appointments";
import Icon from "./icons";
import styles from "./TherapistShell.module.css";

// Persistent sidebar + topbar layout for the therapist workspace.
// Pure chrome: it renders navigation and an <Outlet/> for the active page.
// It carries NO page logic — every page keeps its own handlers/state/data.
// (Admin is intentionally NOT linked here — it is a separate area/login.)

const NAV = {
  Workspace: [
    { to: "/therapist-home", label: "Dashboard", icon: "grid", end: true },
    { to: "/therapist/appointments", label: "Schedule", icon: "calendar" },
    { to: "/therapist/clients", label: "Clients", icon: "users" },
      { to: "/therapist/client-intakes", label: "Intakes", icon: "clipboard" },
    { to: "/therapist/services", label: "Services", icon: "clipboard" },
    { to: "/therapist/availability-rules", label: "Availability", icon: "clock" },
  ],
  Insights: [
    { to: "/therapist/earnings", label: "Earnings", icon: "dollar" },
    { to: "/therapist/analytics", label: "Analytics", icon: "bar" },
  ],
};

// Footer nav (matches the prototype's bottom group).
// "My Profile" lives in the Dashboard quick-actions grid, as in the prototype.
const FOOT_NAV = [
  { to: "/account-settings", label: "Settings", icon: "settings" },
];

function initials(name) {
  if (!name) return "T";
  const p = name.trim().split(/\s+/);
  return ((p[0]?.[0] ?? "") + (p.length > 1 ? p[p.length - 1][0] : "")).toUpperCase();
}

// Renders `children` when given (used for routes that are shared with other
// roles, e.g. Account Settings), otherwise the matched child route via Outlet.
// The shell provides the profile and also consumes it, so the consuming part
// lives one level down from the provider.
export default function TherapistShell({ children }) {
  const { user } = useAuth();
  return (
    <TherapistProfileProvider fallbackName={user?.username || user?.name}>
      <TherapistShellInner>{children}</TherapistShellInner>
    </TherapistProfileProvider>
  );
}

function TherapistShellInner({ children }) {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [drawer, setDrawer] = useState(false);
  // Address the therapist by name, not by login username.
  const location = useLocation();
  const { displayName } = useTherapistProfile();
  const name = displayName;

  // Pending intakes badge the Intakes item, the same affordance the admin
  // console uses for dead letters: the count lives where you would act on it.
  const [pendingIntakes, setPendingIntakes] = useState(0);
  const [pendingNotes, setPendingNotes] = useState(0);
  const [bellOpen, setBellOpen] = useState(false);
  const bellRef = useRef(null);

  /*
   * Dismissal is bound to the document rather than to a full-screen scrim.
   *
   * The scrim was `position: fixed; inset: 0`, which should cover the viewport —
   * but the header sets `backdrop-filter`, and that makes the header a
   * containing block for fixed-position descendants. The scrim was therefore
   * clipped to the header strip and only caught clicks in the top few pixels.
   *
   * pointerdown rather than click, so the panel closes on press instead of
   * lingering until release, and so a drag started outside dismisses it too.
   */
  useEffect(() => {
    if (!bellOpen) return undefined;

    const onPointerDown = (event) => {
      if (!bellRef.current?.contains(event.target)) setBellOpen(false);
    };
    const onKeyDown = (event) => {
      if (event.key !== "Escape") return;
      setBellOpen(false);
      // Focus would otherwise be stranded on a panel that no longer exists.
      bellRef.current?.querySelector("button")?.focus();
    };

    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [bellOpen]);

  /*
   * The shell wraps every route and never unmounts, so a mount-only fetch left
   * this count frozen for the whole session — approving ten intakes still showed
   * the number you logged in with, and only signing out fixed it.
   *
   * Refetching on navigation covers moving between pages; the custom event covers
   * acting on the Intakes page itself, where the path does not change. No polling:
   * the count only moves when the therapist does something.
   */
  const refreshSignals = useCallback(() => {
    getClientIntakes("PENDING")
      .then(list => setPendingIntakes(Array.isArray(list) ? list.length : 0))
      .catch(() => {});   // a badge is never worth surfacing an error for
    getDashboardStats()
      .then(stats => setPendingNotes(Number(stats?.pendingNotes ?? 0)))
      .catch(() => {});
  }, []);

  useEffect(() => { refreshSignals(); }, [location.pathname, refreshSignals]);

  useEffect(() => {
    window.addEventListener("therapy:signals-changed", refreshSignals);
    return () => window.removeEventListener("therapy:signals-changed", refreshSignals);
  }, [refreshSignals]);

  /* Only real, actionable things. An indicator that is always lit teaches people
     to ignore it, which is worse than having none. */
  const alerts = [
    pendingIntakes > 0 && {
      id: "intakes",
      text: `${pendingIntakes} submission${pendingIntakes === 1 ? "" : "s"} waiting for review`,
      to: "/therapist/client-intakes",
    },
    pendingNotes > 0 && {
      id: "notes",
      text: `${pendingNotes} completed session${pendingNotes === 1 ? "" : "s"} without notes`,
      to: "/therapist/clients",
    },
  ].filter(Boolean);

  const handleSignOut = () => { logout(); navigate("/login"); };

  // Reuse the existing global command palette (Ctrl/Cmd-K) rather than
  // re-implementing search — dispatch the same shortcut it already listens for.
  const openSearch = () =>
    window.dispatchEvent(new KeyboardEvent("keydown", { key: "k", ctrlKey: true, bubbles: true }));

  const linkClass = ({ isActive }) => `${styles.navItem} ${isActive ? styles.navActive : ""}`;

  return (
    <div className={styles.shell}>
      <div className={styles.mobbar}>
        <button className={styles.burger} onClick={() => setDrawer(d => !d)} aria-label="Menu"><Icon name="grid" size={20} /></button>
        <span className={styles.mobbrand}>TherapyConnect</span>
      </div>

      {drawer && <div className={styles.scrim} onClick={() => setDrawer(false)} />}

      <aside className={`${styles.sidebar} ${drawer ? styles.sidebarOpen : ""}`}>
        <div className={styles.brand}>
          <span className={styles.mark}><Icon name="heart" size={22} strokeWidth={2.1} /></span>
          <span><b>TherapyConnect</b><span className={styles.brandSub}>Therapist workspace</span></span>
        </div>

        {Object.entries(NAV).map(([group, items]) => (
          <div key={group} className={styles.navGroup}>
            <div className={styles.navLabel}>{group}</div>
            <nav>
              {items.map(it => (
                <NavLink key={it.to} to={it.to} end={it.end} className={linkClass} onClick={() => setDrawer(false)}>
                  <Icon name={it.icon} size={20} className={styles.navIcon} />
                  {it.label}
                  {it.to === "/therapist/client-intakes" && pendingIntakes > 0 && (
                    <span className={`chip chip-warn ${styles.navCount}`}>{pendingIntakes}</span>
                  )}
                </NavLink>
              ))}
            </nav>
          </div>
        ))}

        <div className={styles.sideFoot}>
          {FOOT_NAV.map(it => (
            <NavLink key={it.to} to={it.to} className={linkClass} onClick={() => setDrawer(false)}>
              <Icon name={it.icon} size={20} className={styles.navIcon} />
              {it.label}
            </NavLink>
          ))}
          <button className={styles.navItem} onClick={handleSignOut}>
            <Icon name="logout" size={20} className={styles.navIcon} /> Sign out
          </button>
        </div>
      </aside>

      <main className={styles.main}>
        <header className={styles.topbar}>
          <button className={styles.search} onClick={openSearch} aria-label="Search clients and pages">
            <Icon name="search" size={18} />
            <span className={styles.searchText}>Search clients, sessions…</span>
            <kbd className={styles.kbd}>⌘ K</kbd>
          </button>
          <div className={styles.topActions}>
            <div className={styles.bellWrap} ref={bellRef}>
              <button
                className={styles.iconBtn}
                aria-label={alerts.length ? `Notifications, ${alerts.length} item${alerts.length === 1 ? "" : "s"}` : "Notifications, nothing new"}
                aria-expanded={bellOpen}
                onClick={() => setBellOpen(o => !o)}
              >
                <Icon name="bell" size={18} />
                {/* The dot used to be permanent decoration. It now means something. */}
                {alerts.length > 0 && <span className={styles.notif} />}
              </button>
              {bellOpen && (
                <div className={styles.bellPanel} role="dialog" aria-label="Notifications">
                    <div className={styles.bellHead}>Needs your attention</div>
                    {alerts.length === 0 ? (
                      <div className={styles.bellEmpty}>Nothing waiting. You're up to date.</div>
                    ) : (
                      alerts.map(alert => (
                        <button
                          key={alert.id}
                          className={styles.bellItem}
                          onClick={() => { setBellOpen(false); navigate(alert.to); }}
                        >
                          {alert.text}
                        </button>
                      ))
                  )}
                </div>
              )}
            </div>
            <button className={styles.me} onClick={() => navigate("/account-settings")}>
              <span className={styles.avatar}>{initials(name)}</span>
              <span className={styles.meText}><b>{name}</b><span>Therapist</span></span>
            </button>
          </div>
        </header>
        <div className={styles.page}>
          {children ?? <Outlet />}
        </div>
      </main>
    </div>
  );
}
