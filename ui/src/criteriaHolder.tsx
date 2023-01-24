import ActionBar from "actionBar";
import { CriteriaPlugin } from "cohort";
import CohortToolbar from "cohortToolbar";
import { useState } from "react";

export type CriteriaHolderProps = {
  title: string;
  plugin: CriteriaPlugin<object>;
  doneURL?: string;
  cohort?: boolean;
  defaultBackURL?: string;
};

export default function CriteriaHolder(props: CriteriaHolderProps) {
  const [backURL, setBackURL] = useState<string | undefined>();

  return (
    <>
      <ActionBar
        title={props.title}
        backURL={backURL ?? props.defaultBackURL}
        extraControls={props.cohort ? <CohortToolbar /> : undefined}
      />
      {props.plugin.renderEdit?.(props.doneURL ?? "..", setBackURL)}
    </>
  );
}
