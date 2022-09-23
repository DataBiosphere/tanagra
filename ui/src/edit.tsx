import ActionBar from "actionBar";
import { useGroupAndCriteria } from "hooks";
import React from "react";
import { getCriteriaPlugin, getCriteriaTitle } from "./cohort";

export default function Edit() {
  const { criteria } = useGroupAndCriteria();
  const plugin = getCriteriaPlugin(criteria);

  return (
    <>
      <ActionBar title={getCriteriaTitle(criteria, plugin)} />
      {plugin.renderEdit?.()}
    </>
  );
}
