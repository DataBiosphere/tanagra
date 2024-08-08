import CssBaseline from "@mui/material/CssBaseline";
import { ThemeProvider } from "@mui/material/styles";
import type { Router } from "@remix-run/router";
import { enableMapSet } from "immer";
import "plugins";
import { RouterProvider } from "react-router-dom";
import { createAppRouter } from "router";
import theme from "theme";
import "viz/plugins";

enableMapSet();

export function App() {
  return AppWithRouter({ appRouter: createAppRouter() });
}

export function AppWithRouter({ appRouter }: { appRouter: Router }) {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <RouterProvider router={appRouter} />
    </ThemeProvider>
  );
}
