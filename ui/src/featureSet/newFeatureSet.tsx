import { getCriteriaPlugin } from "cohort";
import CriteriaHolder from "criteriaHolder";
import { useFeatureSet, useNewCriteria } from "hooks";
import { absoluteFeatureSetURL, useBaseParams } from "router";
import { useNavigate } from "util/searchState";

export function NewFeatureSet() {
  const params = useBaseParams();
  const criteria = useNewCriteria();
  const navigate = useNavigate();
  const featureSet = useFeatureSet();

  return (
    <CriteriaHolder
      title={`Adding "${criteria.config.title}" criteria to ${featureSet.name}`}
      plugin={getCriteriaPlugin(criteria)}
      exitAction={() => navigate(absoluteFeatureSetURL(params, featureSet.id))}
      backURL=".."
    />
  );
}
