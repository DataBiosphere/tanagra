import CriteriaHolder from "criteriaHolder";
import { useNewCriteria } from "hooks";
import { getCriteriaPlugin } from "./cohort";

export default function NewCriteria() {
  const criteria = useNewCriteria();

  return (
    <CriteriaHolder
      title={`New "${criteria.config.title}" criteria`}
      plugin={getCriteriaPlugin(criteria)}
      doneURL={"../.."}
      cohort
    />
  );
}
