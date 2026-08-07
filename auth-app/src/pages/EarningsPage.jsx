import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { getEarningsSummary, getEarningsSessions, exportEarningsCsv } from "../api/earnings";
import { useModeMap } from "../context/DeliveryModesContext";
import api from "../api/client";
import {
  ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid, Legend,
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

function daysAgo(n) {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return toISODate(d);
}

/**
 * The ranges a therapist actually asks for. Typing two dates to answer "how did
 * last month go" is four interactions and a mental note of how many days
 * September had; these are one click.
 *
 * Each returns [from, to] rather than mutating, so applying a preset and typing
 * a custom date go through exactly the same setState path.
 */
const RANGE_PRESETS = [
  { id: "7d",    label: "Last 7 days",  range: () => [daysAgo(6), today()] },
  { id: "30d",   label: "Last 30 days", range: () => [daysAgo(29), today()] },
  { id: "month", label: "This month",   range: () => [startOfMonth(), today()] },
  {
    id: "prev",
    label: "Last month",
    range: () => {
      const d = new Date();
      const first = new Date(d.getFullYear(), d.getMonth() - 1, 1);
      // Day 0 of this month is the last day of the previous one, which keeps
      // month lengths and leap years out of this entirely.
      const last = new Date(d.getFullYear(), d.getMonth(), 0);
      return [toISODate(first), toISODate(last)];
    },
  },
  {
    id: "year",
    label: "This year",
    range: () => [toISODate(new Date(new Date().getFullYear(), 0, 1)), today()],
  },
];

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
  const [sessionKind, setSessionKind]     = useState("all");
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
    loadSessions();
  }, []);

  /* Takes the range explicitly. A preset calls this in the same tick as its
     setFromDate/setToDate, and reading fromDate here would fetch the range the
     user just navigated away from — the classic stale-closure fetch. The button
     passes nothing and gets current state, which is what it wants. */
  const loadSessions = async (rangeFrom = fromDate, rangeTo = toDate) => {
    if (!rangeFrom || !rangeTo) { setSessionsError("Please select both dates."); return; }
    if (rangeFrom > rangeTo) { setSessionsError("From date must be before to date."); return; }
    setSessionsLoading(true); setSessionsError(null); setSessionsLoaded(false);
    try {
      const sess = await getEarningsSessions(
        rangeFrom, rangeTo,
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

  /* Loading is the point of choosing a range, so a preset does it rather than
     leaving a primed button and a stale table behind. */
  const applyPreset = (preset) => {
    const [from, to] = preset.range();
    setFromDate(from);
    setToDate(to);
    setSessionsLoaded(false);
    setSessionsError(null);
    loadSessions(from, to);
  };

  const activePreset = RANGE_PRESETS.find(p => {
    const [from, to] = p.range();
    return from === fromDate && to === toDate;
  });

  /**
   * A session was pro bono if it was BOOKED at zero — resolveSessionFee stamps
   * sessionFee = 0 for a DSF client at the moment of booking, and that stamp is
   * what makes history immutable.
   *
   * The row also carries c.dsf, but that is the client's flag TODAY. Reading it
   * meant a client who paid for six months and then moved to DSF had every past
   * paid session re-labelled DSF, with a real fee printed next to the badge
   * contradicting it. Same retroactivity the earnings queries were fixed for;
   * this was the last place still reading the live flag.
   */
  const isProBonoSession = (session) => Number(session?.sessionFee ?? 0) === 0;

  const SESSION_KINDS = [
    { id: "all",  label: "All sessions", match: () => true },
    { id: "paid", label: "Paid",         match: s => !isProBonoSession(s) },
    { id: "dsf",  label: "Pro bono (DSF)", match: isProBonoSession },
    { id: "noshow", label: "No-shows",   match: s => s.status === "ABANDONED" },
  ];

  /* Filtering happens on rows already loaded — no refetch, so switching between
     Paid and Pro bono is instant and cannot disagree with what the totals say. */
  const kind = SESSION_KINDS.find(k => k.id === sessionKind) ?? SESSION_KINDS[0];
  const visibleSessions = sessions.filter(kind.match);
  const paidVisible = visibleSessions.filter(s => !isProBonoSession(s));
  const proBonoVisible = visibleSessions.length - paidVisible.length;
  const visibleTotal = visibleSessions.reduce((acc, s) => acc + Number(s.sessionFee ?? 0), 0);
  /* Divided by PAID sessions, matching the summary card: a pro-bono session in
     the denominator would drag the average below anything actually charged. */
  const avgPerPaid = paidVisible.length > 0 ? visibleTotal / paidVisible.length : 0;

  const handleSort = (field) => {
    if (sortField === field) {
      setSortDir(d => d === "asc" ? "desc" : "asc");
    } else {
      setSortField(field);
      setSortDir("desc");
    }
  };

  const sortedSessions = [...visibleSessions].sort((a, b) => {
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

  // Daily totals for the chart. DSF sessions carry earningAmount 0, so revenue
  // alone renders pro-bono work as nothing at all — it gets its own series
  // (a count, not money) so delivered work stays visible beside income.
  const dailyTotals = (() => {
    if (!sessions.length) return [];
    const byDay = {};
    sessions.forEach((s) => {
      if (!s.startTime) return;
      const key = String(s.startTime).slice(0, 10); // YYYY-MM-DD
      const row = byDay[key] || (byDay[key] = { total: 0, dsfSessions: 0 });
      row.total += Number(s.earningAmount ?? 0);
      if (isProBonoSession(s)) row.dsfSessions += 1;
    });
    return Object.entries(byDay)
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([day, row]) => ({
        day,
        total: row.total,
        dsfSessions: row.dsfSessions,
        // "24 Jul" — readable axis label
        label: new Date(day).toLocaleDateString("en-IN", { day: "numeric", month: "short" }),
      }));
  })();
  const rangeTotal = dailyTotals.reduce((sum, d) => sum + d.total, 0);
  const rangeDsfSessions = dailyTotals.reduce((sum, d) => sum + d.dsfSessions, 0);
  // No-shows still bill (owner decision) — counted so the page can say so.
  const rangeAbandoned = sessions.filter((s) => s.status === "ABANDONED").length;

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
          {/* The export lives with the Session Detail filters, not here. Both
              buttons used to call the same handler with the same arguments, but
              sitting beside "This Week / This Month / All Time" this one read as
              an export of the summary — so it quietly produced a different range
              from the one its position implied. One export, next to the rows it
              writes out. */}
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
              <div className={styles.trendChips}>
                {rangeAbandoned > 0 && (
                  <span className="chip chip-warn" title="No-shows bill the full session fee">
                    {rangeAbandoned} no-show{rangeAbandoned === 1 ? "" : "s"} billed
                  </span>
                )}
                {rangeDsfSessions > 0 && (
                  <span className="chip chip-info" title="Pro bono — delivered work, no income">
                    {rangeDsfSessions} DSF session{rangeDsfSessions === 1 ? "" : "s"}
                  </span>
                )}
                <span className="chip chip-ok">{formatCurrency(rangeTotal)} total</span>
              </div>
            </div>
            <div style={{ width: "100%", height: 220 }}>
              <ResponsiveContainer>
                <BarChart data={dailyTotals} margin={{ top: 4, right: 8, left: 4, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke={GRID_STROKE} vertical={false} />
                  <XAxis dataKey="label" tick={AXIS_TICK} tickLine={false} axisLine={false} />
                  <YAxis
                    yAxisId="money"
                    tick={AXIS_TICK} tickLine={false} axisLine={false} width={62}
                    tickFormatter={(v) => `₹${Number(v).toLocaleString("en-IN")}`}
                  />
                  {/* DSF is a session count, not money — it needs its own scale,
                      otherwise a handful of pro-bono sessions vanish against
                      four-figure revenue bars. */}
                  {rangeDsfSessions > 0 && (
                    <YAxis
                      yAxisId="dsf" orientation="right" allowDecimals={false}
                      tick={AXIS_TICK} tickLine={false} axisLine={false} width={34}
                    />
                  )}
                  <Tooltip
                    {...TOOLTIP}
                    cursor={BAR_CURSOR}
                    formatter={(v, name) =>
                      name === "DSF sessions" ? [v, name] : [formatCurrency(v), name]
                    }
                  />
                  {rangeDsfSessions > 0 && <Legend wrapperStyle={{ fontSize: "0.75rem" }} />}
                  <Bar yAxisId="money" dataKey="total" name="Earnings" fill="#22d3ee" radius={[6, 6, 0, 0]} maxBarSize={54} />
                  {rangeDsfSessions > 0 && (
                    /* Green from the declared chart palette — DSF is delivered
                       work, not a warning. Avatar/identity hues are not valid
                       data-series colours (see the chart audit). */
                    <Bar yAxisId="dsf" dataKey="dsfSessions" name="DSF sessions" fill="#34d399" radius={[6, 6, 0, 0]} maxBarSize={54} />
                  )}
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        )}

        {/* ── Session detail section ── */}
        <section className={styles.sessionSection}>
          <h2 className={styles.sectionTitle}>Session Detail</h2>

          <div className={styles.filterCard}>
            <div className={styles.presetRow}>
              {RANGE_PRESETS.map(preset => (
                <button
                  key={preset.id}
                  type="button"
                  className={`${styles.presetChip} ${activePreset?.id === preset.id ? styles.presetChipOn : ""}`}
                  aria-pressed={activePreset?.id === preset.id}
                  onClick={() => applyPreset(preset)}
                  disabled={sessionsLoading}
                >
                  {preset.label}
                </button>
              ))}
            </div>

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

              <div className={styles.filterField}>
                <label className={styles.dateLabel}>Sessions</label>
                <select
                  className={styles.filterSelect}
                  value={sessionKind}
                  onChange={e => setSessionKind(e.target.value)}
                >
                  {SESSION_KINDS.map(k => (
                    <option key={k.id} value={k.id}>{k.label}</option>
                  ))}
                </select>
              </div>

              <button className="btn btn-primary" onClick={() => loadSessions()} disabled={sessionsLoading}>
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
                {/* Reads as arithmetic that works. "10 sessions, Rs 12,000"
                    invited dividing by 10 when two of those were pro bono and
                    only eight carried a fee. */}
                <p className={styles.tableSub}>
                  {paidVisible.length > 0 ? (
                    <>
                      <strong>{paidVisible.length}</strong> paid session{paidVisible.length !== 1 ? "s" : ""}
                      {" · "}{formatCurrency(visibleTotal)}
                      {" · "}{formatCurrency(avgPerPaid)} avg
                      {proBonoVisible > 0 && (
                        <span className={styles.subMuted}> · {proBonoVisible} pro bono</span>
                      )}
                    </>
                  ) : proBonoVisible > 0 ? (
                    /* Leading with "0 paid" when the view is deliberately the
                       pro-bono one reports an absence instead of the thing asked
                       for. The count that matters here is the sessions given. */
                    <>
                      <strong>{proBonoVisible}</strong> pro bono session{proBonoVisible !== 1 ? "s" : ""}
                      <span className={styles.subMuted}> · no charge</span>
                    </>
                  ) : (
                    <>No sessions</>
                  )}
                </p>
                <div className={styles.tableActions}>
                  {exportError && <span className={styles.exportError}>{exportError}</span>}
                  <button
                    className={styles.exportBtn}
                    onClick={handleExport}
                    disabled={exporting || visibleSessions.length === 0}
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
                        const isProBono = isProBonoSession(s);
                        return (
                          <tr key={s.appointmentId || i} className={`${styles.tr} ${isProBono ? styles.trProBono : ""}`}>
                            <td className={styles.td}>
                              {formatDateTime(s.startTime)}
                              {/* The list now includes billed no-shows, so each row
                                  has to say which it is — otherwise a fee appears
                                  against a session nobody attended, unexplained. */}
                              {s.status === "ABANDONED" && (
                                <span className={`chip chip-warn ${styles.noShowChip}`}>No-show</span>
                              )}
                            </td>
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
                            <strong>{formatCurrency(visibleTotal)}</strong>
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
