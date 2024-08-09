import CssBaseline from "@mui/material/CssBaseline";
import { ThemeProvider } from "@mui/material/styles";
import { Router } from "@remix-run/router";
import { AuthProviderProps } from "auth/provider";
import { enableMapSet } from "immer";
import "plugins";
import { RouterProvider } from "react-router-dom";
import { createAppRouter } from "router";
import theme from "theme";
import "viz/plugins";

enableMapSet();

export function App() {
  return AppWithRouter();
}

export function AppWithRouter(appRouter?: Router) {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <RouterProvider
        router={appRouter ?? createAppRouter({} as AuthProviderProps)}
      />
    </ThemeProvider>
  );
}
