// Theme preference: applied as data-theme on <html>, persisted per browser.
// Dark is the default; index.css defines the token values for both themes.
const STORAGE_KEY = "tc-theme";

export function getTheme() {
  try {
    return localStorage.getItem(STORAGE_KEY) === "light" ? "light" : "dark";
  } catch {
    return "dark";
  }
}

export function applyTheme(theme) {
  document.documentElement.dataset.theme = theme;
}

export function setTheme(theme) {
  try { localStorage.setItem(STORAGE_KEY, theme); } catch { /* private mode */ }
  applyTheme(theme);
}

// called before first render so the page never flashes the wrong theme
export function initTheme() {
  applyTheme(getTheme());
}
