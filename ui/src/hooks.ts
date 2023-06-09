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

export function useCohortAndGroupSection() {
  const cohort = useCohort();

  const { groupSectionId } = useParams<{ groupSectionId: string }>();
  const sectionIndex = Math.max(
    0,
    cohort.groupSections.findIndex((s) => s.id === groupSectionId)
  );
  return { cohort, sectionIndex, section: cohort.groupSections[sectionIndex] };
}

function useOptionalGroupSectionAndGroup(throwOnUnknown: boolean) {
  const cohort = useOptionalCohort(throwOnUnknown);

  const { groupSectionId, groupId } = useParams<{
    groupSectionId: string;
    groupId: string;
  }>();
  const sectionIndex = Math.max(
    0,
    cohort?.groupSections?.findIndex((s) => s.id === groupSectionId) ?? -1
  );
  const section = cohort?.groupSections?.[sectionIndex];
  const group = section?.groups.find((g) => g.id === groupId);
  if (throwOnUnknown && (!section || !group)) {
    throw new PathError(
      `Unknown section "${groupSectionId}" or group "${groupId}".`
    );
  }
  return { section, sectionIndex, group };
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

export function useIsNewCriteria() {
  return !!useOptionalNewCriteria(false);
}

export function useGroupSectionAndGroup() {
  const { section, sectionIndex, group } =
    useOptionalGroupSectionAndGroup(true);
  return {
    section: section as NonNullable<tanagra.GroupSection>,
    sectionIndex,
    group: group as NonNullable<tanagra.Group>,
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

export function useUpdateCriteria(groupId?: string, criteriaId?: string) {
  const cohort = useOptionalCohort(false);
  const { section, group } = useOptionalGroupSectionAndGroup(false);
  const newCriteria = useOptionalNewCriteria(false);
  const conceptSet = useOptionalConceptSet(false);
  const cohortContext = useContext(CohortContext);
  const conceptSetContext = useContext(ConceptSetContext);

  if (cohort && section) {
    if (!cohortContext) {
      throw new Error("Null cohort context when updating a cohort criteria.");
    }

    if (newCriteria) {
      return (data: object) => {
        insertCohortCriteria(cohortContext, section.id, {
          ...newCriteria,
          data: data,
        });
      };
    }

    const gId = groupId ?? group?.id;
    if (gId) {
      return (data: object) => {
        updateCohortCriteria(cohortContext, section.id, gId, data, criteriaId);
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
