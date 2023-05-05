import { getCriteriaPlugin, getCriteriaTitle } from "cohort";
import CriteriaHolder from "criteriaHolder";
import { useConceptSet } from "hooks";
import { useExitAction } from "router";

export default function Edit() {
  const conceptSet = useConceptSet();
  const plugin = getCriteriaPlugin(conceptSet.criteria);
  const exit = useExitAction();

  return (
    <CriteriaHolder
      title={`Editing data feature "${getCriteriaTitle(
        conceptSet.criteria,
        plugin
      )}"`}
      plugin={plugin}
      exitAction={exit}
    />
  );
}
