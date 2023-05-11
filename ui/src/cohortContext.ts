import { defaultGroup, defaultSection } from "cohort";
import { useSource } from "data/source";
import produce from "immer";
import { createContext, useContext, useState } from "react";
import { useParams } from "react-router-dom";
import useSWR, { useSWRConfig } from "swr";
import * as tanagra from "tanagra-api";

type CohortState = {
  past: tanagra.Cohort[];
  present: tanagra.Cohort;
  future: tanagra.Cohort[];
};

type CohortContextData = {
  state: CohortState | null;
  updateState: (update: (state: CohortState) => void) => void;
  updatePresent: (update: (present: tanagra.Cohort) => void) => void;
};

export const CohortContext = createContext<CohortContextData | null>(null);

export function useCohortContext() {
  const context = useContext(CohortContext);
  if (!context) {
    throw new Error("Attempting to use cohort context when not provided.");
  }
  return context;
}

export function useNewCohortContext() {
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
    setState((state) => ({
      past: state?.past ?? [],
      present: cohort,
      future: state?.future ?? [],
    }));
    return cohort;
  });

  const updateCohort = async (newState: CohortState | null) => {
    if (!newState) {
      throw new Error("Invalid null cohort update.");
    }
    setState(newState);
    await source.updateCohort(studyId, newState.present);
    status.mutate();
  };

  const { mutate } = useSWRConfig();

  return {
    ...status,
    isLoading: status.isLoading || !state,
    context: {
      state: state,
      updateState: async (update: (state: CohortState) => void) => {
        updateCohort(produce(state, update));
      },
      updatePresent: async (update: (present: tanagra.Cohort) => void) => {
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
          update(state.present);
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

export function insertCohortCriteria(
  context: CohortContextData,
  sectionId: string,
  criteria: tanagra.Criteria
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
    section.groups.push(defaultGroup(criteria));
  });
}

export function insertCohortCriteriaModifier(
  context: CohortContextData,
  sectionId: string,
  groupId: string,
  criteria: tanagra.Criteria
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
  criteria?: tanagra.Criteria
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
  filter?: tanagra.GroupSectionFilter
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
