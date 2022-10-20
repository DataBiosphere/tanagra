import Toolbar from "@mui/material/Toolbar";
import ActionBar from "actionBar";
import { useNewCriteria } from "hooks";
import React from "react";
import { getCriteriaPlugin } from "./cohort";

export default function NewCriteria() {
  const criteria = useNewCriteria();

  return (
    <>
      <ActionBar title={`New ${criteria.config.title} Concept Set`} />
      <Toolbar />
      {getCriteriaPlugin(criteria).renderEdit?.()}
    </>
  );
}
