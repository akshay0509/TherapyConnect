import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  adminLogout,
  getAnalyticsHealth,
  getAppointmentHealth,
  isAdminLoggedIn,
  replayOutbox,
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
  const [fetchError, setFetchError] = useState("");
  const [loading, setLoading] = useState(true);
  const [lastRefreshed, setLastRefreshed] = useState(null);

  const [replayFrom, setReplayFrom] = useState(todayMidnight());
  const [showConfirm, setShowConfirm] = useState(false);
  const [replaying, setReplaying] = useState(false);
  const [replayResult, setReplayResult] = useState(null);

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
      if (err.response?.status === 401 || err.response?.status === 403) {
        adminLogout();
        navigate("/admin-login", { replace: true });
        return;
      }
      setFetchError("Failed to fetch system health. Check network or try refreshing.");
    } finally {
      setLoading(false);
    }
  }, [navigate]);

  useEffect(() => {
    if (!isAdminLoggedIn()) {
      navigate("/admin-login", { replace: true });
      return;
    }
    fetchHealth();
  }, [navigate, fetchHealth]);

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

  const portainerUrl = `http://${window.location.hostname}:9000`;

  return (
    <div className={styles.page}>
      {/* Header */}
      <div className={styles.header}>
        <span className={styles.headerIcon}>🛡</span>
        <span className={styles.headerTitle}>TherapyConnect Admin</span>
        <div className={styles.headerActions}>
          <button
            className={styles.refreshBtn}
            onClick={fetchHealth}
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

        {/* System status */}
        <div className={styles.section}>
          <div className={styles.sectionTitle}>System Status</div>
          <div className={styles.cardsRow}>
            {renderOutboxCard()}
            {renderAnalyticsCard()}
          </div>
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
    </div>
  );
}
