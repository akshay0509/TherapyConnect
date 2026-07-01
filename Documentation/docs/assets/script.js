/* ============================================================
   TherapyConnect Docs — shared client-side behavior
   - Sidebar injection + active highlighting
   - Theme toggle (persisted)
   - Auto Table of Contents + scroll spy
   - Client-side search index
   - Mermaid init (theme-aware)
   - Mobile menu + collapsibles
   ============================================================ */
(function () {
  "use strict";

  // ---- Site navigation model -------------------------------------------
  const NAV = [
    { group: "Overview", items: [
      { n: "", file: "index.html", title: "Introduction", icon: "🏠" },
      { n: "01", file: "architecture.html", title: "Architecture", icon: "🏗️" },
      { n: "02", file: "services.html", title: "Services", icon: "📦" },
    ]},
    { group: "Reference", items: [
      { n: "03", file: "api.html", title: "API Reference", icon: "🔌" },
      { n: "04", file: "database.html", title: "Data Model", icon: "🗄️" },
      { n: "05", file: "messaging.html", title: "Messaging & Events", icon: "📨" },
      { n: "06", file: "security.html", title: "Security", icon: "🔐" },
    ]},
    { group: "Operations", items: [
      { n: "07", file: "configuration.html", title: "Configuration", icon: "⚙️" },
      { n: "08", file: "deployment.html", title: "Deployment", icon: "🚀" },
      { n: "09", file: "workflows.html", title: "Business Workflows", icon: "🔄" },
    ]},
    { group: "Guides", items: [
      { n: "10", file: "onboarding.html", title: "Onboarding", icon: "🎓" },
      { n: "11", file: "technical-debt.html", title: "Technical Debt", icon: "🛠️" },
    ]},
  ];

  // Flat ordered list for prev/next + search
  const FLAT = NAV.flatMap(g => g.items);

  const SEARCH_INDEX = [
    { t: "Introduction", f: "index.html", k: "overview platform therapy microservices spring boot kafka eureka getting started" },
    { t: "Architecture", f: "architecture.html", k: "topology layers gateway eureka discovery cqrs outbox projections diagram dependency graph" },
    { t: "Services", f: "services.html", k: "gateway user therapist client appointment notification analytics discovery ports responsibilities" },
    { t: "API Reference", f: "api.html", k: "endpoints rest login refresh appointment availability earnings clients notes analytics http" },
    { t: "Data Model", f: "database.html", k: "entities postgres tables jpa hibernate projections outbox refresh tokens er diagram indexes" },
    { t: "Messaging & Events", f: "messaging.html", k: "kafka topics events consumers producers outbox dlq appointment availability client login" },
    { t: "Security", f: "security.html", k: "jwt rs256 rsa oauth2 resource server refresh token rate limit circuit breaker encryption aes gcm" },
    { t: "Configuration", f: "configuration.html", k: "environment variables application properties profiles db url kafka google oauth ports" },
    { t: "Deployment", f: "deployment.html", k: "startup order maven docker postgres kafka eureka run build mvnw topology" },
    { t: "Business Workflows", f: "workflows.html", k: "booking reschedule cancel availability slot generation password reset calendar invite analytics" },
    { t: "Onboarding", f: "onboarding.html", k: "new engineer setup local run debug first change conventions packages" },
    { t: "Technical Debt", f: "technical-debt.html", k: "risks debt coupling todo improvements scalability security gaps recommendations" },
  ];

  const here = (location.pathname.split("/").pop() || "index.html").toLowerCase();

  // ---- Theme -----------------------------------------------------------
  const root = document.documentElement;
  const savedTheme = localStorage.getItem("tc-theme");
  if (savedTheme) root.setAttribute("data-theme", savedTheme);
  else root.setAttribute("data-theme", window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");

  function toggleTheme() {
    const next = root.getAttribute("data-theme") === "dark" ? "light" : "dark";
    root.setAttribute("data-theme", next);
    localStorage.setItem("tc-theme", next);
    syncMermaidTheme();
  }

  // ---- Sidebar collapse (desktop) --------------------------------------
  function toggleSidebar() {
    const collapsed = document.body.classList.toggle("sidebar-collapsed");
    localStorage.setItem("tc-sidebar", collapsed ? "collapsed" : "open");
  }

  // ---- Build chrome ----------------------------------------------------
  function buildTopbar() {
    const bar = document.createElement("header");
    bar.className = "topbar";
    bar.innerHTML = `
      <button class="icon-btn menu-toggle" aria-label="Toggle menu" id="menuToggle">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
      </button>
      <button class="icon-btn sidebar-collapse-btn" aria-label="Collapse sidebar" title="Toggle sidebar ([)" id="sidebarCollapse">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="16" rx="2"/><line x1="9" y1="4" x2="9" y2="20"/></svg>
      </button>
      <a class="brand" href="index.html"><span class="logo">TC</span><span>TherapyConnect <span style="color:var(--text-faint);font-weight:500">Docs</span></span></a>
      <div class="spacer"></div>
      <div class="search-box">
        <span class="search-ico"><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg></span>
        <input id="searchInput" type="text" placeholder="Search docs…" autocomplete="off" aria-label="Search documentation"/>
        <div class="search-results" id="searchResults"></div>
      </div>
      <span class="version-pill">v1.0.0-SNAPSHOT</span>
      <button class="icon-btn" id="themeToggle" aria-label="Toggle theme">
        <svg class="ico-sun" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></svg>
      </button>`;
    document.body.prepend(bar);
    document.getElementById("themeToggle").addEventListener("click", toggleTheme);
    document.getElementById("menuToggle").addEventListener("click", () => {
      document.querySelector(".sidebar").classList.toggle("open");
      document.querySelector(".backdrop").classList.toggle("show");
    });
    document.getElementById("sidebarCollapse").addEventListener("click", toggleSidebar);
    wireSearch();
  }

  function buildSidebar() {
    const nav = document.createElement("nav");
    nav.className = "sidebar";
    nav.setAttribute("aria-label", "Main navigation");
    nav.innerHTML = NAV.map(g => `
      <div class="nav-group">
        <div class="nav-title">${g.group}</div>
        ${g.items.map(it => `
          <a class="nav-link ${it.file.toLowerCase() === here ? "active" : ""}" href="${it.file}">
            <span class="num">${it.n}</span><span>${it.title}</span>
          </a>`).join("")}
      </div>`).join("");
    const bd = document.createElement("div");
    bd.className = "backdrop";
    bd.addEventListener("click", () => {
      nav.classList.remove("open"); bd.classList.remove("show");
    });
    const layout = document.querySelector(".layout");
    layout.prepend(nav);
    document.body.appendChild(bd);
  }

  // ---- TOC + scrollspy -------------------------------------------------
  function buildTOC() {
    const content = document.querySelector(".content");
    if (!content || content.dataset.toc === "off") return;
    const heads = [...content.querySelectorAll("h2, h3")].filter(h => h.id || h.textContent.trim());
    if (heads.length < 2) return;
    heads.forEach(h => { if (!h.id) h.id = slug(h.textContent);
      const a = document.createElement("a"); a.className = "anchor-h"; a.href = "#" + h.id; a.textContent = "#"; h.appendChild(a);
    });
    const toc = document.createElement("aside");
    toc.className = "toc";
    toc.innerHTML = `<div class="toc-title">On this page</div>` +
      heads.map(h => `<a href="#${h.id}" class="${h.tagName === "H3" ? "lvl-3" : ""}">${h.firstChild.textContent}</a>`).join("");
    document.querySelector(".main").appendChild(toc);

    const links = [...toc.querySelectorAll("a")];
    const byId = {}; links.forEach(l => byId[l.getAttribute("href").slice(1)] = l);
    const obs = new IntersectionObserver(entries => {
      entries.forEach(e => {
        if (e.isIntersecting) {
          links.forEach(l => l.classList.remove("active"));
          if (byId[e.target.id]) byId[e.target.id].classList.add("active");
        }
      });
    }, { rootMargin: "-72px 0px -70% 0px" });
    heads.forEach(h => obs.observe(h));
  }

  function slug(s) {
    return s.toLowerCase().replace(/[^\w\s-]/g, "").trim().replace(/\s+/g, "-").slice(0, 60);
  }

  // ---- Breadcrumbs + prev/next ----------------------------------------
  function buildNavAids() {
    const content = document.querySelector(".content");
    if (!content) return;
    const cur = FLAT.find(x => x.file.toLowerCase() === here);
    const title = cur ? cur.title : document.title;
    // breadcrumbs
    if (!content.querySelector(".breadcrumbs")) {
      const bc = document.createElement("div");
      bc.className = "breadcrumbs";
      bc.innerHTML = `<a href="index.html">Docs</a><span class="sep">/</span><span>${title}</span>`;
      content.prepend(bc);
    }
    // prev/next
    const idx = FLAT.findIndex(x => x.file.toLowerCase() === here);
    if (idx >= 0) {
      const prev = FLAT[idx - 1], next = FLAT[idx + 1];
      const pn = document.createElement("nav");
      pn.className = "page-nav";
      pn.innerHTML =
        (prev ? `<a class="prev" href="${prev.file}"><div class="pn-dir">← Previous</div><div class="pn-title">${prev.title}</div></a>` : `<span></span>`) +
        (next ? `<a class="next" href="${next.file}"><div class="pn-dir">Next →</div><div class="pn-title">${next.title}</div></a>` : `<span></span>`);
      content.appendChild(pn);
      const foot = document.createElement("footer");
      foot.className = "page-footer";
      foot.innerHTML = `<div>TherapyConnect — Engineering Documentation</div><div>Generated by automated source analysis · <a href="technical-debt.html">Known limitations</a></div>`;
      content.appendChild(foot);
    }
  }

  // ---- Search ----------------------------------------------------------
  function wireSearch() {
    const input = document.getElementById("searchInput");
    const box = document.getElementById("searchResults");
    if (!input) return;
    function run() {
      const q = input.value.trim().toLowerCase();
      if (!q) { box.classList.remove("show"); return; }
      const terms = q.split(/\s+/);
      const hits = SEARCH_INDEX.map(e => {
        const hay = (e.t + " " + e.k).toLowerCase();
        let score = 0;
        terms.forEach(t => { if (hay.includes(t)) score += e.t.toLowerCase().includes(t) ? 3 : 1; });
        return { e, score };
      }).filter(x => x.score > 0).sort((a, b) => b.score - a.score);
      box.innerHTML = hits.length
        ? hits.map(h => `<a href="${h.e.f}"><span class="r-page">${h.e.t}</span><span>${h.e.k.split(" ").slice(0, 9).join(" ")}…</span></a>`).join("")
        : `<div class="no-res">No matches for “${q}”.</div>`;
      box.classList.add("show");
    }
    input.addEventListener("input", run);
    input.addEventListener("focus", run);
    document.addEventListener("click", e => {
      if (!e.target.closest(".search-box")) box.classList.remove("show");
    });
    input.addEventListener("keydown", e => {
      if (e.key === "Enter") { const first = box.querySelector("a"); if (first) location.href = first.getAttribute("href"); }
      if (e.key === "Escape") box.classList.remove("show");
    });
  }

  // ---- Mermaid ---------------------------------------------------------
  function syncMermaidTheme() {
    if (!window.mermaid) return;
    // Re-render requires reload of original definitions; we re-run on toggle.
    const dark = root.getAttribute("data-theme") === "dark";
    try {
      window.mermaid.initialize({ startOnLoad: false, theme: dark ? "dark" : "default", securityLevel: "loose", flowchart: { curve: "basis" } });
      document.querySelectorAll(".mermaid").forEach(el => {
        if (el.dataset.src) { el.removeAttribute("data-processed"); el.innerHTML = el.dataset.src; }
      });
      window.mermaid.run();
    } catch (e) { /* noop */ }
  }
  function initMermaid() {
    if (!window.mermaid) return;
    document.querySelectorAll(".mermaid").forEach(el => { el.dataset.src = el.textContent; });
    const dark = root.getAttribute("data-theme") === "dark";
    window.mermaid.initialize({ startOnLoad: false, theme: dark ? "dark" : "default", securityLevel: "loose", flowchart: { curve: "basis" } });
    window.mermaid.run();
  }

  // ---- Boot ------------------------------------------------------------
  document.addEventListener("DOMContentLoaded", () => {
    // Restore collapsed sidebar state before building chrome (avoids animation flash)
    if (localStorage.getItem("tc-sidebar") === "collapsed") document.body.classList.add("sidebar-collapsed");
    buildTopbar();
    buildSidebar();
    buildNavAids();
    buildTOC();
    // Mermaid may load async via CDN
    if (window.mermaid) initMermaid();
    else window.addEventListener("load", () => setTimeout(initMermaid, 200));
    // highlight.js if present
    if (window.hljs) document.querySelectorAll("pre code").forEach(b => window.hljs.highlightElement(b));
    // Keyboard shortcut: "[" toggles the sidebar (ignored while typing in the search box)
    document.addEventListener("keydown", e => {
      if (e.key === "[" && !/^(input|textarea)$/i.test(e.target.tagName)) { e.preventDefault(); toggleSidebar(); }
    });
  });
})();
