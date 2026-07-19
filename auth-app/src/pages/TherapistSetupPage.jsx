import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { createTherapistProfile } from "../api/therapistProfile";
import { getAccount } from "../api/user";
import styles from "./TherapistProfilePage.module.css";
import cal from "./TherapistSetupPage.module.css";

const MONTHS = ["January","February","March","April","May","June",
                "July","August","September","October","November","December"];
const GENDERS = ["Male","Female","Non-binary","Prefer not to say"];
const TODAY = new Date();
const CURRENT_YEAR = TODAY.getFullYear();

function getDaysInMonth(year, month) {
  return new Date(year, month + 1, 0).getDate();
}

function getFirstDayOfWeek(year, month) {
  const d = new Date(year, month, 1).getDay();
  return (d + 6) % 7; // shift so Monday = 0
}

function formatDob(d) {
  if (!d) return "";
  return d.toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" });
}

export default function TherapistSetupPage() {
  const { completeSetup, logout } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    firstName: "", lastName: "", email: "", phoneNumber: "",
    dob: null,
    gender: "", yearsOfExperience: "",
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Calendar
  const [calOpen, setCalOpen] = useState(false);
  const [calYear, setCalYear] = useState(CURRENT_YEAR - 30);
  const [calMonth, setCalMonth] = useState(0);
  const calRef = useRef(null);

  // pre-fill from the account email so the profile (invite) email starts
  // identical to the login email instead of being typed twice
  useEffect(() => {
    getAccount()
      .then((account) => {
        setForm((prev) => (prev.email ? prev : { ...prev, email: account.email || "" }));
      })
      .catch(() => {}); // prefill is best-effort — the field stays editable
  }, []);

  useEffect(() => {
    if (!calOpen) return;
    const handler = (e) => {
      if (calRef.current && !calRef.current.contains(e.target)) setCalOpen(false);
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [calOpen]);

  const handleChange = (e) =>
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));

  const selectDate = (day) => {
    const date = new Date(calYear, calMonth, day);
    if (date > TODAY) return;
    setForm((prev) => ({ ...prev, dob: date }));
    setCalOpen(false);
  };

  const prevMonth = () => {
    if (calMonth === 0) { setCalMonth(11); setCalYear(y => y - 1); }
    else setCalMonth(m => m - 1);
  };
  const nextMonth = () => {
    if (calMonth === 11) { setCalMonth(0); setCalYear(y => y + 1); }
    else setCalMonth(m => m + 1);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const { dob, gender, yearsOfExperience, ...rest } = form;
      await createTherapistProfile({
        ...rest,
        gender,
        dob: dob ? dob.toISOString() : null,
        yearsOfExperience: parseInt(yearsOfExperience, 10) || 0,
      });
      await completeSetup();
      navigate("/therapist-home", { replace: true });
    } catch (err) {
      setError(err.message || "Failed to create profile. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const daysInMonth = getDaysInMonth(calYear, calMonth);
  const firstDay = getFirstDayOfWeek(calYear, calMonth);
  const yearOptions = Array.from({ length: CURRENT_YEAR - 1939 }, (_, i) => CURRENT_YEAR - i);

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <span className={styles.logo}>🧠 Therapy Connect</span>
          <span className={styles.rolePill}>Therapist</span>
        </div>
      </header>

      <main className={styles.main}>
        <div className={styles.formWrap}>
          <div className={styles.formHeader}>
            <h2 className={styles.formTitle}>Complete your profile</h2>
            <p className={styles.formSub}>Before you can use the platform, we need a few details about you.</p>
          </div>

          <form onSubmit={handleSubmit} className={styles.form}>

            {/* Name */}
            <div className={styles.formRow}>
              <div className={styles.field}>
                <label className={styles.label} htmlFor="firstName">First Name *</label>
                <input id="firstName" name="firstName" type="text" required
                  value={form.firstName} onChange={handleChange}
                  className={styles.input} placeholder="Jane" />
              </div>
              <div className={styles.field}>
                <label className={styles.label} htmlFor="lastName">Last Name *</label>
                <input id="lastName" name="lastName" type="text" required
                  value={form.lastName} onChange={handleChange}
                  className={styles.input} placeholder="Smith" />
              </div>
            </div>

            {/* Contact */}
            <div className={styles.formRow}>
              <div className={styles.field}>
                <label className={styles.label} htmlFor="email">Email</label>
                <input id="email" name="email" type="email"
                  value={form.email} onChange={handleChange}
                  className={styles.input} placeholder="you@example.com" />
              </div>
              <div className={styles.field}>
                <label className={styles.label} htmlFor="phoneNumber">Phone Number</label>
                <input id="phoneNumber" name="phoneNumber" type="tel"
                  value={form.phoneNumber} onChange={handleChange}
                  className={styles.input} placeholder="+91 98765 43210" />
              </div>
            </div>

            {/* DOB + Experience */}
            <div className={styles.formRow}>
              <div className={styles.field}>
                <label className={styles.label}>Date of Birth</label>
                <div className={cal.calWrap} ref={calRef}>
                  <input
                    type="text"
                    readOnly
                    value={formatDob(form.dob)}
                    placeholder="Select date"
                    className={styles.input}
                    style={{ cursor: "pointer" }}
                    onClick={() => setCalOpen(o => !o)}
                  />
                  {calOpen && (
                    <div className={cal.calPopover}>
                      <div className={cal.calHeader}>
                        <button type="button" className={cal.calNav} onClick={prevMonth}>
                          <ChevronLeft size={14} />
                        </button>
                        <div className={cal.calMonthYear}>
                          <select className={cal.calSelect}
                            value={calMonth}
                            onChange={e => setCalMonth(Number(e.target.value))}>
                            {MONTHS.map((m, i) => <option key={m} value={i}>{m}</option>)}
                          </select>
                          <select className={cal.calSelect}
                            value={calYear}
                            onChange={e => setCalYear(Number(e.target.value))}>
                            {yearOptions.map(y => <option key={y} value={y}>{y}</option>)}
                          </select>
                        </div>
                        <button type="button" className={cal.calNav} onClick={nextMonth}>
                          <ChevronRight size={14} />
                        </button>
                      </div>

                      <div className={cal.calGrid}>
                        {["Mo","Tu","We","Th","Fr","Sa","Su"].map(d => (
                          <div key={d} className={cal.calDayName}>{d}</div>
                        ))}
                        {Array.from({ length: firstDay }).map((_, i) => <div key={`e${i}`} />)}
                        {Array.from({ length: daysInMonth }, (_, i) => i + 1).map(day => {
                          const date = new Date(calYear, calMonth, day);
                          const isFuture = date > TODAY;
                          const isSelected = form.dob &&
                            form.dob.getDate() === day &&
                            form.dob.getMonth() === calMonth &&
                            form.dob.getFullYear() === calYear;
                          return (
                            <button
                              key={day}
                              type="button"
                              disabled={isFuture}
                              onClick={() => selectDate(day)}
                              className={`${cal.calDay} ${isSelected ? cal.calDaySelected : ""}`}
                            >
                              {day}
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  )}
                </div>
              </div>

              <div className={styles.field}>
                <label className={styles.label} htmlFor="yearsOfExperience">Years of Experience</label>
                <input id="yearsOfExperience" name="yearsOfExperience" type="text"
                  inputMode="numeric" maxLength={2}
                  value={form.yearsOfExperience} onChange={handleChange}
                  className={styles.input} placeholder="e.g. 5" />
              </div>
            </div>

            {/* Gender — toggle buttons, fully CSS-controlled */}
            <div className={styles.field}>
              <label className={styles.label}>Gender</label>
              <div className={styles.optionGroup}>
                {GENDERS.map((g) => (
                  <button
                    key={g}
                    type="button"
                    className={`${styles.optionBtn} ${form.gender === g ? styles.optionBtnActive : ""}`}
                    onClick={() => setForm((prev) => ({ ...prev, gender: g }))}
                  >
                    {g}
                  </button>
                ))}
              </div>
            </div>

            {error && (
              <div className={styles.errorBox}>
                <span className={styles.errorIcon}>!</span>{error}
              </div>
            )}

            <div className={styles.formActions}>
              <button type="button" className={styles.cancelBtn} onClick={() => logout()}>
                Sign out
              </button>
              <button type="submit" className={styles.submitBtn} disabled={loading}>
                {loading ? <span className={styles.btnSpinner} /> : "Complete setup"}
              </button>
            </div>

          </form>
        </div>
      </main>
    </div>
  );
}
