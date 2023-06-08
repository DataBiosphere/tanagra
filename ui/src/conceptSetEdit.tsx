import { getCriteriaPlugin, getCriteriaTitle } from "cohort";
import CriteriaHolder from "criteriaHolder";
import { useConceptSet } from "hooks";
import { exitURL, useBaseParams } from "router";

export default function Edit() {
  const conceptSet = useConceptSet();
  const plugin = getCriteriaPlugin(conceptSet.criteria);
  const params = useBaseParams();

  return (
    <CriteriaHolder
      title={`Editing data feature "${getCriteriaTitle(
        conceptSet.criteria,
        plugin
      )}"`}
      plugin={plugin}
      defaultBackURL={exitURL(params)}
      doneURL={exitURL(params)}
    />
  );
}
