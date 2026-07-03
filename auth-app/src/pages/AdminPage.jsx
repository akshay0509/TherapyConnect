import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  adminLogout,
  getAnalyticsHealth,
  getAppointmentHealth,
  getLoginAudit,
  getServicesHealth,
  getUsers,
  isAdminLoggedIn,
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

  // Audit log
  const [audit, setAudit] = useState([]);
  const [auditError, setAuditError] = useState("");
  const [auditFilter, setAuditFilter] = useState("all"); // "all" | "success" | "failure"

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

  const refreshAll = useCallback(() => {
    fetchHealth();
    fetchServices();
    fetchUsers();
    fetchAudit();
  }, [fetchHealth, fetchServices, fetchUsers, fetchAudit]);

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
          </div>
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
