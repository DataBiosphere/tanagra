import { getCriteriaPlugin, sectionName } from "cohort";
import CriteriaHolder from "criteriaHolder";
import { useCohortGroupSectionAndGroup, useNewCriteria } from "hooks";
import { absoluteCohortURL, useBaseParams } from "router";
import { useNavigate } from "util/searchState";

export default function NewCriteria() {
  const params = useBaseParams();
  const criteria = useNewCriteria();
  const navigate = useNavigate();
  const { cohort, section, sectionIndex } = useCohortGroupSectionAndGroup();

  const name = sectionName(section, sectionIndex);

  return (
    <CriteriaHolder
      title={`Adding "${criteria.config.displayName}" criteria to ${name}`}
      plugin={getCriteriaPlugin(criteria)}
      exitAction={() =>
        navigate(absoluteCohortURL(params, cohort.id, section.id, criteria.id))
      }
      backURL=".."
    />
  );
}
