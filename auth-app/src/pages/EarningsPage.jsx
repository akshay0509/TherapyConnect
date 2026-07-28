import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { getEarningsSummary, getEarningsSessions, exportEarningsCsv } from "../api/earnings";
import { useModeMap } from "../context/DeliveryModesContext";
import api from "../api/client";
import {
  ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid,
} from "recharts";
import Icon from "../components/icons";
import { useChartTheme } from "../components/chartTheme";
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

function formatRangeLabel(from, to) {
  if (!from || !to) return "";
  const f = new Date(from), t = new Date(to);
  const opts = { day: "numeric", month: "short" };
  const sameYear = f.getFullYear() === t.getFullYear();
  return `${f.toLocaleDateString("en-IN", opts)} – ${t.toLocaleDateString("en-IN", { ...opts, year: "numeric" })}${sameYear ? "" : ""}`;
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
  const { AXIS_TICK, GRID_STROKE, TOOLTIP, BAR_CURSOR } = useChartTheme();
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
    // Load the default range up-front so the revenue chart and table have data
    // on arrival (the prototype shows populated figures immediately).
    handleLoadSessions();
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

  const sortIcon = (field) => (
    <Icon
      name="chevron"
      size={13}
      className={styles.sortIcon}
      style={{
        opacity: sortField === field ? 1 : 0.35,
        transform: sortField === field && sortDir === "asc" ? "rotate(-90deg)" : "rotate(90deg)",
      }}
    />
  );

  const allModes = Object.values(modeMap);

  // Average earning per paid session this month (derived from the summary)
  const avgPerSession = summary && summary.monthPaidCount > 0
    ? Number(summary.monthEarnings) / summary.monthPaidCount
    : 0;

  // Daily revenue totals for the chart, built from the loaded session rows
  const dailyTotals = (() => {
    if (!sessions.length) return [];
    const byDay = {};
    sessions.forEach((s) => {
      if (!s.startTime) return;
      const key = String(s.startTime).slice(0, 10); // YYYY-MM-DD
      byDay[key] = (byDay[key] || 0) + Number(s.earningAmount ?? 0);
    });
    return Object.entries(byDay)
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([day, total]) => ({
        day,
        total,
        // "24 Jul" — readable axis label
        label: new Date(day).toLocaleDateString("en-IN", { day: "numeric", month: "short" }),
      }));
  })();
  const rangeTotal = dailyTotals.reduce((sum, d) => sum + d.total, 0);

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
      <main className={styles.main}>
        <div className="page-head">
          <div>
            <div className="eyebrow">Finance</div>
            <h1>Earnings</h1>
            <div className="sub">{formatRangeLabel(fromDate, toDate)}</div>
          </div>
          <div className="head-actions">
            <button className="btn" onClick={handleExport} disabled={exporting}>
              <Icon name="download" size={18} /> {exporting ? "Exporting…" : "Export CSV"}
            </button>
          </div>
        </div>

        {/* ── Summary KPIs ── */}
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
            <div className={styles.kpis}>
              {summaryPeriods.map((period, i) => (
                <div key={period.label} className="card kpi">
                  <div className="kpi-top">
                    <span className={`kpi-ic ${["ic-g", "ic-c", "ic-v"][i] || "ic-c"}`}>
                      <Icon name={["dollar", "check", "bar"][i] || "dollar"} size={20} />
                    </span>
                    <span className="kpi-trend flat">{period.paidCount} paid · {period.dsfCount} DSF</span>
                  </div>
                  <div className="kpi-val">{formatCurrency(period.earnings)}</div>
                  <div className="kpi-lbl">{period.label}</div>
                </div>
              ))}
              <div className="card kpi">
                <div className="kpi-top">
                  <span className="kpi-ic ic-a"><Icon name="trend" size={20} /></span>
                  <span className="kpi-trend flat">avg</span>
                </div>
                <div className="kpi-val">{formatCurrency(avgPerSession)}</div>
                <div className="kpi-lbl">Per session (month)</div>
              </div>
            </div>
          )}
        </section>

        {/* ── Revenue trend ── */}
        {sessionsLoaded && dailyTotals.length > 0 && (
          <div className="card" style={{ padding: "20px 24px 24px", marginBottom: 22 }}>
            <div className="panel-h" style={{ padding: "0 0 10px" }}>
              <div>
                <h2>Revenue trend</h2>
                <p>Daily collections · {formatRangeLabel(fromDate, toDate)}</p>
              </div>
              <span className="chip chip-ok">{formatCurrency(rangeTotal)} total</span>
            </div>
            <div style={{ width: "100%", height: 220 }}>
              <ResponsiveContainer>
                <BarChart data={dailyTotals} margin={{ top: 4, right: 8, left: 4, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke={GRID_STROKE} vertical={false} />
                  <XAxis dataKey="label" tick={AXIS_TICK} tickLine={false} axisLine={false} />
                  <YAxis
                    tick={AXIS_TICK} tickLine={false} axisLine={false} width={62}
                    tickFormatter={(v) => `₹${Number(v).toLocaleString("en-IN")}`}
                  />
                  <Tooltip
                    {...TOOLTIP}
                    cursor={BAR_CURSOR}
                    formatter={(v) => [formatCurrency(v), "Earnings"]}
                  />
                  <Bar dataKey="total" name="Earnings" fill="#22d3ee" radius={[6, 6, 0, 0]} maxBarSize={54} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        )}

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
              <span className={styles.dateSep}>to</span>
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

              <button className="btn btn-primary" onClick={handleLoadSessions} disabled={sessionsLoading}>
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
                <div className="table-wrap">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th className={styles.thSort} onClick={() => handleSort("startTime")}>Date / Time{sortIcon("startTime")}</th>
                        <th className={styles.thSort} onClick={() => handleSort("clientName")}>Client{sortIcon("clientName")}</th>
                        <th>Service</th>
                        <th>Mode</th>
                        <th className={styles.thSort} onClick={() => handleSort("sessionFee")}>Fee{sortIcon("sessionFee")}</th>
                        <th>DSF</th>
                        <th className={styles.thSort} onClick={() => handleSort("earningAmount")}>Earning{sortIcon("earningAmount")}</th>
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
