import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  getClientById, getSessionDetails, createSessionNotes, updateSessionNotes,
  updateClient, updateClientStatus, getClientNotes, upsertClientNote,
} from "../api/therapistClients";
import { useModeMap } from "../context/DeliveryModesContext";
import styles from "./ClientDetailPage.module.css";

function getInitials(firstName, lastName) {
  return `${firstName?.[0] ?? ""}${lastName?.[0] ?? ""}`.toUpperCase() || "?";
}
function formatDate(dob) {
  if (!dob) return "—";
  return new Date(dob).toLocaleDateString("en-GB", { day: "2-digit", month: "long", year: "numeric" });
}
function formatDate2(dt) {
  if (!dt) return "—";
  return new Date(dt).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
}
function formatTime(dt) {
  if (!dt) return "—";
  return new Date(dt).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit", hour12: true });
}
function formatDateTime(dt) {
  if (!dt) return "—";
  const d = new Date(dt);
  return d.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" }) + " · " +
    d.toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit", hour12: true });
}

const AVATAR_COLORS = [
  ["#6366f1","#4f46e5"],["#10b981","#059669"],["#f59e0b","#d97706"],
  ["#ec4899","#db2777"],["#8b5cf6","#7c3aed"],["#06b6d4","#0891b2"],["#f97316","#ea580c"],
];
function avatarGradient(id) {
  if (!id) return AVATAR_COLORS[0];
  return AVATAR_COLORS[id.charCodeAt(id.length - 1) % AVATAR_COLORS.length];
}

const MODE_TYPE_ICON = { ONLINE: "💻", OFFLINE_AT_HALUSURU: "📍", OFFLINE_AT_SESHADRIPURAM: "📍" };

function DetailField({ label, value }) {
  return (
    <div className={styles.field}>
      <span className={styles.fieldLabel}>{label}</span>
      <span className={styles.fieldValue}>{value || "—"}</span>
    </div>
  );
}

const EDIT_FIELDS = [
  { name: "firstName",            label: "First Name",      type: "text" },
  { name: "lastName",             label: "Last Name",       type: "text" },
  { name: "dob",                  label: "Date of Birth",   type: "date" },
  { name: "gender",               label: "Gender",          type: "text" },
  { name: "pronouns",             label: "Pronouns",        type: "text" },
  { name: "email",                label: "Email",           type: "email" },
  { name: "phoneNumber",          label: "Phone Number",    type: "tel" },
  { name: "emergencyPhoneNumber", label: "Emergency Phone", type: "tel" },
];

export default function ClientDetailPage() {
  const { clientId } = useParams();
  const navigate = useNavigate();
  const modeMap = useModeMap();

  const [client, setClient]   = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState(null);

  const [activeTab, setActiveTab] = useState("info");

  // ── Edit client ──────────────────────────────────────────
  const [editOpen, setEditOpen]       = useState(false);
  const [editForm, setEditForm]       = useState({});
  const [editLoading, setEditLoading] = useState(false);
  const [editError, setEditError]     = useState(null);

  const openEdit = () => {
    if (!client) return;
    setEditForm({
      firstName:             client.firstName || "",
      lastName:              client.lastName || "",
      dob:                   client.dob ? new Date(client.dob).toISOString().split("T")[0] : "",
      gender:                client.gender || "",
      pronouns:              client.pronouns || "",
      email:                 client.email || "",
      phoneNumber:           client.phoneNumber || "",
      emergencyPhoneNumber:  client.emergencyPhoneNumber || "",
    });
    setEditError(null);
    setEditOpen(true);
  };

  const handleEditSave = async () => {
    setEditLoading(true); setEditError(null);
    try {
      const payload = { ...editForm, dob: editForm.dob ? new Date(editForm.dob).toISOString() : null };
      const updated = await updateClient(clientId, payload);
      setClient(prev => ({ ...prev, ...updated }));
      setEditOpen(false);
    } catch (err) {
      setEditError(err.message);
    } finally {
      setEditLoading(false);
    }
  };

  // ── Status toggle ────────────────────────────────────────
  const [statusLoading, setStatusLoading]   = useState(false);
  const [statusConfirm, setStatusConfirm]   = useState(false);

  const handleStatusChange = async (newStatus) => {
    if (newStatus === "TERMINATED") {
      if (!statusConfirm) { setStatusConfirm(true); return; }
    }
    setStatusConfirm(false);
    setStatusLoading(true);
    try {
      await updateClientStatus(clientId, newStatus);
      setClient(prev => ({ ...prev, status: newStatus }));
    } catch (err) {
      setError(err.message);
    } finally {
      setStatusLoading(false);
    }
  };

  // ── Sessions ─────────────────────────────────────────────
  const [sessions, setSessions]               = useState([]);
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const [sessionsError, setSessionsError]     = useState(null);
  const [sessionsLoaded, setSessionsLoaded]   = useState(false);
  const [notesState, setNotesState]           = useState({});
  const [notesPopup, setNotesPopup]           = useState(null);

  const loadSessions = () => {
    if (sessionsLoaded) return;
    setSessionsLoading(true); setSessionsError(null);
    getSessionDetails(clientId)
      .then(data => { setSessions(data); setSessionsLoaded(true); })
      .catch(e => setSessionsError(e.message))
      .finally(() => setSessionsLoading(false));
  };

  const startEdit = (apptId, existing) => {
    setNotesState(prev => ({ ...prev, [apptId]: { editing: true, draft: existing || "", saving: false, error: null } }));
    setNotesPopup({ appointmentId: apptId, hasExisting: !!existing });
  };
  const cancelEdit = (apptId) => {
    setNotesState(prev => ({ ...prev, [apptId]: { ...prev[apptId], editing: false, error: null } }));
    setNotesPopup(null);
  };
  const updateDraft = (apptId, val) => {
    setNotesState(prev => ({ ...prev, [apptId]: { ...prev[apptId], draft: val } }));
  };
  const saveNotes = async (apptId, hasExisting) => {
    const draft = notesState[apptId]?.draft ?? "";
    setNotesState(prev => ({ ...prev, [apptId]: { ...prev[apptId], saving: true, error: null } }));
    try {
      if (hasExisting) await updateSessionNotes(clientId, apptId, draft);
      else await createSessionNotes(clientId, apptId, draft);
      setSessions(prev => prev.map(s => s.appointmentId === apptId ? { ...s, sessionNotes: draft } : s));
      setNotesState(prev => ({ ...prev, [apptId]: { editing: false, draft: "", saving: false, error: null } }));
      setNotesPopup(null);
    } catch (err) {
      setNotesState(prev => ({ ...prev, [apptId]: { ...prev[apptId], saving: false, error: err.message } }));
    }
  };

  // ── Client notes ─────────────────────────────────────────
  // Backend stores ONE note per therapist-client pair (PUT /therapist/{clientId}/note).
  // getClientNotes wraps the single returned object in an array, or returns [] on 404.
  const [clientNotes, setClientNotes]     = useState([]);
  const [notesLoading, setNotesLoading]   = useState(false);
  const [notesError, setNotesError]       = useState(null);
  const [notesLoaded, setNotesLoaded]     = useState(false);
  const [newNote, setNewNote]             = useState("");
  const [noteSaving, setNoteSaving]       = useState(false);
  const [noteSaveError, setNoteSaveError] = useState(null);

  const loadClientNotes = () => {
    if (notesLoaded) return;
    setNotesLoading(true); setNotesError(null);
    getClientNotes(clientId)
      .then(data => { setClientNotes(data); setNotesLoaded(true); })
      .catch(e => setNotesError(e.message))
      .finally(() => setNotesLoading(false));
  };

  const handleAddNote = async () => {
    if (!newNote.trim()) return;
    setNoteSaving(true); setNoteSaveError(null);
    try {
      await upsertClientNote(clientId, newNote.trim());
      setNewNote("");
      // Re-fetch since PUT upserts and returns void — reset load flag to force reload
      setNotesLoaded(false);
      setNotesLoading(true);
      const data = await getClientNotes(clientId);
      setClientNotes(data);
      setNotesLoaded(true);
      setNotesLoading(false);
    } catch (err) {
      setNoteSaveError(err.message);
    } finally {
      setNoteSaving(false);
    }
  };

  // ── Tab change ───────────────────────────────────────────
  const handleTabChange = (tab) => {
    setActiveTab(tab);
    if (tab === "sessions") loadSessions();
    if (tab === "notes")    loadClientNotes();
  };

  useEffect(() => {
    getClientById(clientId)
      .then(setClient)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, [clientId]);

  useEffect(() => {
    const handler = (e) => {
      if (e.key === "Escape") {
        if (notesPopup) cancelEdit(notesPopup.appointmentId);
        else if (editOpen) setEditOpen(false);
        else if (statusConfirm) setStatusConfirm(false);
      }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [notesPopup, editOpen, statusConfirm]);

  const [from, to] = avatarGradient(clientId);
  const fullName = client ? `${client.firstName ?? ""} ${client.lastName ?? ""}`.trim() : "";

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <button className={styles.back} onClick={() => navigate("/therapist/clients")}>← My Clients</button>
          <span className={styles.logo}>🧠 Therapy Connect</span>
          <span className={styles.rolePill}>Therapist</span>
        </div>
      </header>

      <main className={styles.main}>
        {loading && (
          <div className={styles.center}><div className={styles.spinner} /><p className={styles.loadingText}>Loading client details…</p></div>
        )}
        {!loading && error && (
          <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{error}</div>
        )}

        {!loading && !error && client && (
          <div className={styles.content}>

            {/* Hero */}
            <div className={styles.hero}>
              <div className={styles.avatar} style={{ background: `linear-gradient(135deg, ${from}, ${to})` }}>
                {getInitials(client.firstName, client.lastName)}
              </div>
              <div className={styles.heroText}>
                <h1 className={styles.name}>{fullName || "—"}</h1>
                <p className={styles.clientIdText}>Client ID: {clientId}</p>
                {client.pronouns && <span className={styles.pronounsBadge}>{client.pronouns}</span>}
              </div>

              <div className={styles.heroActions}>
                <div className={styles.statusWrapper}>
                  <button
                    className={`${styles.statusBadge} ${client.status === "ACTIVE" ? styles.statusActive : styles.statusTerminated}`}
                    onClick={() => handleStatusChange(client.status === "ACTIVE" ? "TERMINATED" : "ACTIVE")}
                    disabled={statusLoading || statusConfirm}
                    title="Click to change status"
                  >
                    {statusLoading ? <span className={styles.btnSpinnerSm}/> : (client.status === "ACTIVE" ? "● Active" : "● Terminated")}
                  </button>
                </div>
                <button className={styles.editBtn} onClick={openEdit}>✏️ Edit</button>
              </div>
            </div>

            {/* Status confirm */}
            {statusConfirm && (
              <div className={styles.confirmBanner}>
                <span>Mark this client as <strong>Terminated</strong>? This indicates end of engagement.</span>
                <div className={styles.confirmActions}>
                  <button className={styles.confirmCancelBtn} onClick={() => setStatusConfirm(false)}>Cancel</button>
                  <button className={styles.confirmOkBtn} onClick={() => handleStatusChange("TERMINATED")}>Confirm</button>
                </div>
              </div>
            )}

            {/* Tabs */}
            <div className={styles.tabs}>
              {["info","sessions","notes"].map(tab => (
                <button
                  key={tab}
                  className={`${styles.tab} ${activeTab === tab ? styles.tabActive : ""}`}
                  onClick={() => handleTabChange(tab)}
                >
                  {tab === "info" ? "Personal Info" : tab === "sessions" ? "Sessions" : "Notes"}
                </button>
              ))}
            </div>

            {/* ── Info tab ── */}
            {activeTab === "info" && (
              <div className={styles.card}>
                <h2 className={styles.sectionTitle}>Personal Information</h2>
                <div className={styles.grid}>
                  <DetailField label="First Name"      value={client.firstName} />
                  <DetailField label="Last Name"       value={client.lastName} />
                  <DetailField label="Date of Birth"   value={formatDate(client.dob)} />
                  <DetailField label="Gender"          value={client.gender} />
                  <DetailField label="Pronouns"        value={client.pronouns} />
                  <DetailField label="Email"           value={client.email} />
                  <DetailField label="Phone Number"    value={client.phoneNumber} />
                  <DetailField label="Emergency Phone" value={client.emergencyPhoneNumber} />
                  <DetailField label="DSF"             value={client.dsf ? "Yes" : "No"} />
                </div>
              </div>
            )}

            {/* ── Sessions tab ── */}
            {activeTab === "sessions" && (
              <div className={styles.card}>
                <h2 className={styles.sectionTitle}>Sessions</h2>
                {sessionsLoading && <div className={styles.center}><div className={styles.spinner}/><p className={styles.loadingText}>Loading sessions…</p></div>}
                {sessionsError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{sessionsError}</div>}
                {!sessionsLoading && !sessionsError && sessions.length === 0 && (
                  <div className={styles.center}><span className={styles.drawerEmptyIcon}>🗓️</span><p className={styles.drawerEmptyText}>No sessions found for this client.</p></div>
                )}
                {!sessionsLoading && !sessionsError && sessions.length > 0 && (
                  <div className={styles.sessionList}>
                    {sessions.slice().sort((a,b) => new Date(b.startTime) - new Date(a.startTime)).map(s => {
                      const ns = notesState[s.appointmentId] || {};
                      const mode = modeMap[s.modeId];
                      const modeIcon = MODE_TYPE_ICON[mode?.modeType] ?? "💬";
                      const modeLabel = mode?.displayName ?? s.modeId ?? "—";
                      return (
                        <div key={s.appointmentId} className={styles.sessionCard}>
                          <div className={styles.sessionTop}>
                            <div className={styles.sessionDateTime}>
                              <span className={styles.sessionDate}>{formatDate2(s.startTime)}</span>
                              <span className={styles.sessionTime}>{formatTime(s.startTime)} – {formatTime(s.endTime)}</span>
                            </div>
                            <div className={styles.sessionMeta}>
                              <span className={styles.sessionType}>{modeIcon} {modeLabel}</span>
                              <span className={`${styles.sessionStatus} ${s.status === "CONFIRMED" ? styles.statusConfirmed : styles.statusOther}`}>{s.status}</span>
                            </div>
                          </div>
                          <div className={styles.sessionDivider}/>
                          <div className={styles.notesSection}>
                            {s.sessionNotes ? (
                              <>
                                <p className={styles.notesLabel}>Session Notes</p>
                                <p className={styles.notesText}>{s.sessionNotes}</p>
                                <button className={styles.notesModifyBtn} onClick={() => startEdit(s.appointmentId, s.sessionNotes)}>✏️ Modify</button>
                              </>
                            ) : (
                              <button className={styles.notesAddBtn} onClick={() => startEdit(s.appointmentId, "")}>+ Add Notes</button>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            )}

            {/* ── Notes tab ── */}
            {activeTab === "notes" && (
              <div className={styles.card}>
                <h2 className={styles.sectionTitle}>Client Notes</h2>
                <div className={styles.noteInputArea}>
                  <textarea
                    className={styles.noteTextarea}
                    placeholder="Add a general note about this client…"
                    value={newNote}
                    onChange={e => setNewNote(e.target.value)}
                    rows={3}
                  />
                  {noteSaveError && <p className={styles.notesError}>{noteSaveError}</p>}
                  <div className={styles.noteInputActions}>
                    <button
                      className={styles.noteAddBtn}
                      onClick={handleAddNote}
                      disabled={noteSaving || !newNote.trim()}
                    >
                      {noteSaving ? <span className={styles.btnSpinner}/> : "+ Save Note"}
                    </button>
                  </div>
                </div>

                {notesLoading && <div className={styles.center}><div className={styles.spinner}/></div>}
                {notesError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{notesError}</div>}
                {!notesLoading && !notesError && clientNotes.length === 0 && (
                  <div className={styles.center}><p className={styles.drawerEmptyText}>No notes yet for this client.</p></div>
                )}
                {clientNotes.length > 0 && (
                  <div className={styles.clientNotesList}>
                    {clientNotes.map((n, i) => (
                      <div key={n.noteId || i} className={styles.clientNoteItem}>
                        <p className={styles.clientNoteText}>{n.content}</p>
                        <span className={styles.clientNoteTime}>{formatDateTime(n.updatedAt || n.createdAt)}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </main>

      {/* ── Backdrop ── */}
      <div
        className={`${styles.backdrop} ${!!notesPopup || editOpen ? styles.backdropVisible : ""}`}
        onClick={() => {
          if (notesPopup) cancelEdit(notesPopup.appointmentId);
          else if (editOpen) setEditOpen(false);
        }}
      />

      {/* ── Notes popup (session notes) ── */}
      {notesPopup && (() => {
        const apptId = notesPopup.appointmentId;
        const ns = notesState[apptId] || {};
        return (
          <div className={styles.notesModal}>
            <div className={styles.notesModalHeader}>
              <h3 className={styles.notesModalTitle}>{notesPopup.hasExisting ? "Modify Notes" : "Add Notes"}</h3>
              <button className={styles.closeBtn} onClick={() => cancelEdit(apptId)}>✕</button>
            </div>
            <div className={styles.notesModalBody}>
              <textarea className={styles.notesTextarea} value={ns.draft} onChange={e => updateDraft(apptId, e.target.value)} placeholder="Write session notes here…" autoFocus/>
              {ns.error && <p className={styles.notesError}>{ns.error}</p>}
            </div>
            <div className={styles.notesModalFooter}>
              <button className={styles.notesCancelBtn} onClick={() => cancelEdit(apptId)} disabled={ns.saving}>Cancel</button>
              <button className={styles.notesSaveBtn} onClick={() => saveNotes(apptId, notesPopup.hasExisting)} disabled={ns.saving || !ns.draft?.trim()}>
                {ns.saving ? <span className={styles.btnSpinner}/> : "Save"}
              </button>
            </div>
          </div>
        );
      })()}

      {/* ── Edit client modal ── */}
      {editOpen && (
        <div className={styles.notesModal} style={{ maxWidth: 560 }}>
          <div className={styles.notesModalHeader}>
            <h3 className={styles.notesModalTitle}>Edit Client</h3>
            <button className={styles.closeBtn} onClick={() => setEditOpen(false)}>✕</button>
          </div>
          <div className={styles.notesModalBody} style={{ maxHeight: "60vh", overflowY: "auto" }}>
            <div className={styles.editGrid}>
              {EDIT_FIELDS.map(f => (
                <div key={f.name} className={styles.editField}>
                  <label className={styles.editLabel}>{f.label}</label>
                  <input
                    className={styles.editInput}
                    type={f.type}
                    value={editForm[f.name] || ""}
                    onChange={e => setEditForm(prev => ({ ...prev, [f.name]: e.target.value }))}
                  />
                </div>
              ))}
            </div>
            {editError && <div className={styles.errorBox} style={{ marginTop: 12 }}><span className={styles.errorIcon}>!</span>{editError}</div>}
          </div>
          <div className={styles.notesModalFooter}>
            <button className={styles.notesCancelBtn} onClick={() => setEditOpen(false)} disabled={editLoading}>Cancel</button>
            <button className={styles.notesSaveBtn} onClick={handleEditSave} disabled={editLoading}>
              {editLoading ? <span className={styles.btnSpinner}/> : "Save Changes"}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
