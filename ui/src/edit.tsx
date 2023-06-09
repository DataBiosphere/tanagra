import CriteriaHolder from "criteriaHolder";
import { useGroupSectionAndGroup } from "hooks";
import { getCriteriaPlugin, getCriteriaTitle } from "./cohort";

export default function Edit() {
  const { group } = useGroupSectionAndGroup();
  const plugin = getCriteriaPlugin(group.criteria[0]);

  return (
    <CriteriaHolder
      title={`Editing criteria "${getCriteriaTitle(
        group.criteria[0],
        plugin
      )}"`}
      plugin={plugin}
      cohort
    />
  );
}
