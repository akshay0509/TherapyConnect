import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { createTherapistProfile } from "../api/therapistProfile";
import styles from "./TherapistProfilePage.module.css";

export default function TherapistSetupPage() {
  const { completeSetup, logout } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    firstName: "", lastName: "", email: "", phoneNumber: "",
    dobDay: "", dobMonth: "", dobYear: "",
    gender: "", yearsOfExperience: "",
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleChange = (e) =>
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const { dobDay, dobMonth, dobYear, gender, yearsOfExperience, ...rest } = form;
      const dob = dobDay && dobMonth && dobYear
        ? new Date(`${dobYear}-${String(dobMonth).padStart(2, "0")}-${String(dobDay).padStart(2, "0")}`).toISOString()
        : null;

      await createTherapistProfile({
        ...rest,
        gender,
        dob,
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

            {/* Date of Birth — text inputs, no spinner arrows */}
            <div className={styles.formRow}>
              <div className={styles.field}>
                <label className={styles.label}>Date of Birth</label>
                <div className={styles.dobRow}>
                  <input name="dobDay" type="text" inputMode="numeric"
                    maxLength={2} value={form.dobDay} onChange={handleChange}
                    className={`${styles.input} ${styles.dobNarrow}`}
                    placeholder="DD" />
                  <input name="dobMonth" type="text" inputMode="numeric"
                    maxLength={2} value={form.dobMonth} onChange={handleChange}
                    className={`${styles.input} ${styles.dobNarrow}`}
                    placeholder="MM" />
                  <input name="dobYear" type="text" inputMode="numeric"
                    maxLength={4} value={form.dobYear} onChange={handleChange}
                    className={`${styles.input} ${styles.dobWide}`}
                    placeholder="YYYY" />
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

            {/* Gender — styled select, no native OS popup issues */}
            <div className={styles.formRow}>
              <div className={styles.field}>
                <label className={styles.label} htmlFor="gender">Gender</label>
                <select id="gender" name="gender"
                  value={form.gender} onChange={handleChange}
                  className={`${styles.input} ${styles.select}`}>
                  <option value="">Select gender</option>
                  <option value="Male">Male</option>
                  <option value="Female">Female</option>
                  <option value="Non-binary">Non-binary</option>
                  <option value="Prefer not to say">Prefer not to say</option>
                </select>
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
