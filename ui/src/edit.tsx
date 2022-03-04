import ActionBar from "actionBar";
import React from "react";
import { Redirect, useParams } from "react-router-dom";
import { Cohort, getCriteriaPlugin } from "./cohort";

type EditProps = {
  cohort: Cohort;
};

export default function Edit(props: EditProps) {
  const params =
    useParams<{ cohortId: string; group: string; criteria: string }>();
  const backUrl = `/cohort/${params.cohortId}`;

  const group = props.cohort.groups.find((g) => g.id === params.group);
  const criteria = group?.criteria.find((c) => c.id === params.criteria);

  return (
    <>
      {!criteria ? <Redirect to={backUrl} /> : null}
      <ActionBar
        title={criteria?.name || "Unknown"}
        backUrl={backUrl}
        cohort={props.cohort}
      />
      {!!criteria && !!group
        ? getCriteriaPlugin(criteria).renderEdit(props.cohort, group)
        : null}
    </>
  );
}
