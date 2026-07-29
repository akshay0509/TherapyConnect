import { createContext, useContext } from "react";

/**
 * Context object and consumer hook, kept OUT of the provider's module.
 *
 * React Fast Refresh only works on modules that export components and nothing
 * else. When the provider and this hook lived in one file, Vite logged
 * "Could not Fast Refresh (\"useTherapistProfile\" export is incompatible)"
 * and fell back to a full page invalidate on every edit — which reloads the
 * app and drops the session, forcing a fresh sign-in mid-development.
 */
export const TherapistProfileContext = createContext({
  profile: null,
  firstName: null,
  displayName: null,
});

export function useTherapistProfile() {
  return useContext(TherapistProfileContext);
}
