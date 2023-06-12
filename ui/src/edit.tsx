import CriteriaHolder from "criteriaHolder";
import { useGroupSectionAndGroup } from "hooks";
import { getCriteriaPlugin, getCriteriaTitle, sectionName } from "./cohort";

export default function Edit() {
  const { group, section, sectionIndex } = useGroupSectionAndGroup();
  const plugin = getCriteriaPlugin(group.criteria[0]);

  const name = sectionName(section, sectionIndex);

  return (
    <CriteriaHolder
      title={`Editing criteria "${getCriteriaTitle(
        group.criteria[0],
        plugin
      )}" for group ${name}`}
      plugin={plugin}
      cohort
    />
  );
}
