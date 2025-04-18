import ActionBar from "actionBar";
import { CriteriaPlugin } from "cohort";
import Empty from "components/empty";
import emptyImage from "images/empty.svg";
import GridLayout from "layout/gridLayout";
import { useMemo, useState } from "react";
import { useNavigate } from "util/searchState";

export type CriteriaHolderProps = {
  title: string;
  plugin: CriteriaPlugin<string>;
  exitAction?: () => void;
  backURL?: string;
};

export default function CriteriaHolder(props: CriteriaHolderProps) {
  const navigate = useNavigate();
  const [backAction, setBackAction] = useState<() => void | undefined>();

  const content = useMemo(
    () =>
      props.plugin.renderEdit ? (
        props.plugin.renderEdit(
          props.exitAction ?? (() => navigate("..")),
          setBackAction
        )
      ) : (
        <Empty
          maxWidth="60%"
          minHeight="400px"
          image={emptyImage}
          subtitle="There are no editable properties for this criteria."
        />
      ),
    [navigate, props.plugin, props.exitAction]
  );

  return (
    <GridLayout rows>
      <ActionBar
        title={props.title}
        backAction={backAction ?? props.backURL ?? props.exitAction}
      />
      {content}
    </GridLayout>
  );
}
