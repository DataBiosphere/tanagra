import React from "react";
import { Redirect, useParams } from "react-router-dom";
import ActionBar from "./actionBar";
import { Cohort } from "./cohort";

type EditProps = {
  cohort: Cohort;
};

export default function Edit(props: EditProps) {
  const params = useParams<{ group: string; criteria: string }>();

  const group = props.cohort.findGroup(params.group);
  const criteria = !!group ? group.findCriteria(params.criteria) : null;

  return (
    <>
      {!criteria ? <Redirect to="/" /> : null}
      <ActionBar
        title={criteria?.name || "Unknown"}
        backUrl="/"
        cohort={props.cohort}
      />
      {!!criteria && !!group ? criteria.renderEdit(props.cohort, group) : null}
    </>
  );
}
