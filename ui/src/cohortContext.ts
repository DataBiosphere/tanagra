import { defaultGroup, newSection } from "cohort";
import {
  Cohort,
  Criteria,
  GroupSectionFilter,
  GroupSectionReducingOperator,
} from "data/source";
import { useStudySource } from "data/studySourceContext";
import { useUnderlaySource } from "data/underlaySourceContext";
import deepEqual from "deep-equal";
import produce from "immer";
import { createContext, useContext, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { absoluteCohortURL, BaseParams } from "router";
import useSWR, { useSWRConfig } from "swr";
import { getCriteriaTitle } from "./cohort";

type CohortState = {
  past: Cohort[];
  present: Cohort;
  future: Cohort[];

  // The latest cohort as received from the backend. This is used for things
  // like counts that depend on the backend state to only update once the cohort
  // changes have been committed.
  backendPresent: Cohort;

  saving: boolean;
  showSnackbar: (message: string) => void;
};

type CohortContextData = {
  state: CohortState | null;
  updateState: (update: (state: CohortState) => void) => void;
  updatePresent: (
    update: (present: Cohort, showSnackbar: (message: string) => void) => void
  ) => void;
};

export const CohortContext = createContext<CohortContextData | null>(null);

export function useCohortContext() {
  const context = useContext(CohortContext);
  if (!context) {
    throw new Error("Attempting to use cohort context when not provided.");
  }
  return context;
}

export function useNewCohortContext(showSnackbar: (message: string) => void) {
  const studySource = useStudySource();
  const underlaySource = useUnderlaySource();
  const { studyId, cohortId } =
    useParams<{ studyId: string; cohortId: string }>();

  if (!studyId || !cohortId) {
    throw new Error(
      "Cannot create cohort context without study and cohort IDs."
    );
  }

  const [state, setState] = useState<CohortState | null>(null);

  const key = {
    type: "cohort",
    studyId,
    cohortId,
  };
  const status = useSWR(key, async () => {
    return await studySource.getCohort(studyId, underlaySource, cohortId);
  });

  useEffect(() => {
    setState(
      status.data
        ? {
            past: state?.past ?? [],
            present:
              state?.present && deepEqual(status.data, state.present)
                ? state.present
                : status.data,
            future: state?.future ?? [],

            backendPresent: status.data,

            saving: false,
            showSnackbar,
          }
        : null
    );
  }, [status.data]);

  const updateCohort = async (newState: CohortState | null) => {
    if (!newState) {
      throw new Error("Invalid null cohort update.");
    }
    setState(newState);
    await studySource.updateCohort(studyId, newState.present);

    setState(
      produce(newState, (state) => {
        state.saving = false;
      })
    );

    status.mutate();
  };

  const { mutate } = useSWRConfig();

  return {
    ...status,
    isLoading: status.isLoading || (!state && !status.error),
    context: {
      state: state,
      updateState: async (update: (state: CohortState) => void) => {
        updateCohort(
          produce(state, (state) => {
            if (state) {
              update(state);
              state.saving = true;
            }
          })
        );
      },
      updatePresent: async (
        update: (
          present: Cohort,
          showSnackbar: (message: string) => void
        ) => void
      ) => {
        if (!state) {
          throw new Error("Attempting to update null cohort.");
        }

        // Produce twice, otherwise edits to the present cohort still end up
        // affecting the pushed cohort as well.
        const pushed = produce(state, (state) => {
          state.past.push(state.present);
          state.future = [];
        });
        const newState = produce(pushed, (state) => {
          update(state.present, state.showSnackbar);
          state.saving = true;
        });

        await updateCohort(newState);

        mutate(
          (key: { type: string; studyId: string; list: boolean }) =>
            key.type === "cohort" &&
            key.studyId === studyId &&
            key.list === true,
          undefined,
          { revalidate: true }
        );
      },
    },
  };
}

export function cohortUndoRedo(params: BaseParams, context: CohortContextData) {
  const cohortURL = absoluteCohortURL(params, context.state?.present?.id ?? "");
  return {
    undoURL: context.state?.past?.length ? cohortURL : "",
    redoURL: context.state?.future?.length ? cohortURL : "",
    undoAction: context.state?.past?.length
      ? () => {
          context.updateState((state) => {
            state.future.push(state.present);
            state.present = state.past[state.past.length - 1];
            state.past.pop();
          });
        }
      : undefined,
    redoAction: context.state?.future?.length
      ? () => {
          context.updateState((state) => {
            state.past.push(state.present);
            state.present = state.future[state.future.length - 1];
            state.future.pop();
          });
        }
      : undefined,
  };
}

export function insertCohortCriteria(
  context: CohortContextData,
  sectionId: string,
  criteria: Criteria | Criteria[],
  secondBlock: boolean
) {
  const groups = (Array.isArray(criteria) ? criteria : [criteria]).map((c) =>
    defaultGroup(c)
  );

  context.updatePresent((present, showSnackbar) => {
    const sectionIndex = present.groupSections.findIndex(
      (section) => section.id === sectionId
    );
    if (sectionIndex < 0) {
      throw new Error(
        `Group section ${sectionId} does not exist in cohort ${JSON.stringify(
          present
        )}.`
      );
    }

    const section = present.groupSections[sectionIndex];
    (secondBlock ? section.secondBlockGroups : section.groups).push(...groups);

    let title = `${groups.length} criteria`;
    if (groups.length === 1) {
      title = `"${getCriteriaTitle(groups[0].criteria[0])}"`;
    }

    showSnackbar(`${title} added to group ${sectionIndex + 1}`);
  });

  return groups[0];
}

export function insertCohortCriteriaModifier(
  context: CohortContextData,
  sectionId: string,
  groupId: string,
  criteria: Criteria
) {
  context.updatePresent((present) => {
    const section = present.groupSections.find(
      (section) => section.id === sectionId
    );
    if (!section) {
      throw new Error(
        `Group section ${sectionId} does not exist in cohort ${JSON.stringify(
          present
        )}.`
      );
    }

    const group =
      section.groups.find((g) => g.id === groupId) ??
      section.secondBlockGroups.find((g) => g.id === groupId);
    if (!group) {
      throw new Error(
        `Group ${groupId} does not exist on group section ${JSON.stringify(
          section
        )}.`
      );
    }

    group.criteria.push(criteria);
  });
}

export function deleteCohortCriteriaModifier(
  context: CohortContextData,
  sectionId: string,
  groupId: string,
  criteriaId: string
) {
  context.updatePresent((present) => {
    const section = present.groupSections.find(
      (section) => section.id === sectionId
    );
    if (!section) {
      throw new Error(
        `Group section ${sectionId} does not exist in cohort ${JSON.stringify(
          present
        )}.`
      );
    }

    const group =
      section.groups.find((g) => g.id === groupId) ??
      section.secondBlockGroups.find((g) => g.id === groupId);
    if (!group) {
      throw new Error(
        `Group ${groupId} does not exist on group section ${JSON.stringify(
          section
        )}.`
      );
    }

    group.criteria = group.criteria.filter((c) => c.id != criteriaId);
  });
}

export function updateCohortCriteria(
  context: CohortContextData,
  sectionId: string,
  groupId: string,
  data: string,
  criteriaId?: string
) {
  context.updatePresent((present) => {
    const section = present.groupSections.find(
      (section) => section.id === sectionId
    );
    if (!section) {
      throw new Error(
        `Group section ${sectionId} does not exist in cohort ${JSON.stringify(
          present
        )}.`
      );
    }

    const group =
      section.groups.find((g) => g.id === groupId) ??
      section.secondBlockGroups.find((g) => g.id === groupId);
    if (!group) {
      throw new Error(
        `Group ${groupId} does not exist on group section ${JSON.stringify(
          section
        )}.`
      );
    }

    const index = group.criteria.findIndex((c) => c.id === criteriaId);
    group.criteria[Math.max(0, index ?? 0)].data = data;
  });
}

export function deleteCohortGroup(
  context: CohortContextData,
  sectionId: string,
  groupId: string
) {
  context.updatePresent((present) => {
    const section = present.groupSections.find(
      (section) => section.id === sectionId
    );
    if (!section) {
      throw new Error(
        `Group section ${sectionId} does not exist in cohort ${JSON.stringify(
          present
        )}.`
      );
    }

    let groups = section.groups;
    let index = groups.findIndex((g) => g.id === groupId);
    if (index === -1) {
      groups = section.secondBlockGroups;
      index = groups.findIndex((g) => g.id === groupId);
    }

    if (index === -1) {
      throw new Error(
        `Group ${groupId} does not exist on group section ${JSON.stringify(
          section
        )}.`
      );
    }

    groups.splice(index, 1);
  });
}

export function insertCohortGroupSection(
  context: CohortContextData,
  criteria?: Criteria
) {
  context.updatePresent((present) => {
    present.groupSections.push(newSection(criteria));
  });
}

export function deleteCohortGroupSection(
  context: CohortContextData,
  sectionId: string
) {
  context.updatePresent((present) => {
    if (present.groupSections.length === 1) {
      present.groupSections = [newSection()];
      return;
    }

    const index = present.groupSections.findIndex(
      (section) => section.id === sectionId
    );
    if (index === -1) {
      throw new Error(
        `Group section ${sectionId} does not exist in cohort ${JSON.stringify(
          present
        )}.`
      );
    }

    present.groupSections.splice(index, 1);
  });
}

export type UpdateCohortGroupSectionParams = {
  name?: string;
  filter?: GroupSectionFilter;
  operatorValue?: number;
  firstBlockReducingOperator?: GroupSectionReducingOperator;
  secondBlockReducingOperator?: GroupSectionReducingOperator;
};

export function updateCohortGroupSection(
  context: CohortContextData,
  sectionId: string,
  params: UpdateCohortGroupSectionParams
) {
  context.updatePresent((present) => {
    const section = present.groupSections.find(
      (section) => section.id === sectionId
    );
    if (!section) {
      throw new Error(
        `Group section ${sectionId} does not exist in cohort ${JSON.stringify(
          present
        )}.`
      );
    }

    section.name = params.name ?? section.name;
    section.filter = params.filter ?? section.filter;
    section.operatorValue = params.operatorValue ?? section.operatorValue;
    section.firstBlockReducingOperator =
      params.firstBlockReducingOperator ?? section.firstBlockReducingOperator;
    section.secondBlockReducingOperator =
      params.secondBlockReducingOperator ?? section.secondBlockReducingOperator;
  });
}

export function updateCohort(context: CohortContextData, name: string) {
  context.updatePresent((present) => {
    present.name = name;
  });
}
