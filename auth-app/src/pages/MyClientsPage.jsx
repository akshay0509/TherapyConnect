import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { getTherapistClients, createClient } from "../api/therapistClients";
import styles from "./MyClientsPage.module.css";

function getInitials(name) {
  if (!name) return "?";
  const parts = name.trim().split(" ");
  return parts.length === 1
    ? parts[0][0].toUpperCase()
    : (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

const AVATAR_COLORS = [
  ["#6366f1","#4f46e5"],["#10b981","#059669"],["#f59e0b","#d97706"],
  ["#ec4899","#db2777"],["#8b5cf6","#7c3aed"],["#06b6d4","#0891b2"],["#f97316","#ea580c"],
];
function avatarGradient(id) {
  if (!id) return AVATAR_COLORS[0];
  const idx = id.charCodeAt(id.length - 1) % AVATAR_COLORS.length;
  return AVATAR_COLORS[idx];
}

const EMPTY_FORM = {
  firstName: "", lastName: "", dob: "", phoneNumber: "",
  emergencyPhoneNumber: "", email: "", pronouns: "", gender: "",
  dsf: false,
};

export default function MyClientsPage() {
  const navigate = useNavigate();

  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState("");

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formLoading, setFormLoading] = useState(false);
  const [formError, setFormError] = useState(null);

  const fetchClients = useCallback(() => {
    setLoading(true);
    setError(null);
    getTherapistClients()
      .then(setClients)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { fetchClients(); }, [fetchClients]);

  useEffect(() => {
    const handler = (e) => { if (e.key === "Escape") closeDrawer(); };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, []);

  const openDrawer = () => { setForm(EMPTY_FORM); setFormError(null); setDrawerOpen(true); };
  const closeDrawer = () => { setDrawerOpen(false); setFormError(null); };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormLoading(true);
    setFormError(null);
    try {
      const payload = {
        ...form,
        dob: form.dob ? new Date(form.dob).toISOString() : null,
        dsf: form.dsf,
      };
      await createClient(payload);
      closeDrawer();
      fetchClients(); // re-query and refresh list
    } catch (err) {
      setFormError(err.message);
    } finally {
      setFormLoading(false);
    }
  };

  const filtered = clients.filter((c) =>
    c.clientName?.toLowerCase().includes(search.toLowerCase()) ||
    c.clientId?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <button className={styles.back} onClick={() => navigate("/therapist-home")}>← Back</button>
          <span className={styles.logo}>🧠 Therapy Connect</span>
          <span className={styles.rolePill}>Therapist</span>
        </div>
      </header>

      <main className={styles.main}>
        <div className={styles.topRow}>
          <div>
            <h1 className={styles.heading}>My Clients</h1>
            <p className={styles.sub}>
              {loading ? "Loading…" : `${clients.length} client${clients.length !== 1 ? "s" : ""}`}
            </p>
          </div>
          {!loading && (
            <button className={styles.addBtn} onClick={openDrawer}>
              <span>+</span> Create Client
            </button>
          )}
        </div>

        {/* Search */}
        {!loading && !error && clients.length > 0 && (
          <div className={styles.searchWrap}>
            <span className={styles.searchIcon}>⌕</span>
            <input
              className={styles.search}
              type="text"
              placeholder="Search by name or ID…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
        )}

        {/* Loading */}
        {loading && (
          <div className={styles.center}>
            <div className={styles.spinner} />
            <p className={styles.loadingText}>Fetching clients…</p>
          </div>
        )}

        {/* Error */}
        {!loading && error && (
          <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{error}</div>
        )}

        {/* Empty */}
        {!loading && !error && clients.length === 0 && (
          <div className={styles.center}>
            <span className={styles.emptyIcon}>👥</span>
            <h2 className={styles.emptyTitle}>No clients yet</h2>
            <p className={styles.emptyText}>Create your first client to get started.</p>
            <button className={styles.addBtnLarge} onClick={openDrawer}>+ Create Client</button>
          </div>
        )}

        {/* Client grid */}
        {!loading && !error && filtered.length > 0 && (
          <div className={styles.grid}>
            {filtered.map((c, i) => {
              const [from, to] = avatarGradient(c.clientId);
              return (
                <div
                  className={styles.card}
                  key={c.clientId}
                  style={{ animationDelay: `${i * 0.05}s`, cursor: "pointer" }}
                  onClick={() => navigate(`/therapist/clients/${c.clientId}`)}
                >
                  <div className={styles.cardTop}>
                    <div
                      className={styles.avatar}
                      style={{ background: `linear-gradient(135deg, ${from}, ${to})` }}
                    >
                      {getInitials(c.clientName)}
                    </div>
                    <div className={styles.nameBlock}>
                      <h3 className={styles.clientName}>{c.clientName || "—"}</h3>
                      <span className={styles.clientId}>ID: {c.clientId}</span>
                    </div>
                    {c.dsf && <span className={styles.dsfBadge}>DSF</span>}
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* No search results */}
        {!loading && !error && clients.length > 0 && filtered.length === 0 && (
          <div className={styles.center}>
            <p className={styles.emptyText}>No clients match "{search}"</p>
          </div>
        )}
      </main>

      {/* Backdrop */}
      <div
        className={`${styles.backdrop} ${drawerOpen ? styles.backdropVisible : ""}`}
        onClick={closeDrawer}
      />

      {/* Slide-out drawer */}
      <div className={`${styles.drawer} ${drawerOpen ? styles.drawerOpen : ""}`}>
        <div className={styles.drawerHeader}>
          <div>
            <h2 className={styles.drawerTitle}>Create Client</h2>
            <p className={styles.drawerSub}>Fill in the client's details below</p>
          </div>
          <button className={styles.closeBtn} onClick={closeDrawer} aria-label="Close">✕</button>
        </div>

        <form onSubmit={handleSubmit} className={styles.form}>
          {/* Name row */}
          <div className={styles.formRow}>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="firstName">First Name</label>
              <input id="firstName" name="firstName" type="text" required
                value={form.firstName} onChange={handleChange}
                className={styles.input} placeholder="Jane" />
            </div>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="lastName">Last Name</label>
              <input id="lastName" name="lastName" type="text" required
                value={form.lastName} onChange={handleChange}
                className={styles.input} placeholder="Doe" />
            </div>
          </div>

          {/* Contact row */}
          <div className={styles.formRow}>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="email">Email</label>
              <input id="email" name="email" type="email" required
                value={form.email} onChange={handleChange}
                className={styles.input} placeholder="jane@example.com" />
            </div>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="phoneNumber">Phone Number</label>
              <input id="phoneNumber" name="phoneNumber" type="tel" required
                value={form.phoneNumber} onChange={handleChange}
                className={styles.input} placeholder="+91 98765 43210" />
            </div>
          </div>

          {/* Emergency + DOB */}
          <div className={styles.formRow}>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="emergencyPhoneNumber">Emergency Phone</label>
              <input id="emergencyPhoneNumber" name="emergencyPhoneNumber" type="tel" required
                value={form.emergencyPhoneNumber} onChange={handleChange}
                className={styles.input} placeholder="+91 91234 56789" />
            </div>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="dob">Date of Birth</label>
              <input id="dob" name="dob" type="date" required
                value={form.dob} onChange={handleChange}
                className={`${styles.input} ${styles.dateInput}`}
                max={new Date().toISOString().split("T")[0]} />
            </div>
          </div>

          {/* Gender + Pronouns */}
          <div className={styles.formRow}>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="gender">Gender</label>
              <select id="gender" name="gender" required
                value={form.gender} onChange={handleChange}
                className={`${styles.input} ${styles.select}`}>
                <option value="" disabled>Select gender</option>
                <option value="Male">Male</option>
                <option value="Female">Female</option>
                <option value="Non-binary">Non-binary</option>
                <option value="Other">Other</option>
                <option value="Prefer not to say">Prefer not to say</option>
              </select>
            </div>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="pronouns">Pronouns</label>
              <input id="pronouns" name="pronouns" type="text"
                value={form.pronouns} onChange={handleChange}
                className={styles.input} placeholder="e.g. she/her" />
            </div>
          </div>

          {formError && (
            <div className={styles.errorBox}>
              <span className={styles.errorIcon}>!</span>{formError}
            </div>
          )}

          {/* DSF */}
          <div className={styles.dsfRow}>
            <label className={styles.dsfLabel}>
              <input
                type="checkbox"
                className={styles.dsfCheckbox}
                checked={form.dsf}
                onChange={(e) => setForm(prev => ({ ...prev, dsf: e.target.checked }))}
              />
              <span>DSF</span>
            </label>
          </div>

          <div className={styles.formActions}>
            <button type="button" className={styles.cancelBtn} onClick={closeDrawer}>Cancel</button>
            <button type="submit" className={styles.submitBtn} disabled={formLoading}>
              {formLoading ? <span className={styles.btnSpinner} /> : "Save Client"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
