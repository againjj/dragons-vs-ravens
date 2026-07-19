import { defineConfig } from "vite";

export default defineConfig({
  build: {
    outDir: "build/generated/frontend",
    emptyOutDir: true,
    lib: {
      entry: "src/main/frontend/mtg-entry.tsx",
      formats: ["es"],
      fileName: () => "mtg-entry.js"
    },
    rollupOptions: {
      external: ["react", "react-dom", "@ayaziangames/platform-frontend/game-entry", "@ayaziangames/platform-frontend/api-client"]
    }
  },
  test: {
    environment: "jsdom"
  }
});
