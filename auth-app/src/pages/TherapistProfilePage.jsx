import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getTherapistProfile, createTherapistProfile } from "../api/therapistProfile";
import styles from "./TherapistProfilePage.module.css";

function getInitials(firstName, lastName) {
  return `${firstName?.[0] ?? ""}${lastName?.[0] ?? ""}`.toUpperCase();
}

function formatDate(dob) {
  if (!dob) return "—";
  return new Date(dob).toLocaleDateString("en-GB", {
    day: "2-digit", month: "long", year: "numeric",
  });
}

const EMPTY_FORM = {
  firstName: "",
  lastName: "",
  dob: "",
  phoneNumber: "",
  email: "",
  gender: "",
  yearsOfExperience: "",
  timezone: "",
};

export default function TherapistProfilePage() {
  const navigate = useNavigate();

  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Create form state
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState(null);
  const [formLoading, setFormLoading] = useState(false);
  const [formSuccess, setFormSuccess] = useState(false);

  useEffect(() => {
    getTherapistProfile()
      .then((data) => {
        setProfile(data);
        // If empty/null response, show create form automatically
        if (!data || Object.keys(data).length === 0) setShowForm(true);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    setFormLoading(true);
    setFormError(null);
    try {
      const payload = {
        ...form,
        yearsOfExperience: parseInt(form.yearsOfExperience, 10),
        dob: form.dob ? new Date(form.dob).toISOString() : null,
      };
      const created = await createTherapistProfile(payload);
      setProfile(created);
      setShowForm(false);
      setFormSuccess(true);
    } catch (err) {
      setFormError(err.message);
    } finally {
      setFormLoading(false);
    }
  };

  const hasProfile = profile && (profile.firstName || profile.therapistId);

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <button className={styles.back} onClick={() => navigate("/therapist-home")}>
            ← Back
          </button>
          <span className={styles.logo}>🧠 Therapy Connect</span>
          <span className={styles.rolePill}>Therapist</span>
        </div>
      </header>

      <main className={styles.main}>

        {/* ── Loading ── */}
        {loading && (
          <div className={styles.center}>
            <div className={styles.spinner} />
            <p className={styles.loadingText}>Loading your profile…</p>
          </div>
        )}

        {/* ── Fetch error ── */}
        {!loading && error && (
          <div className={styles.errorBox}>
            <span className={styles.errorIcon}>!</span>{error}
          </div>
        )}

        {/* ── Profile exists ── */}
        {!loading && !error && hasProfile && !showForm && (
          <div className={styles.profileWrap}>
            {formSuccess && (
              <div className={styles.successBox}>
                <span className={styles.successIcon}>✓</span>
                Profile created successfully!
              </div>
            )}

            <div className={styles.profileCard}>
              <div className={styles.profileTop}>
                <div className={styles.bigAvatar}>
                  {getInitials(profile.firstName, profile.lastName)}
                </div>
                <div>
                  <h1 className={styles.profileName}>
                    {profile.firstName} {profile.lastName}
                  </h1>
                  <p className={styles.profileMeta}>
                    {profile.yearsOfExperience} yrs experience &nbsp;·&nbsp; {profile.gender ?? "—"}
                  </p>
                </div>
              </div>

              <div className={styles.profileDivider} />

              <div className={styles.profileGrid}>
                <div className={styles.profileField}>
                  <span className={styles.fieldLabel}>Email</span>
                  <span className={styles.fieldValue}>{profile.email ?? "—"}</span>
                </div>
                <div className={styles.profileField}>
                  <span className={styles.fieldLabel}>Phone</span>
                  <span className={styles.fieldValue}>{profile.phoneNumber ?? "—"}</span>
                </div>
                <div className={styles.profileField}>
                  <span className={styles.fieldLabel}>Date of Birth</span>
                  <span className={styles.fieldValue}>{formatDate(profile.dob)}</span>
                </div>
                <div className={styles.profileField}>
                  <span className={styles.fieldLabel}>Therapist ID</span>
                  <span className={`${styles.fieldValue} ${styles.mono}`}>{profile.therapistId ?? "—"}</span>
                </div>
                <div className={styles.profileField}>
                  <span className={styles.fieldLabel}>Timezone</span>
                  <span className={styles.fieldValue}>{profile.timezone ?? "—"}</span>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ── No profile: prompt ── */}
        {!loading && !error && !hasProfile && !showForm && (
          <div className={styles.center}>
            <div className={styles.emptyIcon}>🧑‍⚕️</div>
            <h2 className={styles.emptyTitle}>No profile found</h2>
            <p className={styles.emptyText}>Set up your therapist profile to get started.</p>
            <button className={styles.createBtn} onClick={() => setShowForm(true)}>
              Create Profile
            </button>
          </div>
        )}

        {/* ── Create form ── */}
        {!loading && showForm && (
          <div className={styles.formWrap}>
            <div className={styles.formHeader}>
              <h2 className={styles.formTitle}>Create your profile</h2>
              <p className={styles.formSub}>Fill in your details to set up your therapist profile.</p>
            </div>

            <form onSubmit={handleCreate} className={styles.form}>
              <div className={styles.formRow}>
                <div className={styles.field}>
                  <label className={styles.label} htmlFor="firstName">First Name</label>
                  <input id="firstName" name="firstName" type="text" required
                    value={form.firstName} onChange={handleChange}
                    className={styles.input} placeholder="John" />
                </div>
                <div className={styles.field}>
                  <label className={styles.label} htmlFor="lastName">Last Name</label>
                  <input id="lastName" name="lastName" type="text" required
                    value={form.lastName} onChange={handleChange}
                    className={styles.input} placeholder="Doe" />
                </div>
              </div>

              <div className={styles.formRow}>
                <div className={styles.field}>
                  <label className={styles.label} htmlFor="email">Email</label>
                  <input id="email" name="email" type="email" required
                    value={form.email} onChange={handleChange}
                    className={styles.input} placeholder="you@example.com" />
                </div>
                <div className={styles.field}>
                  <label className={styles.label} htmlFor="phoneNumber">Phone Number</label>
                  <input id="phoneNumber" name="phoneNumber" type="tel" required
                    value={form.phoneNumber} onChange={handleChange}
                    className={styles.input} placeholder="+91 98765 43210" />
                </div>
              </div>

              <div className={styles.formRow}>
                <div className={styles.field}>
                  <label className={styles.label} htmlFor="dob">Date of Birth</label>
                  <input id="dob" name="dob" type="date" required
                    value={form.dob} onChange={handleChange}
                    className={`${styles.input} ${styles.dateInput}`}
                    max={new Date().toISOString().split("T")[0]} />
                </div>
                <div className={styles.field}>
                  <label className={styles.label} htmlFor="gender">Gender</label>
                  <select id="gender" name="gender" required
                    value={form.gender} onChange={handleChange}
                    className={`${styles.input} ${styles.select}`}>
                    <option value="" disabled>Select gender</option>
                    <option value="Male">Male</option>
                    <option value="Female">Female</option>
                    <option value="Other">Other</option>
                  </select>
                </div>
              </div>

              <div className={styles.formRow}>
                <div className={styles.field}>
                  <label className={styles.label} htmlFor="yearsOfExperience">Years of Experience</label>
                  <input id="yearsOfExperience" name="yearsOfExperience" type="number"
                    min="0" max="60" required
                    value={form.yearsOfExperience} onChange={handleChange}
                    className={styles.input} placeholder="e.g. 5" />
                </div>
                <div className={styles.field}>
                  <label className={styles.label} htmlFor="timezone">Timezone</label>
                  <select id="timezone" name="timezone" required
                    value={form.timezone} onChange={handleChange}
                    className={`${styles.input} ${styles.select}`}>
                    <option value="" disabled>Select timezone</option>
                    <option value="Asia/Kolkata">Asia/Kolkata (IST, UTC+5:30)</option>
                    <option value="Asia/Dubai">Asia/Dubai (GST, UTC+4)</option>
                    <option value="Asia/Singapore">Asia/Singapore (SGT, UTC+8)</option>
                    <option value="Asia/Tokyo">Asia/Tokyo (JST, UTC+9)</option>
                    <option value="Europe/London">Europe/London (GMT/BST)</option>
                    <option value="Europe/Paris">Europe/Paris (CET/CEST, UTC+1/+2)</option>
                    <option value="America/New_York">America/New_York (EST/EDT)</option>
                    <option value="America/Chicago">America/Chicago (CST/CDT)</option>
                    <option value="America/Denver">America/Denver (MST/MDT)</option>
                    <option value="America/Los_Angeles">America/Los_Angeles (PST/PDT)</option>
                    <option value="UTC">UTC</option>
                  </select>
                </div>
              </div>

              {formError && (
                <div className={styles.errorBox}>
                  <span className={styles.errorIcon}>!</span>{formError}
                </div>
              )}

              <div className={styles.formActions}>
                <button type="button" className={styles.cancelBtn}
                  onClick={() => { setShowForm(false); setFormError(null); }}>
                  Cancel
                </button>
                <button type="submit" className={styles.submitBtn} disabled={formLoading}>
                  {formLoading ? <span className={styles.btnSpinner} /> : "Create Profile"}
                </button>
              </div>
            </form>
          </div>
        )}
      </main>
    </div>
  );
}
