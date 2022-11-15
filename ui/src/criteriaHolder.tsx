import ActionBar from "actionBar";
import { CriteriaPlugin } from "cohort";
import UndoRedo from "components/UndoRedo";
import { useState } from "react";

export type CriteriaHolderProps = {
  title: string;
  plugin: CriteriaPlugin<object>;
  showUndoRedo?: boolean;
};

export default function CriteriaHolder(props: CriteriaHolderProps) {
  const [backURL, setBackURL] = useState<string | undefined>();

  return (
    <>
      <ActionBar
        title={props.title}
        backURL={backURL}
        extraControls={props.showUndoRedo ? <UndoRedo /> : undefined}
      />
      {props.plugin.renderEdit?.(setBackURL)}
    </>
  );
}
