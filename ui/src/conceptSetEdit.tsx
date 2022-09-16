import ActionBar from "actionBar";
import { getCriteriaPlugin } from "cohort";
import { useConceptSet } from "hooks";
import React from "react";

export default function Edit() {
  const conceptSet = useConceptSet();

  return (
    <>
      <ActionBar title={conceptSet.criteria.name} />
      {getCriteriaPlugin(conceptSet.criteria).renderEdit?.()}
    </>
  );
}
