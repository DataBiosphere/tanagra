import CriteriaHolder from "criteriaHolder";
import { useNewCriteria } from "hooks";
import { exitURL, useBaseParams } from "router";
import { getCriteriaPlugin } from "./cohort";

export default function NewCriteria() {
  const criteria = useNewCriteria();
  const params = useBaseParams();

  return (
    <CriteriaHolder
      title={`New ${criteria.config.title} Concept Set`}
      plugin={getCriteriaPlugin(criteria)}
      defaultBackURL={exitURL(params)}
      doneURL={exitURL(params)}
    />
  );
}
