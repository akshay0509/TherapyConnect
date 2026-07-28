import { useState } from "react";
import { NavLink, useNavigate, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
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
export default function TherapistShell({ children }) {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [drawer, setDrawer] = useState(false);
  const name = user?.username || user?.name || "Therapist";

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
            <button className={styles.iconBtn} aria-label="Notifications"><Icon name="bell" size={18} /><span className={styles.notif} /></button>
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
