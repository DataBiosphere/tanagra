import CriteriaHolder from "criteriaHolder";
import { useNewCriteria } from "hooks";
import { useExitAction } from "router";
import { getCriteriaPlugin } from "./cohort";

export default function NewCriteria() {
  const criteria = useNewCriteria();
  const exit = useExitAction();

  return (
    <CriteriaHolder
      title={`Adding "${criteria.config.title}" data feature`}
      plugin={getCriteriaPlugin(criteria)}
      exitAction={exit}
    />
  );
}
