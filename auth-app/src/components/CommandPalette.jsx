import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { getTherapistClients } from "../api/therapistClients";
import styles from "./CommandPalette.module.css";

const PAGES = [
  { label: "Schedule",           hint: "Book, reschedule, block time", icon: "🗓", path: "/therapist/appointments" },
  { label: "My Clients",         hint: "Client list and histories",    icon: "👥", path: "/therapist/clients" },
  { label: "My Services",        hint: "Services and delivery modes",  icon: "🧩", path: "/therapist/services" },
  { label: "Availability Rules", hint: "Weekly working hours",         icon: "⏰", path: "/therapist/availability-rules" },
  { label: "Earnings",           hint: "Payments and revenue",         icon: "💰", path: "/therapist/earnings" },
  { label: "Analytics",          hint: "Trends and retention",         icon: "📊", path: "/therapist/analytics" },
  { label: "Account Settings",   hint: "Profile, security, theme",     icon: "⚙️", path: "/account-settings" },
];

const MAX_CLIENT_RESULTS = 6;

function getInitials(name) {
  if (!name) return "?";
  const parts = name.trim().split(/\s+/);
  return ((parts[0]?.[0] ?? "") + (parts.length > 1 ? parts[parts.length - 1][0] : "")).toUpperCase();
}

/**
 * Therapist-only jump-to launcher: Ctrl/Cmd-K anywhere opens a search over
 * clients and pages. Clients are fetched once on first open and reused, so the
 * shortcut stays instant and costs nothing to users who never press it.
 */
export default function CommandPalette() {
  const navigate = useNavigate();
  const { token, role } = useAuth();
  const enabled = Boolean(token) && role === "THERAPIST";

  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [active, setActive] = useState(0);
  const [clients, setClients] = useState([]);
  const [loaded, setLoaded] = useState(false);

  const inputRef = useRef(null);
  const listRef = useRef(null);

  // Global shortcut. Bound whenever a therapist is signed in; Escape only
  // closes the palette, so it never interferes with other Escape handlers.
  useEffect(() => {
    if (!enabled) return;
    const onKey = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        setOpen((o) => !o);
      } else if (e.key === "Escape" && open) {
        e.stopPropagation();
        setOpen(false);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [enabled, open]);

  // Signing out must not leave the palette hanging over the login page.
  useEffect(() => {
    if (!enabled) setOpen(false);
  }, [enabled]);

  useEffect(() => {
    if (!open) return;
    setQuery("");
    setActive(0);
    inputRef.current?.focus();
    if (loaded) return;
    getTherapistClients()
      .then((data) => { setClients(data || []); setLoaded(true); })
      .catch(() => setLoaded(true)); // pages still work without the client list
  }, [open, loaded]);

  const results = useMemo(() => {
    const q = query.trim().toLowerCase();
    const matchedPages = PAGES
      .filter((p) => !q || p.label.toLowerCase().includes(q) || p.hint.toLowerCase().includes(q))
      .map((p) => ({ kind: "page", key: p.path, ...p }));

    const matchedClients = (q ? clients.filter((c) =>
      c.clientName?.toLowerCase().includes(q) || c.clientId?.toLowerCase().includes(q)
    ) : clients)
      .slice(0, MAX_CLIENT_RESULTS)
      .map((c) => ({
        kind: "client",
        key: c.clientId,
        label: c.clientName || "Unnamed client",
        hint: c.clientId,
        path: `/therapist/clients/${c.clientId}`,
      }));

    // Searching by name is the main reason to open this, so clients lead once
    // the user has typed; with an empty box the pages are the useful default.
    return q ? [...matchedClients, ...matchedPages] : [...matchedPages, ...matchedClients];
  }, [query, clients]);

  // Keep the highlight in range as results narrow.
  useEffect(() => { setActive((a) => Math.min(a, Math.max(results.length - 1, 0))); }, [results.length]);

  const go = useCallback((item) => {
    if (!item) return;
    setOpen(false);
    navigate(item.path);
  }, [navigate]);

  const onInputKey = (e) => {
    if (e.key === "ArrowDown")      { e.preventDefault(); setActive((a) => (a + 1) % Math.max(results.length, 1)); }
    else if (e.key === "ArrowUp")   { e.preventDefault(); setActive((a) => (a - 1 + results.length) % Math.max(results.length, 1)); }
    else if (e.key === "Enter")     { e.preventDefault(); go(results[active]); }
  };

  // Follow the highlight when it moves past the visible edge.
  useEffect(() => {
    listRef.current?.querySelector(`[data-idx="${active}"]`)?.scrollIntoView({ block: "nearest" });
  }, [active]);

  if (!enabled || !open) return null;

  return (
    <div className={styles.backdrop} onMouseDown={() => setOpen(false)}>
      <div className={styles.panel} role="dialog" aria-modal="true" aria-label="Search clients and pages"
           onMouseDown={(e) => e.stopPropagation()}>
        <div className={styles.searchRow}>
          <span className={styles.searchIcon}>⌕</span>
          <input
            ref={inputRef}
            className={styles.input}
            placeholder="Search clients or jump to a page…"
            value={query}
            onChange={(e) => { setQuery(e.target.value); setActive(0); }}
            onKeyDown={onInputKey}
            aria-label="Search clients or pages"
          />
          <kbd className={styles.kbd}>esc</kbd>
        </div>

        <div className={styles.list} ref={listRef} role="listbox">
          {results.length === 0 && (
            <p className={styles.empty}>
              {loaded ? `No matches for "${query}"` : "Loading…"}
            </p>
          )}
          {results.map((item, i) => (
            <button
              key={`${item.kind}-${item.key}`}
              data-idx={i}
              type="button"
              role="option"
              aria-selected={i === active}
              className={`${styles.item} ${i === active ? styles.itemActive : ""}`}
              onMouseEnter={() => setActive(i)}
              onClick={() => go(item)}
            >
              <span className={item.kind === "client" ? styles.avatar : styles.pageIcon}>
                {item.kind === "client" ? getInitials(item.label) : item.icon}
              </span>
              <span className={styles.itemText}>
                <span className={styles.itemLabel}>{item.label}</span>
                <span className={styles.itemHint}>{item.hint}</span>
              </span>
              <span className={styles.itemKind}>{item.kind === "client" ? "Client" : "Page"}</span>
            </button>
          ))}
        </div>

        <div className={styles.foot}>
          <span><kbd className={styles.kbd}>↑</kbd><kbd className={styles.kbd}>↓</kbd> navigate</span>
          <span><kbd className={styles.kbd}>↵</kbd> open</span>
          <span><kbd className={styles.kbd}>⌘</kbd><kbd className={styles.kbd}>K</kbd> toggle</span>
        </div>
      </div>
    </div>
  );
}
