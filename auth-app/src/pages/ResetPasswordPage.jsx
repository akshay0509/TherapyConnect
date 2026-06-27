import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { resetPassword } from "../api/user";
import styles from "./LoginPage.module.css";

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");

  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!token) { setError("Invalid or missing reset token."); return; }
    if (password.length < 6) { setError("Password must be at least 6 characters."); return; }
    if (password !== confirm) { setError("Passwords do not match."); return; }
    setLoading(true); setError(null);
    try {
      await resetPassword(token, password);
      setSuccess(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <div className={styles.brand}>
          <div className={styles.logo}>⬡</div>
          <h1 className={styles.title}>Set new password</h1>
          <p className={styles.subtitle}>Enter and confirm your new password below</p>
        </div>

        {!token && (
          <div className={styles.error} role="alert">
            <span className={styles.errorIcon}>!</span>
            This reset link is invalid or has expired.
          </div>
        )}

        {token && !success && (
          <form onSubmit={handleSubmit} className={styles.form}>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="new-pass">New password</label>
              <div className={styles.inputWrapper}>
                <input
                  id="new-pass"
                  type={showPassword ? "text" : "password"}
                  autoComplete="new-password"
                  required
                  minLength={6}
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  className={styles.input}
                  placeholder="••••••••"
                />
                <button type="button" className={styles.toggle} onClick={() => setShowPassword(s => !s)}>
                  {showPassword ? "Hide" : "Show"}
                </button>
              </div>
            </div>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="confirm-pass">Confirm password</label>
              <input
                id="confirm-pass"
                type={showPassword ? "text" : "password"}
                autoComplete="new-password"
                required
                value={confirm}
                onChange={e => setConfirm(e.target.value)}
                className={styles.input}
                placeholder="••••••••"
              />
            </div>
            {error && (
              <div className={styles.error} role="alert">
                <span className={styles.errorIcon}>!</span>{error}
              </div>
            )}
            <button type="submit" className={styles.button} disabled={loading}>
              {loading ? <span className={styles.spinner}/> : "Reset password"}
            </button>
          </form>
        )}

        {success && (
          <div className={styles.form}>
            <div className={styles.success} role="status">
              <span className={styles.successIcon}>✓</span>
              Password reset successfully!
            </div>
            <button className={styles.button} onClick={() => navigate("/login")}>
              Sign in
            </button>
          </div>
        )}
      </div>

      <div className={styles.bg} aria-hidden="true">
        <div className={styles.blob1} />
        <div className={styles.blob2} />
        <div className={styles.grid} />
      </div>
    </div>
  );
}
