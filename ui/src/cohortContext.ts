import { defaultGroup } from "cohort";
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
  groupId: string,
  criteria: tanagra.Criteria
) {
  context.updatePresent((present) => {
    const group = present.groups.find((group) => group.id === groupId);
    if (!group) {
      throw new Error(`Group ${groupId} does not exist in cohort ${present}.`);
    }
    group.criteria.push(criteria);
  });
}

export function updateCohortCriteria(
  context: CohortContextData,
  groupId: string,
  criteriaId: string,
  data: object
) {
  context.updatePresent((present) => {
    const group = present.groups.find((group) => group.id === groupId);
    if (!group) {
      throw new Error(`Group ${groupId} does not exist in cohort ${present}.`);
    }

    const criteria = group.criteria.find((c) => c.id === criteriaId);
    if (!criteria) {
      throw new Error(
        `Criteria ${criteriaId} does not exist on group ${group}.`
      );
    }

    criteria.data = data;
  });
}

export function deleteCohortCriteria(
  context: CohortContextData,
  groupId: string,
  criteriaId: string
) {
  context.updatePresent((present) => {
    const group = present.groups.find((group) => group.id === groupId);
    if (!group) {
      throw new Error(`Group ${groupId} does not exist in cohort ${present}.`);
    }

    const index = group.criteria.findIndex((c) => c.id === criteriaId);
    if (index === -1) {
      throw new Error(
        `Criteria ${criteriaId} does not exist on group ${group}.`
      );
    }

    group.criteria.splice(index, 1);
  });
}

export function insertCohortGroup(
  context: CohortContextData,
  criteria?: tanagra.Criteria
) {
  context.updatePresent((present) => {
    present.groups.push(defaultGroup(criteria));
  });
}

export function deleteCohortGroup(context: CohortContextData, groupId: string) {
  context.updatePresent((present) => {
    if (present.groups.length === 1) {
      present.groups = [defaultGroup()];
      return;
    }

    const index = present.groups.findIndex((group) => group.id === groupId);
    if (index === -1) {
      throw new Error(`Group ${groupId} does not exist in cohort ${present}.`);
    }

    present.groups.splice(index, 1);
  });
}

export function updateCohortGroup(
  context: CohortContextData,
  groupId: string,
  name?: string,
  filter?: tanagra.GroupFilter
) {
  context.updatePresent((present) => {
    const group = present.groups.find((group) => group.id === groupId);
    if (!group) {
      throw new Error(`Group ${groupId} does not exist in cohort ${present}.`);
    }

    group.name = name ?? group.name;
    group.filter = filter ?? group.filter;
  });
}
