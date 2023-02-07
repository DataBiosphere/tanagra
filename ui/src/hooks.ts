import { createCriteria } from "cohort";
import { insertCriteria, updateCriteriaData } from "cohortsSlice";
import { insertConceptSet, updateConceptSetData } from "conceptSetsSlice";
import { useSource } from "data/source";
import { useCallback, useMemo } from "react";
import { TypedUseSelectorHook, useDispatch, useSelector } from "react-redux";
import { useParams } from "react-router-dom";
import { RootState } from "rootReducer";
import { AppDispatch } from "store";
import * as tanagra from "tanagra-api";

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

function useOptionalCohort(throwOnUnknown: boolean) {
  const { cohortId } = useParams<{ cohortId: string }>();
  const cohort = useAppSelector((state) =>
    state.present.cohorts.find((cohort) => cohort.id === cohortId)
  );
  if (throwOnUnknown && !cohort) {
    throw new PathError(`Unknown cohort "${cohortId}".`);
  }
  return cohort;
}

export function useCohort() {
  return useOptionalCohort(true) as NonNullable<tanagra.Cohort>;
}

export function useCohortAndGroup() {
  const cohort = useCohort();

  const { groupId } = useParams<{ groupId: string }>();
  const groupIndex = Math.max(
    0,
    cohort.groups.findIndex((g) => g.id === groupId)
  );
  return { cohort, groupIndex, group: cohort.groups[groupIndex] };
}

function useOptionalGroupAndCriteria(throwOnUnknown: boolean) {
  const cohort = useOptionalCohort(throwOnUnknown);

  const { groupId, criteriaId } = useParams<{
    groupId: string;
    criteriaId: string;
  }>();
  const group =
    cohort?.groups.find((g) => g.id === groupId) ?? cohort?.groups?.[0];
  const criteria = group?.criteria.find((c) => c.id === criteriaId);
  if (throwOnUnknown && (!group || !criteria)) {
    throw new PathError(
      `Unknown group "${groupId}" or criteria "${criteriaId}".`
    );
  }
  return { group, criteria };
}

function useOptionalNewCriteria(throwOnUnknown: boolean) {
  const source = useSource();
  const underlay = useUnderlay();
  const { configId } = useParams<{ configId: string }>();

  const criteria = useMemo(() => {
    for (const config of underlay.uiConfiguration.criteriaConfigs) {
      if (config.id !== configId) {
        continue;
      }

      return createCriteria(source, config);
    }
    return undefined;
  }, [underlay, configId]);

  if (throwOnUnknown && !criteria) {
    throw new PathError("Unknown new criteria config.");
  }
  return criteria;
}

export function useNewCriteria() {
  return useOptionalNewCriteria(true) as NonNullable<tanagra.Criteria>;
}

export function useGroupAndCriteria() {
  const { group, criteria } = useOptionalGroupAndCriteria(true);
  return {
    group: group as NonNullable<tanagra.Group>,
    criteria: criteria as NonNullable<tanagra.Criteria>,
  };
}

function useOptionalConceptSet(throwOnUnknown: boolean) {
  const { conceptSetId } = useParams<{ conceptSetId: string }>();
  const conceptSet = useAppSelector((state) =>
    state.present.conceptSets.find(
      (conceptSet) => conceptSet.id === conceptSetId
    )
  );
  if (throwOnUnknown && !conceptSet) {
    throw new PathError(`Unknown concept set "${conceptSetId}".`);
  }
  return conceptSet;
}

export function useConceptSet() {
  return useOptionalConceptSet(true) as NonNullable<tanagra.ConceptSet>;
}

export function useUpdateCriteria(criteriaId?: string) {
  const underlay = useUnderlay();
  const cohort = useOptionalCohort(false);
  const { group, criteria } = useOptionalGroupAndCriteria(false);
  const newCriteria = useOptionalNewCriteria(false);
  const conceptSet = useOptionalConceptSet(false);
  const dispatch = useAppDispatch();

  if (cohort && group) {
    if (newCriteria) {
      return useCallback(
        (data: object) => {
          dispatch(
            insertCriteria({
              cohortId: cohort.id,
              groupId: group.id,
              criteria: { ...newCriteria, data: data },
            })
          );
        },
        [cohort.id, group.id, newCriteria]
      );
    }

    const cId = criteriaId ?? criteria?.id;
    if (cId) {
      return useCallback(
        (data: object) => {
          dispatch(
            updateCriteriaData({
              cohortId: cohort.id,
              groupId: group.id,
              criteriaId: cId,
              data: data,
            })
          );
        },
        [cohort.id, group?.id, cId]
      );
    }
  }

  if (newCriteria) {
    return useCallback(
      (data: object) => {
        dispatch(
          insertConceptSet(underlay.name, { ...newCriteria, data: data })
        );
      },
      [underlay, newCriteria]
    );
  }

  if (conceptSet) {
    return useCallback(
      (data: object) => {
        dispatch(
          updateConceptSetData({
            conceptSetId: conceptSet.id,
            data: data,
          })
        );
      },
      [conceptSet?.id]
    );
  }

  throw new Error(
    "Either concept set, or cohort, group, and criteria must be defined."
  );
}

export function useUndoRedoUrls() {
  const undoUrlPath = useAppSelector((state) => state.present.url);
  const redoUrlPath = useAppSelector((state) =>
    state.future.length > 0 ? state.future[0].url : ""
  );
  return [undoUrlPath, redoUrlPath];
}
