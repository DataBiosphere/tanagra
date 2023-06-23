import CssBaseline from "@mui/material/CssBaseline";
import { ThemeProvider } from "@mui/material/styles";
import { enableMapSet } from "immer";
import "plugins";
import { RouterProvider } from "react-router-dom";
import { createAppRouter } from "router";
import theme from "./theme";

enableMapSet();

export default function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <RouterProvider router={createAppRouter()} />
    </ThemeProvider>
  );
}
