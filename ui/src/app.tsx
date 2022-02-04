import { useAppSelector } from "hooks";
import { enableMapSet } from "immer";
import "plugins";
import {
  HashRouter,
  Redirect,
  Route,
  Switch,
  useParams,
  useRouteMatch,
} from "react-router-dom";
import "./app.css";
import { Datasets } from "./datasets";
import Edit from "./edit";
import Overview from "./overview";

enableMapSet();

type AppProps = {
  underlayNames: string[];
  entityName: string;
};

export default function App(props: AppProps) {
  return (
    <HashRouter basename="/">
      <Switch>
        <Route path="/cohort/:cohortId">
          <CohortRouter />
        </Route>
        <Route path="/">
          <Datasets
            underlayNames={props.underlayNames}
            entityName={props.entityName}
          />
        </Route>
      </Switch>
    </HashRouter>
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
