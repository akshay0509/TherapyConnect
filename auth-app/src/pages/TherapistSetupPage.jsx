import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { createTherapistProfile } from "../api/therapistProfile";

const s = {
  page: {
    minHeight: "100vh", background: "#080f17",
    display: "flex", alignItems: "center", justifyContent: "center",
    fontFamily: "'DM Sans', sans-serif", padding: "24px 16px",
  },
  card: {
    background: "#0f1923", border: "1px solid rgba(255,255,255,0.07)",
    borderRadius: 20, padding: "40px 36px", width: "100%", maxWidth: 520,
    boxShadow: "0 32px 80px rgba(0,0,0,0.5)",
  },
  logo: { fontSize: "2rem", marginBottom: 8 },
  title: {
    fontFamily: "'Syne', sans-serif", fontSize: "1.5rem", fontWeight: 800,
    color: "#e2e8f0", margin: "0 0 6px",
  },
  subtitle: { fontSize: "0.875rem", color: "#64748b", margin: "0 0 32px" },
  grid: { display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px 20px" },
  fullRow: { gridColumn: "1 / -1" },
  field: { display: "flex", flexDirection: "column", gap: 6 },
  label: { fontSize: "0.8rem", fontWeight: 600, color: "#94a3b8", letterSpacing: "0.04em", textTransform: "uppercase" },
  input: {
    background: "rgba(255,255,255,0.04)", border: "1px solid rgba(255,255,255,0.1)",
    borderRadius: 10, padding: "10px 14px", color: "#e2e8f0", fontSize: "0.9rem",
    outline: "none", width: "100%", boxSizing: "border-box",
  },
  select: {
    background: "rgba(255,255,255,0.04)", border: "1px solid rgba(255,255,255,0.1)",
    borderRadius: 10, padding: "10px 14px", color: "#e2e8f0", fontSize: "0.9rem",
    outline: "none", width: "100%", boxSizing: "border-box",
  },
  button: {
    marginTop: 24, width: "100%", padding: "13px", background: "rgba(245,158,11,0.15)",
    border: "1px solid rgba(245,158,11,0.35)", borderRadius: 12,
    color: "#fbbf24", fontWeight: 700, fontSize: "0.95rem",
    cursor: "pointer", fontFamily: "'DM Sans', sans-serif",
  },
  error: {
    gridColumn: "1 / -1", background: "rgba(239,68,68,0.1)", border: "1px solid rgba(239,68,68,0.25)",
    borderRadius: 10, padding: "10px 14px", color: "#fca5a5", fontSize: "0.85rem",
    display: "flex", alignItems: "center", gap: 8,
  },
  step: { fontSize: "0.78rem", color: "#475569", marginBottom: 20 },
};

export default function TherapistSetupPage() {
  const { completeSetup, logout } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    firstName: "", lastName: "", email: "", phoneNumber: "",
    dob: "", gender: "", yearsOfExperience: "",
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
      await createTherapistProfile({
        ...form,
        yearsOfExperience: parseInt(form.yearsOfExperience, 10) || 0,
        dob: form.dob || null,
      });
      // Refresh the token — server now has therapistId, refresh endpoint will pick it up
      await completeSetup();
      navigate("/therapist-home", { replace: true });
    } catch (err) {
      setError(err.message || "Failed to create profile. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={s.page}>
      <div style={s.card}>
        <div style={s.logo}>🧑‍⚕️</div>
        <h1 style={s.title}>Complete your profile</h1>
        <p style={s.subtitle}>Before you can use the platform, we need a few details about you.</p>
        <p style={s.step}>Step 1 of 1 — Therapist profile</p>

        <form onSubmit={handleSubmit}>
          <div style={s.grid}>

            <div style={s.field}>
              <label style={s.label}>First name *</label>
              <input style={s.input} name="firstName" required value={form.firstName} onChange={handleChange} placeholder="Jane" />
            </div>

            <div style={s.field}>
              <label style={s.label}>Last name *</label>
              <input style={s.input} name="lastName" required value={form.lastName} onChange={handleChange} placeholder="Smith" />
            </div>

            <div style={{ ...s.field, ...s.fullRow }}>
              <label style={s.label}>Email</label>
              <input style={s.input} name="email" type="email" value={form.email} onChange={handleChange} placeholder="you@example.com" />
            </div>

            <div style={s.field}>
              <label style={s.label}>Phone number</label>
              <input style={s.input} name="phoneNumber" value={form.phoneNumber} onChange={handleChange} placeholder="+91 98765 43210" />
            </div>

            <div style={s.field}>
              <label style={s.label}>Date of birth</label>
              <input style={s.input} name="dob" type="date" value={form.dob} onChange={handleChange} />
            </div>

            <div style={s.field}>
              <label style={s.label}>Gender</label>
              <select style={s.select} name="gender" value={form.gender} onChange={handleChange}>
                <option value="">Select</option>
                <option value="Male">Male</option>
                <option value="Female">Female</option>
                <option value="Non-binary">Non-binary</option>
                <option value="Prefer not to say">Prefer not to say</option>
              </select>
            </div>

            <div style={s.field}>
              <label style={s.label}>Years of experience</label>
              <input style={s.input} name="yearsOfExperience" type="number" min="0" max="60" value={form.yearsOfExperience} onChange={handleChange} placeholder="5" />
            </div>

            {error && (
              <div style={s.error}>
                <span>!</span> {error}
              </div>
            )}

          </div>

          <button type="submit" style={s.button} disabled={loading}>
            {loading ? "Setting up…" : "Complete setup"}
          </button>
        </form>

        <div style={{ marginTop: 16, textAlign: "center" }}>
          <button
            type="button"
            onClick={() => logout()}
            style={{ background: "none", border: "none", color: "#475569", fontSize: "0.8rem", cursor: "pointer" }}
          >
            Sign out
          </button>
        </div>
      </div>
    </div>
  );
}
