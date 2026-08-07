import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { getTherapistClients, createClient } from "../api/therapistClients";
import { useAllModes } from "../context/DeliveryModesContext";
import Icon from "../components/icons";
import ClientFormModal from "../components/ClientFormModal";
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
  qualification: "", currentOccupation: "",
  preferredDays: "", preferredTimings: "", preferredModes: "",
  emergencyContactName: "", emergencyContactAge: "", emergencyContactRelationship: "",
  sessionFee: "",
  dsf: false,
};

export default function MyClientsPage() {
  const navigate = useNavigate();
  const allModes = useAllModes();
  const modeOptions = (allModes ?? []).map(m => m.displayName).filter(Boolean);

  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL"); // ALL | ACTIVE | TERMINATED
  const [recentFirst, setRecentFirst] = useState(false);

  const [addModalOpen, setAddModalOpen] = useState(false);
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
    const handler = (e) => { if (e.key === "Escape") closeAddModal(); };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, []);

  const openAddModal = () => { setForm(EMPTY_FORM); setFormError(null); setAddModalOpen(true); };
  const closeAddModal = () => { setAddModalOpen(false); setFormError(null); };

  const handleSubmit = async () => {
    setFormLoading(true);
    setFormError(null);
    try {
      const payload = {
        ...form,
        dob: form.dob ? new Date(form.dob).toISOString() : null,
        dsf: form.dsf,
        // ClientDto types this as Integer — "" will not deserialize.
        emergencyContactAge: form.emergencyContactAge === ""
          ? null : Number(form.emergencyContactAge),
        // Blank means "no negotiated rate" — the delivery-mode price applies.
        sessionFee: form.sessionFee === "" ? null : Number(form.sessionFee),
      };
      await createClient(payload);
      closeAddModal();
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
    setStatusFilter((s) => (s === "ALL" ? "ACTIVE" : s === "ACTIVE" ? "TERMINATED" : "ALL"));

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
            <button className="btn btn-primary" onClick={openAddModal}>
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
              {statusFilter === "ALL" ? "All statuses" : statusFilter === "ACTIVE" ? "Active" : "Terminated"}
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
            <button className="btn btn-primary" onClick={openAddModal}><Icon name="plus" size={16} /> Add client</button>
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
                      {/* Compared against the status the backend actually sends.
                          This tested for "ARCHIVED", which ClientStatus has never
                          had — so a terminated client rendered in the success
                          chip, visually identical to an active one. */}
                      <span className={`chip ${(c.status || "ACTIVE") === "ACTIVE" ? "chip-ok" : "chip-mut"}`}>
                        {statusLabel(c.status)}
                      </span>
                    </div>
                  </div>
                  <div className={styles.cardStats}>
                    <span><b>{c.sessionCount ?? "—"}</b> session{c.sessionCount === 1 ? "" : "s"}</span>
                    <span>Last seen <b>{formatLastSeen(c.lastSeen)}</b></span>
                    {/* DSF clients' pending notes are never surfaced as a concern
                        on the list — only on the client's own page (owner, 29 Jul). */}
                    {!c.dsf && c.pendingNotes > 0 && (
                      <span className={`chip chip-warn ${styles.notesChip}`}
                        title={`${c.pendingNotes} completed session${c.pendingNotes === 1 ? "" : "s"} without notes`}>
                        {c.pendingNotes} note{c.pendingNotes === 1 ? "" : "s"} due
                      </span>
                    )}
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

      {/* One shared form for both Add and Edit (components/ClientFormModal).
          These were two implementations that had drifted apart; sharing the
          component is what actually keeps them identical. */}
      <ClientFormModal
        open={addModalOpen}
        mode="create"
        form={form}
        onChange={(name, value) => setForm(prev => ({ ...prev, [name]: value }))}
        onSubmit={handleSubmit}
        onClose={closeAddModal}
        loading={formLoading}
        error={formError}
        modeOptions={modeOptions}
      />
    </div>
  );
}
