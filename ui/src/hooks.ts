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
import { useSource } from "data/sourceContext";
import { useContext, useEffect } from "react";
import { useParams } from "react-router-dom";
import { absoluteCohortURL, useBaseParams } from "router";
import * as tanagra from "tanagra-api";

export class PathError extends Error {}

export function useUnderlay() {
  return useSource().underlay;
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

export function useCohortGroupSectionAndGroup() {
  const cohort = useCohort();

  const { groupSectionId, groupId } =
    useParams<{ groupSectionId: string; groupId: string }>();
  const sectionIndex = Math.max(
    0,
    cohort.groupSections.findIndex((s) => s.id === groupSectionId)
  );
  const section = cohort.groupSections[sectionIndex];
  return {
    cohort,
    sectionIndex,
    section,
    group: section.groups.find((g) => g.id === groupId),
  };
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

let newCriteria: tanagra.Criteria | undefined;
let newCriteriaRefCount = 0;

function useOptionalNewCriteria(throwOnUnknown: boolean) {
  const source = useSource();
  const underlay = useUnderlay();
  const { configId } = useParams<{ configId: string }>();

  if (!newCriteria) {
    for (const config of underlay.uiConfiguration.criteriaConfigs) {
      if (config.id !== configId) {
        continue;
      }

      newCriteria = createCriteria(source, config);
    }
  }

  useEffect(() => {
    newCriteriaRefCount++;

    return () => {
      newCriteriaRefCount--;
      if (newCriteriaRefCount === 0) {
        newCriteria = undefined;
      }
    };
  }, [underlay, configId]);

  if (throwOnUnknown && !newCriteria) {
    throw new PathError("Unknown new criteria config.");
  }
  return newCriteria;
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
  const conceptSet = useOptionalConceptSet(false);
  const cohortContext = useContext(CohortContext);
  const conceptSetContext = useContext(ConceptSetContext);

  if (cohort && section) {
    if (!cohortContext) {
      throw new Error("Null cohort context when updating a cohort criteria.");
    }

    if (newCriteria) {
      return (data: object) => {
        if (newCriteria) {
          insertCohortCriteria(cohortContext, section.id, {
            ...newCriteria,
            data: data,
          });
        }
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
      if (newCriteria) {
        createConceptSet(conceptSetContext, { ...newCriteria, data: data });
      }
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
