import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tsconfigPaths from "vite-tsconfig-paths";
import EnvironmentPlugin from "vite-plugin-environment";

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    tsconfigPaths(),
    react({
      babel: {
        parserOpts: {
          plugins: ["decorators-legacy", "classProperties"],
        },
      },
    }),
    EnvironmentPlugin("all", { prefix: "REACT_APP_" }),
  ],
  server: {
    port: 3000,
    proxy: {
      "/v2": "http://localhost:8080",
    },
  },
  build: { outDir: "build" },
  esbuild: {
    // Required for decorators.
    target: "es2022",
  },
});
