import { EntitiesApiContext, UnderlaysApiContext } from "apiContext";
import Loading from "components/loading";
import { FilterType } from "data/filter";
import { useAsyncWithApi } from "errors";
import { useAppDispatch } from "hooks";
import { enableMapSet } from "immer";
import "plugins";
import { useCallback, useContext } from "react";
import { HashRouter } from "react-router-dom";
import { AppRouter } from "router";
import { LoadingUserData } from "storage/storage";
import { setUnderlays } from "underlaysSlice";
import "./app.css";

enableMapSet();

// Prepackaged concept sets use _ in the ids to ensure they can't conflict with
// user generated ones.
const prepackagedConceptSets = [
  {
    id: "_demographics",
    name: "Demographics",
    occurrence: "",
  },
  {
    id: "_analgesics",
    name: "Analgesics",
    occurrence: "ingredient_occurrence",
    filter: {
      type: FilterType.Classification,
      occurrenceID: "ingredient_occurrence",
      classificationID: "ingredient",
      keys: [21604253],
    },
  },
];

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

      dispatch(
        setUnderlays(
          entitiesResList.map((entitiesRes, i) => {
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
              prepackagedConceptSets,
            };
          })
        )
      );
    }, [])
  );

  return (
    <LoadingUserData>
      <Loading status={underlaysState}>
        <HashRouter>
          <AppRouter />
        </HashRouter>
      </Loading>
    </LoadingUserData>
  );
}
