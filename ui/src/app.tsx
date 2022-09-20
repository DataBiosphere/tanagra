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
      const res = await underlaysApi.listUnderlays({});
      if (!res?.underlays || res.underlays.length == 0) {
        throw new Error("No underlays are configured.");
      }

      const entitiesResList = await Promise.all(
        res.underlays.map((u) => {
          if (!u.name) {
            throw new Error("Unnamed underlay.");
          }
          return entitiesApi.listEntities({ underlayName: u.name });
        })
      );

      const underlays = entitiesResList.map((entitiesRes, i) => {
        const name = res.underlays?.[i]?.name;
        if (!name) {
          throw new Error("Unnamed underlay.");
        }
        if (!entitiesRes.entities) {
          throw new Error(`No entities in underlay ${name}`);
        }

        const uiConfiguration = res.underlays?.[i]?.uiConfiguration;
        if (!uiConfiguration) {
          throw new Error(`No UI configuration in underlay ${name}`);
        }

        return {
          name,
          primaryEntity: "person",
          entities: entitiesRes.entities,
          uiConfiguration: JSON.parse(uiConfiguration),
        };
      });

      await fetchUserData(dispatch, underlays);

      dispatch(setUnderlays(underlays));
    }, [])
  );

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Loading status={underlaysState}>
        <HashRouter>
          <AppRouter />
        </HashRouter>
      </Loading>
    </ThemeProvider>
  );
}
