import ActionBar from "actionBar";
import { CriteriaPlugin } from "cohort";
import Empty from "components/empty";
import GridLayout from "layout/gridLayout";
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
    <GridLayout rows>
      <ActionBar
        title={props.title}
        backURL={backURL ?? props.defaultBackURL}
      />
      {props.plugin.renderEdit ? (
        props.plugin.renderEdit(props.doneURL ?? "..", setBackURL)
      ) : (
        <Empty
          maxWidth="60%"
          minHeight="400px"
          image="/empty.svg"
          subtitle="There are no editable properties for this criteria."
        />
      )}
    </GridLayout>
  );
}
