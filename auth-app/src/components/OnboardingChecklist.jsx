import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getMyServices } from "../api/therapistServices";
import { getAvailabilityRules } from "../api/availabilityRules";
import Icon from "./icons";
import styles from "./OnboardingChecklist.module.css";

/**
 * First-run setup guide.
 *
 * A new therapist lands on an empty dashboard with no indication of what has to
 * exist before the product works, and the dependencies are invisible: without a
 * service there is nothing to book, and without availability rules no slots are
 * generated at all — so the Schedule page is simply blank with no explanation.
 * Someone who adds a client first hits that wall with nothing to tell them why.
 *
 * Derived entirely from data that already exists, so there is no new endpoint
 * and nothing to keep in sync. Disappears for good once every step is done —
 * a permanently-complete checklist is just clutter.
 */
export default function OnboardingChecklist({ activeClients = 0 }) {
  const navigate = useNavigate();
  const [state, setState] = useState(null);   // null = still loading

  useEffect(() => {
    let alive = true;
    Promise.all([
      getMyServices().catch(() => []),
      getAvailabilityRules().catch(() => []),
    ]).then(([services, rules]) => {
      if (!alive) return;
      setState({
        services: Array.isArray(services) ? services.length : 0,
        rules: Array.isArray(rules) ? rules.length : 0,
      });
    });
    return () => { alive = false; };
  }, []);

  // Never flash a checklist at someone mid-load, and never render one that is
  // already fully satisfied.
  if (!state) return null;

  const steps = [
    {
      key: "service",
      label: "Add a service",
      hint: "What you offer, how long it runs, and the price — you set the delivery mode at the same time",
      done: state.services > 0,
      to: "/therapist/services",
    },
    {
      key: "availability",
      label: "Set your availability",
      hint: "Bookable slots are generated from these rules — without them your schedule stays empty",
      done: state.rules > 0,
      to: "/therapist/availability-rules",
    },
    {
      key: "client",
      label: "Add your first client",
      hint: "Or let them arrive through your intake form",
      done: activeClients > 0,
      to: "/therapist/clients",
    },
  ];

  const remaining = steps.filter(s => !s.done);
  if (remaining.length === 0) return null;

  const doneCount = steps.length - remaining.length;

  return (
    <div className={`card ${styles.wrap} reveal d1`}>
      <div className={styles.head}>
        <div>
          <div className="eyebrow">Getting started</div>
          <h2 className={styles.title}>Finish setting up your practice</h2>
          <p className={styles.sub}>
            {doneCount} of {steps.length} done — complete these and you can start booking sessions.
          </p>
        </div>
        <span className="chip chip-warn">{remaining.length} left</span>
      </div>

      <ol className={styles.steps}>
        {steps.map((step, i) => (
          <li key={step.key} className={`${styles.step} ${step.done ? styles.stepDone : ""}`}>
            <span className={styles.marker} aria-hidden="true">
              {step.done ? <Icon name="check" size={14} /> : i + 1}
            </span>
            <span className={styles.stepText}>
              <b>{step.label}</b>
              <span>{step.hint}</span>
            </span>
            {step.done ? (
              <span className={`chip chip-ok ${styles.stepChip}`}>Done</span>
            ) : (
              <button className="btn btn-sm" onClick={() => navigate(step.to)}>
                Set up <Icon name="chevron" size={14} />
              </button>
            )}
          </li>
        ))}
      </ol>
    </div>
  );
}
