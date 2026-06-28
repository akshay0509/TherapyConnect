import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  ResponsiveContainer, LineChart, Line, BarChart, Bar,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, PieChart, Pie, Cell,
} from "recharts";
import { getAnalyticsSummary, getAnalyticsDaily, getAnalyticsServices } from "../api/analytics";
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

const CHART_COLORS = {
  completed:   "#10b981",
  cancelled:   "#ef4444",
  abandoned:   "#f59e0b",
  rescheduled: "#6366f1",
  earnings:    "#34d399",
  online:      "#38bdf8",
  offline:     "#a78bfa",
};

const PIE_COLORS = ["#10b981", "#6366f1", "#38bdf8", "#f59e0b", "#ef4444", "#a78bfa"];

const PRESET_RANGES = [
  { label: "This month", from: startOfMonth, to: today },
  { label: "Last 7 days", from: () => toISODate(new Date(Date.now() - 6 * 86400000)), to: today },
  { label: "Last 30 days", from: () => toISODate(new Date(Date.now() - 29 * 86400000)), to: today },
  { label: "Last 90 days", from: () => toISODate(new Date(Date.now() - 89 * 86400000)), to: today },
];

export default function AnalyticsPage() {
  const navigate = useNavigate();

  const [fromDate, setFromDate]   = useState(startOfMonth());
  const [toDate, setToDate]       = useState(today());

  const [summary, setSummary]       = useState(null);
  const [daily, setDaily]           = useState([]);
  const [services, setServices]     = useState([]);
  const [loading, setLoading]       = useState(false);
  const [error, setError]           = useState(null);

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

  useEffect(() => { loadData(fromDate, toDate); }, []);

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

  const outcomeData = summary ? [
    { name: "Completed",   value: summary.totalCompleted   },
    { name: "Cancelled",   value: summary.totalCancelled   },
    { name: "Abandoned",   value: summary.totalAbandoned   },
    { name: "Rescheduled", value: summary.totalRescheduled },
  ] : [];

  const paidDsfData = summary ? [
    { name: "Paid",     value: summary.totalPaid },
    { name: "DSF",      value: summary.totalDsf  },
  ] : [];

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <span className={styles.logo}>🧠 Therapy Connect</span>
          <div className={styles.headerRight}>
            <button className={styles.backBtn} onClick={() => navigate("/therapist-home")}>← Dashboard</button>
          </div>
        </div>
      </header>

      <main className={styles.main}>
        <div className={styles.hero}>
          <p className={styles.eyebrow}>Insights ✦</p>
          <h1 className={styles.heading}>Analytics</h1>
        </div>

        {/* ── Date range controls ── */}
        <div className={styles.controls}>
          <div className={styles.presets}>
            {PRESET_RANGES.map((p) => (
              <button key={p.label} className={styles.presetBtn} onClick={() => applyPreset(p)}>
                {p.label}
              </button>
            ))}
          </div>
          <div className={styles.dateInputs}>
            <input type="date" className={styles.dateInput} value={fromDate} onChange={(e) => setFromDate(e.target.value)} />
            <span className={styles.dateSep}>to</span>
            <input type="date" className={styles.dateInput} value={toDate} onChange={(e) => setToDate(e.target.value)} />
            <button className={styles.applyBtn} onClick={handleApply} disabled={loading}>
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
            <div className={styles.kpiCard}>
              <span className={styles.kpiIcon}>✅</span>
              <span className={styles.kpiLabel}>Completed</span>
              <span className={styles.kpiValue}>{summary.totalCompleted}</span>
            </div>
            <div className={styles.kpiCard}>
              <span className={styles.kpiIcon}>❌</span>
              <span className={styles.kpiLabel}>Cancelled</span>
              <span className={styles.kpiValue}>{summary.totalCancelled}</span>
            </div>
            <div className={styles.kpiCard}>
              <span className={styles.kpiIcon}>📊</span>
              <span className={styles.kpiLabel}>Completion rate</span>
              <span className={`${styles.kpiValue} ${styles.kpiGreen}`}>{formatPct(summary.completionRate)}</span>
            </div>
            <div className={styles.kpiCard}>
              <span className={styles.kpiIcon}>💸</span>
              <span className={styles.kpiLabel}>Total earnings</span>
              <span className={`${styles.kpiValue} ${styles.kpiGreen}`}>{formatCurrency(summary.totalEarnings)}</span>
            </div>
            <div className={styles.kpiCard}>
              <span className={styles.kpiIcon}>🔄</span>
              <span className={styles.kpiLabel}>Rescheduled</span>
              <span className={styles.kpiValue}>{summary.totalRescheduled}</span>
            </div>
            <div className={styles.kpiCard}>
              <span className={styles.kpiIcon}>🏥</span>
              <span className={styles.kpiLabel}>DSF sessions</span>
              <span className={styles.kpiValue}>{summary.totalDsf}</span>
            </div>
          </div>
        )}

        {/* ── Charts ── */}
        {daily.length > 0 && (
          <div className={styles.chartsGrid}>
            {/* Sessions over time */}
            <div className={styles.chartCard}>
              <h2 className={styles.chartTitle}>Sessions over time</h2>
              <ResponsiveContainer width="100%" height={220}>
                <LineChart data={daily} margin={{ top: 4, right: 16, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" />
                  <XAxis dataKey="date" tick={{ fill: "#94a3b8", fontSize: 11 }} tickLine={false} />
                  <YAxis tick={{ fill: "#94a3b8", fontSize: 11 }} tickLine={false} axisLine={false} />
                  <Tooltip contentStyle={{ background: "#0f1923", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 8, color: "#e2e8f0" }} />
                  <Legend wrapperStyle={{ fontSize: 12, color: "#94a3b8" }} />
                  <Line type="monotone" dataKey="completedCount"   name="Completed"   stroke={CHART_COLORS.completed}   dot={false} strokeWidth={2} />
                  <Line type="monotone" dataKey="cancelledCount"   name="Cancelled"   stroke={CHART_COLORS.cancelled}   dot={false} strokeWidth={2} />
                  <Line type="monotone" dataKey="rescheduledCount" name="Rescheduled" stroke={CHART_COLORS.rescheduled} dot={false} strokeWidth={2} />
                </LineChart>
              </ResponsiveContainer>
            </div>

            {/* Daily earnings */}
            <div className={styles.chartCard}>
              <h2 className={styles.chartTitle}>Daily earnings (₹)</h2>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={daily} margin={{ top: 4, right: 16, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" />
                  <XAxis dataKey="date" tick={{ fill: "#94a3b8", fontSize: 11 }} tickLine={false} />
                  <YAxis tick={{ fill: "#94a3b8", fontSize: 11 }} tickLine={false} axisLine={false} />
                  <Tooltip contentStyle={{ background: "#0f1923", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 8, color: "#e2e8f0" }}
                    formatter={(value) => [`₹${Number(value).toLocaleString("en-IN")}`, "Earnings"]} />
                  <Bar dataKey="earnings" name="Earnings" fill={CHART_COLORS.earnings} radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>

            {/* Online vs Offline */}
            <div className={styles.chartCard}>
              <h2 className={styles.chartTitle}>Online vs Offline</h2>
              <ResponsiveContainer width="100%" height={220}>
                <PieChart>
                  <Pie data={modeData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`} labelLine={false}>
                    {modeData.map((_, i) => <Cell key={i} fill={[CHART_COLORS.online, CHART_COLORS.offline][i]} />)}
                  </Pie>
                  <Tooltip contentStyle={{ background: "#0f1923", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 8, color: "#e2e8f0" }} />
                </PieChart>
              </ResponsiveContainer>
            </div>

            {/* Paid vs DSF */}
            <div className={styles.chartCard}>
              <h2 className={styles.chartTitle}>Paid vs DSF</h2>
              <ResponsiveContainer width="100%" height={220}>
                <PieChart>
                  <Pie data={paidDsfData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`} labelLine={false}>
                    {paidDsfData.map((_, i) => <Cell key={i} fill={[CHART_COLORS.completed, CHART_COLORS.rescheduled][i]} />)}
                  </Pie>
                  <Tooltip contentStyle={{ background: "#0f1923", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 8, color: "#e2e8f0" }} />
                </PieChart>
              </ResponsiveContainer>
            </div>

            {/* Session outcomes breakdown (bar) */}
            <div className={`${styles.chartCard} ${styles.chartCardWide}`}>
              <h2 className={styles.chartTitle}>Session outcomes</h2>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={daily} margin={{ top: 4, right: 16, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" />
                  <XAxis dataKey="date" tick={{ fill: "#94a3b8", fontSize: 11 }} tickLine={false} />
                  <YAxis tick={{ fill: "#94a3b8", fontSize: 11 }} tickLine={false} axisLine={false} />
                  <Tooltip contentStyle={{ background: "#0f1923", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 8, color: "#e2e8f0" }} />
                  <Legend wrapperStyle={{ fontSize: 12, color: "#94a3b8" }} />
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
          <div className={styles.tableCard}>
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
            <span className={styles.emptyIcon}>📭</span>
            <p className={styles.emptyText}>No data found for the selected period.</p>
          </div>
        )}
      </main>
    </div>
  );
}
