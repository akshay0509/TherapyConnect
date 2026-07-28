import { useEffect, useState, useMemo } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  getClientById, getSessionDetails, createSessionNotes, updateSessionNotes,
  updateClient, updateClientStatus, getClientNotes, upsertClientNote,
} from "../api/therapistClients";
import { useModeMap } from "../context/DeliveryModesContext";
import Icon from "../components/icons";
import styles from "./ClientDetailPage.module.css";

// "COMPLETED" → "Completed" (the prototype uses sentence case, not raw enums)
function titleCase(s) {
  if (!s) return "—";
  return s.charAt(0).toUpperCase() + s.slice(1).toLowerCase();
}
function durationMins(start, end) {
  if (!start || !end) return null;
  const m = Math.round((new Date(end) - new Date(start)) / 60000);
  return m > 0 ? m : null;
}
function ageFromDob(dob) {
  if (!dob) return null;
  const d = new Date(dob), now = new Date();
  let a = now.getFullYear() - d.getFullYear();
  if (now.getMonth() < d.getMonth() || (now.getMonth() === d.getMonth() && now.getDate() < d.getDate())) a--;
  return a >= 0 && a < 130 ? a : null;
}

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

// Prototype avatar palette — cyan/green/violet family, in sync with the app gradient
const AVATAR_COLORS = [
  ["#22d3ee","#34d399"],["#a78bfa","#22d3ee"],["#34d399","#0891b2"],
  ["#fbbf24","#f472b6"],["#22d3ee","#818cf8"],["#818cf8","#22d3ee"],["#34d399","#22d3ee"],
];
function avatarGradient(id) {
  if (!id) return AVATAR_COLORS[0];
  return AVATAR_COLORS[id.charCodeAt(id.length - 1) % AVATAR_COLORS.length];
}

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

  const [activeTab, setActiveTab] = useState("overview");

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
    // Load sessions up-front too so the at-a-glance stats can render on the
    // overview (the Sessions tab reuses the same cache via its loaded guard).
    loadSessions();
  }, [clientId]);

  // At-a-glance metrics, computed from session history.
  // Attendance = completed ÷ (completed + cancelled + abandoned).
  const glance = useMemo(() => {
    if (!sessions.length) return null;
    const done = sessions.filter(s => s.status === "COMPLETED").length;
    const missed = sessions.filter(s => ["CANCELLED", "ABANDONED"].includes(s.status)).length;
    const attendance = (done + missed) > 0 ? Math.round((done / (done + missed)) * 100) : null;
    const counts = {};
    sessions.forEach(s => { if (s.modeId) counts[s.modeId] = (counts[s.modeId] || 0) + 1; });
    const topMode = Object.entries(counts).sort((a, b) => b[1] - a[1])[0]?.[0];
    return { total: sessions.length, attendance, primaryMode: modeMap[topMode]?.displayName ?? "—" };
  }, [sessions, modeMap]);

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
      <main className={styles.main}>
        <button className={styles.backLink} onClick={() => navigate("/therapist/clients")}>
          <Icon name="back" size={16} /> Back to clients
        </button>

        {loading && (
          <div className={styles.center}><div className={styles.spinner} /><p className={styles.loadingText}>Loading client details…</p></div>
        )}
        {!loading && error && (
          <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{error}</div>
        )}

        {!loading && !error && client && (
          <div className={styles.content}>

            {/* Hero */}
            <div className={`card ${styles.hero}`}>
              <div className={`avatar avatar-l ${styles.avatar}`} style={{ background: `linear-gradient(135deg, ${from}, ${to})` }}>
                {getInitials(client.firstName, client.lastName)}
              </div>
              <div className={styles.heroText}>
                <h1 className={styles.name}>{fullName || "—"}</h1>
                <p className={styles.heroMeta}>
                  <span className={styles.clientIdText}>{clientId}</span>
                  {ageFromDob(client.dob) != null && <> · {ageFromDob(client.dob)}</>}
                  {client.pronouns && <> · {client.pronouns}</>}
                  {client.gender && <> · {client.gender}</>}
                </p>
                <div className={styles.heroContact}>
                  {client.email && <span><Icon name="mail" size={15} /> {client.email}</span>}
                  {client.phoneNumber && <span><Icon name="phone" size={15} /> {client.phoneNumber}</span>}
                </div>
              </div>

              <div className={styles.heroActions}>
                <button
                  className={`chip ${client.status === "ACTIVE" ? "chip-ok" : "chip-bad"} ${styles.statusToggle}`}
                  onClick={() => handleStatusChange(client.status === "ACTIVE" ? "TERMINATED" : "ACTIVE")}
                  disabled={statusLoading || statusConfirm}
                  title="Click to change status"
                >
                  {statusLoading ? "…" : (client.status === "ACTIVE" ? "● Active" : "● Terminated")}
                </button>
                <button className="btn" onClick={openEdit}><Icon name="edit" size={16} /> Edit</button>
                <button className="btn btn-primary" onClick={() => navigate("/therapist/appointments")}><Icon name="plus" size={16} /> Book session</button>
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
            <div className="tabbar">
              {[
                ["overview", "Overview"],
                ["details", "Personal details"],
                ["sessions", "Sessions"],
                ["notes", "Notes"],
              ].map(([tab, label]) => (
                <button key={tab} className={activeTab === tab ? "on" : ""} onClick={() => handleTabChange(tab)}>
                  {label}
                </button>
              ))}
            </div>

            {/* ── Overview tab ── */}
            {activeTab === "overview" && (() => {
              const recent = sessions.slice().sort((a, b) => new Date(b.startTime) - new Date(a.startTime));
              const latestNote = recent.find(s => s.sessionNotes);
              return (
                <>
                  <div className="grid-2" style={{ marginBottom: 22 }}>
                    <div className="card">
                      <div className="panel-h"><div>
                        <h2>Session history</h2>
                        <p>{sessions.length} session{sessions.length !== 1 ? "s" : ""}{glance ? ` · ${recent.filter(s => s.status === "COMPLETED").length} completed` : ""}</p>
                      </div></div>
                      {sessionsLoading && <div className={styles.center}><div className={styles.spinner} /></div>}
                      {!sessionsLoading && sessions.length === 0 && (
                        <div className={styles.center} style={{ padding: "40px 0" }}><p className={styles.drawerEmptyText}>No sessions yet.</p></div>
                      )}
                      {recent.slice(0, 6).map(s => {
                        const mode = modeMap[s.modeId];
                        const isOnline = mode?.modeType === "ONLINE";
                        const mins = durationMins(s.startTime, s.endTime);
                        const chip = s.status === "COMPLETED" ? "chip-ok" : ["CANCELLED", "ABANDONED"].includes(s.status) ? "chip-bad" : "chip-warn";
                        return (
                          <div key={s.appointmentId} className="sess">
                            <div className="time"><b>{formatDate2(s.startTime).replace(/ \d{4}$/, "")}</b><span>{formatTime(s.startTime)}</span></div>
                            <div className="info">
                              <b>Therapy session</b>
                              <div className="s">
                                <Icon name={isOnline ? "video" : "pin"} size={13} />
                                {isOnline ? "Online" : (mode?.displayName ?? "Clinic")}{mins ? ` · ${mins} min` : ""}
                              </div>
                            </div>
                            <span className={`chip ${chip}`}>{titleCase(s.status)}</span>
                          </div>
                        );
                      })}
                    </div>
                    <div className="col">
                      <div className="card" style={{ padding: 20 }}>
                        <h2 style={{ margin: "0 0 12px", fontSize: "1rem" }}>Latest note</h2>
                        {latestNote ? (
                          <>
                            <p style={{ color: "var(--text-2)", fontSize: ".88rem", lineHeight: 1.6, margin: 0 }}>{latestNote.sessionNotes}</p>
                            <div style={{ color: "var(--text-4)", fontSize: ".76rem", marginTop: 12 }}>{formatDate2(latestNote.startTime)} · encrypted</div>
                          </>
                        ) : (
                          <p style={{ color: "var(--text-4)", fontSize: ".85rem", margin: 0 }}>
                            No session notes yet. Add one from the Sessions tab.
                          </p>
                        )}
                      </div>
                      <div className="card" style={{ padding: 20 }}>
                        <h2 style={{ margin: "0 0 12px", fontSize: "1rem" }}>At a glance</h2>
                        <div className="rows">
                          <div><span className="k">Total sessions</span><span className="v">{glance ? glance.total : "—"}</span></div>
                          {/* Total paid — awaiting the payments API (AppointmentService); shows "—" until wired */}
                          <div><span className="k">Total paid</span><span className="v">{client.totalPaid != null ? `₹${Number(client.totalPaid).toLocaleString("en-IN")}` : "—"}</span></div>
                          <div><span className="k">Attendance</span><span className="v" style={{ color: "var(--ok-mid)" }}>{glance?.attendance != null ? `${glance.attendance}%` : "—"}</span></div>
                          <div><span className="k">Primary mode</span>{glance && glance.primaryMode !== "—" ? <span className="chip chip-online">{glance.primaryMode}</span> : <span className="v">—</span>}</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </>
              );
            })()}

            {/* ── Personal details tab ── */}
            {activeTab === "details" && (
              <div className="card" style={{ padding: "22px 24px" }}>
                <h2 className={styles.sectionTitle}>Personal details</h2>
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
                  <div className={styles.center}><span className={styles.drawerEmptyIcon}><Icon name="calendar" size={30} /></span><p className={styles.drawerEmptyText}>No sessions found for this client.</p></div>
                )}
                {!sessionsLoading && !sessionsError && sessions.length > 0 && (
                  <div className={styles.sessionList}>
                    {sessions.slice().sort((a,b) => new Date(b.startTime) - new Date(a.startTime)).map(s => {
                      const ns = notesState[s.appointmentId] || {};
                      const mode = modeMap[s.modeId];
                      const isOnline = mode?.modeType === "ONLINE";
                      const modeLabel = mode?.displayName ?? s.modeId ?? "—";
                      return (
                        <div key={s.appointmentId} className={styles.sessionCard}>
                          <div className={styles.sessionTop}>
                            <div className={styles.sessionDateTime}>
                              <span className={styles.sessionDate}>{formatDate2(s.startTime)}</span>
                              <span className={styles.sessionTime}>{formatTime(s.startTime)} – {formatTime(s.endTime)}</span>
                            </div>
                            <div className={styles.sessionMeta}>
                              <span className={styles.sessionType}><Icon name={isOnline ? "video" : "pin"} size={14} /> {modeLabel}</span>
                              <span className={`chip ${s.status === "COMPLETED" ? "chip-ok" : ["CANCELLED","ABANDONED"].includes(s.status) ? "chip-bad" : "chip-warn"}`}>{titleCase(s.status)}</span>
                            </div>
                          </div>
                          <div className={styles.sessionDivider}/>
                          <div className={styles.notesSection}>
                            {s.sessionNotes ? (
                              <>
                                <p className={styles.notesLabel}>Session Notes</p>
                                <p className={styles.notesText}>{s.sessionNotes}</p>
                                <button className={styles.notesModifyBtn} onClick={() => startEdit(s.appointmentId, s.sessionNotes)}><Icon name="edit" size={14} /> Modify</button>
                              </>
                            ) : (
                              <button className={styles.notesAddBtn} onClick={() => startEdit(s.appointmentId, "")}><Icon name="plus" size={14} /> Add Notes</button>
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
              <button className={styles.closeBtn} onClick={() => cancelEdit(apptId)}><Icon name="x" size={18} /></button>
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
            <button className={styles.closeBtn} onClick={() => setEditOpen(false)}><Icon name="x" size={18} /></button>
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
