import CriteriaHolder from "criteriaHolder";
import { useCohortAndGroupSection, useNewCriteria } from "hooks";
import { getCriteriaPlugin, sectionName } from "./cohort";

export default function NewCriteria() {
  const criteria = useNewCriteria();
  const { section, sectionIndex } = useCohortAndGroupSection();

  const name = sectionName(section, sectionIndex);

  return (
    <CriteriaHolder
      title={`Adding "${criteria.config.title}" criteria to ${name}`}
      plugin={getCriteriaPlugin(criteria)}
      doneURL={"../.."}
      cohort
    />
  );
}
