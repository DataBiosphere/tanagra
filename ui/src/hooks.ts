import { createCriteria } from "cohort";
import {
  CohortContext,
  insertCohortCriteria,
  updateCohortCriteria,
} from "cohortContext";
import { FeatureSet } from "data/source";
import { useUnderlaySource } from "data/underlaySourceContext";
import {
  FeatureSetContext,
  insertFeatureSetCriteria,
  updateFeatureSetCriteria,
} from "featureSet/featureSetContext";
import { useContext, useEffect } from "react";
import { useParams } from "react-router-dom";
import * as tanagraUI from "tanagra-ui";

export class PathError extends Error {}

export function useUnderlay() {
  return useUnderlaySource().underlay;
}

export function useStudyId() {
  const { studyId } = useParams<{ studyId: string }>();
  if (!studyId) {
    throw new PathError("Underlay id not found in URL.");
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
  return useOptionalCohort(true) as NonNullable<tanagraUI.UICohort>;
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

let newCriteria: tanagraUI.UICriteria | undefined;
let newCriteriaRefCount = 0;

function useOptionalNewCriteria(throwOnUnknown: boolean) {
  const underlaySource = useUnderlaySource();
  const underlay = useUnderlay();
  const { configId } = useParams<{ configId: string }>();

  if (!newCriteria) {
    for (const config of underlay.uiConfiguration.criteriaConfigs) {
      if (config.id !== configId) {
        continue;
      }

      newCriteria = createCriteria(underlaySource, config);
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
  return useOptionalNewCriteria(true) as NonNullable<tanagraUI.UICriteria>;
}

export function useIsNewCriteria() {
  return !!useOptionalNewCriteria(false);
}

export function useGroupSectionAndGroup() {
  const { section, sectionIndex, group } =
    useOptionalGroupSectionAndGroup(true);
  return {
    section: section as NonNullable<tanagraUI.UIGroupSection>,
    sectionIndex,
    group: group as NonNullable<tanagraUI.UIGroup>,
  };
}

function useOptionalFeatureSet(throwOnUnknown: boolean) {
  const featureSet = useContext(FeatureSetContext)?.state?.present;
  if (throwOnUnknown && !featureSet) {
    throw new PathError(`No valid feature set in current context.`);
  }
  return featureSet;
}

export function useFeatureSet() {
  return useOptionalFeatureSet(true) as NonNullable<FeatureSet>;
}

function useOptionalFeatureSetAndCriteria(throwOnUnknown: boolean) {
  const featureSet = useContext(FeatureSetContext)?.state?.present;
  const { criteriaId } = useParams<{ criteriaId: string }>();
  const criteria = featureSet?.criteria?.find((c) => c.id === criteriaId);
  if (throwOnUnknown && (!featureSet || !criteria)) {
    throw new PathError(`No valid feature set or criteria in current context.`);
  }
  return { featureSet, criteria };
}

export function useFeatureSetAndCriteria() {
  return useOptionalFeatureSetAndCriteria(true) as {
    featureSet: FeatureSet;
    criteria: tanagraUI.UICriteria;
  };
}

export function useUpdateCriteria(groupId?: string, criteriaId?: string) {
  const cohort = useOptionalCohort(false);
  const { section, group } = useOptionalGroupSectionAndGroup(false);
  const { featureSet, criteria: featureSetCriteria } =
    useOptionalFeatureSetAndCriteria(false);

  const cohortContext = useContext(CohortContext);
  const featureSetContext = useContext(FeatureSetContext);

  if (cohort && section) {
    if (!cohortContext) {
      throw new Error("Null cohort context when updating a cohort criteria.");
    }

    if (newCriteria) {
      return (data: string) => {
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
      return (data: string) => {
        updateCohortCriteria(cohortContext, section.id, gId, data, criteriaId);
      };
    }
  }

  if (featureSet) {
    if (!featureSetContext) {
      throw new Error(
        "Null feature set context when updating a feature set criteria."
      );
    }

    if (newCriteria) {
      return (data: string) => {
        if (newCriteria) {
          insertFeatureSetCriteria(featureSetContext, {
            ...newCriteria,
            data: data,
          });
        }
      };
    }

    if (!featureSetCriteria) {
      throw new Error("Null feature set criteria when updating it.");
    }
    return (data: string) => {
      updateFeatureSetCriteria(featureSetContext, data, featureSetCriteria.id);
    };
  }

  throw new Error(
    "Either concept set, or cohort, group, and criteria must be defined."
  );
}
