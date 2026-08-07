import { useEffect, useState } from "react";
import { getClientRisk, saveClientRisk } from "../api/therapistClients";
import Icon from "./icons";
import styles from "./ClientRiskCard.module.css";

/**
 * Clinical risk record for one client.
 *
 * Everything here is therapist-entered. Nothing infers a level from attendance,
 * cancellations or note content — an algorithmic risk score is a clinical
 * assessment made by software that has never met the client, and a therapist
 * who either trusts it or is contradicted by it is worse off than with nothing.
 *
 * Deliberately rendered only on the client's own page, never as a badge in the
 * clients list. Therapists share their screen with clients constantly, and a red
 * chip beside someone's name in a visible list is both stigmatising and a
 * confidentiality hazard.
 */

const LEVELS = [
  { value: "NONE",     label: "None",     hint: "No current concern",            tone: "mut"  },
  { value: "LOW",      label: "Low",      hint: "Reviewed every 90 days",        tone: "ok"   },
  { value: "MODERATE", label: "Moderate", hint: "Reviewed every 60 days",        tone: "warn" },
  { value: "HIGH",     label: "High",     hint: "Reviewed every 15 days",        tone: "bad"  },
];

const TONE_CHIP = { mut: "chip-mut", ok: "chip-ok", warn: "chip-warn", bad: "chip-bad" };

function formatDate(dt) {
  if (!dt) return "—";
  return new Date(dt).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
}
function relativeDays(dt) {
  if (!dt) return null;
  const days = Math.round((new Date(dt) - new Date()) / 86400000);
  if (days === 0) return "today";
  if (days > 0) return `in ${days} day${days === 1 ? "" : "s"}`;
  return `${Math.abs(days)} day${Math.abs(days) === 1 ? "" : "s"} ago`;
}

export default function ClientRiskCard({ clientId, clientName }) {
  const [risk, setRisk] = useState(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    let alive = true;
    setLoading(true);
    getClientRisk(clientId)
      .then(r => { if (alive) setRisk(r); })
      .catch(e => { if (alive) setError(e.message); })
      .finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, [clientId]);

  const openEdit = () => {
    setForm({
      level: risk?.level ?? "NONE",
      concern: risk?.concern ?? "",
      safetyPlan: risk?.safetyPlan ?? "",
      reviewIntervalDaysOverride: risk?.reviewIntervalDaysOverride ?? "",
    });
    setError(null);
    setEditing(true);
  };

  const handleSave = async () => {
    setSaving(true); setError(null);
    try {
      const updated = await saveClientRisk(clientId, {
        level: form.level,
        concern: form.concern.trim() || null,
        safetyPlan: form.safetyPlan.trim() || null,
        reviewIntervalDaysOverride:
          form.reviewIntervalDaysOverride === "" ? null : Number(form.reviewIntervalDaysOverride),
      });
      setRisk(updated);
      setEditing(false);
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className={`card ${styles.card}`}><div className={styles.loading} /></div>;

  const level = LEVELS.find(l => l.value === (risk?.level ?? "NONE")) ?? LEVELS[0];
  const neverAssessed = risk?.neverAssessed;

  return (
    <div className={`card ${styles.card} ${risk?.reviewDue ? styles.cardDue : ""}`}>
      <div className={styles.head}>
        <div>
          <div className="eyebrow">Clinical</div>
          <h2 className={styles.title}>Risk</h2>
        </div>
        {!editing && (
          <button className="btn btn-sm" onClick={openEdit}>
            <Icon name="edit" size={14} /> {neverAssessed ? "Assess" : "Review"}
          </button>
        )}
      </div>

      {!editing ? (
        <>
          {neverAssessed ? (
            /* Not the same as an assessed level of NONE — nobody has looked yet,
               and saying "None" would imply a judgement that was never made. */
            <p className={styles.empty}>
              No risk assessment recorded for {clientName || "this client"} yet.
            </p>
          ) : (
            <>
              <div className={styles.levelRow}>
                <span className={`chip ${TONE_CHIP[level.tone]} ${styles.levelChip}`}>{level.label}</span>
                {risk.reviewDue && (
                  <span className={styles.dueFlag}>
                    <Icon name="alert" size={13} /> Review due
                  </span>
                )}
              </div>

              {risk.concern && (
                <div className={styles.block}>
                  <span className={styles.blockLabel}>Concern</span>
                  <p>{risk.concern}</p>
                </div>
              )}
              {risk.safetyPlan && (
                <div className={styles.block}>
                  <span className={styles.blockLabel}>Safety plan</span>
                  <p>{risk.safetyPlan}</p>
                </div>
              )}

              <div className={styles.meta}>
                <span>Last reviewed <b>{formatDate(risk.lastReviewedAt)}</b></span>
                {risk.reviewDueAt && (
                  <span>Next review <b>{formatDate(risk.reviewDueAt)}</b> ({relativeDays(risk.reviewDueAt)})</span>
                )}
              </div>

              {risk.reviews?.length > 0 && (
                <details className={styles.log}>
                  <summary>Review history ({risk.reviews.length})</summary>
                  <ul>
                    {risk.reviews.map(r => (
                      <li key={r.reviewId}>
                        <span className={styles.logWhen}>{formatDate(r.reviewedAt)}</span>
                        <span className={styles.logWhat}>
                          {r.unchanged
                            ? `Confirmed ${LEVELS.find(l => l.value === r.level)?.label ?? r.level}`
                            : `${r.previousLevel ? (LEVELS.find(l => l.value === r.previousLevel)?.label ?? r.previousLevel) : "First assessment"} → ${LEVELS.find(l => l.value === r.level)?.label ?? r.level}`}
                        </span>
                      </li>
                    ))}
                  </ul>
                </details>
              )}
            </>
          )}
        </>
      ) : (
        <div className={styles.form}>
          <div className={styles.field}>
            <span className={styles.label}>Level</span>
            <div className={styles.levelOptions}>
              {LEVELS.map(l => (
                <button
                  key={l.value} type="button"
                  className={`${styles.levelOption} ${form.level === l.value ? styles.levelOptionOn : ""} ${styles["tone_" + l.tone]}`}
                  onClick={() => setForm(f => ({ ...f, level: l.value }))}
                >
                  <b>{l.label}</b>
                  <span>{l.hint}</span>
                </button>
              ))}
            </div>
          </div>

          <div className={styles.field}>
            <label className={styles.label} htmlFor="risk-concern">Concern</label>
            <textarea
              id="risk-concern" rows={3} className={styles.textarea}
              value={form.concern}
              onChange={e => setForm(f => ({ ...f, concern: e.target.value }))}
              placeholder="What the risk is, in your words."
            />
          </div>

          <div className={styles.field}>
            <label className={styles.label} htmlFor="risk-plan">Safety plan</label>
            <textarea
              id="risk-plan" rows={3} className={styles.textarea}
              value={form.safetyPlan}
              onChange={e => setForm(f => ({ ...f, safetyPlan: e.target.value }))}
              placeholder="What has been agreed with the client."
            />
          </div>

          <div className={styles.field}>
            <label className={styles.label} htmlFor="risk-interval">
              Review interval <span className={styles.optional}>optional</span>
            </label>
            <input
              id="risk-interval" type="number" min="1" className="input"
              value={form.reviewIntervalDaysOverride}
              onChange={e => setForm(f => ({ ...f, reviewIntervalDaysOverride: e.target.value }))}
              placeholder={String(LEVELS.find(l => l.value === form.level)?.value === "NONE" ? "" : "")}
            />
            <span className={styles.hint}>
              Days between reviews for this client. Leave blank to follow the level
              {form.level !== "NONE" ? ` (${{ LOW: 90, MODERATE: 60, HIGH: 15 }[form.level]} days).` : " — None never becomes due."}
            </span>
          </div>

          {error && <p className={styles.error}><Icon name="alert" size={14} /> {error}</p>}

          <div className={styles.actions}>
            <button className="btn" onClick={() => setEditing(false)} disabled={saving}>Cancel</button>
            <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
              {saving ? "Saving…" : "Save review"}
            </button>
          </div>
          <p className={styles.savingNote}>
            Saving records this as a review, whether or not the level changed.
          </p>
        </div>
      )}

      {!editing && error && <p className={styles.error}><Icon name="alert" size={14} /> {error}</p>}
    </div>
  );
}
