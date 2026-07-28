import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getAvailabilityRules, createAvailabilityRules, deleteAvailabilityRule, updateAvailabilityRule } from "../api/availabilityRules";
import Icon from "../components/icons";
import styles from "./AvailabilityRulesPage.module.css";

const DAY_MAP = {
  1: "Monday", 2: "Tuesday", 3: "Wednesday",
  4: "Thursday", 5: "Friday", 6: "Saturday", 7: "Sunday",
};
const ALL_DAYS = [1, 2, 3, 4, 5, 6, 7];

const EMPTY_ROW = () => ({
  _id: crypto.randomUUID(),
  dayOfWeek: "",
  startTime: "",
  endTime: "",
  isActive: true,
});

function formatTime(t) {
  if (!t) return "—";
  // Handle both "HH:mm:ss" and "HH:mm"
  const [h, m] = t.split(":");
  const hour = parseInt(h, 10);
  const ampm = hour >= 12 ? "PM" : "AM";
  const h12 = hour % 12 || 12;
  return `${h12}:${m} ${ampm}`;
}

export default function AvailabilityRulesPage() {
  const navigate = useNavigate();

  const [existingRules, setExistingRules] = useState([]);
  const [newRows, setNewRows] = useState([]);

  // Delete rule
  const [deleteConfirm, setDeleteConfirm] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [deleteError, setDeleteError]     = useState(null);

  // Edit rule
  const [editRule, setEditRule]           = useState(null);
  const [editForm, setEditForm]           = useState({});
  const [editLoading, setEditLoading]     = useState(false);
  const [editError, setEditError]         = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState(null);
  const [saveSuccess, setSaveSuccess] = useState(false);

  useEffect(() => {
    getAvailabilityRules()
      .then(setExistingRules)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const usedDays = [
    ...existingRules.map((r) => r.dayOfWeek),
    ...newRows.map((r) => r.dayOfWeek).filter(Boolean).map(Number),
  ];
  const availableDays = ALL_DAYS.filter((d) => !usedDays.includes(d));
  const totalRows = existingRules.length + newRows.length;
  const canAddMore = totalRows < 7;

  const addRow = () => {
    if (!canAddMore) return;
    setNewRows((prev) => [...prev, EMPTY_ROW()]);
    setSaveSuccess(false);
  };

  const updateRow = (id, field, value) => {
    setNewRows((prev) =>
      prev.map((r) => (r._id === id ? { ...r, [field]: value } : r))
    );
  };

  const removeRow = (id) => {
    setNewRows((prev) => prev.filter((r) => r._id !== id));
  };

  const handleSave = async () => {
    // Validate all new rows
    for (const row of newRows) {
      if (!row.dayOfWeek || !row.startTime || !row.endTime) {
        setSaveError("Please fill in all fields for every new rule.");
        return;
      }
      if (row.startTime >= row.endTime) {
        setSaveError(`End time must be after start time for ${DAY_MAP[row.dayOfWeek]}.`);
        return;
      }
    }

    setSaving(true);
    setSaveError(null);
    try {
      const payload = newRows.map(({ dayOfWeek, startTime, endTime, isActive }) => ({
        dayOfWeek: parseInt(dayOfWeek, 10),
        startTime,
        endTime,
        isActive,
      }));
      const saved = await createAvailabilityRules(payload);
      // Merge saved rules into existing
      setExistingRules((prev) => [...prev, ...(Array.isArray(saved) ? saved : [saved])]);
      setNewRows([]);
      setSaveSuccess(true);
    } catch (err) {
      setSaveError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteRule = async () => {
    if (!deleteConfirm) return;
    setDeleteLoading(true); setDeleteError(null);
    try {
      await deleteAvailabilityRule(deleteConfirm);
      setExistingRules(prev => prev.filter(r => r.ruleId !== deleteConfirm));
      setDeleteConfirm(null);
    } catch (err) {
      setDeleteError(err.message);
    } finally {
      setDeleteLoading(false);
    }
  };

  const openEditRule = (rule) => {
    setEditRule(rule);
    // backend LocalTime serializes as HH:MM:SS — trim to HH:MM so the time
    // input doesn't render a seconds column
    setEditForm({
      dayOfWeek: rule.dayOfWeek,
      startTime: (rule.startTime || "").slice(0, 5),
      endTime:   (rule.endTime || "").slice(0, 5),
      isActive:  rule.isActive,
    });
    setEditError(null);
  };

  const handleEditRule = async () => {
    // same window validation the create flow has (backend validates too)
    if (!editForm.startTime || !editForm.endTime || editForm.startTime >= editForm.endTime) {
      setEditError("End time must be after start time."); return;
    }
    setEditLoading(true); setEditError(null);
    try {
      const updated = await updateAvailabilityRule(editRule.ruleId, editForm);
      setExistingRules(prev => prev.map(r => r.ruleId === editRule.ruleId ? { ...r, ...updated } : r));
      setEditRule(null);
    } catch (err) {
      setEditError(err.message);
    } finally {
      setEditLoading(false);
    }
  };

  // Days selectable in the edit dialog: same one-rule-per-day restriction the
  // create flow enforces — exclude days owned by other rules, keep own day
  const availableForEdit = () => {
    if (!editRule) return ALL_DAYS;
    const takenDays = existingRules
      .filter(r => r.ruleId !== editRule.ruleId)
      .map(r => r.dayOfWeek);
    return ALL_DAYS.filter(d => !takenDays.includes(d) || d === editForm.dayOfWeek);
  };

  // Days available for a specific new row (excluding other new rows' selections except itself)
  const availableForRow = (rowId, currentVal) => {
    const otherNewDays = newRows
      .filter((r) => r._id !== rowId && r.dayOfWeek)
      .map((r) => Number(r.dayOfWeek));
    const takenDays = [...existingRules.map((r) => r.dayOfWeek), ...otherNewDays];
    return ALL_DAYS.filter((d) => !takenDays.includes(d) || d === Number(currentVal));
  };

  return (
    <div className={styles.page}>
      <main className={styles.main}>
        <div className="page-head">
          <div>
            <div className="eyebrow">Working hours</div>
            <h1>Availability Rules</h1>
            <div className="sub">
              {loading ? "Loading…" : "Your weekly template. Slots are generated from these."}
            </div>
          </div>
          {!loading && canAddMore && (
            <button className="btn btn-primary" onClick={addRow}>
              <Icon name="plus" size={16} /> Add rule
            </button>
          )}
          {!loading && !canAddMore && (
            <span className="chip chip-ok"><Icon name="check" size={14} /> All 7 days set</span>
          )}
        </div>

        {/* ── Loading ── */}
        {loading && (
          <div className={styles.center}>
            <div className={styles.spinner} />
            <p className={styles.loadingText}>Fetching rules…</p>
          </div>
        )}

        {/* ── Fetch error ── */}
        {!loading && error && (
          <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{error}</div>
        )}

        {!loading && !error && (
          <>
            {/* ── Success banner ── */}
            {saveSuccess && (
              <div className={styles.successBox}>
                <span className={styles.successIcon}><Icon name="check" size={16} /></span>
                Rules saved successfully!
              </div>
            )}

            {/* ── Week overview strip ── */}
            <div className={styles.weekStrip}>
              {ALL_DAYS.map((d) => {
                const isExisting = existingRules.some((r) => r.dayOfWeek === d);
                const isNew = newRows.some((r) => Number(r.dayOfWeek) === d);
                return (
                  <div
                    key={d}
                    className={`${styles.dayPill} ${isExisting ? styles.dayPillSet : ""} ${isNew ? styles.dayPillNew : ""}`}
                  >
                    {DAY_MAP[d].slice(0, 3)}
                  </div>
                );
              })}
            </div>

            {/* ── Existing rules table ── */}
            {existingRules.length > 0 && (
              <div className="card" style={{ padding: "8px 0" }}>
                <div className="table-wrap">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Day</th>
                        <th>Hours</th>
                        <th>Status</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      {existingRules
                        .slice()
                        .sort((a, b) => a.dayOfWeek - b.dayOfWeek)
                        .map((rule) => (
                          <tr key={rule.ruleId}>
                            <td><b>{DAY_MAP[rule.dayOfWeek]}</b></td>
                            <td>{formatTime(rule.startTime)} – {formatTime(rule.endTime)}</td>
                            <td>
                              <span className={`chip ${rule.isActive ? "chip-ok" : "chip-mut"}`}>
                                {rule.isActive ? "Active" : "Inactive"}
                              </span>
                            </td>
                            <td style={{ textAlign: "right" }}>
                              <button className={styles.iconBtn} onClick={() => openEditRule(rule)} title="Edit" aria-label="Edit rule">
                                <Icon name="edit" size={16} />
                              </button>
                              <button className={`${styles.iconBtn} ${styles.iconBtnDanger}`} onClick={() => { setDeleteConfirm(rule.ruleId); setDeleteError(null); }} title="Delete" aria-label="Delete rule">
                                <Icon name="x" size={16} />
                              </button>
                            </td>
                          </tr>
                        ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {existingRules.length === 0 && newRows.length === 0 && (
              <div className={styles.center}>
                <span className={styles.emptyIcon}><Icon name="clock" size={38} /></span>
                <h2 className={styles.emptyTitle}>No rules yet</h2>
                <p className={styles.emptyText}>Add your weekly availability to let clients book sessions.</p>
                <button className="btn btn-primary" onClick={addRow}><Icon name="plus" size={16} /> Add rule</button>
              </div>
            )}

            {/* ── New rows ── */}
            {newRows.length > 0 && (
              <div className={styles.newRowsSection}>
                <h3 className={styles.newRowsTitle}>New Rules</h3>
                <div className={styles.newRowsList}>
                  {newRows.map((row) => (
                    <div className={styles.newRow} key={row._id}>

                      {/* Day */}
                      <div className={styles.newRowField}>
                        <label className={styles.newRowLabel}>Day</label>
                        <select
                          className={styles.select}
                          value={row.dayOfWeek}
                          onChange={(e) => updateRow(row._id, "dayOfWeek", e.target.value)}
                          required
                        >
                          <option value="" disabled>Select day</option>
                          {availableForRow(row._id, row.dayOfWeek).map((d) => (
                            <option key={d} value={d}>{DAY_MAP[d]}</option>
                          ))}
                        </select>
                      </div>

                      {/* Start time */}
                      <div className={styles.newRowField}>
                        <label className={styles.newRowLabel}>Start Time</label>
                        <input
                          type="time"
                          className={styles.timeInput}
                          value={row.startTime}
                          onChange={(e) => updateRow(row._id, "startTime", e.target.value)}
                          required
                        />
                      </div>

                      {/* End time */}
                      <div className={styles.newRowField}>
                        <label className={styles.newRowLabel}>End Time</label>
                        <input
                          type="time"
                          className={styles.timeInput}
                          value={row.endTime}
                          onChange={(e) => updateRow(row._id, "endTime", e.target.value)}
                          required
                        />
                      </div>

                      {/* Active toggle */}
                      <div className={styles.newRowField}>
                        <label className={styles.newRowLabel}>Active</label>
                        <div
                          className={`${styles.toggle} ${row.isActive ? styles.toggleOn : ""}`}
                          onClick={() => updateRow(row._id, "isActive", !row.isActive)}
                          role="switch"
                          aria-checked={row.isActive}
                          tabIndex={0}
                          onKeyDown={(e) => e.key === " " && updateRow(row._id, "isActive", !row.isActive)}
                        >
                          <div className={styles.toggleThumb} />
                        </div>
                      </div>

                      {/* Remove */}
                      <button
                        className={styles.removeBtn}
                        onClick={() => removeRow(row._id)}
                        aria-label="Remove row"
                        title="Remove"
                      ><Icon name="x" size={16} /></button>
                    </div>
                  ))}
                </div>

                {/* Validation error */}
                {saveError && (
                  <div className={styles.errorBox}>
                    <span className={styles.errorIcon}>!</span>{saveError}
                  </div>
                )}

                {/* Save / Cancel */}
                <div className={styles.saveRow}>
                  <button
                    className="btn btn-ghost"
                    onClick={() => { setNewRows([]); setSaveError(null); }}
                  >
                    Discard
                  </button>
                  <button
                    className="btn btn-primary"
                    onClick={handleSave}
                    disabled={saving}
                  >
                    {saving ? <span className={styles.btnSpinner} /> : `Save ${newRows.length} Rule${newRows.length > 1 ? "s" : ""}`}
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </main>

      {/* ── Delete confirm modal ── */}
      {deleteConfirm && (
        <>
          <div className={styles.modalBackdrop} onClick={() => setDeleteConfirm(null)} />
          <div className={styles.modal}>
            <div className={styles.modalHeader}>
              <h2 className={styles.modalTitle}>Delete Rule</h2>
              <button className={styles.closeBtn} onClick={() => setDeleteConfirm(null)}><Icon name="x" size={18} /></button>
            </div>
            <div className={styles.modalBody}>
              <p className={styles.deleteWarning}>Are you sure you want to delete this availability rule? Existing slots generated from this rule will not be affected, but no new slots will be generated for this day.</p>
              {deleteError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{deleteError}</div>}
              <div className={styles.modalActions}>
                <button className={styles.cancelBtn} onClick={() => setDeleteConfirm(null)}>Cancel</button>
                <button className={styles.deleteBtnConfirm} onClick={handleDeleteRule} disabled={deleteLoading}>
                  {deleteLoading ? <span className={styles.btnSpinner}/> : "Delete"}
                </button>
              </div>
            </div>
          </div>
        </>
      )}

      {/* ── Edit rule modal ── */}
      {editRule && (
        <>
          <div className={styles.modalBackdrop} onClick={() => setEditRule(null)} />
          <div className={styles.modal}>
            <div className={styles.modalHeader}>
              <h2 className={styles.modalTitle}>Edit Rule</h2>
              <button className={styles.closeBtn} onClick={() => setEditRule(null)}><Icon name="x" size={18} /></button>
            </div>
            <div className={styles.modalBody}>
              <div className={styles.editGrid}>
                <div className={styles.editField}>
                  <label className={styles.editLabel}>Day</label>
                  <select className={styles.select} value={editForm.dayOfWeek}
                    onChange={e => setEditForm(p => ({ ...p, dayOfWeek: Number(e.target.value) }))}>
                    {availableForEdit().map(d => <option key={d} value={d}>{DAY_MAP[d]}</option>)}
                  </select>
                </div>
                <div className={styles.editField}>
                  <label className={styles.editLabel}>Start Time</label>
                  <input className={styles.timeInput} type="time" value={editForm.startTime}
                    onChange={e => setEditForm(p => ({ ...p, startTime: e.target.value }))} />
                </div>
                <div className={styles.editField}>
                  <label className={styles.editLabel}>End Time</label>
                  <input className={styles.timeInput} type="time" value={editForm.endTime}
                    onChange={e => setEditForm(p => ({ ...p, endTime: e.target.value }))} />
                </div>
              </div>
              <div className={styles.editActiveRow}>
                <span className={styles.editLabel}>Active</span>
                <div className={`${styles.toggle} ${editForm.isActive ? styles.toggleOn : ""}`}
                  onClick={() => setEditForm(p => ({ ...p, isActive: !p.isActive }))} role="switch" tabIndex={0}>
                  <div className={styles.toggleThumb}/>
                </div>
              </div>
              {editError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{editError}</div>}
              <div className={styles.modalActions}>
                <button className={styles.cancelBtn} onClick={() => setEditRule(null)}>Cancel</button>
                <button className={styles.saveBtn} onClick={handleEditRule} disabled={editLoading}>
                  {editLoading ? <span className={styles.btnSpinner}/> : "Save Changes"}
                </button>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
