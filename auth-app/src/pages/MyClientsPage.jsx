import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { getTherapistClients, createClient } from "../api/therapistClients";
import Icon from "../components/icons";
import styles from "./MyClientsPage.module.css";

function getInitials(name) {
  if (!name) return "?";
  const parts = name.trim().split(" ");
  return parts.length === 1
    ? parts[0][0].toUpperCase()
    : (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

// ── Enrichment display helpers (data supplied by the Phase-B backend;
//    these degrade gracefully to "—" until then) ──
function formatJoined(iso) {
  if (!iso) return null;
  try { return new Date(iso).toLocaleDateString("en-IN", { month: "short", year: "numeric" }); }
  catch { return null; }
}
function formatLastSeen(iso) {
  if (!iso) return "—";
  const then = new Date(iso), now = new Date();
  const days = Math.floor((now - then) / 86400000);
  if (days <= 0) return "today";
  if (days === 1) return "1d ago";
  if (days < 7) return `${days}d ago`;
  if (days < 30) return `${Math.floor(days / 7)}w ago`;
  if (days < 365) return `${Math.floor(days / 30)}mo ago`;
  return `${Math.floor(days / 365)}y ago`;
}
function statusLabel(status) {
  const s = (status || "ACTIVE").toUpperCase();
  return s.charAt(0) + s.slice(1).toLowerCase();
}

// Prototype avatar palette — cyan/green/violet family, in sync with the app gradient
const AVATAR_COLORS = [
  ["#22d3ee","#34d399"],["#a78bfa","#22d3ee"],["#34d399","#0891b2"],
  ["#fbbf24","#f472b6"],["#22d3ee","#818cf8"],["#818cf8","#22d3ee"],["#34d399","#22d3ee"],
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
  const [statusFilter, setStatusFilter] = useState("ALL"); // ALL | ACTIVE | ARCHIVED
  const [recentFirst, setRecentFirst] = useState(false);

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

  const previewName = [form.firstName, form.lastName].filter(p => p && p.trim()).join(" ").trim();
  const previewInitials = ((form.firstName?.[0] ?? "") + (form.lastName?.[0] ?? "")).toUpperCase();

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

  let filtered = clients.filter((c) =>
    c.clientName?.toLowerCase().includes(search.toLowerCase()) ||
    c.clientId?.toLowerCase().includes(search.toLowerCase())
  );
  if (statusFilter !== "ALL") {
    filtered = filtered.filter((c) => (c.status || "ACTIVE").toUpperCase() === statusFilter);
  }
  if (recentFirst) {
    filtered = [...filtered].sort((a, b) => new Date(b.lastSeen || 0) - new Date(a.lastSeen || 0));
  }
  const cycleStatus = () =>
    setStatusFilter((s) => (s === "ALL" ? "ACTIVE" : s === "ACTIVE" ? "ARCHIVED" : "ALL"));

  return (
    <div className={styles.page}>
      <main className={styles.main}>
        <div className="page-head">
          <div>
            <div className="eyebrow">People</div>
            <h1>My Clients</h1>
            <div className="sub">
              {loading ? "Loading…" : `${clients.length} client${clients.length !== 1 ? "s" : ""}`}
            </div>
          </div>
          {!loading && (
            <button className="btn btn-primary" onClick={openDrawer}>
              <Icon name="plus" size={16} /> Add client
            </button>
          )}
        </div>

        {/* Search + filters */}
        {!loading && !error && clients.length > 0 && (
          <div className={styles.controls}>
            <label className={styles.searchWrap}>
              <span className={styles.searchIcon}><Icon name="search" size={18} /></span>
              <input
                className={styles.search}
                type="text"
                placeholder="Search by name…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </label>
            <button className={`btn ${statusFilter !== "ALL" ? styles.filterOn : ""}`} onClick={cycleStatus}>
              {statusFilter === "ALL" ? "All statuses" : statusFilter === "ACTIVE" ? "Active" : "Archived"}
            </button>
            <button className={`btn ${recentFirst ? styles.filterOn : ""}`} onClick={() => setRecentFirst((r) => !r)}>
              Recently seen
            </button>
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
            <span className={styles.emptyIcon}><Icon name="users" size={38} /></span>
            <h2 className={styles.emptyTitle}>No clients yet</h2>
            <p className={styles.emptyText}>Create your first client to get started.</p>
            <button className="btn btn-primary" onClick={openDrawer}><Icon name="plus" size={16} /> Add client</button>
          </div>
        )}

        {/* Client grid */}
        {!loading && !error && filtered.length > 0 && (
          <div className={styles.grid}>
            {filtered.map((c, i) => {
              const [from, to] = avatarGradient(c.clientId);
              return (
                <div
                  className={`card ${styles.card}`}
                  key={c.clientId}
                  style={{ animationDelay: `${i * 0.05}s`, cursor: "pointer" }}
                  onClick={() => navigate(`/therapist/clients/${c.clientId}`)}
                >
                  <div className={styles.cardRow}>
                    <div className={`avatar avatar-m ${styles.avatar}`}
                      style={{ background: `linear-gradient(135deg, ${from}, ${to})` }}>
                      {getInitials(c.clientName)}
                    </div>
                    <div className={styles.nameBlock}>
                      <b className={styles.clientName}>{c.clientName || "—"}</b>
                      <div className={styles.clientId}>
                        {c.clientId}{formatJoined(c.createdAt) ? ` · joined ${formatJoined(c.createdAt)}` : ""}
                      </div>
                    </div>
                    <div className={styles.badges}>
                      {c.dsf && <span className="chip chip-info">DSF</span>}
                      <span className={`chip ${(c.status || "ACTIVE") === "ARCHIVED" ? "chip-mut" : "chip-ok"}`}>
                        {statusLabel(c.status)}
                      </span>
                    </div>
                  </div>
                  <div className={styles.cardStats}>
                    <span><b>{c.sessionCount ?? "—"}</b> session{c.sessionCount === 1 ? "" : "s"}</span>
                    <span>Last seen <b>{formatLastSeen(c.lastSeen)}</b></span>
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
            <h2 className={styles.drawerTitle}>Add client</h2>
            <p className={styles.drawerSub}>Fill in the client's details below</p>
          </div>
          <button className={styles.closeBtn} onClick={closeDrawer} aria-label="Close"><Icon name="x" size={18} /></button>
        </div>

        <form onSubmit={handleSubmit} className={styles.form}>

          {/* Live preview: the client is represented by an initials avatar
              everywhere else in the app, so the form builds one as you type
              instead of opening as a bare stack of inputs. */}
          <div className={styles.preview}>
            <span className={`avatar avatar-l ${styles.previewAvatar}`}>
              {previewInitials || <Icon name="users" size={22} />}
            </span>
            <div className={styles.previewText}>
              <b>{previewName || "New client"}</b>
              <span>{form.email || "No email yet"}</span>
            </div>
          </div>

          <div className={styles.groupLabel}>Identity</div>
          <div className={styles.formRow}>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="firstName">First name</label>
              <input id="firstName" name="firstName" type="text" required
                value={form.firstName} onChange={handleChange}
                className="input" placeholder="Jane" />
            </div>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="lastName">Last name</label>
              <input id="lastName" name="lastName" type="text" required
                value={form.lastName} onChange={handleChange}
                className="input" placeholder="Doe" />
            </div>
          </div>
          <div className={styles.formRow}>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="dob">Date of birth</label>
              <input id="dob" name="dob" type="date" required
                value={form.dob} onChange={handleChange}
                className={`input ${styles.dateInput}`}
                max={new Date().toISOString().split("T")[0]} />
            </div>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="gender">Gender</label>
              <select id="gender" name="gender" required
                value={form.gender} onChange={handleChange}
                className={`input ${styles.select}`}>
                <option value="" disabled>Select gender</option>
                <option value="Male">Male</option>
                <option value="Female">Female</option>
                <option value="Non-binary">Non-binary</option>
                <option value="Other">Other</option>
                <option value="Prefer not to say">Prefer not to say</option>
              </select>
            </div>
          </div>
          <div className={styles.field}>
            <label className={styles.label} htmlFor="pronouns">
              Pronouns <span className={styles.optional}>optional</span>
            </label>
            <input id="pronouns" name="pronouns" type="text"
              value={form.pronouns} onChange={handleChange}
              className="input" placeholder="e.g. she/her" />
          </div>

          <div className={styles.groupLabel}>Contact</div>
          <div className={styles.formRow}>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="email">Email</label>
              <input id="email" name="email" type="email" required
                value={form.email} onChange={handleChange}
                className="input" placeholder="jane@example.com" />
            </div>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="phoneNumber">Phone number</label>
              <input id="phoneNumber" name="phoneNumber" type="tel" required
                value={form.phoneNumber} onChange={handleChange}
                className="input" placeholder="+91 98765 43210" />
            </div>
          </div>

          <div className={styles.groupLabel}>Emergency &amp; billing</div>
          <div className={styles.field}>
            <label className={styles.label} htmlFor="emergencyPhoneNumber">Emergency phone</label>
            <input id="emergencyPhoneNumber" name="emergencyPhoneNumber" type="tel" required
              value={form.emergencyPhoneNumber} onChange={handleChange}
              className="input" placeholder="+91 91234 56789" />
            <span className={styles.hint}>Someone to reach if the client can&apos;t be contacted.</span>
          </div>

          {/* DSF is the partner non-profit; its students are seen pro bono.
              Earnings queries exclude these clients (c.dsf = false) and count
              their sessions separately, so this flag drives real reporting. */}
          <div className={styles.toggleRow}>
            <div className={styles.toggleText}>
              <b>DSF student &mdash; pro bono</b>
              <span>Sessions with this client are provided free of charge. They&apos;re counted separately and excluded from earnings.</span>
            </div>
            <button
              type="button"
              role="switch"
              aria-checked={form.dsf}
              aria-label="DSF student, seen pro bono"
              className={`switch ${form.dsf ? "on" : ""}`}
              onClick={() => setForm(prev => ({ ...prev, dsf: !prev.dsf }))}
            />
          </div>

          {formError && (
            <div className={styles.errorBox}>
              <span className={styles.errorIcon}>!</span>{formError}
            </div>
          )}

          <div className={styles.formActions}>
            <button type="button" className="btn" onClick={closeDrawer}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={formLoading}>
              {formLoading ? <span className={styles.btnSpinner} /> : "Add client"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
