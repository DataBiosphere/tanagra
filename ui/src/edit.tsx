import CriteriaHolder from "criteriaHolder";
import { useGroupAndCriteria } from "hooks";
import { getCriteriaPlugin, getCriteriaTitle } from "./cohort";

export default function Edit() {
  const { criteria } = useGroupAndCriteria();
  const plugin = getCriteriaPlugin(criteria);

  return (
    <CriteriaHolder
      title={getCriteriaTitle(criteria, plugin)}
      plugin={plugin}
      showUndoRedo
    />
  );
}
