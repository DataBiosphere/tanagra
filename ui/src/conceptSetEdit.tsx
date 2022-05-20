import ActionBar from "actionBar";
import { getCriteriaPlugin } from "cohort";
import { updateConceptSetData } from "conceptSetsSlice";
import { useAppDispatch, useConceptSet } from "hooks";
import React from "react";

export default function Edit() {
  const dispatch = useAppDispatch();
  const conceptSet = useConceptSet();

  return (
    <>
      <ActionBar title={conceptSet.criteria.name} />
      {getCriteriaPlugin(conceptSet.criteria).renderEdit((data: object) => {
        dispatch(
          updateConceptSetData({
            conceptSetId: conceptSet.id,
            data: data,
          })
        );
      })}
    </>
  );
}
