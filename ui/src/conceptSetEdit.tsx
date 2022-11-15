import { getCriteriaPlugin, getCriteriaTitle } from "cohort";
import CriteriaHolder from "criteriaHolder";
import { useConceptSet } from "hooks";

export default function Edit() {
  const conceptSet = useConceptSet();
  const plugin = getCriteriaPlugin(conceptSet.criteria);

  return (
    <CriteriaHolder
      title={getCriteriaTitle(conceptSet.criteria, plugin)}
      plugin={plugin}
    />
  );
}
