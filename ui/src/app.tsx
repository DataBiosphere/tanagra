import React, { useState } from "react";
import { HashRouter, Route, Switch } from "react-router-dom";
import "./app.css";
import { ConceptCriteria } from "./criteria/concept";
import { Dataset, Group, GroupKind } from "./dataset";
import { DatasetUpdaterProvider } from "./datasetUpdaterContext";
import Edit from "./edit";
import Overview from "./overview";

type AppProps = {
  underlayName: string;
  entityName: string;
};

export default function App(props: AppProps) {
  const [dataset, setDataset] = useState<Dataset>(
    new Dataset(
      props.underlayName,
      props.entityName,
      [
        new Group(GroupKind.Included, [
          new ConceptCriteria(
            "Contains Conditions Code",
            "condition_occurrence"
          ),
          new ConceptCriteria(
            "Contains Conditions Code 2",
            "condition_occurrence"
          ),
        ]),
        new Group(GroupKind.Included, [
          new ConceptCriteria(
            "Contains Conditions Code 3",
            "condition_occurrence"
          ),
          new ConceptCriteria(
            "Contains Conditions Code 4",
            "condition_occurrence"
          ),
        ]),
      ],
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
    <DatasetUpdaterProvider dataset={dataset} setDataset={setDataset}>
      <HashRouter basename="/">
        <Switch>
          <Route path="/edit/:group/:criteria">
            <Edit dataset={dataset} />
          </Route>
          <Route path="/">
            <Overview dataset={dataset} />
          </Route>
        </Switch>
      </HashRouter>
    </DatasetUpdaterProvider>
  );
}
