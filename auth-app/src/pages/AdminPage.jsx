import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  adminLogout,
  adminResetPassword,
  getAnalyticsHealth,
  getAppointmentHealth,
  getDltMessages,
  getKafkaOverview,
  getLoginAudit,
  getServicesHealth,
  getUsers,
  isAdminLoggedIn,
  replayDlt,
  replayOutbox,
  updateUserStatus,
} from "../api/admin";
import Icon from "../components/icons";
import styles from "./AdminPage.module.css";

function todayMidnight() {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  // Format as local datetime string for the input (YYYY-MM-DDTHH:MM:SS)
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T00:00:00`;
}

function formatDateTime(isoString) {
  if (!isoString) return "—";
  try {
    const d = new Date(isoString);
    return d.toLocaleString();
  } catch {
    return isoString;
  }
}

function timeAgo(isoString) {
  if (!isoString) return "";
  try {
    const diff = Math.floor((Date.now() - new Date(isoString).getTime()) / 60000);
    if (diff < 1) return "< 1 min ago";
    if (diff === 1) return "1 min ago";
    if (diff < 60) return `${diff} mins ago`;
    return `${Math.floor(diff / 60)}h ${diff % 60}m ago`;
  } catch {
    return "";
  }
}

const TABS = [
  { id: "overview", label: "Overview", icon: "grid" },
  { id: "dlq", label: "Dead letters", icon: "alert" },
  { id: "users", label: "Users", icon: "users" },
  { id: "activity", label: "Activity", icon: "activity" },
  { id: "recovery", label: "Recovery", icon: "refresh" },
];

/* Ports are fixed per service and make the health list scannable, exactly as
   the prototype shows them. Keyed by the name the health endpoint reports. */
const SERVICE_PORTS = {
  gateway: ":8091", user: ":5000", therapist: ":5001",
  appointment: ":5002", client: ":5003", notification: ":5004", analytics: ":5005",
};

function titleCase(value) {
  return String(value || "")
    .toLowerCase()
    .split("_")
    .filter(Boolean)
    .map(w => w[0].toUpperCase() + w.slice(1))
    .join(" ");
}

export default function AdminPage() {
  const navigate = useNavigate();

  const [apptHealth, setApptHealth] = useState(null);
  const [analyticsHealth, setAnalyticsHealth] = useState(null);
  const [services, setServices] = useState([]);
  const [fetchError, setFetchError] = useState("");
  const [loading, setLoading] = useState(true);
  const [lastRefreshed, setLastRefreshed] = useState(null);

  const [replayFrom, setReplayFrom] = useState(todayMidnight());
  const [showConfirm, setShowConfirm] = useState(false);
  const [replaying, setReplaying] = useState(false);
  const [replayResult, setReplayResult] = useState(null);

  // User management
  const [users, setUsers] = useState([]);
  const [usersError, setUsersError] = useState("");
  const [userActionId, setUserActionId] = useState(null); // userId currently being updated
  const [pendingAction, setPendingAction] = useState(null); // { user, field, value, label }

  // Force password reset
  const [resetTarget, setResetTarget] = useState(null); // user object or null
  const [resetPw, setResetPw] = useState("");
  const [resetBusy, setResetBusy] = useState(false);
  const [resetError, setResetError] = useState("");

  // Audit log
  const [audit, setAudit] = useState([]);
  const [auditError, setAuditError] = useState("");
  const [auditFilter, setAuditFilter] = useState("all"); // "all" | "success" | "failure"
  const [auditLimit, setAuditLimit] = useState(10);
  const [tab, setTab] = useState("overview");

  // Kafka / DLQ
  const [kafka, setKafka] = useState(null);
  const [kafkaError, setKafkaError] = useState("");
  const [dltView, setDltView] = useState(null); // { topic, loading, messages, error }
  const [dltReplayTarget, setDltReplayTarget] = useState(null); // topic awaiting confirm
  const [dltReplaying, setDltReplaying] = useState(false);
  const [dltResult, setDltResult] = useState(null); // { type, message }

  const handleAuthError = useCallback(
    (err) => {
      if (err.response?.status === 401 || err.response?.status === 403) {
        adminLogout();
        navigate("/admin-login", { replace: true });
        return true;
      }
      return false;
    },
    [navigate]
  );

  const fetchHealth = useCallback(async () => {
    setFetchError("");
    setLoading(true);
    try {
      const [appt, analytics] = await Promise.all([
        getAppointmentHealth(),
        getAnalyticsHealth(),
      ]);
      setApptHealth(appt);
      setAnalyticsHealth(analytics);
      setLastRefreshed(new Date().toLocaleString());

      // Auto-populate replay field with estimated issue time if outbox is stale
      if (appt?.outbox?.estimatedIssueStartedAt) {
        setReplayFrom(appt.outbox.estimatedIssueStartedAt);
      }
    } catch (err) {
      if (handleAuthError(err)) return;
      setFetchError("Failed to fetch system health. Check network or try refreshing.");
    } finally {
      setLoading(false);
    }
  }, [handleAuthError]);

  const fetchServices = useCallback(async () => {
    try {
      const data = await getServicesHealth();
      setServices(data);
    } catch (err) {
      if (handleAuthError(err)) return;
      setServices([]);
    }
  }, [handleAuthError]);

  const fetchUsers = useCallback(async () => {
    setUsersError("");
    try {
      const data = await getUsers();
      setUsers(data);
    } catch (err) {
      if (handleAuthError(err)) return;
      setUsersError("Failed to load users.");
    }
  }, [handleAuthError]);

  const fetchAudit = useCallback(async () => {
    setAuditError("");
    try {
      const data = await getLoginAudit();
      setAudit(data);
    } catch (err) {
      if (handleAuthError(err)) return;
      setAuditError("Failed to load login audit.");
    }
  }, [handleAuthError]);

  const fetchKafka = useCallback(async () => {
    setKafkaError("");
    try {
      const data = await getKafkaOverview();
      setKafka(data);
    } catch (err) {
      if (handleAuthError(err)) return;
      setKafka(null);
      setKafkaError(err.response?.data?.error || "Failed to load Kafka status.");
    }
  }, [handleAuthError]);

  const refreshAll = useCallback(() => {
    fetchHealth();
    fetchServices();
    fetchUsers();
    fetchAudit();
    fetchKafka();
  }, [fetchHealth, fetchServices, fetchUsers, fetchAudit, fetchKafka]);

  useEffect(() => {
    if (!isAdminLoggedIn()) {
      navigate("/admin-login", { replace: true });
      return;
    }
    refreshAll();
  }, [navigate, refreshAll]);

  function handleLogout() {
    adminLogout();
    navigate("/admin-login", { replace: true });
  }

  async function handleReplay() {
    setShowConfirm(false);
    setReplaying(true);
    setReplayResult(null);
    try {
      const result = await replayOutbox(replayFrom);
      setReplayResult({ type: "success", message: result.message });
      // Refresh health after replay
      setTimeout(fetchHealth, 2000);
    } catch (err) {
      const msg = err.response?.data?.error || "Replay failed. See logs.";
      setReplayResult({ type: "failure", message: msg });
    } finally {
      setReplaying(false);
    }
  }

  // ── User status actions ──────────────────────────────────────────
  async function confirmUserAction() {
    const { user, field, value } = pendingAction;
    setPendingAction(null);
    setUserActionId(user.userId);
    setUsersError("");
    try {
      const updated = await updateUserStatus(user.userId, { [field]: value });
      setUsers((prev) => prev.map((u) => (u.userId === updated.userId ? updated : u)));
    } catch (err) {
      if (handleAuthError(err)) return;
      setUsersError(err.response?.data?.error || "Failed to update user.");
    } finally {
      setUserActionId(null);
    }
  }

  function requestUserAction(user, field, value, label) {
    setPendingAction({ user, field, value, label });
  }

  // ── DLQ actions ──────────────────────────────────────────────────
  async function openDltMessages(topic) {
    setDltView({ topic, loading: true, messages: [], error: "" });
    try {
      const messages = await getDltMessages(topic, 20);
      setDltView({ topic, loading: false, messages, error: "" });
    } catch (err) {
      if (handleAuthError(err)) return;
      setDltView({
        topic,
        loading: false,
        messages: [],
        error: err.response?.data?.error || "Failed to load messages.",
      });
    }
  }

  async function confirmDltReplay() {
    const topic = dltReplayTarget;
    setDltReplayTarget(null);
    setDltReplaying(true);
    setDltResult(null);
    try {
      const result = await replayDlt(topic);
      setDltResult({ type: "success", message: `Replayed ${result.replayed} message(s) from ${topic}.` });
      setTimeout(fetchKafka, 2000);
    } catch (err) {
      if (handleAuthError(err)) return;
      setDltResult({ type: "failure", message: err.response?.data?.error || "Replay failed. See logs." });
    } finally {
      setDltReplaying(false);
    }
  }

  function openResetDialog(user) {
    setResetTarget(user);
    setResetPw("");
    setResetError("");
  }

  function generatePassword() {
    const chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%";
    let pw = "";
    const rand = new Uint32Array(12);
    crypto.getRandomValues(rand);
    for (let i = 0; i < 12; i++) pw += chars[rand[i] % chars.length];
    setResetPw(pw);
  }

  async function confirmResetPassword() {
    if (!resetPw || resetPw.length < 8) {
      setResetError("Password must be at least 8 characters.");
      return;
    }
    setResetBusy(true);
    setResetError("");
    try {
      await adminResetPassword(resetTarget.userId, resetPw);
      setResetTarget(null);
    } catch (err) {
      if (handleAuthError(err)) return;
      setResetError(err.response?.data?.error || "Failed to reset password.");
    } finally {
      setResetBusy(false);
    }
  }

  // ── Outbox status rendering ──────────────────────────────────────
  // ── Analytics status rendering ───────────────────────────────────
  // ── Kafka / DLQ card ─────────────────────────────────────────────
  // ── DLQ tables ───────────────────────────────────────────────────
  function renderKafkaSection() {
    const dlts = kafka?.dlts || [];
    const groups = kafka?.groups || [];
    const stableStates = ["Stable", "STABLE"];
    return (
      <div className={`card ${styles.tablePanel}`}>
        {kafkaError && <div className={styles.fetchError}>{kafkaError}</div>}
        {dltResult && (
          <div className={`${styles.replayResult} ${styles[dltResult.type]}`}>{dltResult.message}</div>
        )}
        <table className="data-table">
          <thead>
            <tr>
              <th>DLT Topic</th>
              <th>Pending</th>
              <th>Total</th>
              <th>Last Failure</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {dlts.map((d) => (
              <tr key={d.topic}>
                <td className={styles.cellUsername}>{d.topic}</td>
                <td>
                  {d.pending > 0 ? (
                    <span className="chip chip-warn">{d.pending}</span>
                  ) : (
                    <span className="chip chip-ok">0</span>
                  )}
                </td>
                <td className={styles.cellMuted}>{d.total}</td>
                <td className={styles.cellMuted}>
                  {d.lastMessageAt ? (
                    <>
                      {formatDateTime(d.lastMessageAt)}{" "}
                      <span className={styles.cellFaint}>({timeAgo(d.lastMessageAt)})</span>
                    </>
                  ) : (
                    "—"
                  )}
                </td>
                <td>
                  <div className={styles.rowActions}>
                    <button className="btn btn-sm" onClick={() => openDltMessages(d.topic)}>
                      View
                    </button>
                    <button
                      className={`btn btn-sm ${styles.okBtn}`}
                      disabled={dltReplaying || d.pending === 0}
                      onClick={() => setDltReplayTarget(d.topic)}
                    >
                      {dltReplaying ? "…" : "Replay"}
                    </button>
                  </div>
                </td>
              </tr>
            ))}
            {!dlts.length && (
              <tr>
                <td colSpan={5} className={styles.emptyNote}>
                  No dead-letter topics exist yet — nothing has ever failed processing.
                </td>
              </tr>
            )}
          </tbody>
        </table>
        {/* Same panel header as everywhere else, rather than a bespoke label */}
        <div className="panel-h">
          <div>
            <h2>Consumer groups</h2>
            <p>Every group is expected to be Stable with zero lag</p>
          </div>
        </div>
        <table className="data-table">
          <thead>
            <tr>
              <th>Group</th>
              <th>State</th>
              <th>Lag</th>
              <th>Topics</th>
            </tr>
          </thead>
          <tbody>
            {groups.map((g) => (
              <tr key={g.groupId}>
                <td className={styles.cellUsername}>{g.groupId}</td>
                <td>
                  <span className={`chip ${stableStates.includes(g.state) ? "chip-ok" : "chip-bad"}`}>
                    {g.state}
                  </span>
                </td>
                <td>
                  {g.totalLag > 0 ? (
                    <span className="chip chip-bad">{g.totalLag}</span>
                  ) : (
                    <span className={styles.cellMuted}>0</span>
                  )}
                </td>
                <td className={styles.cellMuted}>
                  {(g.topics || []).map((t) => `${t.topic}${t.lag > 0 ? ` (+${t.lag})` : ""}`).join(", ") || "—"}
                </td>
              </tr>
            ))}
            {!groups.length && (
              <tr>
                <td colSpan={4} className={styles.emptyNote}>No consumer groups found.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    );
  }

  // ── Services grid ────────────────────────────────────────────────
  function renderServicesGrid() {
    if (!services.length) {
      return <div className={styles.emptyNote}>Service status unavailable.</div>;
    }
    return (
      <div className={styles.servicesGrid}>
        {services.map((svc) => {
          const isUp = svc.status === "UP";
          const isDegraded = svc.status === "DEGRADED";
          const dotClass = isUp ? styles.dotOk : isDegraded ? styles.dotWarn : styles.dotError;
          const nameClass = isUp ? "" : styles.serviceNameDown;
          return (
            <span key={svc.name} className={`chip chip-${isUp ? "ok" : isDegraded ? "warn" : "bad"} ${styles.serviceChip}`}>
              <span className={`${styles.statusDot} ${dotClass}`} />
              <span className={`${styles.serviceName} ${nameClass}`}>
                {svc.name.replace("-service", "")}
              </span>
              <span className={styles.serviceStatus}>{titleCase(svc.status)}</span>
            </span>
          );
        })}
      </div>
    );
  }

  // ── Users table ──────────────────────────────────────────────────
  function renderUsersTable() {
    return (
      <div className={`card ${styles.tablePanel}`}>
        {usersError && <div className={styles.fetchError}>{usersError}</div>}
        <table className="data-table">
          <thead>
            <tr>
              <th>Username</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th>Last Login</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.userId}>
                <td className={styles.cellUsername}>{u.username}</td>
                <td className={styles.cellMuted}>{u.email}</td>
                <td>
                  <span className="chip chip-info">{u.userRole ? titleCase(u.userRole) : "—"}</span>
                </td>
                <td>
                  {!u.enabled ? (
                    <span className="chip chip-mut">Disabled</span>
                  ) : u.accountLocked ? (
                    <span className="chip chip-bad">
                      Locked{u.failedAttempts > 0 ? ` · ${u.failedAttempts}` : ""}
                    </span>
                  ) : (
                    <span className="chip chip-ok">Active</span>
                  )}
                </td>
                <td className={styles.cellMuted}>
                  {u.lastLoginTime ? timeAgo(u.lastLoginTime) : "never"}
                </td>
                <td>
                  <div className={styles.rowActions}>
                    {u.enabled ? (
                      <button
                        className={`btn btn-sm ${styles.dangerBtn}`}
                        disabled={userActionId === u.userId}
                        onClick={() => requestUserAction(u, "enabled", false, `Disable account "${u.username}"? The user will not be able to log in.`)}
                      >
                        Disable
                      </button>
                    ) : (
                      <button
                        className={`btn btn-sm ${styles.okBtn}`}
                        disabled={userActionId === u.userId}
                        onClick={() => requestUserAction(u, "enabled", true, `Enable account "${u.username}"?`)}
                      >
                        Enable
                      </button>
                    )}
                    {u.accountLocked ? (
                      <button
                        className={`btn btn-sm ${styles.okBtn}`}
                        disabled={userActionId === u.userId}
                        onClick={() => requestUserAction(u, "locked", false, `Unlock account "${u.username}"? Failed attempts will be reset.`)}
                      >
                        Unlock
                      </button>
                    ) : (
                      <button
                        className="btn btn-sm"
                        disabled={userActionId === u.userId}
                        onClick={() => requestUserAction(u, "locked", true, `Lock account "${u.username}"?`)}
                      >
                        Lock
                      </button>
                    )}
                    <button
                      className="btn btn-sm"
                      disabled={userActionId === u.userId}
                      onClick={() => openResetDialog(u)}
                    >
                      Reset PW
                    </button>
                  </div>
                </td>
              </tr>
            ))}
            {!users.length && (
              <tr>
                <td colSpan={6} className={styles.emptyNote}>No users found.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    );
  }

  // ── Audit table ──────────────────────────────────────────────────
  const filteredAudit = audit.filter((a) =>
    auditFilter === "all" ? true : auditFilter === "success" ? a.success : !a.success
  );
  // 100 rows at ~75px each made the audit log longer than everything else on
  // the page combined. Show a screenful, reveal the rest on request.
  const visibleAudit = filteredAudit.slice(0, auditLimit);

  function renderAuditTable() {
    return (
      <div className={`card ${styles.tablePanel}`}>
        {auditError && <div className={styles.fetchError}>{auditError}</div>}
        <div className={styles.auditFilters}>
          {["all", "success", "failure"].map((f) => (
            <button
              key={f}
              className={`${styles.filterBtn} ${auditFilter === f ? styles.filterBtnActive : ""}`}
              onClick={() => setAuditFilter(f)}
            >
              {f === "all" ? "All" : f === "success" ? "Success" : "Failed"}
            </button>
          ))}
          <span className={styles.auditCount}>
            {filteredAudit.length} of last {audit.length} events
          </span>
        </div>
        <table className="data-table">
          <thead>
            <tr>
              <th>Time</th>
              <th>Username</th>
              <th>Result</th>
              <th>IP Address</th>
              <th>Reason</th>
            </tr>
          </thead>
          <tbody>
            {visibleAudit.map((a) => (
              <tr key={a.id}>
                {/* Relative time leads — it is what you scan an audit log for.
                    The absolute stamp sits under it rather than wrapping mid-phrase. */}
                <td className={styles.cellMuted}>
                  <span className={styles.auditAgo}>{timeAgo(a.loginAt)}</span>
                  <span className={styles.cellFaint}>{formatDateTime(a.loginAt)}</span>
                </td>
                <td className={styles.cellUsername}>{a.username || "—"}</td>
                <td>
                  {a.success ? (
                    <span className="chip chip-ok">Success</span>
                  ) : (
                    <span className="chip chip-bad">Failed</span>
                  )}
                </td>
                <td className={styles.cellMuted}>{a.ipAddress || "—"}</td>
                <td className={styles.cellMuted}>{a.failureReason || "—"}</td>
              </tr>
            ))}
            {!filteredAudit.length && (
              <tr>
                <td colSpan={5} className={styles.emptyNote}>No login events.</td>
              </tr>
            )}
            {filteredAudit.length > visibleAudit.length && (
              <tr>
                <td colSpan={5} className={styles.showMoreCell}>
                  <button className="btn btn-sm" onClick={() => setAuditLimit(l => l + 20)}>
                    Show 20 more · {filteredAudit.length - visibleAudit.length} remaining
                  </button>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    );
  }

  const portainerUrl = "http://therapyconnect.duckdns.org:9000";

  // ── Console summary ──────────────────────────────────────────────
  const servicesUp = services.filter(s => s.status === "UP").length;
  const totalLag = (kafka?.groups || []).reduce((sum, g) => sum + (g.totalLag || 0), 0);
  // Field names match renderKafkaCard: kafka.totalPending / kafka.dlts.
  const dltPending = kafka?.totalPending ?? 0;
  const outboxPending = apptHealth?.outbox?.pendingCount ?? 0;
  const servicesDown = services.filter(s => s.status && s.status !== "UP");

  /* Only genuine problems. Anything listed here is something an operator has to
     act on, which is why each entry jumps to the tab that fixes it. */
  const attention = [
    dltPending > 0 && {
      label: `${dltPending} dead-lettered event${dltPending === 1 ? "" : "s"}`,
      detail: "Unhandled consumer failures — review and replay",
      tab: "dlq",
    },
    outboxPending > 0 && {
      label: `${outboxPending} outbox event${outboxPending === 1 ? "" : "s"} pending`,
      detail: "Publisher may be stalled",
      tab: "recovery",
    },
    servicesDown.length > 0 && {
      label: `${servicesDown.length} service${servicesDown.length === 1 ? "" : "s"} not up`,
      detail: servicesDown.map(s => s.name).join(", "),
      tab: "recovery",
    },
  ].filter(Boolean);

  return (
    <div className={styles.page}>
      {/* A slim brand bar only — the page opening now lives in .page-head,
          matching every other page rather than a console-only chrome. */}
      {/* Sidebar — the prototype puts the admin console inside the app shell,
          so it gets a rail like every other view. Its own nav, because this is
          a separate auth realm and must not offer therapist routes. */}
      <aside className={styles.sidebar}>
        <div className={styles.brand}>
          <span className={styles.mark}><Icon name="shield" size={19} strokeWidth={2} /></span>
          <div>
            <b>TherapyConnect</b>
            <span className={styles.brandSub}>Admin console</span>
          </div>
        </div>
        <div className={styles.navLabel}>Operations</div>
        <nav className={styles.nav}>
          {TABS.map(t => (
            <button
              key={t.id}
              className={`${styles.navItem} ${tab === t.id ? styles.navActive : ""}`}
              onClick={() => setTab(t.id)}
              aria-current={tab === t.id ? "page" : undefined}
            >
              <Icon name={t.icon} size={17} className={styles.navIcon} />
              {t.label}
              {t.id === "dlq" && dltPending > 0 && (
                <span className={`chip chip-bad ${styles.navCount}`}>{dltPending}</span>
              )}
            </button>
          ))}
        </nav>
        <div className={styles.sideFoot}>
          <span className={`chip chip-warn ${styles.restrictedChip}`}>Restricted</span>
          <button className={`${styles.navItem} ${styles.logoutBtn}`} onClick={handleLogout}>
            <Icon name="logout" size={17} className={styles.navIcon} /> Sign out
          </button>
        </div>
      </aside>

      

      {/* Content */}
      <div className={styles.main}>
      <div className={styles.content}>
        {fetchError && <div className={styles.fetchError}>{fetchError}</div>}

        {/* The same page opening every other page in the app uses */}
        <div className="page-head">
          <div>
            <div className="eyebrow">System</div>
            <h1>Admin <span className="g">console</span></h1>
            <div className="sub">
              {lastRefreshed ? `Last refreshed ${lastRefreshed}` : "Service health, users and event pipeline"}
            </div>
          </div>
          <div className="head-actions">
            <span className={`chip ${attention.length ? "chip-warn" : "chip-ok"}`}>
              <Icon name={attention.length ? "alert" : "check"} size={13} />
              {attention.length ? "Needs attention" : "All systems operational"}
            </span>
            <button className="btn" onClick={refreshAll} disabled={loading}>
              {loading ? "Loading…" : <><Icon name="refresh" size={16} /> Refresh</>}
            </button>
          </div>
        </div>

        {/* ── At a glance ─────────────────────────────────────────────────
            The prototype's KPI tile: icon chip, big number, label. Four
            numbers an operator actually checks. */}
        <div className={`${styles.kpiRow} reveal d1`}>
          <div className="card kpi">
            <div className="kpi-top">
              <span className={`kpi-ic ${servicesUp === services.length && services.length ? "ic-g" : "ic-a"}`}>
                <Icon name="server" size={20} />
              </span>
              <span className={`kpi-trend ${servicesUp === services.length && services.length ? "up" : "flat"}`}>
                {services.length ? (servicesUp === services.length ? "all up" : "degraded") : "—"}
              </span>
            </div>
            <div className="kpi-val">{services.length ? `${servicesUp}/${services.length}` : "—"}</div>
            <div className="kpi-lbl">Services healthy</div>
          </div>

          <div className="card kpi">
            <div className="kpi-top">
              <span className={`kpi-ic ${dltPending > 0 ? "ic-a" : "ic-g"}`}>
                <Icon name="alert" size={20} />
              </span>
              <span className="kpi-trend flat">{(kafka?.dlts || []).length} topic(s)</span>
            </div>
            <div className="kpi-val">{dltPending}</div>
            <div className="kpi-lbl">Dead-lettered events</div>
          </div>

          <div className="card kpi">
            <div className="kpi-top">
              <span className={`kpi-ic ${outboxPending > 0 ? "ic-a" : "ic-g"}`}>
                <Icon name="refresh" size={20} />
              </span>
              <span className="kpi-trend flat">
                {analyticsHealth?.lastProcessedDate ? `to ${analyticsHealth.lastProcessedDate}` : "outbox"}
              </span>
            </div>
            <div className="kpi-val">{outboxPending}</div>
            <div className="kpi-lbl">Events pending publish</div>
          </div>

          <div className="card kpi">
            <div className="kpi-top">
              <span className="kpi-ic ic-c"><Icon name="users" size={20} /></span>
              <span className="kpi-trend flat">{(kafka?.groups || []).length} groups</span>
            </div>
            <div className="kpi-val">{users.length}</div>
            <div className="kpi-lbl">User accounts</div>
          </div>
        </div>

        {/* ── Attention band ──────────────────────────────────────────────
            An ops console should lead with what needs doing. When everything
            is healthy this collapses to a single quiet line, so a red band is
            always meaningful. */}
        {attention.length > 0 ? (
          <div className={styles.attention}>
            <span className={styles.attentionIcon}><Icon name="alert" size={16} /></span>
            <div className={styles.attentionList}>
              {attention.map(a => (
                <button key={a.label} className={styles.attentionItem} onClick={() => setTab(a.tab)}>
                  <strong>{a.label}</strong>
                  <span>{a.detail}</span>
                </button>
              ))}
            </div>
          </div>
        ) : (
          <div className={styles.allClear}>
            <Icon name="check" size={15} /> All systems nominal — no action required
          </div>
        )}

        {/* ── Overview: the prototype's two-column layout ───────────────── */}
        {tab === "overview" && (
          <div className="grid-2 reveal d3">
            <div className="card">
              <div className="panel-h">
                <div>
                  <h2>Service health</h2>
                  <p>Live via Eureka + actuator</p>
                </div>
                <span className={`chip ${servicesUp === services.length && services.length ? "chip-ok" : "chip-warn"}`}>
                  {services.length ? `${servicesUp} of ${services.length} up` : "unknown"}
                </span>
              </div>
              <div className="table-wrap">
                <table className="data-table">
                  <tbody>
                    {services.map(svc => {
                      const up = svc.status === "UP";
                      const key = String(svc.name || "").toLowerCase().replace(/[-_]?service$/, "");
                      return (
                        <tr key={svc.name}>
                          <td><b className={styles.svcName}>{titleCase(String(svc.name || "").replace(/[-_]?service$/i, ""))}</b></td>
                          <td className={styles.svcPort}>{SERVICE_PORTS[key] ?? "—"}</td>
                          <td style={{ textAlign: "right" }}>
                            <span className={`chip ${up ? "chip-ok" : "chip-bad"}`}>{svc.status || "UNKNOWN"}</span>
                          </td>
                        </tr>
                      );
                    })}
                    {!services.length && (
                      <tr><td className={styles.emptyNote}>Service status unavailable.</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            <div className={styles.sideStack}>
              <div className="card" style={{ padding: 22 }}>
                <div className={styles.miniHead}>
                  <h2>Event pipeline</h2>
                  <span className={`chip ${dltPending > 0 ? "chip-bad" : "chip-ok"}`}>
                    {dltPending > 0 ? "Attention" : "Stable"}
                  </span>
                </div>
                <p className={styles.miniSub}>Dead-letter queues &amp; consumer lag</p>
                <div className={styles.miniList}>
                  {(kafka?.dlts || []).map(d => (
                    <div key={d.topic} className={styles.miniRow}>
                      <span>{d.topic}</span>
                      <b className={(d.pending ?? 0) > 0 ? styles.numBad : styles.numOk}>{d.pending ?? 0}</b>
                    </div>
                  ))}
                  <div className={styles.miniRow}>
                    <span>Consumer lag</span>
                    <b className={totalLag > 0 ? styles.numBad : styles.numOk}>{totalLag}</b>
                  </div>
                  <div className={styles.miniRow}>
                    <span>Outbox pending</span>
                    <b className={outboxPending > 0 ? styles.numBad : styles.numOk}>{outboxPending}</b>
                  </div>
                </div>
              </div>

              <div className="card" style={{ padding: 22 }}>
                <div className={styles.miniHead}>
                  <h2>Recent logins</h2>
                  <button className="link" onClick={() => setTab("activity")}>
                    View all <Icon name="chevron" size={14} />
                  </button>
                </div>
                <div className={styles.miniList} style={{ marginTop: 12 }}>
                  {audit.slice(0, 5).map(a => (
                    <div key={a.id} className={styles.miniRow}>
                      <span>
                        {a.username || "—"} <span className={styles.cellFaint}>· {timeAgo(a.loginAt)}</span>
                      </span>
                      <span className={`chip ${a.success ? "chip-ok" : "chip-bad"}`}>
                        {a.success ? "Success" : "Failed"}
                      </span>
                    </div>
                  ))}
                  {!audit.length && <div className={styles.emptyNote}>No login events.</div>}
                </div>
              </div>
            </div>
          </div>
        )}

        {tab === "dlq" && (
          <div className="card reveal d5">
            <div className="panel-h">
              <div>
                <h2>Dead letter queues</h2>
                <p>Events a consumer could not process — review the payload, then replay</p>
              </div>
            </div>
            {renderKafkaSection()}
          </div>
        )}
        {tab === "users" && (
          <div className="card reveal d5">
            <div className="panel-h">
              <div>
                <h2>User accounts</h2>
                <p>Enable, lock or force a password reset</p>
              </div>
              <span className="chip chip-mut">{users.length} total</span>
            </div>
            {renderUsersTable()}
          </div>
        )}
        {tab === "activity" && (
          <div className="card reveal d5">
            <div className="panel-h">
              <div>
                <h2>Login activity</h2>
                <p>Most recent authentication attempts across the platform</p>
              </div>
            </div>
            {renderAuditTable()}
          </div>
        )}

        {/* Recovery actions */}
        {tab === "recovery" && (
        <div className={styles.section}>
          <div className={styles.recoveryPanel}>
            <p className={styles.recoveryDesc}>
              If Kafka went down, events may be marked as published but were never consumed.
              Use this to reset events back to "unpublished" — the outbox scheduler will
              re-send them to Kafka within seconds. The field is auto-populated with the
              estimated issue start time when the outbox is stale.
            </p>
            <div className={styles.recoveryRow}>
              <span className={styles.recoveryLabel}>Replay from:</span>
              <input
                className={styles.datetimeInput}
                type="text"
                placeholder="YYYY-MM-DDTHH:MM:SS"
                value={replayFrom}
                onChange={(e) => setReplayFrom(e.target.value)}
              />
              <button
                className={styles.replayBtn}
                onClick={() => setShowConfirm(true)}
                disabled={replaying || !replayFrom}
              >
                {replaying ? "Replaying…" : "Replay Events"}
              </button>
            </div>
            {replayResult && (
              <div className={`${styles.replayResult} ${styles[replayResult.type]}`}>
                {replayResult.message}
              </div>
            )}
          </div>

          {/* Container management belongs with the other recovery levers */}
          <div className={styles.portainerPanel}>
            <div className={styles.portainerText}>
              <h3><Icon name="server" size={17} /> Portainer</h3>
              <p>Start, stop, and restart Docker containers from your browser</p>
            </div>
            <a
              href={portainerUrl}
              target="_blank"
              rel="noopener noreferrer"
              className={`btn btn-primary ${styles.portainerBtn}`}
            >
              Open Portainer <Icon name="chevron" size={15} />
            </a>
          </div>
        </div>
        )}
      </div>
      </div>

      {/* Confirm replay dialog */}
      {showConfirm && (
        <div className={styles.overlay}>
          <div className={styles.dialog}>
            <h3>Confirm Event Replay</h3>
            <p>
              This will reset all outbox events from{" "}
              <strong>{replayFrom}</strong> onwards back to "unpublished".
              The scheduler will re-send them to Kafka within seconds.
              <br /><br />
              Events already consumed by analytics/notifications will be
              processed again — this is intentional during recovery.
            </p>
            <div className={styles.dialogActions}>
              <button className={styles.cancelBtn} onClick={() => setShowConfirm(false)}>
                Cancel
              </button>
              <button className={styles.confirmBtn} onClick={handleReplay}>
                Yes, Replay
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Force password reset dialog */}
      {resetTarget && (
        <div className={styles.overlay}>
          <div className={styles.dialog}>
            <h3>Reset Password</h3>
            <p>
              Set a new password for <strong>{resetTarget.username}</strong>.
              Their current password stops working immediately — share the new
              one with them through a secure channel.
            </p>
            <div className={styles.resetRow}>
              <input
                className={styles.datetimeInput}
                type="text"
                placeholder="new password (min 8 chars)"
                value={resetPw}
                onChange={(e) => setResetPw(e.target.value)}
                autoFocus
              />
              <button type="button" className="btn btn-sm" onClick={generatePassword}>
                Generate
              </button>
            </div>
            {resetError && <div className={styles.fetchError}>{resetError}</div>}
            <div className={styles.dialogActions}>
              <button className={styles.cancelBtn} onClick={() => setResetTarget(null)} disabled={resetBusy}>
                Cancel
              </button>
              <button className={styles.confirmBtn} onClick={confirmResetPassword} disabled={resetBusy}>
                {resetBusy ? "Saving…" : "Reset Password"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* DLT messages viewer */}
      {dltView && (
        <div className={styles.overlay}>
          <div className={`${styles.dialog} ${styles.dialogWide}`}>
            <h3>Dead Letters · {dltView.topic}</h3>
            {dltView.loading && <p>Loading…</p>}
            {dltView.error && <div className={styles.fetchError}>{dltView.error}</div>}
            {!dltView.loading && !dltView.error && !dltView.messages.length && (
              <p>No pending messages — everything on this DLT has been replayed.</p>
            )}
            <div className={styles.dltMsgList}>
              {dltView.messages.map((m) => (
                <div key={`${m.partition}-${m.offset}`} className={styles.dltMsg}>
                  <div className={styles.dltMsgMeta}>
                    {formatDateTime(m.timestamp)} · partition {m.partition} · offset {m.offset}
                    {m.key ? ` · key ${m.key}` : ""}
                  </div>
                  {m.exceptionMessage && <div className={styles.dltMsgError}>{m.exceptionMessage}</div>}
                  <pre className={styles.dltPayload}>{m.payload}</pre>
                </div>
              ))}
            </div>
            <div className={styles.dialogActions}>
              <button className={styles.cancelBtn} onClick={() => setDltView(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Confirm DLT replay dialog */}
      {dltReplayTarget && (
        <div className={styles.overlay}>
          <div className={styles.dialog}>
            <h3>Confirm DLT Replay</h3>
            <p>
              Re-publish all pending messages from <strong>{dltReplayTarget}</strong> back to
              the original topic. Consumers are idempotent — events that already succeeded
              are skipped on redelivery.
            </p>
            <div className={styles.dialogActions}>
              <button className={styles.cancelBtn} onClick={() => setDltReplayTarget(null)}>
                Cancel
              </button>
              <button className={styles.confirmBtn} onClick={confirmDltReplay}>
                Yes, Replay
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Confirm user action dialog */}
      {pendingAction && (
        <div className={styles.overlay}>
          <div className={styles.dialog}>
            <h3>Confirm Action</h3>
            <p>{pendingAction.label}</p>
            <div className={styles.dialogActions}>
              <button className={styles.cancelBtn} onClick={() => setPendingAction(null)}>
                Cancel
              </button>
              <button className={styles.confirmBtn} onClick={confirmUserAction}>
                Confirm
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
