import { defaultGroup, defaultSection } from "cohort";
import { useSource } from "data/sourceContext";
import { useUnderlay } from "hooks";
import produce from "immer";
import { createContext, useContext, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { absoluteCohortURL, BaseParams } from "router";
import useSWR, { useSWRConfig } from "swr";
import * as tanagraUI from "tanagra-ui";
import {
  getCriteriaPlugin,
  getCriteriaTitle,
  sectionName,
  upgradeCriteria,
} from "./cohort";

type CohortState = {
  past: tanagraUI.UICohort[];
  present: tanagraUI.UICohort;
  future: tanagraUI.UICohort[];

  saving: boolean;
  showSnackbar: (message: string) => void;
};

type CohortContextData = {
  state: CohortState | null;
  updateState: (update: (state: CohortState) => void) => void;
  updatePresent: (
    update: (
      present: tanagraUI.UICohort,
      showSnackbar: (message: string) => void
    ) => void
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
  const underlay = useUnderlay();
  const source = useSource();
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
    const cohort = await source.getCohort(studyId, cohortId);
    for (const gs of cohort.groupSections) {
      for (const g of gs.groups) {
        for (const c of g.criteria) {
          upgradeCriteria(c, underlay.uiConfiguration.criteriaConfigs);
        }
      }
    }
    return cohort;
  });

  useEffect(
    () =>
      setState(
        status.data
          ? {
              past: state?.past ?? [],
              present: status.data,
              future: state?.future ?? [],

              saving: false,
              showSnackbar,
            }
          : null
      ),
    [status.data]
  );

  const updateCohort = async (newState: CohortState | null) => {
    if (!newState) {
      throw new Error("Invalid null cohort update.");
    }
    setState(newState);
    await source.updateCohort(studyId, newState.present);

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
    isLoading: status.isLoading || !state,
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
          present: tanagraUI.UICohort,
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
  criteria: tanagraUI.UICriteria
) {
  const group = defaultGroup(criteria);

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
    section.groups.push(group);

    const plugin = getCriteriaPlugin(criteria);
    const title = getCriteriaTitle(criteria, plugin);
    const name = sectionName(section, sectionIndex);

    showSnackbar(`"${title}" added to group ${name}`);
  });

  return group;
}

export function insertCohortCriteriaModifier(
  context: CohortContextData,
  sectionId: string,
  groupId: string,
  criteria: tanagraUI.UICriteria
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

    const group = section.groups.find((g) => g.id === groupId);
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

    const group = section.groups.find((g) => g.id === groupId);
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
  data: object,
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

    const group = section.groups.find((g) => g.id === groupId);
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

    const index = section.groups.findIndex((g) => g.id === groupId);
    if (index === -1) {
      throw new Error(
        `Group ${groupId} does not exist on group section ${JSON.stringify(
          section
        )}.`
      );
    }

    section.groups.splice(index, 1);
  });
}

export function insertCohortGroupSection(
  context: CohortContextData,
  criteria?: tanagraUI.UICriteria
) {
  context.updatePresent((present) => {
    present.groupSections.push(defaultSection(criteria));
  });
}

export function deleteCohortGroupSection(
  context: CohortContextData,
  sectionId: string
) {
  context.updatePresent((present) => {
    if (present.groupSections.length === 1) {
      present.groupSections = [defaultSection()];
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

export function updateCohortGroupSection(
  context: CohortContextData,
  sectionId: string,
  name?: string,
  filter?: tanagraUI.UIGroupSectionFilter
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

    section.name = name ?? section.name;
    section.filter = filter ?? section.filter;
  });
}

export function updateCohort(context: CohortContextData, name: string) {
  context.updatePresent((present) => {
    present.name = name;
  });
}
