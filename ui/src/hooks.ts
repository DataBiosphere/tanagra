import { createCriteria } from "cohort";
import {
  CohortContext,
  insertCohortCriteria,
  updateCohortCriteria,
} from "cohortContext";
import { Cohort, Criteria, FeatureSet, Group, GroupSection } from "data/source";
import { useUnderlaySource } from "data/underlaySourceContext";
import {
  FeatureSetContext,
  insertFeatureSetCriteria,
  updateFeatureSetCriteria,
} from "featureSet/featureSetContext";
import { createContext, useContext } from "react";
import { useParams } from "react-router-dom";
import { useIsSecondBlock } from "router";

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

function useOptionalCohort(throwOnUnknown: boolean, backend?: boolean) {
  const state = useContext(CohortContext)?.state;
  const cohort = backend ? state?.backendPresent : state?.present;
  if (throwOnUnknown && !cohort) {
    throw new PathError(`No valid cohort in current context.`);
  }
  return cohort;
}

export function useCohort() {
  return useOptionalCohort(true) as NonNullable<Cohort>;
}

export function useBackendCohort() {
  return useOptionalCohort(true, true) as NonNullable<Cohort>;
}

export function useCohortGroupSectionAndGroup() {
  const cohort = useCohort();

  const { groupSectionId, groupId } = useParams<{
    groupSectionId: string;
    groupId: string;
  }>();
  const sectionIndex = Math.max(
    0,
    cohort.groupSections.findIndex((s) => s.id === groupSectionId)
  );
  const section = cohort.groupSections[sectionIndex];
  return {
    cohort,
    sectionIndex,
    section,
    group:
      section.groups.find((g) => g.id === groupId) &&
      section.secondBlockGroups.find((g) => g.id === groupId),
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
  const group =
    section?.groups.find((g) => g.id === groupId) ??
    section?.secondBlockGroups.find((g) => g.id === groupId);
  if (throwOnUnknown && (!section || !group)) {
    throw new PathError(
      `Unknown section "${groupSectionId}" or group "${groupId}".`
    );
  }
  return { section, sectionIndex, group };
}

export const NewCriteriaContext = createContext<Criteria | null>(null);

export function useNewCriteria() {
  const underlaySource = useUnderlaySource();
  const underlay = useUnderlay();
  const { configId } = useParams<{ configId: string }>();

  const selector = underlay.criteriaSelectors.find((s) => s.name === configId);
  if (!selector) {
    throw new Error(`Unknown selector config ${configId}.`);
  }

  return createCriteria(underlaySource, selector);
}

export function useIsNewCriteria() {
  return !!useContext(NewCriteriaContext);
}

export function useGroupSectionAndGroup() {
  const { section, sectionIndex, group } =
    useOptionalGroupSectionAndGroup(true);
  return {
    section: section as NonNullable<GroupSection>,
    sectionIndex,
    group: group as NonNullable<Group>,
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
    criteria: Criteria;
  };
}

export function useUpdateCriteria(groupId?: string, criteriaId?: string) {
  const cohort = useOptionalCohort(false);
  const { section, group } = useOptionalGroupSectionAndGroup(false);
  const { featureSet, criteria: featureSetCriteria } =
    useOptionalFeatureSetAndCriteria(false);
  const newCriteria = useContext(NewCriteriaContext);

  const cohortContext = useContext(CohortContext);
  const featureSetContext = useContext(FeatureSetContext);

  const secondBlock = useIsSecondBlock();

  if (cohort && section) {
    if (!cohortContext) {
      throw new Error("Null cohort context when updating a cohort criteria.");
    }

    if (newCriteria) {
      return (data: string) => {
        if (newCriteria) {
          insertCohortCriteria(
            cohortContext,
            section.id,
            {
              ...newCriteria,
              data: data,
            },
            secondBlock
          );
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
    "Either feature set, or cohort, group, and criteria must be defined."
  );
}
