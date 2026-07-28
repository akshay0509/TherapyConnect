import { createContext, useContext, useEffect, useState } from "react";
import { getTherapistProfile } from "../api/therapistProfile";

/**
 * The JWT only carries the username, but the workspace should address the
 * therapist by name. The profile is fetched once here and shared, so the
 * topbar and the dashboard greeting don't each request it.
 */
const TherapistProfileContext = createContext({ profile: null, firstName: null, displayName: null });

export function TherapistProfileProvider({ fallbackName, children }) {
  const [profile, setProfile] = useState(null);

  useEffect(() => {
    let cancelled = false;
    getTherapistProfile()
      .then(data => { if (!cancelled) setProfile(data); })
      // A missing or failed profile must never blank the greeting — the
      // username fallback keeps the page usable.
      .catch(() => { if (!cancelled) setProfile(null); });
    return () => { cancelled = true; };
  }, []);

  const firstName = profile?.firstName?.trim() || null;
  const fullName = [profile?.firstName, profile?.lastName]
    .filter(part => part && part.trim())
    .join(" ")
    .trim() || null;

  const value = {
    profile,
    // greeting: "Good morning, Saipriya" reads better than the full name
    firstName: firstName || fallbackName || "Therapist",
    // topbar: the full name where there's room for it
    displayName: fullName || fallbackName || "Therapist",
  };

  return (
    <TherapistProfileContext.Provider value={value}>
      {children}
    </TherapistProfileContext.Provider>
  );
}

export function useTherapistProfile() {
  return useContext(TherapistProfileContext);
}
