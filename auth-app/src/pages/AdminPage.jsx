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
  function renderOutboxCard() {
    if (!apptHealth) {
      return (
        <div className={`${styles.card} ${styles.cardNeutral}`}>
          <div className={styles.cardLabel}>Outbox / Kafka Producer</div>
          <div className={`${styles.statusBadge} ${styles.neutral}`}>
            <span className={`${styles.statusDot} ${styles.dotNeutral}`} />
            No data
          </div>
        </div>
      );
    }

    const { outbox } = apptHealth;
    const isStale = outbox?.status === "STALE";
    const cardClass = isStale ? styles.cardError : styles.cardOk;
    const badgeClass = isStale ? styles.error : styles.ok;
    const dotClass = isStale ? styles.dotError : styles.dotOk;

    return (
      <div className={`${styles.card} ${cardClass}`}>
        <div className={styles.cardLabel}>Outbox / Kafka Producer</div>
        <div className={`${styles.statusBadge} ${badgeClass}`}>
          <span className={`${styles.statusDot} ${dotClass}`} />
          {isStale ? "STALE — Events stuck" : "HEALTHY"}
        </div>
        <div className={styles.cardDetail}>
          <strong>{outbox?.pendingCount ?? 0}</strong> event(s) pending
          {outbox?.oldestPendingAt && (
            <>
              <br />
              Oldest: {formatDateTime(outbox.oldestPendingAt)}{" "}
              <span style={{ color: "#94a3b8" }}>({timeAgo(outbox.oldestPendingAt)})</span>
            </>
          )}
          {isStale && outbox?.estimatedIssueStartedAt && (
            <>
              <br />
              <strong style={{ color: "#dc2626" }}>
                Issue started: {formatDateTime(outbox.estimatedIssueStartedAt)}
              </strong>
            </>
          )}
        </div>
      </div>
    );
  }

  // ── Analytics status rendering ───────────────────────────────────
  function renderAnalyticsCard() {
    if (!analyticsHealth) {
      return (
        <div className={`${styles.card} ${styles.cardNeutral}`}>
          <div className={styles.cardLabel}>Analytics Consumer</div>
          <div className={`${styles.statusBadge} ${styles.neutral}`}>
            <span className={`${styles.statusDot} ${styles.dotNeutral}`} />
            No data
          </div>
        </div>
      );
    }

    const { status, lastProcessedDate, daysBehind } = analyticsHealth;
    const isOk = status === "OK";
    const isNoData = status === "NO_DATA";
    const cardClass = isOk ? styles.cardOk : isNoData ? styles.cardNeutral : styles.cardWarn;
    const badgeClass = isOk ? styles.ok : isNoData ? styles.neutral : styles.warn;
    const dotClass = isOk ? styles.dotOk : isNoData ? styles.dotNeutral : styles.dotWarn;

    const label =
      isNoData ? "NO DATA YET" :
      isOk ? "UP TO DATE" :
      `BEHIND BY ${daysBehind} DAY${daysBehind !== 1 ? "S" : ""}`;

    return (
      <div className={`${styles.card} ${cardClass}`}>
        <div className={styles.cardLabel}>Analytics Consumer</div>
        <div className={`${styles.statusBadge} ${badgeClass}`}>
          <span className={`${styles.statusDot} ${dotClass}`} />
          {label}
        </div>
        <div className={styles.cardDetail}>
          {lastProcessedDate ? (
            <>
              Last processed: <strong>{lastProcessedDate}</strong>
              {!isOk && (
                <><br />Events after this date need replay</>
              )}
            </>
          ) : (
            "No events processed yet"
          )}
        </div>
      </div>
    );
  }

  // ── Kafka / DLQ card ─────────────────────────────────────────────
  function renderKafkaCard() {
    if (!kafka) {
      return (
        <div className={`${styles.card} ${styles.cardNeutral}`}>
          <div className={styles.cardLabel}>Kafka / Dead Letters</div>
          <div className={`${styles.statusBadge} ${styles.neutral}`}>
            <span className={`${styles.statusDot} ${styles.dotNeutral}`} />
            {kafkaError ? "UNREACHABLE" : "No data"}
          </div>
          {kafkaError && <div className={styles.cardDetail}>{kafkaError}</div>}
        </div>
      );
    }

    const pending = kafka.totalPending ?? 0;
    const totalLag = (kafka.groups || []).reduce((sum, g) => sum + (g.totalLag || 0), 0);
    const hasDead = pending > 0;
    const hasLag = totalLag > 0;
    const cardClass = hasDead ? styles.cardError : hasLag ? styles.cardWarn : styles.cardOk;
    const badgeClass = hasDead ? styles.error : hasLag ? styles.warn : styles.ok;
    const dotClass = hasDead ? styles.dotError : hasLag ? styles.dotWarn : styles.dotOk;
    const label = hasDead
      ? `${pending} DEAD-LETTERED`
      : hasLag
        ? `LAG: ${totalLag} EVENT${totalLag !== 1 ? "S" : ""}`
        : "ALL CLEAR";

    return (
      <div className={`${styles.card} ${cardClass}`}>
        <div className={styles.cardLabel}>Kafka / Dead Letters</div>
        <div className={`${styles.statusBadge} ${badgeClass}`}>
          <span className={`${styles.statusDot} ${dotClass}`} />
          {label}
        </div>
        <div className={styles.cardDetail}>
          {(kafka.dlts || []).length} DLT topic(s) · {(kafka.groups || []).length} consumer group(s)
          {hasDead && (
            <>
              <br />
              <strong style={{ color: "#dc2626" }}>Unhandled failures — see Dead Letter Queues below</strong>
            </>
          )}
        </div>
      </div>
    );
  }

  // ── DLQ tables ───────────────────────────────────────────────────
  function renderKafkaSection() {
    const dlts = kafka?.dlts || [];
    const groups = kafka?.groups || [];
    const stableStates = ["Stable", "STABLE"];
    return (
      <div className={styles.tablePanel}>
        {kafkaError && <div className={styles.fetchError}>{kafkaError}</div>}
        {dltResult && (
          <div className={`${styles.replayResult} ${styles[dltResult.type]}`}>{dltResult.message}</div>
        )}
        <table className={styles.table}>
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
                    <span className={`${styles.pill} ${styles.pillDisabled}`}>{d.pending}</span>
                  ) : (
                    <span className={`${styles.pill} ${styles.pillActive}`}>0</span>
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
                    <button className={styles.miniBtn} onClick={() => openDltMessages(d.topic)}>
                      View
                    </button>
                    <button
                      className={`${styles.miniBtn} ${styles.miniBtnOk}`}
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
        <div className={styles.groupsSubTitle}>Consumer Groups</div>
        <table className={styles.table}>
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
                  <span className={`${styles.pill} ${stableStates.includes(g.state) ? styles.pillActive : styles.pillLocked}`}>
                    {g.state}
                  </span>
                </td>
                <td>
                  {g.totalLag > 0 ? (
                    <span className={`${styles.pill} ${styles.pillLocked}`}>{g.totalLag}</span>
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
            <div key={svc.name} className={styles.serviceChip}>
              <span className={`${styles.statusDot} ${dotClass}`} />
              <span className={`${styles.serviceName} ${nameClass}`}>
                {svc.name.replace("-service", "")}
              </span>
              <span className={styles.serviceStatus}>{svc.status}</span>
            </div>
          );
        })}
      </div>
    );
  }

  // ── Users table ──────────────────────────────────────────────────
  function renderUsersTable() {
    return (
      <div className={styles.tablePanel}>
        {usersError && <div className={styles.fetchError}>{usersError}</div>}
        <table className={styles.table}>
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
                  <span className={styles.roleBadge}>{u.userRole || "—"}</span>
                </td>
                <td>
                  {!u.enabled ? (
                    <span className={`${styles.pill} ${styles.pillDisabled}`}>DISABLED</span>
                  ) : u.accountLocked ? (
                    <span className={`${styles.pill} ${styles.pillLocked}`}>
                      LOCKED{u.failedAttempts > 0 ? ` (${u.failedAttempts})` : ""}
                    </span>
                  ) : (
                    <span className={`${styles.pill} ${styles.pillActive}`}>ACTIVE</span>
                  )}
                </td>
                <td className={styles.cellMuted}>
                  {u.lastLoginTime ? timeAgo(u.lastLoginTime) : "never"}
                </td>
                <td>
                  <div className={styles.rowActions}>
                    {u.enabled ? (
                      <button
                        className={`${styles.miniBtn} ${styles.miniBtnDanger}`}
                        disabled={userActionId === u.userId}
                        onClick={() => requestUserAction(u, "enabled", false, `Disable account "${u.username}"? The user will not be able to log in.`)}
                      >
                        Disable
                      </button>
                    ) : (
                      <button
                        className={`${styles.miniBtn} ${styles.miniBtnOk}`}
                        disabled={userActionId === u.userId}
                        onClick={() => requestUserAction(u, "enabled", true, `Enable account "${u.username}"?`)}
                      >
                        Enable
                      </button>
                    )}
                    {u.accountLocked ? (
                      <button
                        className={`${styles.miniBtn} ${styles.miniBtnOk}`}
                        disabled={userActionId === u.userId}
                        onClick={() => requestUserAction(u, "locked", false, `Unlock account "${u.username}"? Failed attempts will be reset.`)}
                      >
                        Unlock
                      </button>
                    ) : (
                      <button
                        className={styles.miniBtn}
                        disabled={userActionId === u.userId}
                        onClick={() => requestUserAction(u, "locked", true, `Lock account "${u.username}"?`)}
                      >
                        Lock
                      </button>
                    )}
                    <button
                      className={styles.miniBtn}
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

  function renderAuditTable() {
    return (
      <div className={styles.tablePanel}>
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
        <table className={styles.table}>
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
            {filteredAudit.map((a) => (
              <tr key={a.id}>
                <td className={styles.cellMuted}>
                  {formatDateTime(a.loginAt)}{" "}
                  <span className={styles.cellFaint}>({timeAgo(a.loginAt)})</span>
                </td>
                <td className={styles.cellUsername}>{a.username || "—"}</td>
                <td>
                  {a.success ? (
                    <span className={`${styles.pill} ${styles.pillActive}`}>SUCCESS</span>
                  ) : (
                    <span className={`${styles.pill} ${styles.pillDisabled}`}>FAILED</span>
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
          </tbody>
        </table>
      </div>
    );
  }

  const portainerUrl = "http://therapyconnect.duckdns.org:9000";

  return (
    <div className={styles.page}>
      {/* Header */}
      <div className={styles.header}>
        <span className={styles.headerIcon}>🛡</span>
        <span className={styles.headerTitle}>TherapyConnect Admin</span>
        <div className={styles.headerActions}>
          <button
            className={styles.refreshBtn}
            onClick={refreshAll}
            disabled={loading}
          >
            {loading ? "Loading…" : "↻ Refresh"}
          </button>
          <button className={styles.logoutBtn} onClick={handleLogout}>
            Logout
          </button>
        </div>
      </div>

      {/* Meta bar */}
      {lastRefreshed && (
        <div className={styles.metaBar}>Last refreshed: {lastRefreshed}</div>
      )}

      {/* Content */}
      <div className={styles.content}>
        {fetchError && <div className={styles.fetchError}>{fetchError}</div>}

        {/* Services */}
        <div className={styles.section}>
          <div className={styles.sectionTitle}>Services</div>
          {renderServicesGrid()}
        </div>

        {/* System status */}
        <div className={styles.section}>
          <div className={styles.sectionTitle}>System Status</div>
          <div className={styles.cardsRow}>
            {renderOutboxCard()}
            {renderAnalyticsCard()}
            {renderKafkaCard()}
          </div>
        </div>

        {/* Kafka / DLQ */}
        <div className={styles.section}>
          <div className={styles.sectionTitle}>Dead Letter Queues</div>
          {renderKafkaSection()}
        </div>

        {/* User management */}
        <div className={styles.section}>
          <div className={styles.sectionTitle}>Users</div>
          {renderUsersTable()}
        </div>

        {/* Login audit */}
        <div className={styles.section}>
          <div className={styles.sectionTitle}>Login Activity</div>
          {renderAuditTable()}
        </div>

        {/* Recovery actions */}
        <div className={styles.section}>
          <div className={styles.sectionTitle}>Recovery Actions</div>
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
        </div>

        {/* Portainer */}
        <div className={styles.section}>
          <div className={styles.sectionTitle}>Container Management</div>
          <div className={styles.portainerPanel}>
            <div className={styles.portainerText}>
              <h3>🐳 Portainer</h3>
              <p>Start, stop, and restart Docker containers from your browser</p>
            </div>
            <a
              href={portainerUrl}
              target="_blank"
              rel="noopener noreferrer"
              className={styles.portainerBtn}
            >
              Open Portainer →
            </a>
          </div>
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
              <button type="button" className={styles.miniBtn} onClick={generatePassword}>
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
