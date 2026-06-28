import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { getEarningsSummary, getEarningsSessions, exportEarningsCsv } from "../api/earnings";
import { useModeMap } from "../context/DeliveryModesContext";
import api from "../api/client";
import styles from "./EarningsPage.module.css";

function toISODate(date) {
  const d = new Date(date);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function startOfMonth() {
  const d = new Date();
  return toISODate(new Date(d.getFullYear(), d.getMonth(), 1));
}

function today() {
  return toISODate(new Date());
}

function formatCurrency(value) {
  const n = Number(value ?? 0);
  return `₹${n.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function formatDateTime(dt) {
  if (!dt) return "—";
  return new Date(dt).toLocaleString("en-IN", {
    day: "2-digit", month: "short", year: "numeric",
    hour: "2-digit", minute: "2-digit", hour12: true,
  });
}

function formatServiceType(serviceType) {
  if (!serviceType) return "—";
  return serviceType.toLowerCase().split("_").map(w => w[0].toUpperCase() + w.slice(1)).join(" ");
}

export default function EarningsPage() {
  const navigate = useNavigate();
  const modeMap = useModeMap();

  // Summary (auto-loads on mount)
  const [summary, setSummary]             = useState(null);
  const [summaryLoading, setSummaryLoading] = useState(true);
  const [summaryError, setSummaryError]   = useState(null);

  // Services for filter dropdown
  const [services, setServices]           = useState([]);

  // Sessions filter
  const [fromDate, setFromDate]           = useState(startOfMonth());
  const [toDate, setToDate]               = useState(today());
  const [filterServiceId, setFilterServiceId] = useState("");
  const [filterModeId, setFilterModeId]   = useState("");

  // Sessions result
  const [sessions, setSessions]           = useState([]);
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const [sessionsError, setSessionsError] = useState(null);
  const [sessionsLoaded, setSessionsLoaded] = useState(false);

  // Export
  const [exporting, setExporting]         = useState(false);
  const [exportError, setExportError]     = useState(null);

  // Sort
  const [sortField, setSortField]         = useState("startTime");
  const [sortDir, setSortDir]             = useState("desc");

  useEffect(() => {
    async function loadInitial() {
      setSummaryLoading(true);
      try {
        const [sum, svcResp] = await Promise.all([
          getEarningsSummary(),
          api.get("/therapist/therapist-services"),
        ]);
        setSummary(sum);
        setServices(svcResp.data ?? []);
      } catch (err) {
        setSummaryError(err.message || "Failed to load earnings summary.");
      } finally {
        setSummaryLoading(false);
      }
    }
    loadInitial();
  }, []);

  const handleLoadSessions = async () => {
    if (!fromDate || !toDate) { setSessionsError("Please select both dates."); return; }
    if (fromDate > toDate) { setSessionsError("From date must be before to date."); return; }
    setSessionsLoading(true); setSessionsError(null); setSessionsLoaded(false);
    try {
      const sess = await getEarningsSessions(
        fromDate, toDate,
        filterServiceId || null,
        filterModeId || null
      );
      setSessions(sess);
      setSessionsLoaded(true);
    } catch (err) {
      setSessionsError(err.message);
    } finally {
      setSessionsLoading(false);
    }
  };

  const handleExport = async () => {
    setExporting(true); setExportError(null);
    try {
      await exportEarningsCsv(fromDate, toDate, filterServiceId || null, filterModeId || null);
    } catch (err) {
      setExportError(err.message);
    } finally {
      setExporting(false);
    }
  };

  const handleSort = (field) => {
    if (sortField === field) {
      setSortDir(d => d === "asc" ? "desc" : "asc");
    } else {
      setSortField(field);
      setSortDir("desc");
    }
  };

  const sortedSessions = [...sessions].sort((a, b) => {
    let va = a[sortField], vb = b[sortField];
    if (sortField === "startTime") { va = new Date(va); vb = new Date(vb); }
    if (sortField === "earningAmount" || sortField === "sessionFee") { va = Number(va ?? 0); vb = Number(vb ?? 0); }
    if (va < vb) return sortDir === "asc" ? -1 : 1;
    if (va > vb) return sortDir === "asc" ? 1 : -1;
    return 0;
  });

  const sortIcon = (field) => {
    if (sortField !== field) return " ↕";
    return sortDir === "asc" ? " ↑" : " ↓";
  };

  const allModes = Object.values(modeMap);

  const summaryPeriods = summary ? [
    {
      label: "This Week",
      earnings: summary.weekEarnings,
      paidCount: summary.weekPaidCount,
      dsfCount: summary.weekDsfCount,
    },
    {
      label: "This Month",
      earnings: summary.monthEarnings,
      paidCount: summary.monthPaidCount,
      dsfCount: summary.monthDsfCount,
    },
    {
      label: "All Time",
      earnings: summary.lifetimeEarnings,
      paidCount: summary.lifetimePaidCount,
      dsfCount: summary.lifetimeDsfCount,
    },
  ] : [];

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <button className={styles.back} onClick={() => navigate("/therapist-home")}>← Back</button>
          <span className={styles.logo}>Therapy Connect</span>
          <span className={styles.rolePill}>Therapist</span>
        </div>
      </header>

      <main className={styles.main}>
        <div className={styles.topRow}>
          <h1 className={styles.heading}>Earnings Report</h1>
          <p className={styles.sub}>Track your session earnings by week, month, and all time</p>
        </div>

        {/* ── Summary cards ── */}
        <section className={styles.summarySection}>
          {summaryLoading && (
            <div className={styles.summaryLoading}>
              <span className={styles.spinner} /> Loading summary...
            </div>
          )}
          {summaryError && (
            <div className={styles.errorBox}>
              <span className={styles.errorIcon}>!</span>{summaryError}
            </div>
          )}
          {!summaryLoading && !summaryError && summary && (
            <div className={styles.summaryGrid}>
              {summaryPeriods.map(period => (
                <div key={period.label} className={styles.periodCard}>
                  <div className={styles.periodLabel}>{period.label}</div>
                  <div className={styles.periodEarnings}>{formatCurrency(period.earnings)}</div>
                  <div className={styles.periodMeta}>
                    <span className={styles.periodPaid}>{period.paidCount} paid</span>
                    <span className={styles.periodDivider}>·</span>
                    <span className={styles.periodDsf}>{period.dsfCount} DSF</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* ── Session detail section ── */}
        <section className={styles.sessionSection}>
          <h2 className={styles.sectionTitle}>Session Detail</h2>

          <div className={styles.filterCard}>
            <div className={styles.dateRow}>
              <div className={styles.dateField}>
                <label className={styles.dateLabel}>From</label>
                <input
                  type="date"
                  className={styles.dateInput}
                  value={fromDate}
                  max={toDate}
                  onChange={e => { setFromDate(e.target.value); setSessionsLoaded(false); }}
                />
              </div>
              <span className={styles.dateSep}>→</span>
              <div className={styles.dateField}>
                <label className={styles.dateLabel}>To</label>
                <input
                  type="date"
                  className={styles.dateInput}
                  value={toDate}
                  min={fromDate}
                  max={today()}
                  onChange={e => { setToDate(e.target.value); setSessionsLoaded(false); }}
                />
              </div>

              <div className={styles.filterField}>
                <label className={styles.dateLabel}>Service</label>
                <select
                  className={styles.filterSelect}
                  value={filterServiceId}
                  onChange={e => { setFilterServiceId(e.target.value); setSessionsLoaded(false); }}
                >
                  <option value="">All services</option>
                  {services.map(svc => (
                    <option key={svc.serviceId} value={svc.serviceId}>
                      {formatServiceType(svc.serviceType)}
                    </option>
                  ))}
                </select>
              </div>

              <div className={styles.filterField}>
                <label className={styles.dateLabel}>Mode</label>
                <select
                  className={styles.filterSelect}
                  value={filterModeId}
                  onChange={e => { setFilterModeId(e.target.value); setSessionsLoaded(false); }}
                >
                  <option value="">All modes</option>
                  {allModes.map(mode => (
                    <option key={mode.modeId} value={mode.modeId}>
                      {mode.displayName}
                    </option>
                  ))}
                </select>
              </div>

              <button className={styles.loadBtn} onClick={handleLoadSessions} disabled={sessionsLoading}>
                {sessionsLoading ? <span className={styles.btnSpinner} /> : "Load Sessions"}
              </button>
            </div>

            {sessionsError && (
              <div className={styles.errorBox}>
                <span className={styles.errorIcon}>!</span>{sessionsError}
              </div>
            )}
          </div>

          {sessionsLoaded && (
            <div className={styles.tableSection}>
              <div className={styles.tableSectionHeader}>
                <p className={styles.tableSub}>
                  {sessions.length} completed session{sessions.length !== 1 ? "s" : ""}
                </p>
                <div className={styles.tableActions}>
                  {exportError && <span className={styles.exportError}>{exportError}</span>}
                  <button
                    className={styles.exportBtn}
                    onClick={handleExport}
                    disabled={exporting || sessions.length === 0}
                  >
                    {exporting ? <span className={styles.btnSpinner} /> : "Export CSV"}
                  </button>
                </div>
              </div>

              {sessions.length === 0 ? (
                <div className={styles.emptyState}>
                  <p className={styles.emptyText}>No completed sessions match the selected filters.</p>
                </div>
              ) : (
                <div className={styles.tableWrapper}>
                  <table className={styles.table}>
                    <thead>
                      <tr>
                        <th className={styles.th} onClick={() => handleSort("startTime")}>Date / Time{sortIcon("startTime")}</th>
                        <th className={styles.th} onClick={() => handleSort("clientName")}>Client{sortIcon("clientName")}</th>
                        <th className={styles.th}>Service</th>
                        <th className={styles.th}>Mode</th>
                        <th className={styles.th} onClick={() => handleSort("sessionFee")}>Fee{sortIcon("sessionFee")}</th>
                        <th className={styles.th}>DSF</th>
                        <th className={styles.th} onClick={() => handleSort("earningAmount")}>Earning{sortIcon("earningAmount")}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {sortedSessions.map((s, i) => {
                        const mode = modeMap[s.modeId];
                        const modeLabel = mode?.displayName ?? s.modeId ?? "—";
                        const svc = services.find(sv => sv.serviceId === s.serviceId);
                        const svcLabel = svc ? formatServiceType(svc.serviceType) : (s.serviceId ?? "—");
                        const isProBono = s.dsf;
                        return (
                          <tr key={s.appointmentId || i} className={`${styles.tr} ${isProBono ? styles.trProBono : ""}`}>
                            <td className={styles.td}>{formatDateTime(s.startTime)}</td>
                            <td className={styles.td}>
                              <div className={styles.clientCell}>
                                <span className={styles.clientAvatar}>{s.clientName?.[0]?.toUpperCase() ?? "?"}</span>
                                <span className={styles.clientName}>{s.clientName || "—"}</span>
                              </div>
                            </td>
                            <td className={styles.td}>
                              <span className={styles.serviceChip}>{svcLabel}</span>
                            </td>
                            <td className={styles.td}>
                              <span className={styles.modeChip}>{modeLabel}</span>
                            </td>
                            <td className={styles.td}>
                              <span className={styles.feeCell}>{formatCurrency(s.sessionFee)}</span>
                            </td>
                            <td className={styles.td}>
                              {isProBono
                                ? <span className={styles.dsfBadge}>DSF</span>
                                : <span className={styles.noTag}>—</span>}
                            </td>
                            <td className={styles.td}>
                              <span className={`${styles.earningCell} ${isProBono ? styles.earningCellZero : styles.earningCellPositive}`}>
                                {formatCurrency(s.earningAmount)}
                              </span>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                    <tfoot>
                      <tr className={styles.tfootRow}>
                        <td className={styles.td} colSpan={4}><strong>Total</strong></td>
                        <td className={styles.td}>
                          <span className={styles.feeCell}>
                            <strong>{formatCurrency(sessions.reduce((acc, s) => acc + Number(s.sessionFee ?? 0), 0))}</strong>
                          </span>
                        </td>
                        <td className={styles.td} />
                        <td className={styles.td}>
                          <span className={`${styles.earningCell} ${styles.earningCellPositive}`}>
                            <strong>{formatCurrency(sessions.reduce((acc, s) => acc + Number(s.earningAmount ?? 0), 0))}</strong>
                          </span>
                        </td>
                      </tr>
                    </tfoot>
                  </table>
                </div>
              )}
            </div>
          )}

          {!sessionsLoaded && !sessionsLoading && (
            <div className={styles.emptyState}>
              <p className={styles.emptyText}>Select a date range and click <strong>Load Sessions</strong> to view session detail.</p>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
