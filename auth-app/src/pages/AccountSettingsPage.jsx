import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { getAccount, updateAccount } from "../api/user";
import { updateTherapistEmail } from "../api/therapistProfile";
import styles from "./AccountSettingsPage.module.css";

// mirror of the backend rule in UserService.validateUsername
const USERNAME_RE = /^[A-Za-z0-9._-]{3,30}$/;
const USERNAME_RULES =
  "Username must be 3-30 characters using only letters, numbers, dots, dashes or underscores (no spaces).";

export default function AccountSettingsPage() {
  const navigate = useNavigate();
  const { user, role } = useAuth();

  // current values from the server — the page is read-only until Edit
  const [account, setAccount] = useState(null); // { username, email, userRole }
  const [accountError, setAccountError] = useState(null);

  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({ username: "", email: "", currentPassword: "" });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  useEffect(() => {
    getAccount()
      .then(setAccount)
      .catch((e) => setAccountError(e.message));
  }, []);

  const startEdit = () => {
    setForm({
      username: account?.username || user?.username || "",
      email: account?.email || "",
      currentPassword: "",
    });
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
          setAccount(updated);
          setError(
            "Account email was updated, but syncing the calendar-invite email failed (" +
            syncErr.message + "). Edit and save the same email again to retry the sync."
          );
          setForm(prev => ({ ...prev, currentPassword: "" }));
          return;
        }
      }
      setAccount(updated);
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
        <div className={styles.content}>
          <div className={styles.hero}>
            <h1 className={styles.heading}>Account Settings</h1>
            <p className={styles.sub}>
              {editing
                ? "Modify your details below. Your current password is required to confirm the changes."
                : "Your account details. Use Edit to change your username or email."}
            </p>
          </div>

          <div className={styles.card}>
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
                </div>
                <div className={styles.viewRow}>
                  <span className={styles.label}>Role</span>
                  <span className={styles.viewValue}>{account?.userRole ?? role ?? "—"}</span>
                </div>

                {accountError && (
                  <div className={styles.errorBox}>
                    <span className={styles.errorIcon}>!</span>{accountError}
                  </div>
                )}
                {success && (
                  <div className={styles.successBox}>
                    <span className={styles.successIcon}>✓</span> {success}
                  </div>
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
                  <input
                    name="username"
                    type="text"
                    autoComplete="off"
                    value={form.username}
                    onChange={handleChange}
                    className={styles.input}
                  />
                  <span className={styles.hint}>3–30 characters — letters, numbers, dots, dashes or underscores</span>
                </div>

                <div className={styles.field}>
                  <label className={styles.label}>Email</label>
                  <input
                    name="email"
                    type="email"
                    autoComplete="off"
                    value={form.email}
                    onChange={handleChange}
                    className={styles.input}
                  />
                  {role === "THERAPIST" && (
                    <span className={styles.hint}>Also used for your calendar invites — both update together</span>
                  )}
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

                <div className={styles.actions}>
                  <button type="button" className={styles.cancelBtn} onClick={cancelEdit} disabled={loading}>Cancel</button>
                  <button type="submit" className={styles.submitBtn} disabled={loading}>
                    {loading ? <span className={styles.btnSpinner}/> : "Save changes"}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}
