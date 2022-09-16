import ActionBar from "actionBar";
import { useGroupAndCriteria } from "hooks";
import React from "react";
import { getCriteriaPlugin } from "./cohort";

export default function Edit() {
  const { criteria } = useGroupAndCriteria();

  return (
    <>
      <ActionBar title={criteria.name} />
      {getCriteriaPlugin(criteria).renderEdit?.()}
    </>
  );
}
