import { createCriteria } from "cohort";
import {
  CohortContext,
  insertCohortCriteria,
  updateCohortCriteria,
} from "cohortContext";
import {
  ConceptSetContext,
  createConceptSet,
  updateConceptSet,
} from "conceptSetContext";
import { useSource } from "data/source";
import { useContext, useMemo } from "react";
import { TypedUseSelectorHook, useDispatch, useSelector } from "react-redux";
import { useParams } from "react-router-dom";
import { RootState } from "rootReducer";
import { absoluteCohortURL, useBaseParams } from "router";
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

export function useStudyId() {
  const { studyId } = useParams<{ studyId: string }>();
  if (!studyId) {
    throw new PathError("Study id not found in URL.");
  }
  return studyId;
}

function useOptionalCohort(throwOnUnknown: boolean) {
  const cohort = useContext(CohortContext)?.state?.present;
  if (throwOnUnknown && !cohort) {
    throw new PathError(`No valid cohort in current context.`);
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
  const conceptSet = useContext(ConceptSetContext)?.state?.conceptSet;
  if (throwOnUnknown && !conceptSet) {
    throw new PathError(`No valid concept set in current context.`);
  }
  return conceptSet;
}

export function useConceptSet() {
  return useOptionalConceptSet(true) as NonNullable<tanagra.ConceptSet>;
}

export function useUpdateCriteria(criteriaId?: string) {
  const cohort = useOptionalCohort(false);
  const { group, criteria } = useOptionalGroupAndCriteria(false);
  const newCriteria = useOptionalNewCriteria(false);
  const conceptSet = useOptionalConceptSet(false);
  const cohortContext = useContext(CohortContext);
  const conceptSetContext = useContext(ConceptSetContext);

  if (cohort && group) {
    if (!cohortContext) {
      throw new Error("Null cohort context when updating a cohort criteria.");
    }

    if (newCriteria) {
      return (data: object) => {
        insertCohortCriteria(cohortContext, group.id, {
          ...newCriteria,
          data: data,
        });
      };
    }

    const cId = criteriaId ?? criteria?.id;
    if (cId) {
      return (data: object) => {
        updateCohortCriteria(cohortContext, group.id, cId, data);
      };
    }
  }

  if (newCriteria) {
    if (!conceptSetContext) {
      throw new Error("Null concept set context when creating a concept set.");
    }

    return (data: object) => {
      createConceptSet(conceptSetContext, { ...newCriteria, data: data });
    };
  }

  if (conceptSet) {
    if (!conceptSetContext) {
      throw new Error("Null concept set context when updating a concept set.");
    }

    return (data: object) => {
      updateConceptSet(conceptSetContext, data);
    };
  }

  throw new Error(
    "Either concept set, or cohort, group, and criteria must be defined."
  );
}

export function useUndoRedoUrls() {
  const params = useBaseParams();
  const context = useContext(CohortContext);

  if (context?.state) {
    const cohortURL = absoluteCohortURL(params, context.state.present.id);
    return [
      context.state.past.length > 0 ? cohortURL : "",
      context.state.future.length > 0 ? cohortURL : "",
    ];
  }

  return ["", ""];
}

export function useUndoAction() {
  const context = useContext(CohortContext);

  if (context?.state && context.state.past.length > 0) {
    return () => {
      context.updateState((state) => {
        state.future.push(state.present);
        state.present = state.past[state.past.length - 1];
        state.past.pop();
      });
    };
  }

  return null;
}

export function useRedoAction() {
  const context = useContext(CohortContext);

  if (context?.state && context.state.future.length > 0) {
    return () => {
      context.updateState((state) => {
        state.past.push(state.present);
        state.present = state.future[state.future.length - 1];
        state.future.pop();
      });
    };
  }

  return null;
}
