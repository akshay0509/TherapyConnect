import { useEffect, useMemo, useState } from "react";
import { useModeMap } from "../context/DeliveryModesContext";
import styles from "./SessionTimer.module.css";

// only sessions that can still be sat in are worth counting down
const ACTIVE_STATUSES = ["SCHEDULED", "CONFIRMED", "RESCHEDULED"];

const LEAD_MS  = 15 * 60 * 1000; // card appears this long before the start
const GRACE_MS = 30 * 60 * 1000; // card lingers this long past the end
const WRAP_MS  =  5 * 60 * 1000; // "wrapping up" once this little is left

const PHASE_LABEL = {
  upcoming: "Starts soon",
  live:     "In session",
  wrap:     "Wrapping up",
  overtime: "Running over",
};

function pad(n) {
  return String(n).padStart(2, "0");
}

// mm:ss, widening to h:mm:ss only once it needs to
function clock(ms) {
  const secs = Math.max(0, Math.round(ms / 1000));
  const h = Math.floor(secs / 3600);
  const m = Math.floor((secs % 3600) / 60);
  const s = secs % 60;
  return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${pad(m)}:${pad(s)}`;
}

function formatTime(dt) {
  return new Date(dt).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit", hour12: true });
}

/**
 * Live countdown for the session the therapist is in, or about to be in.
 * Renders nothing unless a session falls inside the lead/grace window.
 *
 * Ticks on its own rather than reading a clock from the parent, so a page
 * holding a heavy timeline doesn't re-render once a second.
 */
export default function SessionTimer({ appointments, onOpen, sticky = false, className = "" }) {
  const modeMap = useModeMap();
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);

  // sorted so that when two windows overlap the earlier session wins —
  // the one already running beats the one about to start
  const trackable = useMemo(() => (appointments ?? [])
    .filter(a => a.startTime && a.endTime && ACTIVE_STATUSES.includes(a.status))
    .map(a => ({ ...a, start: new Date(a.startTime).getTime(), end: new Date(a.endTime).getTime() }))
    .sort((a, b) => a.start - b.start), [appointments]);

  const session = trackable.find(a => now >= a.start - LEAD_MS && now <= a.end + GRACE_MS);
  if (!session) return null;

  const total     = session.end - session.start;
  const elapsed   = now - session.start;
  const remaining = session.end - now;

  const phase = now < session.start   ? "upcoming"
              : remaining < 0         ? "overtime"
              : remaining <= WRAP_MS  ? "wrap"
              :                         "live";

  const pct = total > 0 ? Math.min(100, Math.max(0, (elapsed / total) * 100)) : 0;
  const modeName = modeMap[session.modeId]?.displayName;

  const headline = phase === "upcoming" ? clock(session.start - now)
                 : phase === "overtime" ? `+${clock(-remaining)}`
                 :                        clock(remaining);

  const caption = phase === "upcoming" ? "until start"
                : phase === "overtime" ? "past the booked end"
                :                        "left";

  return (
    <div className={`${styles.card} ${styles[phase]} ${sticky ? styles.sticky : ""} ${className}`} role="timer">
      <div className={styles.top}>
        <span className={styles.phase}>
          <span className={styles.dot} />
          {PHASE_LABEL[phase]}
        </span>
        <span className={styles.client}>{session.clientName}</span>
      </div>

      <div className={styles.readout}>
        <div className={styles.big}>
          <span className={styles.bigValue}>{headline}</span>
          <span className={styles.caption}>{caption}</span>
        </div>
        <div className={styles.small}>
          <span className={styles.smallValue}>{phase === "upcoming" ? "—" : clock(elapsed)}</span>
          <span className={styles.caption}>elapsed of {Math.round(total / 60000)} min</span>
        </div>
      </div>

      <div className={styles.track}>
        <div className={styles.fill} style={{ width: `${pct}%` }} />
      </div>

      <div className={styles.foot}>
        <span className={styles.when}>
          {formatTime(session.startTime)} – {formatTime(session.endTime)}
          {modeName ? ` · ${modeName}` : ""}
        </span>
        {onOpen && (
          <button type="button" className={styles.openBtn} onClick={() => onOpen(session)}>
            Details
          </button>
        )}
      </div>
    </div>
  );
}
