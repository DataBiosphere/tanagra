import React from "react";
import { Redirect, useParams } from "react-router-dom";
import ActionBar from "./actionBar";
import { Dataset } from "./dataset";

type EditProps = {
  dataset: Dataset;
};

export default function Edit(props: EditProps) {
  const params = useParams<{ group: string; criteria: string }>();

  const group = props.dataset.findGroup(params.group);
  const criteria = !!group ? group.findCriteria(params.criteria) : null;

  return (
    <>
      {!criteria ? <Redirect to="/" /> : null}
      <ActionBar
        title={criteria?.name || "Unknown"}
        backUrl="/"
        dataset={props.dataset}
      />
      {!!criteria && !!group ? criteria.renderEdit(props.dataset, group) : null}
    </>
  );
}
