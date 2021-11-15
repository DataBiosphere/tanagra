import * as _ from "lodash";
import React, { useCallback, useState } from "react";
import {
  HashRouter,
  Redirect,
  Route,
  Switch,
  useParams,
  useRouteMatch,
} from "react-router-dom";
import "./app.css";
import { Cohort } from "./cohort";
import { CohortUpdaterProvider } from "./cohortUpdaterContext";
import { Datasets } from "./datasets";
import Edit from "./edit";
import Overview from "./overview";
import { UserData } from "./userData";

type AppProps = {
  underlayNames: string[];
  entityName: string;
};

export default function App(props: AppProps) {
  const [userData, setUserData] = useState<UserData>(
    new UserData(props.entityName)
  );

  const update = useCallback(
    (callback: (userData: UserData) => void) => {
      const newUserData = _.cloneDeep(userData);
      callback(newUserData);
      setUserData(newUserData);
    },
    [userData, setUserData]
  );

  return (
    <HashRouter basename="/">
      <Switch>
        <Route path="/cohort/:cohortId">
          <CohortRouter userData={userData} update={update} />
        </Route>
        <Route path="/">
          <Datasets
            underlayNames={props.underlayNames}
            userData={userData}
            update={update}
          />
        </Route>
      </Switch>
    </HashRouter>
  );
}

type CohortRouterProps = {
  userData: UserData;
  update: (callback: (userData: UserData) => void) => void;
};

function CohortRouter(props: CohortRouterProps) {
  const match = useRouteMatch();
  const { cohortId } = useParams<{ cohortId: string }>();
  const cohort = props.userData.findCohort(cohortId);

  const setCohort = useCallback(
    (cohort: Cohort) => {
      props.update((userData: UserData) => {
        const index = userData.findCohortIndex(cohort.id);
        if (index < 0) {
          throw new Error("cohort not found");
        }
        userData.cohorts[index] = cohort;
      });
    },
    [props]
  );

  return cohort ? (
    <CohortUpdaterProvider cohort={cohort} setCohort={setCohort}>
      <Switch>
        <Route path={`${match.path}/edit/:group/:criteria`}>
          <Edit cohort={cohort} />
        </Route>
        <Route path={match.path}>
          <Overview cohort={cohort} />
        </Route>
      </Switch>
    </CohortUpdaterProvider>
  ) : (
    <Redirect to="/" />
  );
}
