import { EntitiesApiContext, UnderlaysApiContext } from "apiContext";
import { useAsyncWithApi } from "errors";
import { useAppDispatch, useAppSelector } from "hooks";
import { enableMapSet } from "immer";
import Loading from "loading";
import "plugins";
import { useCallback, useContext } from "react";
import {
  HashRouter,
  Redirect,
  Route,
  Switch,
  useParams,
  useRouteMatch,
} from "react-router-dom";
import { setUnderlays } from "underlaysSlice";
import "./app.css";
import { Datasets } from "./datasets";
import Edit from "./edit";
import Overview from "./overview";

enableMapSet();

type AppProps = {
  entityName: string;
};

export default function App(props: AppProps) {
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

            return {
              name,
              entities: entitiesRes.entities,
            };
          })
        )
      );
    }, [])
  );

  return (
    <Loading status={underlaysState}>
      <HashRouter basename="/">
        <Switch>
          <Route path="/cohort/:cohortId">
            <CohortRouter />
          </Route>
          <Route path="/">
            <Datasets entityName={props.entityName} />
          </Route>
        </Switch>
      </HashRouter>
    </Loading>
  );
}

function CohortRouter() {
  const match = useRouteMatch();
  const { cohortId } = useParams<{ cohortId: string }>();
  const cohort = useAppSelector((state) =>
    state.cohorts.find((c) => c.id === cohortId)
  );

  return cohort ? (
    <Switch>
      <Route path={`${match.path}/edit/:group/:criteria`}>
        <Edit cohort={cohort} />
      </Route>
      <Route path={match.path}>
        <Overview cohort={cohort} />
      </Route>
    </Switch>
  ) : (
    <Redirect to="/" />
  );
}
