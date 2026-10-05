import { useEffect, useRef, useState } from "react";
import Icon from "./icons";
import { meetingView } from "../utils/meetingLink.mjs";
import styles from "./MeetingJoin.module.css";

export default function MeetingJoin({ appointment, modeType, onRefresh }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const generation = useRef(0);
  useEffect(() => {
    generation.current += 1;
    setLoading(false);
    setError(null);
    return () => { generation.current += 1; };
  }, [appointment?.appointmentId, appointment?.startTime, appointment?.status, appointment?.appointmentStatus, modeType]);
  const view = meetingView(appointment, modeType);
  useEffect(() => {
    if (appointment?.meetingStatus === "READY" && view?.failed && !view.url) {
      console.warn("Meeting marked READY has an invalid Google Meet URL", {
        appointmentId: appointment.appointmentId,
      });
    }
  }, [appointment?.appointmentId, appointment?.meetingStatus, appointment?.meetingUrl, view?.failed, view?.url]);
  if (!view) return null;
  const refresh = async (event) => {
    event.stopPropagation();
    const request = generation.current;
    setLoading(true);
    setError(null);
    try { await onRefresh?.(); }
    catch { if (request === generation.current) setError("Couldn't refresh meeting status. Try again."); }
    finally { if (request === generation.current) setLoading(false); }
  };
  return (
    <div className={styles.control}>
      {view.url ? (
        <a className={styles.join} href={view.url} target="_blank" rel="noopener noreferrer"
          onClick={event => event.stopPropagation()}>
          <Icon name="video" size={16} /> Join Google Meet
          <span className={styles.srOnly}> (opens in a new tab)</span>
        </a>
      ) : (
        <>
          <span className={styles.message} role="status">
            {view.failed ? "Meeting link is unavailable" : "Meeting update pending"}
          </span>
          {onRefresh && <button type="button" className={styles.refresh} onClick={refresh} disabled={loading}>
            {loading ? "Refreshing…" : "Refresh meeting status"}
          </button>}
        </>
      )}
      {error && <span className={styles.error} role="alert">{error}</span>}
    </div>
  );
}
