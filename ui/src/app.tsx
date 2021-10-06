import React, { useState } from "react";
import { HashRouter, Route, Switch } from "react-router-dom";
import "./app.css";
import { ConceptCriteria } from "./criteria/concept";
import { DataSet, Group, GroupKind } from "./dataSet";
import Edit from "./edit";
import Overview from "./overview";

type AppProps = {
  underlayName: string;
};

export default function App(props: AppProps) {
  const [dataSet, setDataSet] = useState<DataSet>(
    new DataSet(props.underlayName, [
      new Group(GroupKind.Included, [
        new ConceptCriteria("Contains Conditions Code", "condition_occurrence"),
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
    ])
  );

  return (
    <HashRouter basename="/">
      <Switch>
        <Route path="/edit/:group/:criteria">
          <Edit dataSet={dataSet} />
        </Route>
        <Route path="/">
          <Overview dataSet={dataSet} />
        </Route>
      </Switch>
    </HashRouter>
  );
}
