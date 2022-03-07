import ActionBar from "actionBar";
import { useCohortOrFail, useUnderlayOrFail } from "hooks";
import React from "react";
import { Redirect, useParams } from "react-router-dom";
import { Cohort, getCriteriaPlugin } from "./cohort";

type EditProps = {
  cohort: Cohort;
};

export default function Edit(props: EditProps) {
  const underlay = useUnderlayOrFail();
  const cohort = useCohortOrFail();
  const backUrl = `/${underlay.name}/${cohort.id}`;

  const params = useParams<{ group: string; criteria: string }>();
  const group = props.cohort.groups.find((g) => g.id === params.group);
  const criteria = group?.criteria.find((c) => c.id === params.criteria);

  return (
    <>
      {!criteria ? <Redirect to={backUrl} /> : null}
      <ActionBar title={criteria?.name || "Unknown"} backUrl={backUrl} />
      {!!criteria && !!group
        ? getCriteriaPlugin(criteria).renderEdit(props.cohort, group)
        : null}
    </>
  );
}
