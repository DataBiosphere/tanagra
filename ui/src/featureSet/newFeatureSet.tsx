import { getCriteriaPlugin } from "cohort";
import CriteriaHolder from "criteriaHolder";
import { useFeatureSet, NewCriteriaContext, useNewCriteria } from "hooks";
import { absoluteFeatureSetURL, useBaseParams } from "router";
import { useNavigate } from "util/searchState";

export function NewFeatureSet() {
  const params = useBaseParams();
  const criteria = useNewCriteria();
  const navigate = useNavigate();
  const featureSet = useFeatureSet();

  return (
    <NewCriteriaContext.Provider value={criteria}>
      <CriteriaHolder
        title={`Adding "${criteria.config.displayName}" criteria to ${featureSet.name}`}
        plugin={getCriteriaPlugin(criteria)}
        exitAction={() =>
          navigate(absoluteFeatureSetURL(params, featureSet.id))
        }
        backURL=".."
      />
    </NewCriteriaContext.Provider>
  );
}
