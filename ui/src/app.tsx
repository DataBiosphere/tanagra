import React, { useState } from "react";
import { HashRouter, Route, Switch } from "react-router-dom";
import "./app.css";
import { Cohort } from "./cohort";
import { CohortUpdaterProvider } from "./cohortUpdaterContext";
import Edit from "./edit";
import Overview from "./overview";

type AppProps = {
  underlayName: string;
  entityName: string;
};

export default function App(props: AppProps) {
  const [cohort, setCohort] = useState<Cohort>(
    new Cohort(
      props.underlayName,
      props.entityName,
      // TODO(tjennison): Populate from an actual source.
      [
        "person_id",
        "condition_occurrence_id",
        "condition_concept_id",
        "condition_name",
      ]
    )
  );

  return (
    <CohortUpdaterProvider cohort={cohort} setCohort={setCohort}>
      <HashRouter basename="/">
        <Switch>
          <Route path="/edit/:group/:criteria">
            <Edit cohort={cohort} />
          </Route>
          <Route path="/">
            <Overview cohort={cohort} />
          </Route>
        </Switch>
      </HashRouter>
    </CohortUpdaterProvider>
  );
}
