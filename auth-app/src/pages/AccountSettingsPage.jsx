import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { getAccount, updateAccount, changePassword } from "../api/user";
import { updateTherapistEmail } from "../api/therapistProfile";
import { getTheme, setTheme } from "../theme";
import styles from "./AccountSettingsPage.module.css";

// mirror of the backend rule in UserService.validateUsername
const USERNAME_RE = /^[A-Za-z0-9._-]{3,30}$/;
const USERNAME_RULES =
  "Username must be 3-30 characters using only letters, numbers, dots, dashes or underscores (no spaces).";
const MIN_PASSWORD_LENGTH = 8;

const SECTIONS = [
  { id: "profile", icon: "👤", label: "Profile" },
  { id: "security", icon: "🔒", label: "Security" },
  { id: "preferences", icon: "🎨", label: "Preferences" },
];

function formatDateTime(iso) {
  if (!iso) return "—";
  try { return new Date(iso).toLocaleString(); } catch { return iso; }
}

function formatDate(iso) {
  if (!iso) return "—";
  try {
    return new Date(iso).toLocaleDateString(undefined, { day: "numeric", month: "long", year: "numeric" });
  } catch { return iso; }
}

function formatCountdown(ms) {
  if (ms <= 0) return "Expired";
  const secs = Math.round(ms / 1000);
  const pad = (n) => String(n).padStart(2, "0");
  return `${pad(Math.floor(secs / 60))}:${pad(secs % 60)}`;
}

// coarse, dependency-free UA description — enough for "was that me?"
function describeDevice(ua) {
  if (!ua) return "Unknown device";
  const browser =
    ua.includes("Edg/") ? "Edge" :
    ua.includes("OPR/") ? "Opera" :
    ua.includes("Chrome/") ? "Chrome" :
    ua.includes("Firefox/") ? "Firefox" :
    ua.includes("Safari/") ? "Safari" : "Browser";
  const os =
    ua.includes("Windows") ? "Windows" :
    ua.includes("Android") ? "Android" :
    ua.includes("iPhone") || ua.includes("iPad") ? "iOS" :
    ua.includes("Mac OS") ? "macOS" :
    ua.includes("Linux") ? "Linux" : "";
  return os ? `${browser} on ${os}` : browser;
}

export default function AccountSettingsPage() {
  const navigate = useNavigate();
  const { user, role, sessionExpiresAt, staySignedIn } = useAuth();

  const [activeSection, setActiveSection] = useState("profile");

  // live countdown for the "This session" card — only ticks while Security is open
  const [nowMs, setNowMs] = useState(() => Date.now());
  const [extending, setExtending] = useState(false);
  useEffect(() => {
    if (activeSection !== "security") return;
    const id = setInterval(() => setNowMs(Date.now()), 1000);
    return () => clearInterval(id);
  }, [activeSection]);

  const handleExtend = async () => {
    setExtending(true);
    try { await staySignedIn(); } finally { setExtending(false); }
  };

  // theme preference — applied instantly, persisted per browser
  const [theme, setThemeState] = useState(getTheme());
  const chooseTheme = (t) => { setTheme(t); setThemeState(t); };

  // current values from the server — everything is read-only until Edit
  const [account, setAccount] = useState(null);
  const [accountError, setAccountError] = useState(null);

  // ── profile edit state ──
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({ username: "", email: "", currentPassword: "" });
  const [showPassword, setShowPassword] = useState(false);
  // anti-autofill: the password input renders readOnly and unlocks on focus —
  // Chrome never injects saved credentials into read-only fields, which is
  // the only reliable way to stop it treating username+password as a login
  // form (autocomplete hints alone are ignored)
  const [pwUnlocked, setPwUnlocked] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // ── change password state ──
  const [pwForm, setPwForm] = useState({ currentPassword: "", newPassword: "", confirmPassword: "" });
  const [showPw, setShowPw] = useState(false);
  const [pwLoading, setPwLoading] = useState(false);
  const [pwError, setPwError] = useState(null);
  const [pwSuccess, setPwSuccess] = useState(false);

  useEffect(() => {
    getAccount()
      .then(setAccount)
      .catch((e) => setAccountError(e.message));
  }, []);

  // ── profile handlers ──
  const startEdit = () => {
    setForm({
      username: account?.username || user?.username || "",
      email: account?.email || "",
      currentPassword: "",
    });
    setPwUnlocked(false);
    setError(null);
    setSuccess(null);
    setEditing(true);
  };

  const cancelEdit = () => {
    setEditing(false);
    setError(null);
  };

  const handleChange = (e) => setForm(prev => ({ ...prev, [e.target.name]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.currentPassword) { setError("Current password is required to confirm changes."); return; }
    const username = form.username.trim();
    const email = form.email.trim();
    const usernameChanged = username && username !== account?.username;
    const emailChanged = email && email !== account?.email;
    if (!usernameChanged && !emailChanged) {
      setError("Nothing changed — modify the username or email, or cancel.");
      return;
    }
    if (usernameChanged && !USERNAME_RE.test(username)) { setError(USERNAME_RULES); return; }
    setLoading(true); setError(null); setSuccess(null);
    try {
      const updated = await updateAccount({
        currentUsername: account?.username || user?.username,
        username: usernameChanged ? username : undefined,
        email: emailChanged ? email : undefined,
        currentPassword: form.currentPassword,
      });
      // the therapist's invite/contact email mirrors the account email —
      // sync it so calendar invites follow the change
      if (emailChanged && role === "THERAPIST") {
        try {
          await updateTherapistEmail(email);
        } catch (syncErr) {
          setAccount(prev => ({ ...prev, ...updated }));
          setError(
            "Account email was updated, but syncing the calendar-invite email failed (" +
            syncErr.message + "). Edit and save the same email again to retry the sync."
          );
          setForm(prev => ({ ...prev, currentPassword: "" }));
          return;
        }
      }
      setAccount(prev => ({ ...prev, ...updated }));
      setEditing(false);
      setSuccess(usernameChanged
        ? "Account updated. Use your new username the next time you sign in."
        : "Account updated successfully!");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // ── password handlers ──
  const handlePwChange = (e) => setPwForm(prev => ({ ...prev, [e.target.name]: e.target.value }));

  const handlePwSubmit = async (e) => {
    e.preventDefault();
    setPwSuccess(false);
    if (!pwForm.currentPassword) { setPwError("Enter your current password."); return; }
    if (pwForm.newPassword.length < MIN_PASSWORD_LENGTH) {
      setPwError(`New password must be at least ${MIN_PASSWORD_LENGTH} characters.`); return;
    }
    if (pwForm.newPassword !== pwForm.confirmPassword) {
      setPwError("New password and confirmation do not match."); return;
    }
    setPwLoading(true); setPwError(null);
    try {
      await changePassword(pwForm.currentPassword, pwForm.newPassword);
      setPwForm({ currentPassword: "", newPassword: "", confirmPassword: "" });
      setPwSuccess(true);
    } catch (err) {
      setPwError(err.message);
    } finally {
      setPwLoading(false);
    }
  };

  const switchSection = (id) => {
    setActiveSection(id);
    setSuccess(null);
    setPwSuccess(false);
  };

  // ── sections ──
  const renderProfile = () => (
    <>
      <h2 className={styles.sectionTitle}>Profile</h2>
      <p className={styles.sectionSub}>
        {editing
          ? "Modify your details below. Your current password is required to confirm the changes."
          : "Your account details. Use Edit to change your username or email."}
      </p>

      {!editing ? (
        <div className={styles.viewList}>
          <div className={styles.viewRow}>
            <span className={styles.label}>Username</span>
            <span className={styles.viewValue}>{account?.username ?? user?.username ?? "—"}</span>
          </div>
          <div className={styles.viewRow}>
            <span className={styles.label}>Email</span>
            <span className={styles.viewValue}>
              {account ? (account.email || "—") : accountError ? "unavailable" : "Loading…"}
            </span>
            {role === "THERAPIST" && (
              <span className={styles.hint}>Also used for your calendar invites</span>
            )}
          </div>
          <div className={styles.viewRow}>
            <span className={styles.label}>Role</span>
            <span className={styles.viewValue}>{account?.userRole ?? role ?? "—"}</span>
          </div>
          <div className={styles.viewRow}>
            <span className={styles.label}>Member since</span>
            <span className={styles.viewValue}>{formatDate(account?.createdAt)}</span>
          </div>

          {accountError && (
            <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{accountError}</div>
          )}
          {success && (
            <div className={styles.successBox}><span className={styles.successIcon}>✓</span> {success}</div>
          )}

          <div className={styles.actions}>
            <button type="button" className={styles.submitBtn} onClick={startEdit} disabled={!account}>
              ✎ Edit details
            </button>
          </div>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.field}>
            <label className={styles.label}>Username</label>
            <input name="username" type="text" autoComplete="off"
              value={form.username} onChange={handleChange} className={styles.input} />
            <span className={styles.hint}>3–30 characters — letters, numbers, dots, dashes or underscores</span>
          </div>

          <div className={styles.field}>
            <label className={styles.label}>Email</label>
            <input name="email" type="email" autoComplete="off"
              value={form.email} onChange={handleChange} className={styles.input} />
            {role === "THERAPIST" && (
              <span className={styles.hint}>Also used for your calendar invites — both update together</span>
            )}
          </div>

          <div className={styles.divider}/>

          <div className={styles.field}>
            <label className={styles.label}>Current password <span className={styles.required}>*</span></label>
            <div className={styles.inputWrapper}>
              <input name="confirmIdentity" type={showPassword ? "text" : "password"}
                autoComplete="one-time-code" required
                readOnly={!pwUnlocked}
                onFocus={() => setPwUnlocked(true)}
                value={form.currentPassword}
                onChange={(e) => setForm(prev => ({ ...prev, currentPassword: e.target.value }))}
                className={styles.input} placeholder="••••••••" />
              <button type="button" className={styles.toggle} onClick={() => setShowPassword(s => !s)}>
                {showPassword ? "Hide" : "Show"}
              </button>
            </div>
          </div>

          {error && (
            <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{error}</div>
          )}

          <div className={styles.actions}>
            <button type="button" className={styles.cancelBtn} onClick={cancelEdit} disabled={loading}>Cancel</button>
            <button type="submit" className={styles.submitBtn} disabled={loading}>
              {loading ? <span className={styles.btnSpinner}/> : "Save changes"}
            </button>
          </div>
        </form>
      )}
    </>
  );

  const renderSecurity = () => (
    <>
      <h2 className={styles.sectionTitle}>Security</h2>
      <p className={styles.sectionSub}>Your current session, sign-in activity and password.</p>

      <div className={styles.subCard}>
        <h3 className={styles.subCardTitle}>This session</h3>
        <div className={styles.sessionRow}>
          <div className={styles.metaItem}>
            <span className={styles.label}>Signs you out in</span>
            <span className={styles.sessionClock}>
              {sessionExpiresAt ? formatCountdown(sessionExpiresAt - nowMs) : "—"}
            </span>
          </div>
          <button type="button" className={styles.extendBtn} onClick={handleExtend} disabled={extending}>
            {extending ? "Extending…" : "Extend session"}
          </button>
        </div>
        <span className={styles.hint}>
          Extending renews it without signing you out. A reminder appears automatically as the
          time runs low.
        </span>
      </div>

      <div className={styles.subCard}>
        <h3 className={styles.subCardTitle}>Last sign-in</h3>
        <div className={styles.metaGrid}>
          <div className={styles.metaItem}>
            <span className={styles.label}>When</span>
            <span className={styles.viewValue}>{formatDateTime(account?.lastLoginTime)}</span>
          </div>
          <div className={styles.metaItem}>
            <span className={styles.label}>IP address</span>
            <span className={styles.viewValue}>{account?.lastLoginIp || "—"}</span>
          </div>
          <div className={styles.metaItem}>
            <span className={styles.label}>Device</span>
            <span className={styles.viewValue} title={account?.lastLoginUserAgent || ""}>
              {describeDevice(account?.lastLoginUserAgent)}
            </span>
          </div>
        </div>
        <span className={styles.hint}>If this doesn't look like you, change your password below.</span>
      </div>

      <div className={styles.subCard}>
        <h3 className={styles.subCardTitle}>Change password</h3>
        <form onSubmit={handlePwSubmit} className={styles.form}>
          <div className={styles.field}>
            <label className={styles.label}>Current password <span className={styles.required}>*</span></label>
            <div className={styles.inputWrapper}>
              <input name="currentPassword" type={showPw ? "text" : "password"}
                autoComplete="current-password" required
                value={pwForm.currentPassword} onChange={handlePwChange}
                className={styles.input} placeholder="••••••••" />
              <button type="button" className={styles.toggle} onClick={() => setShowPw(s => !s)}>
                {showPw ? "Hide" : "Show"}
              </button>
            </div>
          </div>
          <div className={styles.field}>
            <label className={styles.label}>New password <span className={styles.required}>*</span></label>
            <input name="newPassword" type={showPw ? "text" : "password"}
              autoComplete="new-password" required
              value={pwForm.newPassword} onChange={handlePwChange}
              className={styles.input} placeholder="At least 8 characters" />
          </div>
          <div className={styles.field}>
            <label className={styles.label}>Confirm new password <span className={styles.required}>*</span></label>
            <input name="confirmPassword" type={showPw ? "text" : "password"}
              autoComplete="new-password" required
              value={pwForm.confirmPassword} onChange={handlePwChange}
              className={styles.input} placeholder="Repeat the new password" />
            {pwForm.confirmPassword && pwForm.newPassword !== pwForm.confirmPassword && (
              <span className={styles.mismatch}>Passwords do not match</span>
            )}
          </div>

          {pwError && (
            <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{pwError}</div>
          )}
          {pwSuccess && (
            <div className={styles.successBox}><span className={styles.successIcon}>✓</span> Password changed successfully.</div>
          )}

          <div className={styles.actions}>
            <button type="submit" className={styles.submitBtn} disabled={pwLoading}>
              {pwLoading ? <span className={styles.btnSpinner}/> : "Change password"}
            </button>
          </div>
        </form>
      </div>
    </>
  );

  const renderPreferences = () => (
    <>
      <h2 className={styles.sectionTitle}>Preferences</h2>
      <p className={styles.sectionSub}>Display options.</p>

      <div className={styles.subCard}>
        <h3 className={styles.subCardTitle}>Theme</h3>
        <div className={styles.themeRow}>
          <button type="button"
            className={`${styles.themeOption} ${theme === "dark" ? styles.themeOptionActive : ""}`}
            onClick={() => chooseTheme("dark")}>
            🌙 Dark
          </button>
          <button type="button"
            className={`${styles.themeOption} ${theme === "light" ? styles.themeOptionActive : ""}`}
            onClick={() => chooseTheme("light")}>
            ☀️ Light
          </button>
        </div>
        <span className={styles.hint}>Applies immediately and is remembered on this browser.</span>
      </div>
    </>
  );

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <button className={styles.back} onClick={() => navigate(-1)}>← Back</button>
          <span className={styles.logo}>🧠 Therapy Connect</span>
          <span className={styles.rolePill}>{role || user?.role || "User"}</span>
        </div>
      </header>

      <main className={styles.main}>
        <div className={styles.hero}>
          <h1 className={styles.heading}>Account Settings</h1>
        </div>

        <div className={styles.settingsGrid}>
          <nav className={styles.sideNav}>
            {SECTIONS.map((s) => (
              <button
                key={s.id}
                type="button"
                className={`${styles.navItem} ${activeSection === s.id ? styles.navItemActive : ""}`}
                onClick={() => switchSection(s.id)}
              >
                <span className={styles.navIcon}>{s.icon}</span> {s.label}
              </button>
            ))}
          </nav>

          <div className={styles.card}>
            {activeSection === "profile" && renderProfile()}
            {activeSection === "security" && renderSecurity()}
            {activeSection === "preferences" && renderPreferences()}
          </div>
        </div>
      </main>
    </div>
  );
}
