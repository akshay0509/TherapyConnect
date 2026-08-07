import { useEffect, useMemo, useState } from "react";
import { getClientIntakes, approveClientIntake, rejectClientIntake } from "../api/clientIntakes";
import Icon from "../components/icons";
import styles from "./ClientIntakesPage.module.css";

const STATUSES = ["PENDING", "APPROVED", "REJECTED"];
const STATUS_CHIP = { PENDING: "warn", APPROVED: "ok", REJECTED: "bad" };

// Editable fields, grouped so a 17-field form doesn't read as one wall.
// Keys must match the IntakeClientData record exactly — anything else is
// dropped on deserialization and the therapist's correction silently vanishes.
const GROUPS = [
  {
    label: "Identity",
    fields: [
      { key: "fullName", label: "Full name", required: true },
      { key: "firstName", label: "First name", half: true },
      { key: "lastName", label: "Last name", half: true },
      { key: "dob", label: "Date of birth", half: true, type: "date", required: true },
      { key: "gender", label: "Gender", half: true },
      { key: "pronouns", label: "Pronouns" },
    ],
  },
  {
    label: "Background",
    fields: [
      { key: "qualification", label: "Qualification", half: true },
      { key: "occupation", label: "Occupation", half: true },
    ],
  },
  {
    label: "Contact",
    fields: [
      { key: "email", label: "Email", half: true, type: "email" },
      { key: "phoneNumber", label: "Phone number", half: true, type: "tel" },
    ],
  },
  {
    label: "Preferences",
    fields: [
      { key: "preferredDays", label: "Preferred days" },
      { key: "preferredTimings", label: "Preferred timings" },
      { key: "preferredModes", label: "Preferred modes" },
    ],
  },
  {
    label: "Emergency contact",
    fields: [
      { key: "emergencyContactName", label: "Name", half: true },
      { key: "emergencyContactRelationship", label: "Relationship", half: true },
      { key: "emergencyPhoneNumber", label: "Phone", half: true, type: "tel" },
      { key: "emergencyContactAge", label: "Age", half: true, type: "number" },
    ],
  },
];

function titleCase(value) {
  return String(value || "").toLowerCase().split("_").filter(Boolean)
    .map(w => w[0].toUpperCase() + w.slice(1)).join(" ");
}

function fullName(client) {
  if (!client) return "Unnamed";
  const joined = [client.firstName, client.lastName].filter(p => p && p.trim()).join(" ").trim();
  return joined || client.fullName || "Unnamed";
}

function initials(client) {
  const n = fullName(client);
  const parts = n.split(/\s+/).filter(Boolean);
  return ((parts[0]?.[0] ?? "") + (parts.length > 1 ? parts[parts.length - 1][0] : "")).toUpperCase() || "?";
}

function formatWhen(value) {
  if (!value) return "—";
  return new Date(value).toLocaleString("en-IN", {
    day: "numeric", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit", hour12: true,
  });
}

/** ApproveIntakeRequest binds to IntakeClientData, a Java record. Blank text
 *  inputs must go over as null rather than "" — an empty string will not coerce
 *  into its Integer emergencyContactAge and fails the whole request. */
function toClientPayload(draft) {
  const out = {};
  for (const [key, value] of Object.entries(draft ?? {})) {
    out[key] = typeof value === "string" && value.trim() === "" ? null : value;
  }
  const age = draft?.emergencyContactAge;
  out.emergencyContactAge = age === "" || age == null ? null : Number(age);
  /* A pro-bono client carries no stored fee: sessionFee == 0 means pro bono,
     and that is decided at booking by resolveSessionFee. Storing a rate next to
     the flag would be two answers to one question, and the flag wins anyway. */
  const fee = draft?.sessionFee;
  out.sessionFee = draft?.dsf || fee === "" || fee == null ? null : Number(fee);
  out.dsf = !!draft?.dsf;
  return out;
}

/** Mirrors validateForCreation on the server, so a missing field is visible
 *  before the therapist commits rather than coming back as a 400. */
function blockers(draft) {
  const missing = [];
  if (!String(draft?.fullName ?? "").trim()) missing.push("a full name");
  if (!draft?.dob) missing.push("a date of birth");
  if (!draft?.consent) missing.push("recorded consent");
  /* normaliseSessionFee throws on a negative amount. Catching it here turns a
     400 on submit into something the therapist can see and correct in place. */
  const fee = draft?.sessionFee;
  if (!draft?.dsf && fee !== "" && fee != null && !(Number(fee) > 0)) {
    missing.push("a positive session fee");
  }
  return missing;
}

export default function ClientIntakesPage() {
  const [status, setStatus] = useState("PENDING");
  const [items, setItems] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [draft, setDraft] = useState(null);
  const [submitted, setSubmitted] = useState(null);
  const [reason, setReason] = useState("");
  const [mode, setMode] = useState("create");   // create | reject
  const [showRaw, setShowRaw] = useState(false);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);

  const selected = useMemo(
    () => items.find(i => i.intakeId === selectedId) ?? null,
    [items, selectedId]
  );

  async function load(nextStatus = status) {
    setLoading(true);
    setError(null);
    try {
      const data = await getClientIntakes(nextStatus);
      setItems(data ?? []);
      setSelectedId(null);
      setDraft(null);
    } catch (err) {
      setError(err.message);
      setItems([]);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load("PENDING"); }, []);

  function open(intake) {
    setSelectedId(intake.intakeId);
    // The submitted answers are the starting point; the therapist can correct
    // typos before the client record is created. rawAnswers stays untouched.
    setDraft({ ...intake.client });
    /* Frozen copy of what the client actually submitted. Reviewing means
       COMPARING, and the raw answers sit far below the form — so any field the
       therapist edits shows the original inline instead of making them scroll. */
    setSubmitted({ ...intake.client });
    setMode("create");
    setReason("");
    setShowRaw(false);
    setError(null);
  }

  async function run(action) {
    setBusy(true);
    setError(null);
    try {
      await action();
      setNotice("Intake updated.");
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  const setField = (key, value) => setDraft(prev => ({ ...prev, [key]: value }));
  const setDsf = (on) => setDraft(prev => ({ ...prev, dsf: on, sessionFee: on ? "" : prev.sessionFee }));
  const missing = blockers(draft);

  return (
    <div className="page-wrap">
      <div className="page-head">
        <div>
          <div className="eyebrow">People</div>
          <h1>Client intakes</h1>
          <div className="sub">Submissions from your intake form, reviewed before they become clients.</div>
        </div>
        <div className="head-actions">
          <button className="btn" onClick={() => load()} disabled={loading}>
            <Icon name="refresh" size={16} /> Refresh
          </button>
        </div>
      </div>

      <div className={styles.filters}>
        {STATUSES.map(s => (
          <button
            key={s}
            className={`chip ${status === s ? "chip-online" : "chip-mut"} ${styles.filterChip}`}
            onClick={() => { setStatus(s); load(s); }}
          >
            {titleCase(s)}
          </button>
        ))}
        {!loading && <span className={styles.count}>{items.length} {items.length === 1 ? "submission" : "submissions"}</span>}
      </div>

      {error && !selected && (
        <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{error}</div>
      )}
      {notice && <div className={styles.notice}><Icon name="check" size={14} /> {notice}</div>}

      <div className={`grid-2 ${styles.grid}`}>
        {/* ── Left: submissions ── */}
        <div className={styles.listCol}>
          {loading ? (
            <div className={`card ${styles.empty}`}><div className={styles.spinner} /></div>
          ) : items.length === 0 ? (
            <div className={`card ${styles.empty}`}>
              <span className={styles.emptyIcon}><Icon name="clipboard" size={26} /></span>
              <p>No {titleCase(status).toLowerCase()} submissions.</p>
              <p className={styles.emptyHint}>New form responses appear here for review.</p>
            </div>
          ) : (
            items.map(it => (
              <button
                key={it.intakeId}
                className={`card ${styles.item} ${selectedId === it.intakeId ? styles.itemOn : ""}`}
                onClick={() => open(it)}
              >
                <span className="avatar avatar-m">{initials(it.client)}</span>
                <span className={styles.itemText}>
                  <b>{fullName(it.client)}</b>
                  <span>{it.client?.email || "No email"}</span>
                  <span className={styles.itemWhen}>{formatWhen(it.submittedAt)}</span>
                </span>
                <span className={`chip chip-${STATUS_CHIP[it.status] || "mut"}`}>{titleCase(it.status)}</span>
              </button>
            ))
          )}
        </div>

        {/* ── Right: review ── */}
        <div className={`card ${styles.review}`}>
          {!selected ? (
            <div className={styles.reviewEmpty}>
              <span className={styles.emptyIcon}><Icon name="cursor" size={26} /></span>
              <p>Select a submission<br />to review it</p>
            </div>
          ) : (
            <div className={styles.reviewBody}>
              <div className={styles.reviewHead}>
                <span className="avatar avatar-l">{initials(selected.client)}</span>
                <div className={styles.reviewIdent}>
                  <b>{fullName(selected.client)}</b>
                  <span>Submitted {formatWhen(selected.submittedAt)}</span>
                </div>
                <span className={`chip chip-${selected.client?.consent ? "ok" : "bad"}`}>
                  {selected.client?.consent ? "Consented" : "No consent"}
                </span>
              </div>

              {selected.status === "PENDING" ? (
                <>
                  {draft && (
                    <div className={styles.form}>
                      <p className={styles.modeHint}>
                        Correct anything the client mistyped. Changed fields show what they originally wrote; the full submission is kept verbatim below.
                      </p>
                      {GROUPS.map(group => (
                        <div key={group.label}>
                          <div className={styles.groupLabel}>{group.label}</div>
                          <div className={styles.fieldGrid}>
                            {group.fields.map(f => (
                              <div key={f.key} className={f.half ? styles.half : styles.full}>
                                <label className={styles.label} htmlFor={f.key}>
                                  {f.label}
                                  {f.required && <span className={styles.req} aria-hidden="true"> *</span>}
                                </label>
                                <input
                                  id={f.key}
                                  className={`input ${(submitted?.[f.key] ?? "") !== (draft[f.key] ?? "") ? styles.inputChanged : ""}`}
                                  type={f.type || "text"}
                                  value={draft[f.key] ?? ""}
                                  onChange={e => setField(f.key, e.target.value)}
                                />
                                {/* Shown only where the therapist has changed something,
                                    so the form stays quiet until a correction is made. */}
                                {(submitted?.[f.key] ?? "") !== (draft[f.key] ?? "") && (
                                  <span className={styles.originalHint}>
                                    they wrote: {String(submitted?.[f.key] ?? "").trim() || <em>nothing</em>}
                                    <button type="button" className={styles.revertBtn}
                                      onClick={() => setField(f.key, submitted?.[f.key] ?? "")}>
                                      revert
                                    </button>
                                  </span>
                                )}
                              </div>
                            ))}
                          </div>
                        </div>
                      ))}

                      {/* Not from the form: no intake item asks a client to price
                          their own therapy. Asked here because approval is the
                          one moment the therapist is already looking at this
                          person — otherwise every migrated client needs a second
                          pass through the edit form just to become bookable. */}
                      <div>
                        <div className={styles.groupLabel}>Fees</div>
                        <div className={styles.fieldGrid}>
                          <div className={styles.half}>
                            <label className={styles.label} htmlFor="sessionFee">Session fee (₹)</label>
                            <input
                              id="sessionFee" className="input"
                              type="number" min="1" step="1"
                              placeholder={draft.dsf ? "Pro bono — no charge" : "Falls back to the service price"}
                              value={draft.dsf ? "" : (draft.sessionFee ?? "")}
                              disabled={!!draft.dsf}
                              onChange={e => setField("sessionFee", e.target.value)}
                            />
                          </div>
                          <div className={styles.half}>
                            <label className={styles.label}>Pro bono</label>
                            <label className={styles.checkRow}>
                              <input type="checkbox" checked={!!draft.dsf}
                                onChange={e => setDsf(e.target.checked)} />
                              <span>Discounted / free sessions</span>
                            </label>
                          </div>
                        </div>
                        <p className={styles.feeHint}>
                          {draft.dsf
                            ? "Sessions for this client are booked at zero and stay out of earnings."
                            : "Leave blank to charge whatever the delivery mode costs at the time of booking."}
                        </p>
                      </div>
                    </div>
                  )}

                  {mode === "reject" && (
                    <div className={`${styles.form} ${styles.rejectPanel}`}>
                      <p className={styles.rejectHint}>
                        <Icon name="alert" size={14} />
                        Rejecting keeps the submission for your records but creates no client.
                      </p>
                      <div className={styles.full}>
                        <label className={styles.label}>Reason <span className={styles.optional}>optional</span></label>
                        <textarea className="input" rows={3} value={reason}
                          onChange={e => setReason(e.target.value)}
                          placeholder="e.g. duplicate submission, out of catchment…" />
                      </div>
                    </div>
                  )}
                </>
              ) : (
                <div className={styles.decided}>
                  <span className={`chip chip-${STATUS_CHIP[selected.status] || "mut"}`}>{titleCase(selected.status)}</span>
                  <span>Reviewed {formatWhen(selected.reviewedAt)}</span>
                  {selected.rejectionReason && <p className={styles.reasonText}>{selected.rejectionReason}</p>}
                </div>
              )}

              {/* The immutable original, kept verbatim for the record */}
              <button type="button" className={styles.rawToggle} onClick={() => setShowRaw(v => !v)}>
                <Icon name="chevron" size={14} className={showRaw ? styles.chevOpen : ""} />
                Original answers
              </button>
              {showRaw && (
                <div className={styles.rawList}>
                  {Object.entries(selected.rawAnswers ?? {}).map(([k, v]) => (
                    <div key={k} className={styles.rawRow}>
                      <span className={styles.rawKey}>{k}</span>
                      <span className={styles.rawVal}>{typeof v === "object" ? JSON.stringify(v) : String(v)}</span>
                    </div>
                  ))}
                  {!Object.keys(selected.rawAnswers ?? {}).length && (
                    <div className={styles.rawEmpty}>No raw answers recorded.</div>
                  )}
                </div>
              )}

              {error && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{error}</div>}

              {selected.status === "PENDING" && mode === "create" && missing.length > 0 && (
                <p className={styles.blocked}>
                  <Icon name="alert" size={14} />
                  This submission still needs {missing.join(", ").replace(/, ([^,]*)$/, " and $1")} before a client can be created.
                </p>
              )}

              {/* Approve and reject are two outcomes of one decision, so both are
                 visible from the moment the submission opens. Reject used to be
                 a tab you had to notice and switch to first, which read as a
                 view of the record rather than an action on it. It stays a
                 two-step — the reason panel opens before anything is sent — so
                 visibility does not turn into an accidental click. */}
              {selected.status === "PENDING" && (
                <div className={styles.actions}>
                  <button className="btn" onClick={() => { setSelectedId(null); setDraft(null); setMode("create"); setReason(""); }}>Cancel</button>
                  <span className={styles.actionsGap} />
                  {mode === "create" ? (
                    <>
                      <button className={`btn ${styles.dangerBtn}`} disabled={busy}
                        onClick={() => setMode("reject")}>
                        Reject
                      </button>
                      <button className="btn btn-primary" disabled={busy || missing.length > 0}
                        onClick={() => run(() => approveClientIntake(selected.intakeId, { client: toClientPayload(draft) }))}>
                        {busy ? <span className={styles.btnSpinner} /> : "Create client"}
                      </button>
                    </>
                  ) : (
                    <>
                      <button className="btn" disabled={busy}
                        onClick={() => { setMode("create"); setReason(""); }}>
                        Back
                      </button>
                      <button className={`btn ${styles.dangerSolid}`} disabled={busy}
                        onClick={() => run(() => rejectClientIntake(selected.intakeId, reason))}>
                        {busy ? <span className={styles.btnSpinner} /> : "Confirm rejection"}
                      </button>
                    </>
                  )}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
