import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { updateAccount } from "../api/user";
import styles from "./AccountSettingsPage.module.css";

// mirror of the backend rule in UserService.validateUsername
const USERNAME_RE = /^[A-Za-z0-9._-]{3,30}$/;
const USERNAME_RULES =
  "Username must be 3-30 characters using only letters, numbers, dots, dashes or underscores (no spaces).";

export default function AccountSettingsPage() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const [form, setForm] = useState({
    username: user?.username || "",
    email: "",
    currentPassword: "",
  });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  const handleChange = (e) => setForm(prev => ({ ...prev, [e.target.name]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.currentPassword) { setError("Current password is required to make changes."); return; }
    if (!form.username && !form.email) { setError("Please provide a new username or email to update."); return; }
    const username = form.username.trim();
    if (username && !USERNAME_RE.test(username)) { setError(USERNAME_RULES); return; }
    setLoading(true); setError(null); setSuccess(false);
    try {
      await updateAccount({
        currentUsername: user?.username,
        username: username || undefined,
        email: form.email || undefined,
        currentPassword: form.currentPassword,
      });
      setSuccess(true);
      setForm(prev => ({ ...prev, currentPassword: "", email: "" }));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <button className={styles.back} onClick={() => navigate(-1)}>← Back</button>
          <span className={styles.logo}>🧠 Therapy Connect</span>
          <span className={styles.rolePill}>{user?.role || "User"}</span>
        </div>
      </header>

      <main className={styles.main}>
        <div className={styles.content}>
          <div className={styles.hero}>
            <h1 className={styles.heading}>Account Settings</h1>
            <p className={styles.sub}>Update your username or email. Your current password is required to confirm any changes.</p>
          </div>

          <div className={styles.card}>
            <form onSubmit={handleSubmit} className={styles.form}>
              <div className={styles.field}>
                <label className={styles.label}>Username</label>
                <input
                  name="username"
                  type="text"
                  autoComplete="username"
                  value={form.username}
                  onChange={handleChange}
                  className={styles.input}
                  placeholder="New username"
                />
                <span className={styles.hint}>Leave unchanged to keep your current username</span>
              </div>

              <div className={styles.field}>
                <label className={styles.label}>New email <span className={styles.optional}>(optional)</span></label>
                <input
                  name="email"
                  type="email"
                  autoComplete="email"
                  value={form.email}
                  onChange={handleChange}
                  className={styles.input}
                  placeholder="new@email.com"
                />
              </div>

              <div className={styles.divider}/>

              <div className={styles.field}>
                <label className={styles.label}>Current password <span className={styles.required}>*</span></label>
                <div className={styles.inputWrapper}>
                  <input
                    name="currentPassword"
                    type={showPassword ? "text" : "password"}
                    autoComplete="current-password"
                    required
                    value={form.currentPassword}
                    onChange={handleChange}
                    className={styles.input}
                    placeholder="••••••••"
                  />
                  <button type="button" className={styles.toggle} onClick={() => setShowPassword(s => !s)}>
                    {showPassword ? "Hide" : "Show"}
                  </button>
                </div>
              </div>

              {error && (
                <div className={styles.errorBox}>
                  <span className={styles.errorIcon}>!</span>{error}
                </div>
              )}
              {success && (
                <div className={styles.successBox}>
                  <span className={styles.successIcon}>✓</span> Account updated successfully!
                </div>
              )}

              <div className={styles.actions}>
                <button type="button" className={styles.cancelBtn} onClick={() => navigate(-1)}>Cancel</button>
                <button type="submit" className={styles.submitBtn} disabled={loading}>
                  {loading ? <span className={styles.btnSpinner}/> : "Save changes"}
                </button>
              </div>
            </form>
          </div>
        </div>
      </main>
    </div>
  );
}
