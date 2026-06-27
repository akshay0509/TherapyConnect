import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getMyServices, createService, updateService, deleteService } from "../api/therapistServices";
import styles from "./MyServicesPage.module.css";

const SERVICE_TYPE_LABEL = {
  INDIVIDUAL_THERAPY: "Individual Therapy",
  COUPLES_THERAPY: "Couples Therapy",
};
const SERVICE_TYPE_ICON = {
  INDIVIDUAL_THERAPY: "🧍",
  COUPLES_THERAPY: "👫",
};

const EMPTY_FORM = {
  serviceType: "",
  duration: "",
  price: "",
  isActive: true,
};

export default function MyServicesPage() {
  const navigate = useNavigate();

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
  const [deleteConfirm, setDeleteConfirm] = useState(null); // serviceId
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [deleteError, setDeleteError]     = useState(null);

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
        price: parseFloat(form.price),
        isActive: form.isActive,
      };
      const created = await createService(payload);
      setServices((prev) => [...prev, created]);
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
    setEditForm({ serviceType: s.serviceType, duration: String(s.duration), price: String(s.price), isActive: s.isActive });
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
        price: parseFloat(editForm.price),
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

        {/* ── Loading ── */}
        {loading && (
          <div className={styles.center}>
            <div className={styles.spinner} />
            <p className={styles.loadingText}>Fetching your services…</p>
          </div>
        )}

        {/* ── Error ── */}
        {!loading && error && (
          <div className={styles.errorBox}>
            <span className={styles.errorIcon}>!</span>{error}
          </div>
        )}

        {/* ── Empty ── */}
        {!loading && !error && services.length === 0 && (
          <div className={styles.center}>
            <span className={styles.emptyIcon}>📋</span>
            <h2 className={styles.emptyTitle}>No services yet</h2>
            <p className={styles.emptyText}>Add your first therapy service to get started.</p>
            <button className={styles.addBtnLarge} onClick={openForm}>+ Add Service</button>
          </div>
        )}

        {/* ── Services grid ── */}
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
                  <div className={styles.statDivider} />
                  <div className={styles.stat}>
                    <span className={styles.statLabel}>Price</span>
                    <span className={styles.statValue}>₹{parseFloat(s.price).toFixed(2)}</span>
                  </div>
                </div>

                <div className={styles.serviceId}>ID: {s.serviceId}</div>
                <div className={styles.cardActions}>
                  <button className={styles.cardEditBtn} onClick={() => openEdit(s)}>✏️ Edit</button>
                  <button className={styles.cardDeleteBtn} onClick={() => { setDeleteConfirm(s.serviceId); setDeleteError(null); }}>🗑 Delete</button>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>

      {/* ── Backdrop ── */}
      <div className={`${styles.backdrop} ${showForm || !!editService || !!deleteConfirm ? styles.backdropVisible : ""}`}
        onClick={() => { closeForm(); closeEdit(); setDeleteConfirm(null); }} />

      {/* ── Slide-up form panel ── */}
      <div className={`${styles.formPanel} ${showForm ? styles.formPanelOpen : ""}`}>
        <div className={styles.formPanelHeader}>
          <h2 className={styles.formPanelTitle}>Add New Service</h2>
          <button className={styles.closeBtn} onClick={closeForm} aria-label="Close">✕</button>
        </div>

        <form onSubmit={handleSubmit} className={styles.form}>
          {/* Service Type */}
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
            {/* Duration */}
            <div className={styles.field}>
              <label className={styles.label} htmlFor="duration">Duration (minutes)</label>
              <input
                id="duration" name="duration" type="number"
                min="1" max="480" required
                value={form.duration} onChange={handleChange}
                className={styles.input} placeholder="e.g. 60"
              />
            </div>

            {/* Price */}
            <div className={styles.field}>
              <label className={styles.label} htmlFor="price">Price (₹)</label>
              <input
                id="price" name="price" type="number"
                min="0" step="0.01" required
                value={form.price} onChange={handleChange}
                className={styles.input} placeholder="e.g. 1500.00"
              />
            </div>
          </div>

          {/* isActive toggle */}
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
            <div className={styles.errorBox}>
              <span className={styles.errorIcon}>!</span>{formError}
            </div>
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
              <div className={styles.field}>
                <label className={styles.label}>Price (₹)</label>
                <input className={styles.input} type="number" min="0" step="0.01" required
                  value={editForm.price} onChange={e => setEditForm(p => ({ ...p, price: e.target.value }))} placeholder="e.g. 1500.00" />
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

      {/* ── Delete confirm modal ── */}
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
    </div>
  );
}
