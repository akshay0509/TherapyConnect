import { createContext, useContext, useEffect, useState, useCallback } from "react";
import { getDeliveryModes } from "../api/deliveryModes";
import { useAuth } from "./AuthContext";

const DeliveryModesContext = createContext({
  modeMap: {},      // modeId → TherapyDeliveryModeDto
  allModes: [],     // flat list
  refresh: () => {},
});

export function DeliveryModesProvider({ children }) {
  const { token } = useAuth();
  const [modeMap, setModeMap] = useState({});
  const [allModes, setAllModes] = useState([]);

  const refresh = useCallback(() => {
    getDeliveryModes()
      .then((modes) => {
        const map = {};
        modes.forEach((m) => { map[m.modeId] = m; });
        setModeMap(map);
        setAllModes(modes);
      })
      .catch(() => {});
  }, []);

  // Only fetch when there is a valid token. Without this guard the provider
  // fires a GET on the login page, which returns 401 and the global Axios
  // interceptor does window.location.href = "/login" — causing an infinite
  // reload loop that manifests as page flickering.
  useEffect(() => {
    if (token) refresh();
  }, [token, refresh]);

  return (
    <DeliveryModesContext.Provider value={{ modeMap, allModes, refresh }}>
      {children}
    </DeliveryModesContext.Provider>
  );
}

export function useModeMap() {
  return useContext(DeliveryModesContext).modeMap;
}

export function useAllModes() {
  return useContext(DeliveryModesContext).allModes;
}

export function useDeliveryModes() {
  return useContext(DeliveryModesContext);
}
