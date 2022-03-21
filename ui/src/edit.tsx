import ActionBar from "actionBar";
import { useCohort, useGroupAndCriteria } from "hooks";
import React from "react";
import { getCriteriaPlugin } from "./cohort";

export default function Edit() {
  const cohort = useCohort();
  const { group, criteria } = useGroupAndCriteria();

  return (
    <>
      <ActionBar title={criteria.name} />
      {getCriteriaPlugin(criteria).renderEdit(cohort, group)}
    </>
  );
}
