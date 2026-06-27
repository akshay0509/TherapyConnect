import { useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { createUserRequest, forgotPassword } from "../api/user";
import styles from "./LoginPage.module.css";

export default function LoginPage() {
  const { login, loading, error } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const sessionExpired = location.state?.sessionExpired;

  const [mode, setMode] = useState("login"); // "login" | "create" | "forgot"
  const [form, setForm] = useState({ username: "", email: "", password: "", role: "" });
  const [showPassword, setShowPassword] = useState(false);
  const [createLoading, setCreateLoading] = useState(false);
  const [createError, setCreateError] = useState(null);
  const [createSuccess, setCreateSuccess] = useState(false);

  // Forgot password
  const [forgotEmail, setForgotEmail] = useState("");
  const [forgotLoading, setForgotLoading] = useState(false);
  const [forgotError, setForgotError] = useState(null);
  const [forgotSuccess, setForgotSuccess] = useState(false);

  const handleChange = (e) =>
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));

  const switchMode = (newMode) => {
    setMode(newMode);
    setForm({ username: "", email: "", password: "", role: "" });
    setCreateError(null);
    setCreateSuccess(false);
    setShowPassword(false);
    setForgotEmail("");
    setForgotError(null);
    setForgotSuccess(false);
  };

  const handleForgotPassword = async (e) => {
    e.preventDefault();
    if (!forgotEmail) { setForgotError("Please enter your email address."); return; }
    setForgotLoading(true); setForgotError(null); setForgotSuccess(false);
    try {
      await forgotPassword(forgotEmail);
      setForgotSuccess(true);
    } catch (err) {
      setForgotError(err.message);
    } finally {
      setForgotLoading(false);
    }
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    const { success, role } = await login(form.username, form.password);
    if (success) {
      if (role === "THERAPIST") navigate("/therapist-home");
      else navigate("/"); // CLIENT or any other role
    }
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    if (!form.role) { setCreateError("Please select a role."); return; }
    setCreateLoading(true);
    setCreateError(null);
    setCreateSuccess(false);
    try {
      await createUserRequest(form.username, form.email, form.password, form.role);
      setCreateSuccess(true);
      setForm({ username: "", email: "", password: "", role: "" });
    } catch (err) {
      setCreateError(err.message);
    } finally {
      setCreateLoading(false);
    }
  };

  const isLogin = mode === "login";
  const isForgot = mode === "forgot";

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        {/* Brand */}
        <div className={styles.brand}>
          <div className={styles.logo}>⬡</div>
          <h1 className={styles.title}>
            {isLogin ? "Welcome back" : isForgot ? "Reset password" : "Create account"}
          </h1>
          <p className={styles.subtitle}>
            {isLogin ? "Sign in to your account" : isForgot ? "We'll send a reset link to your email" : "Fill in the details below"}
          </p>
        </div>

        {/* Mode tabs */}
        <div className={styles.tabs}>
          <button
            type="button"
            className={`${styles.tab} ${isLogin ? styles.tabActive : ""}`}
            onClick={() => switchMode("login")}
          >
            Sign in
          </button>
          <button
            type="button"
            className={`${styles.tab} ${!isLogin && !isForgot ? styles.tabActive : ""}`}
            onClick={() => switchMode("create")}
          >
            Create user
          </button>
        </div>

        {/* ── LOGIN FORM ── */}
        {isLogin && (
          <form onSubmit={handleLogin} className={styles.form}>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="username">Username</label>
              <input
                id="username"
                name="username"
                type="text"
                autoComplete="username"
                required
                value={form.username}
                onChange={handleChange}
                className={styles.input}
                placeholder="your username"
              />
            </div>

            <div className={styles.field}>
              <label className={styles.label} htmlFor="password">Password</label>
              <div className={styles.inputWrapper}>
                <input
                  id="password"
                  name="password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="current-password"
                  required
                  value={form.password}
                  onChange={handleChange}
                  className={styles.input}
                  placeholder="••••••••"
                />
                <button
                  type="button"
                  className={styles.toggle}
                  onClick={() => setShowPassword((s) => !s)}
                >
                  {showPassword ? "Hide" : "Show"}
                </button>
              </div>
            </div>

            {error && (
              <div className={styles.error} role="alert">
                <span className={styles.errorIcon}>!</span>
                {error}
              </div>
            )}

            <button type="submit" className={styles.button} disabled={loading}>
              {loading ? <span className={styles.spinner} /> : "Sign in"}
            </button>
            <button type="button" className={styles.forgotLink} onClick={() => switchMode("forgot")}>
              Forgot password?
            </button>
          </form>
        )}

        {/* ── CREATE USER FORM ── */}
        {!isLogin && !isForgot && (
          <form onSubmit={handleCreate} className={styles.form}>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="new-username">Username</label>
              <input
                id="new-username"
                name="username"
                type="text"
                autoComplete="off"
                required
                value={form.username}
                onChange={handleChange}
                className={styles.input}
                placeholder="choose a username"
              />
            </div>

            <div className={styles.field}>
              <label className={styles.label} htmlFor="email">Email</label>
              <input
                id="email"
                name="email"
                type="email"
                autoComplete="email"
                required
                value={form.email}
                onChange={handleChange}
                className={styles.input}
                placeholder="you@example.com"
              />
            </div>

            <div className={styles.field}>
              <label className={styles.label}>Role</label>
              <div className={styles.roleGroup}>
                {["CLIENT", "THERAPIST"].map(r => (
                  <button
                    key={r}
                    type="button"
                    className={`${styles.roleOption} ${form.role === r ? styles.roleOptionActive : ""}`}
                    onClick={() => setForm(prev => ({ ...prev, role: r }))}
                  >
                    {r === "CLIENT" ? "👤 Client" : "🧑‍⚕️ Therapist"}
                  </button>
                ))}
              </div>
            </div>

            <div className={styles.field}>
              <label className={styles.label} htmlFor="new-password">Password</label>
              <div className={styles.inputWrapper}>
                <input
                  id="new-password"
                  name="password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="new-password"
                  required
                  value={form.password}
                  onChange={handleChange}
                  className={styles.input}
                  placeholder="••••••••"
                />
                <button
                  type="button"
                  className={styles.toggle}
                  onClick={() => setShowPassword((s) => !s)}
                >
                  {showPassword ? "Hide" : "Show"}
                </button>
              </div>
            </div>

            {createError && (
              <div className={styles.error} role="alert">
                <span className={styles.errorIcon}>!</span>
                {createError}
              </div>
            )}

            {createSuccess && (
              <div className={styles.success} role="status">
                <span className={styles.successIcon}>✓</span>
                User created! You can now{" "}
                <button
                  type="button"
                  className={styles.inlineLink}
                  onClick={() => switchMode("login")}
                >
                  sign in
                </button>
                .
              </div>
            )}

            <button type="submit" className={styles.button} disabled={createLoading}>
              {createLoading ? <span className={styles.spinner} /> : "Create user"}
            </button>
          </form>
        )}
        {/* ── FORGOT PASSWORD FORM ── */}
        {isForgot && (
          <form onSubmit={handleForgotPassword} className={styles.form}>
            {!forgotSuccess ? (
              <>
                <div className={styles.field}>
                  <label className={styles.label} htmlFor="forgot-email">Email address</label>
                  <input
                    id="forgot-email"
                    type="email"
                    autoComplete="email"
                    required
                    value={forgotEmail}
                    onChange={e => setForgotEmail(e.target.value)}
                    className={styles.input}
                    placeholder="you@example.com"
                  />
                </div>
                {forgotError && (
                  <div className={styles.error} role="alert">
                    <span className={styles.errorIcon}>!</span>{forgotError}
                  </div>
                )}
                <button type="submit" className={styles.button} disabled={forgotLoading}>
                  {forgotLoading ? <span className={styles.spinner}/> : "Send reset link"}
                </button>
              </>
            ) : (
              <div className={styles.success} role="status">
                <span className={styles.successIcon}>✓</span>
                If an account with that email exists, a reset link has been sent. Check your inbox.
              </div>
            )}
            <button type="button" className={styles.forgotLink} onClick={() => switchMode("login")}>
              ← Back to sign in
            </button>
          </form>
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
