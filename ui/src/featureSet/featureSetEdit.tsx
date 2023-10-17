import { getCriteriaPlugin, getCriteriaTitle } from "cohort";
import CriteriaHolder from "criteriaHolder";
import { useFeatureSetAndCriteria } from "hooks";

export function FeatureSetEdit() {
  const { criteria } = useFeatureSetAndCriteria();
  const plugin = getCriteriaPlugin(criteria);

  return (
    <CriteriaHolder
      title={`Editing criteria "${getCriteriaTitle(criteria, plugin)}"`}
      plugin={plugin}
    />
  );
}
