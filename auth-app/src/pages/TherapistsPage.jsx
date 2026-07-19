import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { getTherapists, getTherapistServices } from "../api/therapist";
import styles from "./TherapistsPage.module.css";

function getInitials(firstName, lastName) {
  return `${firstName?.[0] ?? ""}${lastName?.[0] ?? ""}`.toUpperCase();
}

function formatDate(dob) {
  if (!dob) return "—";
  return new Date(dob).toLocaleDateString("en-GB", {
    day: "2-digit", month: "short", year: "numeric",
  });
}

const SERVICE_TYPE_LABEL = {
  INDIVIDUAL_THERAPY: "Individual Therapy",
  COUPLES_THERAPY: "Couples Therapy",
};

const SERVICE_TYPE_ICON = {
  INDIVIDUAL_THERAPY: "🧍",
  COUPLES_THERAPY: "👫",
};

const GENDER_ICON = { Male: "♂", Female: "♀", Other: "⚧" };

export default function TherapistsPage() {
  const navigate = useNavigate();
  const [therapists, setTherapists] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState("");

  // Drawer state
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [selectedTherapist, setSelectedTherapist] = useState(null);
  const [services, setServices] = useState([]);
  const [servicesLoading, setServicesLoading] = useState(false);
  const [servicesError, setServicesError] = useState(null);

  useEffect(() => {
    getTherapists()
      .then(setTherapists)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const openDrawer = useCallback(async (therapist) => {
    setSelectedTherapist(therapist);
    setDrawerOpen(true);
    setServices([]);
    setServicesError(null);
    setServicesLoading(true);
    try {
      const data = await getTherapistServices(therapist.therapistId);
      setServices(data);
    } catch (e) {
      setServicesError(e.message);
    } finally {
      setServicesLoading(false);
    }
  }, []);

  const closeDrawer = useCallback(() => {
    setDrawerOpen(false);
    setTimeout(() => setSelectedTherapist(null), 300);
  }, []);

  // Close on Escape key
  useEffect(() => {
    const handler = (e) => { if (e.key === "Escape") closeDrawer(); };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [closeDrawer]);

  const filtered = therapists.filter((t) => {
    const q = search.toLowerCase();
    return (
      t.firstName?.toLowerCase().includes(q) ||
      t.lastName?.toLowerCase().includes(q) ||
      t.email?.toLowerCase().includes(q) ||
      t.gender?.toLowerCase().includes(q)
    );
  });

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <button className={styles.back} onClick={() => navigate("/client-home")}>← Back</button>
          <span className={styles.logo}>🧠 Therapy Connect</span>
        </div>
      </header>

      <main className={styles.main}>
        <div className={styles.hero}>
          <h1 className={styles.heading}>Therapists</h1>
          <p className={styles.sub}>
            {loading ? "Loading..." : `${therapists.length} therapist${therapists.length !== 1 ? "s" : ""} registered`}
          </p>
        </div>

        {!loading && !error && (
          <div className={styles.searchWrap}>
            <span className={styles.searchIcon}>⌕</span>
            <input
              className={styles.search}
              type="text"
              placeholder="Search by name, email, gender…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
        )}

        {loading && (
          <div className={styles.center}>
            <div className={styles.spinner} />
            <p className={styles.loadingText}>Fetching therapists…</p>
          </div>
        )}

        {error && (
          <div className={styles.errorBox}>
            <span className={styles.errorIcon}>!</span>{error}
          </div>
        )}

        {!loading && !error && filtered.length === 0 && (
          <div className={styles.center}>
            <p className={styles.empty}>
              {search ? "No therapists match your search." : "No therapists found."}
            </p>
          </div>
        )}

        <div className={styles.grid}>
          {filtered.map((t, i) => (
            <div
              className={`${styles.card} ${selectedTherapist?.therapistId === t.therapistId && drawerOpen ? styles.cardActive : ""}`}
              key={t.therapistId}
              style={{ animationDelay: `${i * 0.05}s` }}
              onClick={() => openDrawer(t)}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => e.key === "Enter" && openDrawer(t)}
            >
              <div className={styles.cardTop}>
                <div className={styles.avatar}>{getInitials(t.firstName, t.lastName)}</div>
                <div className={styles.nameBlock}>
                  <h3 className={styles.name}>{t.firstName} {t.lastName}</h3>
                  <span className={styles.gender}>{GENDER_ICON[t.gender] ?? "—"} {t.gender ?? "—"}</span>
                </div>
                <div className={styles.expBadge}>
                  <span className={styles.expNum}>{t.yearsOfExperience}</span>
                  <span className={styles.expLabel}>yrs</span>
                </div>
              </div>

              <div className={styles.divider} />

              <div className={styles.details}>
                <div className={styles.detail}>
                  <span className={styles.detailLabel}>Email</span>
                  <span className={styles.detailValue}>{t.email ?? "—"}</span>
                </div>
                <div className={styles.detail}>
                  <span className={styles.detailLabel}>Phone</span>
                  <span className={styles.detailValue}>{t.phoneNumber ?? "—"}</span>
                </div>
                <div className={styles.detail}>
                  <span className={styles.detailLabel}>Date of Birth</span>
                  <span className={styles.detailValue}>{formatDate(t.dob)}</span>
                </div>
              </div>

              <div className={styles.cardFooter}>
                <span className={styles.viewServices}>View services →</span>
              </div>
            </div>
          ))}
        </div>
      </main>

      {/* ── Backdrop ── */}
      <div
        className={`${styles.backdrop} ${drawerOpen ? styles.backdropVisible : ""}`}
        onClick={closeDrawer}
      />

      {/* ── Drawer ── */}
      <div className={`${styles.drawer} ${drawerOpen ? styles.drawerOpen : ""}`}>
        {selectedTherapist && (
          <>
            <div className={styles.drawerHeader}>
              <div className={styles.drawerAvatar}>
                {getInitials(selectedTherapist.firstName, selectedTherapist.lastName)}
              </div>
              <div className={styles.drawerNameBlock}>
                <h2 className={styles.drawerName}>
                  {selectedTherapist.firstName} {selectedTherapist.lastName}
                </h2>
                <p className={styles.drawerMeta}>
                  {selectedTherapist.yearsOfExperience} yrs experience &nbsp;·&nbsp; {selectedTherapist.gender ?? "—"}
                </p>
              </div>
              <button className={styles.closeBtn} onClick={closeDrawer} aria-label="Close">✕</button>
            </div>

            <div className={styles.drawerDivider} />

            <div className={styles.drawerSection}>
              <h3 className={styles.drawerSectionTitle}>Services offered</h3>

              {servicesLoading && (
                <div className={styles.drawerCenter}>
                  <div className={styles.drawerSpinner} />
                  <p className={styles.loadingText}>Loading services…</p>
                </div>
              )}

              {servicesError && (
                <div className={styles.errorBox}>
                  <span className={styles.errorIcon}>!</span>{servicesError}
                </div>
              )}

              {!servicesLoading && !servicesError && services.length === 0 && (
                <p className={styles.empty}>No services listed.</p>
              )}

              <div className={styles.serviceList}>
                {services.map((s) => (
                  <div className={styles.serviceCard} key={s.serviceId}>
                    <div className={styles.serviceTop}>
                      <span className={styles.serviceIcon}>
                        {SERVICE_TYPE_ICON[s.serviceType] ?? "💬"}
                      </span>
                      <div className={styles.serviceInfo}>
                        <span className={styles.serviceType}>
                          {SERVICE_TYPE_LABEL[s.serviceType] ?? s.serviceType}
                        </span>
                        <span className={`${styles.serviceBadge} ${s.isActive ? styles.badgeActive : styles.badgeInactive}`}>
                          {s.isActive ? "Active" : "Inactive"}
                        </span>
                      </div>
                    </div>
                    {/* price removed: service-level price is dead — fees come
                        from delivery modes, chosen at booking time */}
                    <div className={styles.serviceDetails}>
                      <div className={styles.serviceStat}>
                        <span className={styles.serviceStatLabel}>Duration</span>
                        <span className={styles.serviceStatValue}>{s.duration} min</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
