import { useEffect, useRef, useState } from "react";
import styles from "./SessionExpiry.module.css";

// The chip surfaces before the blocking dialog does, so the session can be
// extended without being interrupted. AuthContext owns the dialog's threshold.
const CHIP_AT_MS   = 5 * 60 * 1000;
const URGENT_AT_MS = 60 * 1000;

function pad(n) {
  return String(n).padStart(2, "0");
}

function clock(ms) {
  const secs = Math.max(0, Math.round(ms / 1000));
  return `${pad(Math.floor(secs / 60))}:${pad(secs % 60)}`;
}

/**
 * Countdown to access-token expiry, in two escalating forms:
 * a corner chip from five minutes out, then the blocking dialog AuthContext
 * raises at two. Renders nothing while the session has plenty of time left.
 */
export default function SessionExpiry({ expiresAt, showWarning, onExtend, onSignOut }) {
  const [now, setNow] = useState(() => Date.now());
  const [extending, setExtending] = useState(false);
  const extendBtnRef = useRef(null);

  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);

  // pull focus to the safe action once the dialog interrupts
  useEffect(() => {
    if (showWarning) extendBtnRef.current?.focus();
  }, [showWarning]);

  if (!expiresAt) return null;

  const remaining = expiresAt - now;
  if (!showWarning && remaining > CHIP_AT_MS) return null;

  const handleExtend = async () => {
    setExtending(true);
    try {
      await onExtend();
    } finally {
      setExtending(false);
    }
  };

  if (showWarning) {
    return (
      <div className={styles.backdrop}>
        <div className={styles.dialog} role="alertdialog" aria-modal="true" aria-labelledby="session-expiry-title">
          <div className={styles.dialogIcon}>&#x23F1;</div>
          <h2 className={styles.dialogTitle} id="session-expiry-title">Session expiring</h2>
          <p className={styles.dialogBody}>
            You&apos;ll be signed out in <strong className={styles.dialogClock}>{clock(remaining)}</strong>.
            Extend it to carry on where you left off.
          </p>
          <div className={styles.dialogActions}>
            <button type="button" className={styles.ghostBtn} onClick={onSignOut}>Sign out now</button>
            <button type="button" className={styles.primaryBtn} onClick={handleExtend} disabled={extending} ref={extendBtnRef}>
              {extending ? "Extending…" : "Stay signed in"}
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={`${styles.chip} ${remaining <= URGENT_AT_MS ? styles.urgent : ""}`} role="timer">
      <span className={styles.chipDot} />
      <span className={styles.chipText}>
        Session ends in <strong className={styles.chipClock}>{clock(remaining)}</strong>
      </span>
      <button type="button" className={styles.chipBtn} onClick={handleExtend} disabled={extending}>
        {extending ? "…" : "Extend"}
      </button>
    </div>
  );
}
