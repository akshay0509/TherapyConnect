import styles from "./ChipSelect.module.css";

/** Preference fields are stored as one comma-joined string — the same shape the
 *  Google Forms intake produces when it joins a multi-choice answer with ", ".
 *  These helpers keep the UI and that storage format in agreement. */
export function splitList(value) {
  return String(value ?? "").split(",").map(part => part.trim()).filter(Boolean);
}

/**
 * Whether a stored value is a short list of labels, or prose.
 *
 * Real intake answers to "preferred days and timings" are sentences:
 * "Any day apart from Wednesday, Thursday and Friday. Time either between 11am
 * to 1pm or between 4pm to 8pm". Comma-splitting that produces fragments that
 * are both unreadable as chips and actively misleading — the fragment
 * "Thursday and Friday…" reads as a preference when the client said the
 * opposite. Worse, as chips they are toggleable, so one stray click silently
 * drops that text for good.
 *
 * So anything that doesn't look like a tidy list is treated as free text and
 * shown, and edited, verbatim.
 */
export function looksLikeChipList(value) {
  const parts = splitList(value);
  if (parts.length === 0) return true;                 // nothing to misread
  return parts.length <= 8 && parts.every(part => part.length <= 18);
}

/**
 * Multi-select rendered as chips.
 *
 * Values arriving from a Google Form are free text, so anything already stored
 * that isn't one of our options is shown as an extra chip rather than dropped —
 * otherwise opening the editor would silently discard what the client wrote.
 */
export default function ChipSelect({ options, value, onChange, label }) {
  /* Prose can't be represented as chips without losing it, so it gets a plain
     textarea instead. The therapist can still rewrite it into a list if they
     want chips back. */
  if (!looksLikeChipList(value)) {
    return (
      <div className={styles.wrap}>
        <textarea
          className={styles.freeText}
          value={value ?? ""}
          onChange={e => onChange(e.target.value)}
          rows={3}
          aria-label={label}
        />
        <span className={styles.freeTextHint}>
          Kept as written by the client. Replace with a comma-separated list to pick from options.
        </span>
      </div>
    );
  }

  const selected = splitList(value);
  const extras = selected.filter(item => !options.includes(item));
  const all = [...options, ...extras];

  const toggle = (option) => {
    const next = selected.includes(option)
      ? selected.filter(item => item !== option)
      : [...selected, option];
    // Emit in option order so the stored string reads consistently regardless
    // of the order the therapist happened to click things in.
    const known = options.filter(option => next.includes(option));
    const unknown = next.filter(item => !options.includes(item));
    onChange([...known, ...unknown].join(", "));
  };

  return (
    <div className={styles.wrap} role="group" aria-label={label}>
      {all.map(option => {
        const on = selected.includes(option);
        return (
          <button
            key={option}
            type="button"
            aria-pressed={on}
            className={`chip ${on ? "chip-online" : "chip-mut"} ${styles.chip}`}
            onClick={() => toggle(option)}
          >
            {option}
          </button>
        );
      })}
    </div>
  );
}
