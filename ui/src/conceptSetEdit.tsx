import Toolbar from "@mui/material/Toolbar";
import ActionBar from "actionBar";
import { getCriteriaPlugin, getCriteriaTitle } from "cohort";
import { useConceptSet } from "hooks";
import React from "react";

export default function Edit() {
  const conceptSet = useConceptSet();
  const plugin = getCriteriaPlugin(conceptSet.criteria);

  return (
    <>
      <ActionBar title={getCriteriaTitle(conceptSet.criteria, plugin)} />
      <Toolbar />
      {plugin.renderEdit?.()}
    </>
  );
}
