import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  ResponsiveContainer, LineChart, Line, BarChart, Bar,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, PieChart, Pie, Cell,
} from "recharts";
import {
  getAnalyticsSummary, getAnalyticsDaily, getAnalyticsServices,
  getAnalyticsRetention, getAnalyticsRetentionFrequency,
} from "../api/analytics";
import Icon from "../components/icons";
import { useChartTheme, KEEP_ORDER, dateTick, dateLabel, makePieLabel } from "../components/chartTheme";
import styles from "./AnalyticsPage.module.css";

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
  return `₹${n.toLocaleString("en-IN", { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`;
}

function formatPct(value) {
  return `${Number(value ?? 0).toFixed(1)}%`;
}

function formatDays(value) {
  const n = Number(value ?? 0);
  return n === 0 ? "—" : `${n.toFixed(0)}d`;
}

// Chart palette — the prototype's declared chart tokens (--cyan/--green/--amber/
// --red), whose own donut runs cyan → green → amber. Amber is kept for attention
// states only, where it's a minority slice; across half a pie it reads as alarm.
// The violet/pink shades in the prototype belong to avatar gradients only and are
// deliberately absent: they carry personal identity, never a data series.
const C = {
  cyan:   "#22d3ee",
  green:  "#34d399",
  amber:  "#fbbf24",
  red:    "#f87171",
  slate:  "#8497b1", // neutral outcome — no positive/negative signal
};

// paddingAngle carves a visible slit even where a slice has no value, so the gap
// is only applied when two or more slices are actually drawn.
const gapFor = (data) => (data.filter((d) => d.value > 0).length > 1 ? 2 : 0);

const CHART_COLORS = {
  completed:   C.green,
  cancelled:   C.red,
  abandoned:   C.amber,
  rescheduled: C.slate, // moved, not lost — reads neutral against green/red
  earnings:    C.cyan,  // matches the Earnings page chart
  // Delivery mode is a neutral split, so it uses the brand pair — the same
  // cyan → green the prototype's donut opens with. (The .chip.clinic amber is
  // right for a small badge but reads as an alarm across half a pie.)
  online:      C.cyan,
  offline:     C.green,
  paid:        C.green, // fee collected
  dsf:         C.amber, // did-not-show fee — attention, and always a minority slice
  retained:    C.green,
  churned:     C.red,
  frequency:   C.cyan,
};

const PRESET_RANGES = [
  { label: "This month", from: startOfMonth, to: today },
  { label: "Last 7 days", from: () => toISODate(new Date(Date.now() - 6 * 86400000)), to: today },
  { label: "Last 30 days", from: () => toISODate(new Date(Date.now() - 29 * 86400000)), to: today },
  { label: "Last 90 days", from: () => toISODate(new Date(Date.now() - 89 * 86400000)), to: today },
];

export default function AnalyticsPage() {
  const navigate = useNavigate();
  // Chart chrome follows the light/dark toggle (Recharts can't read var()).
  const { AXIS_TICK, GRID_STROKE, TOOLTIP, BAR_CURSOR, LINE_CURSOR, PIE_LABEL_FILL, LEGEND_STYLE } = useChartTheme();
  const pieLabel = makePieLabel(PIE_LABEL_FILL);

  const [fromDate, setFromDate]   = useState(startOfMonth());
  const [toDate, setToDate]       = useState(today());

  const [summary, setSummary]       = useState(null);
  const [daily, setDaily]           = useState([]);
  const [services, setServices]     = useState([]);
  const [loading, setLoading]       = useState(false);
  const [error, setError]           = useState(null);

  const [retention, setRetention]   = useState(null);
  const [frequency, setFrequency]   = useState([]);
  const [retentionLoading, setRetentionLoading] = useState(true);
  const [retentionError, setRetentionError]     = useState(null);

  async function loadData(from, to) {
    setLoading(true);
    setError(null);
    try {
      const [sum, dailyData, svcData] = await Promise.all([
        getAnalyticsSummary(from, to),
        getAnalyticsDaily(from, to),
        getAnalyticsServices(from, to),
      ]);
      setSummary(sum);
      setDaily(dailyData);
      setServices(svcData);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function loadRetention() {
    setRetentionLoading(true);
    setRetentionError(null);
    try {
      const [ret, freq] = await Promise.all([
        getAnalyticsRetention(),
        getAnalyticsRetentionFrequency(),
      ]);
      setRetention(ret);
      setFrequency(freq);
    } catch (err) {
      setRetentionError(err.message);
    } finally {
      setRetentionLoading(false);
    }
  }

  useEffect(() => {
    loadData(fromDate, toDate);
    loadRetention();
  }, []);

  function applyPreset(preset) {
    const from = preset.from();
    const to   = preset.to();
    setFromDate(from);
    setToDate(to);
    loadData(from, to);
  }

  function handleApply() {
    loadData(fromDate, toDate);
  }

  const modeData = summary ? [
    { name: "Online",  value: summary.totalOnline  },
    { name: "Offline", value: summary.totalOffline },
  ] : [];

  const paidDsfData = summary ? [
    { name: "Paid", value: summary.totalPaid },
    { name: "DSF",  value: summary.totalDsf  },
  ] : [];

  const retentionPieData = retention ? [
    { name: "Retained",    value: retention.retainedClients },
    { name: "Single visit", value: retention.totalUniqueClients - retention.retainedClients },
  ] : [];

  return (
    <div className={styles.page}>
      <main className={styles.main}>
        <div className="page-head">
          <div>
            <div className="eyebrow">Insights</div>
            <h1>Analytics</h1>
            <div className="sub">Session trends, outcomes and service mix</div>
          </div>
          <div className="head-actions">
            <div className={styles.presets}>
              {PRESET_RANGES.map((p) => (
                <button key={p.label} className="btn btn-sm" onClick={() => applyPreset(p)}>
                  {p.label}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* ── Date range controls ── */}
        <div className={styles.controls}>
          <div className={styles.dateInputs}>
            <input type="date" className={styles.dateInput} value={fromDate} onChange={(e) => setFromDate(e.target.value)} />
            <span className={styles.dateSep}>to</span>
            <input type="date" className={styles.dateInput} value={toDate} onChange={(e) => setToDate(e.target.value)} />
            <button className="btn btn-primary" onClick={handleApply} disabled={loading}>
              {loading ? "Loading…" : "Apply"}
            </button>
          </div>
        </div>

        {error && (
          <div className={styles.errorBox}>
            <span className={styles.errorIcon}>!</span>{error}
          </div>
        )}

        {/* ── Summary KPI cards ── */}
        {summary && (
          <div className={styles.kpiGrid}>
            {[
              { icon: "check",    tone: "ic-g", label: "Completed",       value: summary.totalCompleted },
              { icon: "x",        tone: "ic-a", label: "Cancelled",       value: summary.totalCancelled },
              { icon: "bar",      tone: "ic-c", label: "Completion rate", value: formatPct(summary.completionRate), green: true },
              { icon: "dollar",   tone: "ic-g", label: "Total earnings",  value: formatCurrency(summary.totalEarnings), green: true },
              { icon: "clock",    tone: "ic-v", label: "Rescheduled",     value: summary.totalRescheduled },
              { icon: "heart",    tone: "ic-c", label: "DSF sessions",    value: summary.totalDsf },
            ].map((k) => (
              <div key={k.label} className="card kpi">
                <div className="kpi-top">
                  <span className={`kpi-ic ${k.tone}`}><Icon name={k.icon} size={20} /></span>
                </div>
                <div className="kpi-val" style={k.green ? { color: "var(--ok-mid)" } : undefined}>{k.value}</div>
                <div className="kpi-lbl">{k.label}</div>
              </div>
            ))}
          </div>
        )}

        {/* ── Charts ── */}
        {daily.length > 0 && (
          <div className={styles.chartsGrid}>
            {/* Sessions over time */}
            <div className={`card ${styles.chartCard}`}>
              <h2 className={styles.chartTitle}>Sessions over time</h2>
              <ResponsiveContainer width="100%" height={220}>
                <LineChart data={daily} margin={{ top: 4, right: 16, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke={GRID_STROKE} />
                  <XAxis dataKey="date" tick={AXIS_TICK} tickLine={false} axisLine={false} tickFormatter={dateTick} />
                  <YAxis tick={AXIS_TICK} tickLine={false} axisLine={false} />
                  <Tooltip {...TOOLTIP} cursor={LINE_CURSOR} labelFormatter={dateLabel} itemSorter={KEEP_ORDER} />
                  <Legend wrapperStyle={LEGEND_STYLE} />
                  <Line type="monotone" dataKey="completedCount"   name="Completed"   stroke={CHART_COLORS.completed}   dot={false} strokeWidth={2}
                    activeDot={{ r: 4, strokeWidth: 0, fill: CHART_COLORS.completed }} />
                  <Line type="monotone" dataKey="cancelledCount"   name="Cancelled"   stroke={CHART_COLORS.cancelled}   dot={false} strokeWidth={2}
                    activeDot={{ r: 4, strokeWidth: 0, fill: CHART_COLORS.cancelled }} />
                  <Line type="monotone" dataKey="rescheduledCount" name="Rescheduled" stroke={CHART_COLORS.rescheduled} dot={false} strokeWidth={2}
                    activeDot={{ r: 4, strokeWidth: 0, fill: CHART_COLORS.rescheduled }} />
                </LineChart>
              </ResponsiveContainer>
            </div>

            {/* Daily earnings */}
            <div className={`card ${styles.chartCard}`}>
              <h2 className={styles.chartTitle}>Daily earnings (₹)</h2>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={daily} margin={{ top: 4, right: 16, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke={GRID_STROKE} />
                  <XAxis dataKey="date" tick={AXIS_TICK} tickLine={false} axisLine={false} tickFormatter={dateTick} />
                  <YAxis tick={AXIS_TICK} tickLine={false} axisLine={false} />
                  <Tooltip {...TOOLTIP} cursor={BAR_CURSOR} labelFormatter={dateLabel}
                    formatter={(value) => [`₹${Number(value).toLocaleString("en-IN")}`, "Earnings"]} />
                  <Bar dataKey="earnings" name="Earnings" fill={CHART_COLORS.earnings} radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>

            {/* Online vs Offline */}
            <div className={`card ${styles.chartCard}`}>
              <h2 className={styles.chartTitle}>Online vs Offline</h2>
              <ResponsiveContainer width="100%" height={220}>
                <PieChart>
                  <Pie data={modeData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} stroke="none" paddingAngle={gapFor(modeData)} label={pieLabel} labelLine={false}>
                    {modeData.map((_, i) => <Cell key={i} fill={[CHART_COLORS.online, CHART_COLORS.offline][i]} />)}
                  </Pie>
                  <Tooltip {...TOOLTIP} />
                </PieChart>
              </ResponsiveContainer>
            </div>

            {/* Paid vs DSF */}
            <div className={`card ${styles.chartCard}`}>
              <h2 className={styles.chartTitle}>Paid vs DSF</h2>
              <ResponsiveContainer width="100%" height={220}>
                <PieChart>
                  <Pie data={paidDsfData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} stroke="none" paddingAngle={gapFor(paidDsfData)} label={pieLabel} labelLine={false}>
                    {paidDsfData.map((_, i) => <Cell key={i} fill={[CHART_COLORS.paid, CHART_COLORS.dsf][i]} />)}
                  </Pie>
                  <Tooltip {...TOOLTIP} />
                </PieChart>
              </ResponsiveContainer>
            </div>

            {/* Session outcomes breakdown (stacked bar) */}
            <div className={`card ${styles.chartCard} ${styles.chartCardWide}`}>
              <h2 className={styles.chartTitle}>Session outcomes</h2>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={daily} margin={{ top: 4, right: 16, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke={GRID_STROKE} />
                  <XAxis dataKey="date" tick={AXIS_TICK} tickLine={false} axisLine={false} tickFormatter={dateTick} />
                  <YAxis tick={AXIS_TICK} tickLine={false} axisLine={false} />
                  <Tooltip {...TOOLTIP} cursor={BAR_CURSOR} labelFormatter={dateLabel} itemSorter={KEEP_ORDER} />
                  <Legend wrapperStyle={LEGEND_STYLE} />
                  <Bar dataKey="completedCount" name="Completed" stackId="a" fill={CHART_COLORS.completed} />
                  <Bar dataKey="cancelledCount" name="Cancelled" stackId="a" fill={CHART_COLORS.cancelled} />
                  <Bar dataKey="abandonedCount" name="Abandoned" stackId="a" fill={CHART_COLORS.abandoned} radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        )}

        {/* ── Service breakdown table ── */}
        {services.length > 0 && (
          <div className={`card ${styles.tableCard}`}>
            <h2 className={styles.chartTitle}>Breakdown by service</h2>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th className={styles.th}>Service ID</th>
                  <th className={styles.th}>Completed sessions</th>
                  <th className={styles.th}>Earnings</th>
                </tr>
              </thead>
              <tbody>
                {services.map((s) => (
                  <tr key={s.serviceId} className={styles.tr}>
                    <td className={styles.td}>{s.serviceId}</td>
                    <td className={styles.td}>{s.completedCount}</td>
                    <td className={styles.td}>{formatCurrency(s.earnings)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {!loading && !error && daily.length === 0 && (
          <div className={styles.emptyState}>
            <span className={styles.emptyIcon}><Icon name="bar" size={34} /></span>
            <p className={styles.emptyText}>No data found for the selected period.</p>
          </div>
        )}

        {/* ── Retention section ── */}
        <div className={styles.sectionDivider}>
          <h2 className={styles.sectionLabel}>Client retention</h2>
        </div>

        {retentionError && (
          <div className={styles.errorBox}>
            <span className={styles.errorIcon}>!</span>{retentionError}
          </div>
        )}

        {retentionLoading && (
          <div className={styles.emptyState}>
            <p className={styles.emptyText}>Loading retention data…</p>
          </div>
        )}

        {!retentionLoading && !retentionError && retention && (
          <>
            {/* Retention KPI cards */}
            <div className={styles.kpiGrid}>
              {[
                { icon: "users",    tone: "ic-c", label: "Unique clients",        value: retention.totalUniqueClients },
                { icon: "trend",    tone: "ic-g", label: "Retention rate",        value: formatPct(retention.retentionRate), tint: "var(--ok-mid)" },
                { icon: "bar",      tone: "ic-c", label: "Avg sessions / client", value: Number(retention.avgSessionsPerClient).toFixed(1) },
                { icon: "alert",    tone: "ic-a", label: "Churned (30d)",         value: retention.churnedClients, tint: retention.churnedClients > 0 ? "var(--danger-mid)" : undefined },
                { icon: "check",    tone: "ic-g", label: "Retained clients",      value: retention.retainedClients, tint: "var(--ok-mid)" },
                { icon: "calendar", tone: "ic-v", label: "Avg client lifetime",   value: formatDays(retention.avgClientLifetimeDays) },
              ].map((k) => (
                <div key={k.label} className="card kpi">
                  <div className="kpi-top">
                    <span className={`kpi-ic ${k.tone}`}><Icon name={k.icon} size={20} /></span>
                  </div>
                  <div className="kpi-val" style={k.tint ? { color: k.tint } : undefined}>{k.value}</div>
                  <div className="kpi-lbl">{k.label}</div>
                </div>
              ))}
            </div>

            {/* Retention charts */}
            {retention.totalUniqueClients > 0 && (
              <div className={styles.chartsGrid}>
                {/* Retained vs single-visit pie */}
                <div className={`card ${styles.chartCard}`}>
                  <h2 className={styles.chartTitle}>Retained vs single-visit</h2>
                  <ResponsiveContainer width="100%" height={220}>
                    <PieChart>
                      <Pie
                        data={retentionPieData}
                        dataKey="value"
                        nameKey="name"
                        cx="50%"
                        cy="50%"
                        outerRadius={80}
                        stroke="none"
                        paddingAngle={gapFor(retentionPieData)}
                        label={pieLabel}
                        labelLine={false}
                      >
                        {/* "Single visit" is neutral, not churn — slate, not red or brand cyan */}
                        {retentionPieData.map((_, i) => (
                          <Cell key={i} fill={[CHART_COLORS.retained, C.slate][i]} />
                        ))}
                      </Pie>
                      <Tooltip {...TOOLTIP} />
                    </PieChart>
                  </ResponsiveContainer>
                </div>

                {/* Sessions per client frequency bar */}
                <div className={`card ${styles.chartCard}`}>
                  <h2 className={styles.chartTitle}>Sessions per client</h2>
                  <ResponsiveContainer width="100%" height={220}>
                    <BarChart data={frequency} margin={{ top: 4, right: 16, left: 0, bottom: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke={GRID_STROKE} />
                      <XAxis dataKey="bucket" tick={AXIS_TICK} tickLine={false} axisLine={false} />
                      <YAxis allowDecimals={false} tick={AXIS_TICK} tickLine={false} axisLine={false} />
                      <Tooltip
                        {...TOOLTIP} cursor={BAR_CURSOR}
                        formatter={(value) => [value, "Clients"]}
                      />
                      <Bar dataKey="clientCount" name="Clients" fill={CHART_COLORS.frequency} radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
}
