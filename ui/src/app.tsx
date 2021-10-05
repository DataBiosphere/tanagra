import React, { useState } from "react";
import { HashRouter, Route, Switch } from "react-router-dom";
import { v4 as uuid } from "uuid";
import "./app.css";
import ConceptList from "./conceptList";
import { DataSet } from "./dataSet";
import Overview from "./overview";

type AppProps = {
  underlayName: string;
};

export default function App(props: AppProps) {
  const [dataSet, setDataSet] = useState<DataSet>({
    included: [
      {
        id: uuid(),
        criteria: [
          {
            id: uuid(),
            name: "criteria 1",
            code: "7",
          },
          {
            id: uuid(),
            name: "criteria 2",
            code: "123",
          },
        ],
      },
      {
        id: uuid(),
        criteria: [
          {
            id: uuid(),
            name: "criteria A",
            code: "456",
          },
          {
            id: uuid(),
            name: "criteria B",
            code: "789",
          },
          {
            id: uuid(),
            name: "criteria C",
            code: "7777777",
          },
        ],
      },
    ],
  });

  return (
    <HashRouter basename="/">
      <Switch>
        <Route path="/concepts/:group/:criteria">
          <ConceptList
            underlayName={props.underlayName}
            dataSet={dataSet}
            filter="condition_occurrence"
          />
        </Route>
        <Route path="/">
          <Overview underlayName={props.underlayName} dataSet={dataSet} />
        </Route>
      </Switch>
    </HashRouter>
  );
}
