import { TypedUseSelectorHook, useDispatch, useSelector } from "react-redux";
import { useParams } from "react-router-dom";
import { RootState } from "rootReducer";
import { AppDispatch } from "store";

export const useAppDispatch = () => useDispatch<AppDispatch>();
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;

export class PathError extends Error {}

export function useUnderlay() {
  const { underlayName } = useParams<{ underlayName: string }>();
  const underlay = useAppSelector((state) =>
    state.present.underlays.find((underlay) => underlay.name === underlayName)
  );
  if (!underlay) {
    throw new PathError(`Unknown underlay "${underlayName}".`);
  }
  return underlay;
}

export function useCohort() {
  const { cohortId } = useParams<{ cohortId: string }>();
  const cohort = useAppSelector((state) =>
    state.present.cohorts.find((cohort) => cohort.id === cohortId)
  );
  if (!cohort) {
    throw new PathError(`Unknown cohort "${cohortId}".`);
  }
  return cohort;
}

export function useGroupAndCriteria() {
  const cohort = useCohort();

  const { groupId, criteriaId } =
    useParams<{ groupId: string; criteriaId: string }>();
  const group = cohort.groups.find((g) => g.id === groupId);
  const criteria = group?.criteria.find((c) => c.id === criteriaId);
  if (!group || !criteria) {
    throw new PathError(
      `Unknown group "${groupId}" or criteria "${criteriaId}".`
    );
  }
  return { group, criteria };
}

export function useConceptSet() {
  const { conceptSetId } = useParams<{ conceptSetId: string }>();
  const conceptSet = useAppSelector((state) =>
    state.present.conceptSets.find(
      (conceptSet) => conceptSet.id === conceptSetId
    )
  );
  if (!conceptSet) {
    throw new PathError(`Unknown concept set "${conceptSetId}".`);
  }
  return conceptSet;
}

export function useUndoRedoUrls() {
  const undoUrlPath = useAppSelector((state) => state.present.url);
  const redoUrlPath = useAppSelector((state) =>
    state.future.length > 0 ? state.future[0].url : ""
  );
  return [undoUrlPath, redoUrlPath];
}
