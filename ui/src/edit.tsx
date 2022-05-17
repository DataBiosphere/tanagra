import ActionBar from "actionBar";
import { updateCriteriaData } from "cohortsSlice";
import { useAppDispatch, useCohort, useGroupAndCriteria } from "hooks";
import React from "react";
import { getCriteriaPlugin } from "./cohort";

export default function Edit() {
  const dispatch = useAppDispatch();
  const cohort = useCohort();
  const { group, criteria } = useGroupAndCriteria();

  return (
    <>
      <ActionBar title={criteria.name} />
      {getCriteriaPlugin(criteria).renderEdit((data: object) => {
        dispatch(
          updateCriteriaData({
            cohortId: cohort.id,
            groupId: group.id,
            criteriaId: criteria.id,
            data: data,
          })
        );
      })}
    </>
  );
}
