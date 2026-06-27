import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // Proxies /auth/* → your Spring Boot backend
      // This avoids CORS issues during development
      "/auth": {
        target: "http://localhost:8091",
        changeOrigin: true,
      },
      "/api": {
        target: "http://localhost:8091",
        changeOrigin: true,
      },
    },
  },
});
