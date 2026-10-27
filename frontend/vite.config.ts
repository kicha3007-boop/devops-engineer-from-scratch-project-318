import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => ({
  plugins: [react()],
  server: {
    host: true,
    proxy: {
      '/api': {
        target: "http://localhost:8080"
      }
    }
  },
  build: {
    sourcemap: mode === "development",
  },
  base: "./",
}));
