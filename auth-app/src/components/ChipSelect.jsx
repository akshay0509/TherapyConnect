import styles from "./ChipSelect.module.css";

/** Preference fields are stored as one comma-joined string — the same shape the
 *  Google Forms intake produces when it joins a multi-choice answer with ", ".
 *  These helpers keep the UI and that storage format in agreement. */
export function splitList(value) {
  return String(value ?? "").split(",").map(part => part.trim()).filter(Boolean);
}

/**
 * Multi-select rendered as chips.
 *
 * Values arriving from a Google Form are free text, so anything already stored
 * that isn't one of our options is shown as an extra chip rather than dropped —
 * otherwise opening the editor would silently discard what the client wrote.
 */
export default function ChipSelect({ options, value, onChange, label }) {
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
