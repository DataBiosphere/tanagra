import CriteriaHolder from "criteriaHolder";
import { useCohortAndGroupSection, useNewCriteria } from "hooks";
import { useNavigate } from "react-router-dom";
import { getCriteriaPlugin, sectionName } from "./cohort";

export default function NewCriteria() {
  const criteria = useNewCriteria();
  const navigate = useNavigate();
  const { section, sectionIndex } = useCohortAndGroupSection();

  const name = sectionName(section, sectionIndex);

  return (
    <CriteriaHolder
      title={`Adding "${criteria.config.title}" criteria to ${name}`}
      plugin={getCriteriaPlugin(criteria)}
      exitAction={() => navigate("../..")}
      backURL=".."
    />
  );
}
