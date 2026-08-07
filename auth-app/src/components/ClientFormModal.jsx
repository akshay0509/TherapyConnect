import { useEffect, useMemo, useState } from "react";
import ChipSelect from "./ChipSelect";
import Icon from "./icons";
import styles from "./ClientFormModal.module.css";

/**
 * The single client form, used for both Add client and Edit client.
 *
 * These were two separate implementations that had drifted apart — different
 * shells, different field grouping, different button styling, and the edit one
 * had quietly borrowed the session-notes modal's chrome. Sharing one component
 * is what actually stops them diverging again; matching them by hand only holds
 * until the next change.
 *
 * Layout is a section rail beside a single-section pane rather than one long
 * scroll of twenty inputs. On edit that means landing straight on the thing you
 * came to change; on create it breaks an intimidating form into six short ones.
 *
 * Because only one section is mounted at a time, native `required` validation
 * cannot see the others — so requirements are declared in SECTIONS and checked
 * here, and submitting jumps to the first section that is missing something.
 */

const GENDER_OPTIONS = ["Female", "Male", "Non-binary", "Prefer not to say", "Other"];
const DAY_OPTIONS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
const TIMING_OPTIONS = ["Morning", "Afternoon", "Evening"];

const SECTIONS = [
  {
    key: "identity", label: "Identity", icon: "users",
    blurb: "Who the client is",
    required: ["firstName", "lastName", "dob", "gender"],
  },
  {
    key: "contact", label: "Contact", icon: "mail",
    blurb: "How to reach them",
    required: ["email", "phoneNumber"],
  },
  {
    key: "background", label: "Background", icon: "clipboard",
    blurb: "Context for your notes",
    required: [],
  },
  {
    key: "billing", label: "Billing", icon: "dollar",
    blurb: "What a session costs",
    required: [],
  },
  {
    key: "preferences", label: "Preferences", icon: "clock",
    blurb: "When and how they prefer to meet",
    required: [],
  },
  {
    key: "emergency", label: "Emergency contact", icon: "shield",
    blurb: "Who to call if needed",
    required: ["emergencyPhoneNumber"],
  },
];

function Field({ label, htmlFor, hint, optional, children, wide }) {
  return (
    <div className={`${styles.field} ${wide ? styles.fieldWide : ""}`}>
      <label className={styles.label} htmlFor={htmlFor}>
        {label}{optional && <span className={styles.optional}>optional</span>}
      </label>
      {children}
      {hint && <span className={styles.hint}>{hint}</span>}
    </div>
  );
}

export default function ClientFormModal({
  open,
  mode = "create",              // "create" | "edit"
  form,
  onChange,                     // (name, value) => void
  onSubmit,
  onClose,
  loading = false,
  error = null,
  modeOptions = [],
}) {
  const [active, setActive] = useState("identity");
  const [showErrors, setShowErrors] = useState(false);

  // Reopening should always start at the top, not wherever it was left.
  useEffect(() => { if (open) { setActive("identity"); setShowErrors(false); } }, [open]);

  useEffect(() => {
    if (!open) return;
    const onKey = e => { if (e.key === "Escape" && !loading) onClose(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, loading, onClose]);

  /* Requirements apply to creation only. An existing client may predate a field
     — intake submissions often arrive without a gender or DOB — and forcing one
     in before an unrelated edit can be saved would be hostile. */
  const missing = useMemo(() => {
    if (mode !== "create") return {};
    const out = {};
    SECTIONS.forEach(s => {
      const gaps = s.required.filter(n => !String(form[n] ?? "").trim());
      if (gaps.length) out[s.key] = gaps;
    });
    return out;
  }, [form, mode]);

  const firstIncomplete = SECTIONS.find(s => missing[s.key])?.key ?? null;

  const handleSubmit = e => {
    e.preventDefault();
    if (firstIncomplete) { setShowErrors(true); setActive(firstIncomplete); return; }
    onSubmit();
  };

  if (!open) return null;

  const set = name => e => onChange(name, e.target.value);
  const initials = ((form.firstName?.[0] ?? "") + (form.lastName?.[0] ?? "")).toUpperCase();
  const displayName = [form.firstName, form.lastName].filter(Boolean).join(" ").trim();
  const idx = SECTIONS.findIndex(s => s.key === active);
  const section = SECTIONS[idx];
  const gapsHere = showErrors ? (missing[active] ?? []) : [];
  const invalid = name => gapsHere.includes(name);

  return (
    <div className={styles.wrap} role="dialog" aria-modal="true" aria-labelledby="client-form-title">
      <div className={styles.backdrop} onClick={() => !loading && onClose()} />

      <div className={styles.modal}>
        {/* Identity banner — the client is an initials avatar everywhere else in
            the app, so the form builds one as you type rather than opening as a
            bare stack of inputs. */}
        <header className={styles.head}>
          <span className={`avatar avatar-l ${styles.avatar}`}>
            {initials || <Icon name="users" size={22} />}
          </span>
          <div className={styles.headText}>
            <h2 className={styles.title} id="client-form-title">
              {mode === "create" ? "Add client" : "Edit client"}
            </h2>
            <p className={styles.sub}>
              {displayName || (mode === "create" ? "New client" : "Update this client's details")}
            </p>
          </div>
          <button type="button" className={styles.close} onClick={onClose} aria-label="Close" disabled={loading}>
            <Icon name="x" size={18} />
          </button>
        </header>

        <form className={styles.body} onSubmit={handleSubmit}>
          <nav className={styles.rail} aria-label="Form sections">
            {SECTIONS.map((s, i) => {
              const incomplete = showErrors && missing[s.key];
              return (
                <button
                  key={s.key} type="button"
                  className={`${styles.railItem} ${active === s.key ? styles.railActive : ""} ${incomplete ? styles.railIncomplete : ""}`}
                  onClick={() => setActive(s.key)}
                >
                  <span className={styles.railIcon}><Icon name={s.icon} size={15} /></span>
                  <span className={styles.railLabel}>{s.label}</span>
                  {incomplete
                    ? <span className={styles.railDot} title={`${missing[s.key].length} required field(s) missing`} />
                    : <span className={styles.railNum}>{i + 1}</span>}
                </button>
              );
            })}
          </nav>

          <div className={styles.pane}>
            <div className={styles.paneHead}>
              <h3>{section.label}</h3>
              <p>{section.blurb}</p>
            </div>

            <div className={styles.grid}>
              {active === "identity" && (
                <>
                  <Field label="First name" htmlFor="firstName">
                    <input id="firstName" className={`input ${invalid("firstName") ? styles.inputBad : ""}`}
                      value={form.firstName} onChange={set("firstName")} placeholder="Jethala" />
                  </Field>
                  <Field label="Last name" htmlFor="lastName">
                    <input id="lastName" className={`input ${invalid("lastName") ? styles.inputBad : ""}`}
                      value={form.lastName} onChange={set("lastName")} placeholder="Gada" />
                  </Field>
                  <Field label="Date of birth" htmlFor="dob">
                    <input id="dob" type="date" className={`input ${invalid("dob") ? styles.inputBad : ""}`}
                      value={form.dob} onChange={set("dob")} />
                  </Field>
                  <Field label="Gender" htmlFor="gender">
                    <select id="gender" className={`input ${invalid("gender") ? styles.inputBad : ""}`}
                      value={form.gender} onChange={set("gender")}>
                      <option value="">Select…</option>
                      {GENDER_OPTIONS.map(g => <option key={g} value={g}>{g}</option>)}
                    </select>
                  </Field>
                  <Field label="Pronouns" htmlFor="pronouns" optional wide>
                    <input id="pronouns" className="input" value={form.pronouns}
                      onChange={set("pronouns")} placeholder="she/her" />
                  </Field>
                </>
              )}

              {active === "contact" && (
                <>
                  <Field label="Email" htmlFor="email"
                    hint="Calendar invites and session links are sent here.">
                    <input id="email" type="email" className={`input ${invalid("email") ? styles.inputBad : ""}`}
                      value={form.email} onChange={set("email")} placeholder="name@example.com" />
                  </Field>
                  <Field label="Phone number" htmlFor="phoneNumber">
                    <input id="phoneNumber" type="tel" className={`input ${invalid("phoneNumber") ? styles.inputBad : ""}`}
                      value={form.phoneNumber} onChange={set("phoneNumber")} placeholder="9876543210" />
                  </Field>
                </>
              )}

              {active === "background" && (
                <>
                  <Field label="Qualification" htmlFor="qualification" optional>
                    <input id="qualification" className="input" value={form.qualification}
                      onChange={set("qualification")} placeholder="e.g. B.Sc Psychology" />
                  </Field>
                  <Field label="Occupation" htmlFor="currentOccupation" optional>
                    <input id="currentOccupation" className="input" value={form.currentOccupation}
                      onChange={set("currentOccupation")} placeholder="e.g. Student" />
                  </Field>
                </>
              )}

              {active === "billing" && (
                <>
                  <Field label="Session fee (₹)" htmlFor="sessionFee" optional
                    hint={form.dsf
                      ? "Pro bono overrides any rate set here."
                      : "A negotiated rate for this client. Leave blank to charge the standard service price."}>
                    <input id="sessionFee" type="number" min="0" step="1" className="input"
                      value={form.sessionFee} onChange={set("sessionFee")}
                      disabled={form.dsf} placeholder="e.g. 1200" />
                  </Field>
                  <div className={`${styles.field} ${styles.fieldWide}`}>
                    {/* DSF is the partner non-profit; its students are seen pro bono.
                        Sessions are stamped at zero when booked, so this drives real
                        reporting rather than being a label. */}
                    <div className={styles.toggleRow}>
                      <div className={styles.toggleText}>
                        <b>Pro bono (DSF)</b>
                        <span>Sessions are provided free of charge, counted separately and excluded from earnings.</span>
                      </div>
                      <button type="button" role="switch" aria-checked={!!form.dsf}
                        aria-label="Pro bono, DSF student"
                        className={`switch ${form.dsf ? "on" : ""}`}
                        onClick={() => onChange("dsf", !form.dsf)} />
                    </div>
                  </div>
                </>
              )}

              {active === "preferences" && (
                <>
                  <Field label="Preferred days" optional wide>
                    <ChipSelect label="Preferred days" options={DAY_OPTIONS}
                      value={form.preferredDays} onChange={v => onChange("preferredDays", v)} />
                  </Field>
                  <Field label="Preferred timings" optional wide>
                    <ChipSelect label="Preferred timings" options={TIMING_OPTIONS}
                      value={form.preferredTimings} onChange={v => onChange("preferredTimings", v)} />
                  </Field>
                  {modeOptions.length > 0 && (
                    <Field label="Preferred modes" optional wide>
                      <ChipSelect label="Preferred modes" options={modeOptions}
                        value={form.preferredModes} onChange={v => onChange("preferredModes", v)} />
                    </Field>
                  )}
                </>
              )}

              {active === "emergency" && (
                <>
                  <Field label="Emergency phone" htmlFor="emergencyPhoneNumber">
                    <input id="emergencyPhoneNumber" type="tel"
                      className={`input ${invalid("emergencyPhoneNumber") ? styles.inputBad : ""}`}
                      value={form.emergencyPhoneNumber} onChange={set("emergencyPhoneNumber")} placeholder="9876543210" />
                  </Field>
                  <Field label="Name" htmlFor="emergencyContactName" optional>
                    <input id="emergencyContactName" className="input" value={form.emergencyContactName}
                      onChange={set("emergencyContactName")} placeholder="Daya Gada" />
                  </Field>
                  <Field label="Relationship" htmlFor="emergencyContactRelationship" optional>
                    <input id="emergencyContactRelationship" className="input"
                      value={form.emergencyContactRelationship}
                      onChange={set("emergencyContactRelationship")} placeholder="Spouse" />
                  </Field>
                  <Field label="Age" htmlFor="emergencyContactAge" optional>
                    <input id="emergencyContactAge" type="number" min="0" max="120" className="input"
                      value={form.emergencyContactAge} onChange={set("emergencyContactAge")} placeholder="48" />
                  </Field>
                </>
              )}
            </div>

            {gapsHere.length > 0 && (
              <p className={styles.sectionError}>
                <Icon name="alert" size={14} /> Fill the highlighted field{gapsHere.length > 1 ? "s" : ""} to continue.
              </p>
            )}
            {error && (
              <p className={styles.formError}><Icon name="alert" size={14} /> {error}</p>
            )}
          </div>

          <footer className={styles.foot}>
            <span className={styles.footStep}>Step {idx + 1} of {SECTIONS.length}</span>
            <div className={styles.footBtns}>
              <button type="button" className="btn" onClick={onClose} disabled={loading}>Cancel</button>
              {idx > 0 && (
                <button type="button" className="btn" disabled={loading}
                  onClick={() => setActive(SECTIONS[idx - 1].key)}>Back</button>
              )}
              {idx < SECTIONS.length - 1 && (
                <button type="button" className="btn" disabled={loading}
                  onClick={() => setActive(SECTIONS[idx + 1].key)}>Next</button>
              )}
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading
                  ? <span className={styles.spinner} />
                  : mode === "create" ? "Add client" : "Save changes"}
              </button>
            </div>
          </footer>
        </form>
      </div>
    </div>
  );
}
