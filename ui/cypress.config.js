import { defineConfig } from "cypress";

export default defineConfig({
  defaultCommandTimeout: 30000,
  fixturesFolder: false,
  viewportWidth: 1280,
  viewportHeight: 720,
  e2e: {
    setupNodeEvents(on, config) {},
  },
});
