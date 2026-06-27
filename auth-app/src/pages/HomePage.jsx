import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import styles from "./HomePage.module.css";

export default function HomePage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <span className={styles.logo}>⬡ MyApp</span>
          <div className={styles.headerRight}>
            <span className={styles.username}>
              {user?.username || user?.name || "User"}
            </span>
            <button onClick={handleLogout} className={styles.logoutBtn}>
              Sign out
            </button>
          </div>
        </div>
      </header>

      <main className={styles.main}>
        <div className={styles.hero}>
          <p className={styles.eyebrow}>You're in ✦</p>
          <h1 className={styles.heading}>
            Welcome back,{" "}
            <span className={styles.accent}>
              {user?.username || user?.name || "friend"}
            </span>
          </h1>
          <p className={styles.body}>
            You've successfully authenticated. This is your home page — build
            something great from here.
          </p>
        </div>

        {/* ── Quick actions ── */}
        <div className={styles.actions}>
          <button className={styles.actionBtn} onClick={() => navigate("/therapists")}>
            <span className={styles.actionIcon}>🧠</span>
            <div>
              <div className={styles.actionTitle}>Therapists</div>
              <div className={styles.actionSub}>Browse registered therapists</div>
            </div>
            <span className={styles.actionArrow}>→</span>
          </button>
        </div>

        <div className={styles.cards}>
          <div className={styles.card}>
            <div className={styles.cardIcon}>🔐</div>
            <h3 className={styles.cardTitle}>JWT Active</h3>
            <p className={styles.cardText}>
              Your token is stored and will be sent automatically with every API request.
            </p>
          </div>
          <div className={styles.card}>
            <div className={styles.cardIcon}>🛡️</div>
            <h3 className={styles.cardTitle}>Protected Routes</h3>
            <p className={styles.cardText}>
              Unauthenticated users are redirected to login. Token expiry triggers automatic logout.
            </p>
          </div>
          <div className={styles.card}>
            <div className={styles.cardIcon}>⚡</div>
            <h3 className={styles.cardTitle}>Ready to Build</h3>
            <p className={styles.cardText}>
              Add your own pages, API calls, and components. The auth layer is fully wired up.
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}
