import Box from "@mui/material/Box";
import CssBaseline from "@mui/material/CssBaseline";
import { ThemeProvider } from "@mui/material/styles";
import { EntitiesApiContext, UnderlaysApiContext } from "apiContext";
import Loading from "components/loading";
import { useAsyncWithApi } from "errors";
import { useAppDispatch } from "hooks";
import { enableMapSet } from "immer";
import "plugins";
import { useCallback, useContext } from "react";
import { HashRouter } from "react-router-dom";
import { AppRouter } from "router";
import { fetchUserData } from "storage/storage";
import { setUnderlays } from "underlaysSlice";
import "./app.css";
import theme from "./theme";

enableMapSet();

export default function App() {
  const dispatch = useAppDispatch();
  const underlaysApi = useContext(UnderlaysApiContext);
  const entitiesApi = useContext(EntitiesApiContext);

  const underlaysState = useAsyncWithApi(
    useCallback(async () => {
      const res = await underlaysApi.listUnderlaysV2({});
      if (!res?.underlays || res.underlays.length == 0) {
        throw new Error("No underlays are configured.");
      }

      const underlays = await Promise.all(
        res.underlays.map(async (underlay) => {
          const entitiesRes = await entitiesApi.listEntitiesV2({
            underlayName: underlay.name,
          });
          if (!entitiesRes?.entities) {
            throw new Error(`No entities in underlay ${underlay.name}`);
          }

          if (!underlay.uiConfiguration) {
            throw new Error(`No UI configuration in underlay ${name}`);
          }

          return {
            name: underlay.name,
            displayName: underlay.displayName ?? underlay.name,
            primaryEntity: underlay.primaryEntity,
            entities: entitiesRes.entities,
            uiConfiguration: JSON.parse(underlay.uiConfiguration),
          };
        })
      );

      await fetchUserData(dispatch, underlays);

      dispatch(setUnderlays(underlays));
    }, [])
  );

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Loading status={underlaysState}>
        <HashRouter>
          <Box
            sx={{
              display: "grid",
              width: "100%",
              height: "100%",
              gridTemplateColumns: "1fr",
              gridTemplateRows: (theme) => `${theme.spacing(6)} 1fr`,
              gridTemplateAreas: "'actionBar' 'content'",
            }}
          >
            <Box
              sx={{
                gridArea: "content",
                width: "100%",
                minWidth: "100%",
                height: "100%",
                minHeight: "100%",
              }}
            >
              <AppRouter />
            </Box>
          </Box>
        </HashRouter>
      </Loading>
    </ThemeProvider>
  );
}
