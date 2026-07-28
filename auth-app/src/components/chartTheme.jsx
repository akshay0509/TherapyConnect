// Recharts takes literal colour values, not var(--token), so chart chrome can't
// be styled from CSS. Instead of hardcoding one theme's hex codes (which left the
// axis ticks at ~3:1 contrast and the grid all but invisible in light mode), the
// values are read off the live custom properties and re-read whenever the theme
// flips — so charts follow the light/dark toggle like every other surface.
//
// Series colours are deliberately NOT in here: the brand hues (cyan/green/amber/
// red) are fixed identity and read correctly on both themes.
import { useEffect, useState } from "react";

function readChartTheme() {
  const cs = getComputedStyle(document.documentElement);
  const v = (name, fallback) => cs.getPropertyValue(name).trim() || fallback;

  const surface = v("--surface", "#101a2c");
  const text1   = v("--text-1", "#eaf1fb");
  const text2   = v("--text-2", "#9fb0c8");
  const text3   = v("--text-3", "#8497b1");
  const line    = v("--line",   "rgba(150,180,220,0.10)");
  const line2   = v("--line-2", "rgba(150,180,220,0.18)");

  return {
    // Axis tick labels and gridlines track the body text / hairline tokens.
    AXIS_TICK: { fill: text3, fontSize: 11 },
    GRID_STROKE: line,
    // Pie labels use one muted tone; the hue lives in the slice + legend swatch.
    PIE_LABEL_FILL: text2,
    LEGEND_STYLE: { fontSize: 12, paddingTop: 8 },
    // contentStyle alone leaves the series rows and the label in Recharts'
    // defaults, so itemStyle/labelStyle are set explicitly too.
    TOOLTIP: {
      contentStyle: {
        background: surface,
        border: `1px solid ${line2}`,
        borderRadius: 10,
        boxShadow: "0 12px 30px rgba(0,0,0,0.28)",
        color: text1,
      },
      itemStyle: { color: text1, fontSize: 12, padding: "2px 0" },
      labelStyle: { color: text3, fontSize: 11, fontWeight: 700, marginBottom: 4 },
    },
    // Bars get a brand-tinted hover wash (the default is an opaque grey block).
    BAR_CURSOR: { fill: "rgba(34,211,238,0.07)" },
    // Line charts draw a vertical rule instead, which Recharts defaults to a
    // glaring #ccc.
    LINE_CURSOR: { stroke: line2, strokeWidth: 1 },
  };
}

export function useChartTheme() {
  const [theme, setTheme] = useState(readChartTheme);

  useEffect(() => {
    const root = document.documentElement;
    const obs = new MutationObserver(() => setTheme(readChartTheme()));
    obs.observe(root, { attributes: true, attributeFilter: ["data-theme"] });
    return () => obs.disconnect();
  }, []);

  return theme;
}

// Keeps tooltip rows in series-declaration order so they match the legend
// (Recharts otherwise reorders them and the two lists disagree).
export const KEEP_ORDER = () => 0;

// The API returns ISO dates; "2026-07-13" is too long for an axis tick.
export const dateTick = (d) =>
  new Date(d).toLocaleDateString("en-IN", { day: "numeric", month: "short" });
export const dateLabel = (d) =>
  new Date(d).toLocaleDateString("en-IN", { weekday: "short", day: "numeric", month: "short", year: "numeric" });

// Recharts paints pie labels with the slice's own fill, which makes a multi-hue
// pie read as scattered text. Recharts hands us the computed x/y/anchor.
export function makePieLabel(fill) {
  return function pieLabel({ x, y, textAnchor, name, percent }) {
    // A slice with no data still gets a label ("DSF 0%"), which just adds noise
    // next to a slice that isn't drawn.
    if (!percent) return null;
    return (
      <text x={x} y={y} textAnchor={textAnchor} dominantBaseline="central"
        fill={fill} fontSize={11} fontWeight={600}>
        {`${name} ${(percent * 100).toFixed(0)}%`}
      </text>
    );
  };
}
