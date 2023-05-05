import ActionBar from "actionBar";
import { CriteriaPlugin } from "cohort";
import Empty from "components/empty";
import GridLayout from "layout/gridLayout";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

export type CriteriaHolderProps = {
  title: string;
  plugin: CriteriaPlugin<object>;
  cohort?: boolean;
  exitAction?: () => void;
};

export default function CriteriaHolder(props: CriteriaHolderProps) {
  const navigate = useNavigate();
  const [backURL, setBackURL] = useState<string | undefined>();

  return (
    <GridLayout rows>
      <ActionBar title={props.title} backAction={backURL ?? props.exitAction} />
      {props.plugin.renderEdit ? (
        props.plugin.renderEdit(
          props.exitAction ?? (() => navigate("..")),
          setBackURL
        )
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
