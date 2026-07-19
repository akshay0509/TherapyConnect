import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getMyServices, createService, updateService, deleteService } from "../api/therapistServices";
import {
  getDeliveryModes, createDeliveryMode, updateDeliveryMode, deleteDeliveryMode,
} from "../api/deliveryModes";
import { useDeliveryModes } from "../context/DeliveryModesContext";
import styles from "./MyServicesPage.module.css";

const SERVICE_TYPE_LABEL = {
  INDIVIDUAL_THERAPY: "Individual Therapy",
  COUPLES_THERAPY: "Couples Therapy",
};
const SERVICE_TYPE_ICON = {
  INDIVIDUAL_THERAPY: "🧍",
  COUPLES_THERAPY: "👫",
};

const MODE_TYPE_LABEL = {
  ONLINE: "Online",
  OFFLINE: "Offline",
};
const MODE_TYPE_ICON = {
  ONLINE: "💻",
  OFFLINE: "📍",
};

// no service-level price: pricing lives on delivery modes
const EMPTY_FORM = {
  serviceType: "",
  duration: "",
  isActive: true,
};

const EMPTY_MODE_FORM = {
  modeType: "",
  displayName: "",
  address: "",
  price: "",
  isActive: true,
};

export default function MyServicesPage() {
  const navigate = useNavigate();
  const { refresh: refreshModeContext } = useDeliveryModes();

  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formLoading, setFormLoading] = useState(false);
  const [formError, setFormError] = useState(null);

  // Edit service
  const [editService, setEditService]     = useState(null);
  const [editForm, setEditForm]           = useState(EMPTY_FORM);
  const [editLoading, setEditLoading]     = useState(false);
  const [editError, setEditError]         = useState(null);

  // Delete service
  const [deleteConfirm, setDeleteConfirm] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [deleteError, setDeleteError]     = useState(null);

  // Delivery modes per service
  const [expandedServiceId, setExpandedServiceId] = useState(null);
  const [modesMap, setModesMap]                   = useState({}); // serviceId → modes[]
  const [modesLoading, setModesLoading]           = useState(false);

  // Delivery mode form
  const [modeFormServiceId, setModeFormServiceId]   = useState(null);
  const [modeForm, setModeForm]                     = useState(EMPTY_MODE_FORM);
  const [modeFormLoading, setModeFormLoading]       = useState(false);
  const [modeFormError, setModeFormError]           = useState(null);

  // Edit mode
  const [editMode, setEditMode]         = useState(null); // full mode object
  const [editModeForm, setEditModeForm] = useState(EMPTY_MODE_FORM);
  const [editModeLoading, setEditModeLoading] = useState(false);
  const [editModeError, setEditModeError]     = useState(null);

  // Delete mode
  const [deleteModeConfirm, setDeleteModeConfirm] = useState(null); // { modeId, serviceId }
  const [deleteModeLoading, setDeleteModeLoading] = useState(false);
  const [deleteModeError, setDeleteModeError]     = useState(null);

  useEffect(() => {
    fetchServices();
  }, []);

  const fetchServices = () => {
    setLoading(true);
    setError(null);
    getMyServices()
      .then(setServices)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({ ...prev, [name]: type === "checkbox" ? checked : value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormLoading(true);
    setFormError(null);
    try {
      const payload = {
        serviceType: form.serviceType,
        duration: parseInt(form.duration, 10),
        isActive: form.isActive,
      };
      // createService returns void — re-fetch to get the new service with its generated ID
      await createService(payload);
      await fetchServices();
      setForm(EMPTY_FORM);
      setShowForm(false);
    } catch (err) {
      setFormError(err.message);
    } finally {
      setFormLoading(false);
    }
  };

  const openForm = () => { setForm(EMPTY_FORM); setFormError(null); setShowForm(true); };
  const closeForm = () => { setShowForm(false); setFormError(null); };

  const openEdit = (s) => {
    setEditService(s);
    setEditForm({ serviceType: s.serviceType, duration: String(s.duration), isActive: s.isActive });
    setEditError(null);
  };
  const closeEdit = () => { setEditService(null); setEditError(null); };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    setEditLoading(true); setEditError(null);
    try {
      const payload = {
        serviceType: editForm.serviceType,
        duration: parseInt(editForm.duration, 10),
        isActive: editForm.isActive,
      };
      const updated = await updateService(editService.serviceId, payload);
      setServices(prev => prev.map(s => s.serviceId === editService.serviceId ? { ...s, ...updated } : s));
      closeEdit();
    } catch (err) {
      setEditError(err.message);
    } finally {
      setEditLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteConfirm) return;
    setDeleteLoading(true); setDeleteError(null);
    try {
      await deleteService(deleteConfirm);
      setServices(prev => prev.filter(s => s.serviceId !== deleteConfirm));
      setDeleteConfirm(null);
    } catch (err) {
      setDeleteError(err.message);
    } finally {
      setDeleteLoading(false);
    }
  };

  // ── Delivery modes ────────────────────────────────────────
  const toggleExpand = async (serviceId) => {
    if (expandedServiceId === serviceId) { setExpandedServiceId(null); return; }
    setExpandedServiceId(serviceId);
    if (!modesMap[serviceId]) {
      setModesLoading(true);
      try {
        const modes = await getDeliveryModes(serviceId);
        setModesMap(prev => ({ ...prev, [serviceId]: modes }));
      } catch {
        setModesMap(prev => ({ ...prev, [serviceId]: [] }));
      } finally {
        setModesLoading(false);
      }
    }
  };

  const openModeForm = (serviceId) => {
    setModeFormServiceId(serviceId);
    setModeForm(EMPTY_MODE_FORM);
    setModeFormError(null);
  };
  const closeModeForm = () => { setModeFormServiceId(null); setModeFormError(null); };

  const handleModeSubmit = async (e) => {
    e.preventDefault();
    setModeFormLoading(true); setModeFormError(null);
    try {
      const payload = {
        serviceId: modeFormServiceId,
        modeType: modeForm.modeType,
        displayName: modeForm.displayName,
        address: modeForm.address || undefined,
        price: parseFloat(modeForm.price),
        isActive: modeForm.isActive,
      };
      const created = await createDeliveryMode(payload);
      setModesMap(prev => ({ ...prev, [modeFormServiceId]: [...(prev[modeFormServiceId] || []), created] }));
      refreshModeContext();
      closeModeForm();
    } catch (err) {
      setModeFormError(err.message);
    } finally {
      setModeFormLoading(false);
    }
  };

  const openEditMode = (mode) => {
    setEditMode(mode);
    setEditModeForm({
      modeType: mode.modeType,
      displayName: mode.displayName,
      address: mode.address || "",
      price: mode.price != null ? String(mode.price) : "",
      isActive: mode.isActive,
    });
    setEditModeError(null);
  };
  const closeEditMode = () => { setEditMode(null); setEditModeError(null); };

  const handleEditModeSubmit = async (e) => {
    e.preventDefault();
    setEditModeLoading(true); setEditModeError(null);
    try {
      const payload = {
        modeType: editModeForm.modeType,
        displayName: editModeForm.displayName,
        address: editModeForm.address || undefined,
        price: parseFloat(editModeForm.price),
        isActive: editModeForm.isActive,
      };
      const updated = await updateDeliveryMode(editMode.modeId, payload);
      const sid = editMode.serviceId;
      setModesMap(prev => ({
        ...prev,
        [sid]: (prev[sid] || []).map(m => m.modeId === editMode.modeId ? { ...m, ...updated } : m),
      }));
      refreshModeContext();
      closeEditMode();
    } catch (err) {
      setEditModeError(err.message);
    } finally {
      setEditModeLoading(false);
    }
  };

  const handleDeleteMode = async () => {
    if (!deleteModeConfirm) return;
    setDeleteModeLoading(true); setDeleteModeError(null);
    try {
      await deleteDeliveryMode(deleteModeConfirm.modeId);
      setModesMap(prev => ({
        ...prev,
        [deleteModeConfirm.serviceId]: (prev[deleteModeConfirm.serviceId] || []).filter(m => m.modeId !== deleteModeConfirm.modeId),
      }));
      refreshModeContext();
      setDeleteModeConfirm(null);
    } catch (err) {
      setDeleteModeError(err.message);
    } finally {
      setDeleteModeLoading(false);
    }
  };

  const anyModalOpen = showForm || !!editService || !!deleteConfirm || !!modeFormServiceId || !!editMode || !!deleteModeConfirm;

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
          <div className={styles.topRowText}>
            <h1 className={styles.heading}>My Services</h1>
            <p className={styles.sub}>
              {loading ? "Loading…" : `${services.length} service${services.length !== 1 ? "s" : ""} listed`}
            </p>
          </div>
          {!loading && (
            <button className={styles.addBtn} onClick={openForm}>
              <span className={styles.addIcon}>+</span> Add Service
            </button>
          )}
        </div>

        {loading && (
          <div className={styles.center}>
            <div className={styles.spinner} />
            <p className={styles.loadingText}>Fetching your services…</p>
          </div>
        )}

        {!loading && error && (
          <div className={styles.errorBox}>
            <span className={styles.errorIcon}>!</span>{error}
          </div>
        )}

        {!loading && !error && services.length === 0 && (
          <div className={styles.center}>
            <span className={styles.emptyIcon}>📋</span>
            <h2 className={styles.emptyTitle}>No services yet</h2>
            <p className={styles.emptyText}>Add your first therapy service to get started.</p>
            <button className={styles.addBtnLarge} onClick={openForm}>+ Add Service</button>
          </div>
        )}

        {!loading && !error && services.length > 0 && (
          <div className={styles.grid}>
            {services.map((s, i) => (
              <div className={styles.card} key={s.serviceId} style={{ animationDelay: `${i * 0.06}s` }}>
                <div className={styles.cardTop}>
                  <span className={styles.serviceIcon}>{SERVICE_TYPE_ICON[s.serviceType] ?? "💬"}</span>
                  <div className={styles.serviceInfo}>
                    <span className={styles.serviceType}>
                      {SERVICE_TYPE_LABEL[s.serviceType] ?? s.serviceType}
                    </span>
                    <span className={`${styles.badge} ${s.isActive ? styles.badgeActive : styles.badgeInactive}`}>
                      {s.isActive ? "Active" : "Inactive"}
                    </span>
                  </div>
                </div>

                <div className={styles.cardDivider} />

                <div className={styles.statsRow}>
                  <div className={styles.stat}>
                    <span className={styles.statLabel}>Duration</span>
                    <span className={styles.statValue}>{s.duration} <span className={styles.statUnit}>min</span></span>
                  </div>
                </div>

                <div className={styles.serviceId}>ID: {s.serviceId}</div>
                <div className={styles.cardActions}>
                  <button className={styles.cardEditBtn} onClick={() => openEdit(s)}>✏️ Edit</button>
                  <button className={styles.cardDeleteBtn} onClick={() => { setDeleteConfirm(s.serviceId); setDeleteError(null); }}>🗑 Delete</button>
                  <button className={styles.modesToggleBtn} onClick={() => toggleExpand(s.serviceId)}>
                    {expandedServiceId === s.serviceId ? "▲ Modes" : "▼ Delivery Modes"}
                  </button>
                </div>

                {/* ── Delivery modes section ── */}
                {expandedServiceId === s.serviceId && (
                  <div className={styles.modesSection}>
                    <div className={styles.modesSectionHeader}>
                      <span className={styles.modesSectionTitle}>Delivery Modes</span>
                      <button className={styles.addModeBtn} onClick={() => openModeForm(s.serviceId)}>+ Add Mode</button>
                    </div>

                    {modesLoading && !modesMap[s.serviceId] && (
                      <div className={styles.modesLoading}><div className={styles.spinnerSm}/></div>
                    )}

                    {modesMap[s.serviceId]?.length === 0 && (
                      <p className={styles.modesEmpty}>No delivery modes configured for this service.</p>
                    )}

                    {(modesMap[s.serviceId] || []).map(mode => (
                      <div key={mode.modeId} className={styles.modeItem}>
                        <div className={styles.modeItemLeft}>
                          <span className={styles.modeItemIcon}>{MODE_TYPE_ICON[mode.modeType] ?? "💬"}</span>
                          <div className={styles.modeItemInfo}>
                            <span className={styles.modeItemName}>{mode.displayName}</span>
                            <span className={styles.modeItemType}>{MODE_TYPE_LABEL[mode.modeType] ?? mode.modeType}</span>
                            {mode.address && <span className={styles.modeItemAddress}>{mode.address}</span>}
                            {mode.price != null && <span className={styles.modeItemPrice}>₹{parseFloat(mode.price).toFixed(2)}</span>}
                          </div>
                        </div>
                        <div className={styles.modeItemActions}>
                          <span className={`${styles.modeBadge} ${mode.isActive ? styles.badgeActive : styles.badgeInactive}`}>
                            {mode.isActive ? "Active" : "Inactive"}
                          </span>
                          <button className={styles.modeEditBtn} onClick={() => openEditMode(mode)}>✏️</button>
                          <button className={styles.modeDeleteBtn} onClick={() => { setDeleteModeConfirm({ modeId: mode.modeId, serviceId: s.serviceId }); setDeleteModeError(null); }}>🗑</button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </main>

      {/* ── Backdrop ── */}
      <div className={`${styles.backdrop} ${anyModalOpen ? styles.backdropVisible : ""}`}
        onClick={() => { closeForm(); closeEdit(); setDeleteConfirm(null); closeModeForm(); closeEditMode(); setDeleteModeConfirm(null); }} />

      {/* ── Add service slide-up panel ── */}
      <div className={`${styles.formPanel} ${showForm ? styles.formPanelOpen : ""}`}>
        <div className={styles.formPanelHeader}>
          <h2 className={styles.formPanelTitle}>Add New Service</h2>
          <button className={styles.closeBtn} onClick={closeForm} aria-label="Close">✕</button>
        </div>

        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.field}>
            <label className={styles.label}>Service Type</label>
            <div className={styles.typeOptions}>
              {Object.entries(SERVICE_TYPE_LABEL).map(([value, label]) => (
                <label
                  key={value}
                  className={`${styles.typeOption} ${form.serviceType === value ? styles.typeOptionSelected : ""}`}
                >
                  <input
                    type="radio"
                    name="serviceType"
                    value={value}
                    checked={form.serviceType === value}
                    onChange={handleChange}
                    className={styles.hiddenRadio}
                    required
                  />
                  <span className={styles.typeOptionIcon}>{SERVICE_TYPE_ICON[value]}</span>
                  <span className={styles.typeOptionLabel}>{label}</span>
                </label>
              ))}
            </div>
          </div>

          <div className={styles.formRow}>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="duration">Duration (minutes)</label>
              <input
                id="duration" name="duration" type="number"
                min="1" max="480" required
                value={form.duration} onChange={handleChange}
                className={styles.input} placeholder="e.g. 60"
              />
            </div>
          </div>

          <div className={styles.field}>
            <label className={styles.toggleRow}>
              <div className={styles.toggleInfo}>
                <span className={styles.label}>Active</span>
                <span className={styles.toggleSub}>Make this service immediately available</span>
              </div>
              <div
                className={`${styles.toggle} ${form.isActive ? styles.toggleOn : ""}`}
                onClick={() => setForm((p) => ({ ...p, isActive: !p.isActive }))}
                role="switch"
                aria-checked={form.isActive}
                tabIndex={0}
                onKeyDown={(e) => e.key === " " && setForm((p) => ({ ...p, isActive: !p.isActive }))}
              >
                <div className={styles.toggleThumb} />
              </div>
            </label>
          </div>

          {formError && (
            <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{formError}</div>
          )}

          <div className={styles.formActions}>
            <button type="button" className={styles.cancelBtn} onClick={closeForm}>Cancel</button>
            <button type="submit" className={styles.submitBtn} disabled={formLoading}>
              {formLoading ? <span className={styles.btnSpinner} /> : "Create Service"}
            </button>
          </div>
        </form>
      </div>

      {/* ── Edit Service modal ── */}
      {editService && (
        <div className={styles.modal}>
          <div className={styles.modalHeader}>
            <h2 className={styles.formPanelTitle}>Edit Service</h2>
            <button className={styles.closeBtn} onClick={closeEdit}>✕</button>
          </div>
          <form onSubmit={handleEditSubmit} className={styles.form}>
            <div className={styles.field}>
              <label className={styles.label}>Service Type</label>
              <div className={styles.typeOptions}>
                {Object.entries(SERVICE_TYPE_LABEL).map(([value, label]) => (
                  <label key={value} className={`${styles.typeOption} ${editForm.serviceType === value ? styles.typeOptionSelected : ""}`}>
                    <input type="radio" name="editServiceType" value={value}
                      checked={editForm.serviceType === value}
                      onChange={() => setEditForm(p => ({ ...p, serviceType: value }))}
                      className={styles.hiddenRadio} required />
                    <span className={styles.typeOptionIcon}>{SERVICE_TYPE_ICON[value]}</span>
                    <span className={styles.typeOptionLabel}>{label}</span>
                  </label>
                ))}
              </div>
            </div>
            <div className={styles.formRow}>
              <div className={styles.field}>
                <label className={styles.label}>Duration (minutes)</label>
                <input className={styles.input} type="number" min="1" max="480" required
                  value={editForm.duration} onChange={e => setEditForm(p => ({ ...p, duration: e.target.value }))} placeholder="e.g. 60" />
              </div>
            </div>
            <div className={styles.field}>
              <label className={styles.toggleRow}>
                <div className={styles.toggleInfo}>
                  <span className={styles.label}>Active</span>
                  <span className={styles.toggleSub}>Make this service available for booking</span>
                </div>
                <div className={`${styles.toggle} ${editForm.isActive ? styles.toggleOn : ""}`}
                  onClick={() => setEditForm(p => ({ ...p, isActive: !p.isActive }))} role="switch" tabIndex={0}>
                  <div className={styles.toggleThumb} />
                </div>
              </label>
            </div>
            {editError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{editError}</div>}
            <div className={styles.formActions}>
              <button type="button" className={styles.cancelBtn} onClick={closeEdit}>Cancel</button>
              <button type="submit" className={styles.submitBtn} disabled={editLoading}>
                {editLoading ? <span className={styles.btnSpinner}/> : "Save Changes"}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* ── Delete service confirm modal ── */}
      {deleteConfirm && (
        <div className={styles.modal}>
          <div className={styles.modalHeader}>
            <h2 className={styles.formPanelTitle}>Delete Service</h2>
            <button className={styles.closeBtn} onClick={() => setDeleteConfirm(null)}>✕</button>
          </div>
          <div className={styles.form}>
            <p className={styles.deleteWarning}>Are you sure you want to delete this service? This cannot be undone.</p>
            {deleteError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{deleteError}</div>}
            <div className={styles.formActions}>
              <button className={styles.cancelBtn} onClick={() => setDeleteConfirm(null)}>Cancel</button>
              <button className={styles.deleteBtnConfirm} onClick={handleDelete} disabled={deleteLoading}>
                {deleteLoading ? <span className={styles.btnSpinner}/> : "🗑 Delete"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Add delivery mode modal ── */}
      {modeFormServiceId && (
        <div className={styles.modal}>
          <div className={styles.modalHeader}>
            <h2 className={styles.formPanelTitle}>Add Delivery Mode</h2>
            <button className={styles.closeBtn} onClick={closeModeForm}>✕</button>
          </div>
          <form onSubmit={handleModeSubmit} className={styles.form}>
            <div className={styles.field}>
              <label className={styles.label}>Mode Type</label>
              <div className={styles.typeOptions}>
                {Object.entries(MODE_TYPE_LABEL).map(([value, label]) => (
                  <label key={value} className={`${styles.typeOption} ${modeForm.modeType === value ? styles.typeOptionSelected : ""}`}>
                    <input type="radio" name="modeType" value={value}
                      checked={modeForm.modeType === value}
                      onChange={() => setModeForm(p => ({ ...p, modeType: value }))}
                      className={styles.hiddenRadio} required />
                    <span className={styles.typeOptionIcon}>{MODE_TYPE_ICON[value]}</span>
                    <span className={styles.typeOptionLabel}>{label}</span>
                  </label>
                ))}
              </div>
            </div>
            <div className={styles.field}>
              <label className={styles.label}>Display Name</label>
              <input className={styles.input} type="text" required placeholder="e.g. Video call via Zoom"
                value={modeForm.displayName} onChange={e => setModeForm(p => ({ ...p, displayName: e.target.value }))} />
            </div>
            {modeForm.modeType && modeForm.modeType !== "ONLINE" && (
              <div className={styles.field}>
                <label className={styles.label}>Address</label>
                <input className={styles.input} type="text" placeholder="e.g. 12 Main Street, Halusuru"
                  value={modeForm.address} onChange={e => setModeForm(p => ({ ...p, address: e.target.value }))} />
              </div>
            )}
            <div className={styles.field}>
              <label className={styles.label}>Price (₹)</label>
              <input className={styles.input} type="number" min="0.01" step="0.01" required placeholder="e.g. 1500.00"
                value={modeForm.price} onChange={e => setModeForm(p => ({ ...p, price: e.target.value }))} />
            </div>
            <div className={styles.field}>
              <label className={styles.toggleRow}>
                <div className={styles.toggleInfo}>
                  <span className={styles.label}>Active</span>
                  <span className={styles.toggleSub}>Make this mode available for booking</span>
                </div>
                <div className={`${styles.toggle} ${modeForm.isActive ? styles.toggleOn : ""}`}
                  onClick={() => setModeForm(p => ({ ...p, isActive: !p.isActive }))} role="switch" tabIndex={0}>
                  <div className={styles.toggleThumb} />
                </div>
              </label>
            </div>
            {modeFormError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{modeFormError}</div>}
            <div className={styles.formActions}>
              <button type="button" className={styles.cancelBtn} onClick={closeModeForm}>Cancel</button>
              <button type="submit" className={styles.submitBtn} disabled={modeFormLoading}>
                {modeFormLoading ? <span className={styles.btnSpinner}/> : "Create Mode"}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* ── Edit delivery mode modal ── */}
      {editMode && (
        <div className={styles.modal}>
          <div className={styles.modalHeader}>
            <h2 className={styles.formPanelTitle}>Edit Delivery Mode</h2>
            <button className={styles.closeBtn} onClick={closeEditMode}>✕</button>
          </div>
          <form onSubmit={handleEditModeSubmit} className={styles.form}>
            <div className={styles.field}>
              <label className={styles.label}>Mode Type</label>
              <div className={styles.typeOptions}>
                {Object.entries(MODE_TYPE_LABEL).map(([value, label]) => (
                  <label key={value} className={`${styles.typeOption} ${editModeForm.modeType === value ? styles.typeOptionSelected : ""}`}>
                    <input type="radio" name="editModeType" value={value}
                      checked={editModeForm.modeType === value}
                      onChange={() => setEditModeForm(p => ({ ...p, modeType: value }))}
                      className={styles.hiddenRadio} required />
                    <span className={styles.typeOptionIcon}>{MODE_TYPE_ICON[value]}</span>
                    <span className={styles.typeOptionLabel}>{label}</span>
                  </label>
                ))}
              </div>
            </div>
            <div className={styles.field}>
              <label className={styles.label}>Display Name</label>
              <input className={styles.input} type="text" required
                value={editModeForm.displayName} onChange={e => setEditModeForm(p => ({ ...p, displayName: e.target.value }))} />
            </div>
            {editModeForm.modeType && editModeForm.modeType !== "ONLINE" && (
              <div className={styles.field}>
                <label className={styles.label}>Address</label>
                <input className={styles.input} type="text"
                  value={editModeForm.address} onChange={e => setEditModeForm(p => ({ ...p, address: e.target.value }))} />
              </div>
            )}
            <div className={styles.field}>
              <label className={styles.label}>Price (₹)</label>
              <input className={styles.input} type="number" min="0.01" step="0.01" required
                value={editModeForm.price} onChange={e => setEditModeForm(p => ({ ...p, price: e.target.value }))} />
            </div>
            <div className={styles.field}>
              <label className={styles.toggleRow}>
                <div className={styles.toggleInfo}>
                  <span className={styles.label}>Active</span>
                </div>
                <div className={`${styles.toggle} ${editModeForm.isActive ? styles.toggleOn : ""}`}
                  onClick={() => setEditModeForm(p => ({ ...p, isActive: !p.isActive }))} role="switch" tabIndex={0}>
                  <div className={styles.toggleThumb} />
                </div>
              </label>
            </div>
            {editModeError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{editModeError}</div>}
            <div className={styles.formActions}>
              <button type="button" className={styles.cancelBtn} onClick={closeEditMode}>Cancel</button>
              <button type="submit" className={styles.submitBtn} disabled={editModeLoading}>
                {editModeLoading ? <span className={styles.btnSpinner}/> : "Save Changes"}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* ── Delete mode confirm modal ── */}
      {deleteModeConfirm && (
        <div className={styles.modal}>
          <div className={styles.modalHeader}>
            <h2 className={styles.formPanelTitle}>Delete Delivery Mode</h2>
            <button className={styles.closeBtn} onClick={() => setDeleteModeConfirm(null)}>✕</button>
          </div>
          <div className={styles.form}>
            <p className={styles.deleteWarning}>Are you sure you want to delete this delivery mode? This cannot be undone.</p>
            {deleteModeError && <div className={styles.errorBox}><span className={styles.errorIcon}>!</span>{deleteModeError}</div>}
            <div className={styles.formActions}>
              <button className={styles.cancelBtn} onClick={() => setDeleteModeConfirm(null)}>Cancel</button>
              <button className={styles.deleteBtnConfirm} onClick={handleDeleteMode} disabled={deleteModeLoading}>
                {deleteModeLoading ? <span className={styles.btnSpinner}/> : "🗑 Delete"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
