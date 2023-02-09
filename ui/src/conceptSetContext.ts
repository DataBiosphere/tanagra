import { useSource } from "data/source";
import { useUnderlay } from "hooks";
import produce from "immer";
import { createContext, useContext, useState } from "react";
import { useParams } from "react-router-dom";
import useSWR, { useSWRConfig } from "swr";
import * as tanagra from "tanagra-api";

// SWR treats falsy values as failures, so track uncreated concept sets here.
type ConceptSetContextState = {
  conceptSet: tanagra.ConceptSet | null;
};

type ConceptSetContextData = {
  state: ConceptSetContextState;
  updateState: (update: (state: ConceptSetContextState) => void) => void;
};

export const ConceptSetContext = createContext<ConceptSetContextData | null>(
  null
);

export function useConceptSetContext() {
  const context = useContext(ConceptSetContext);
  if (!context) {
    throw new Error("Attempting to use concept set context when not provided.");
  }
  return context;
}

export function useNewConceptSetContext() {
  const underlay = useUnderlay();
  const source = useSource();
  const { studyId, conceptSetId } =
    useParams<{ studyId: string; conceptSetId: string }>();

  if (!studyId) {
    throw new Error("Cannot create concept set context without a study ID.");
  }

  const [state, setState] = useState<ConceptSetContextState>({
    conceptSet: null,
  });

  const key = {
    type: "conceptSet",
    studyId,
    conceptSetId,
  };
  const status = useSWR(key, async () => {
    const newState: ConceptSetContextState = { conceptSet: null };
    if (conceptSetId) {
      newState.conceptSet = await source.getConceptSet(studyId, conceptSetId);
    }

    setState(newState);
    return newState;
  });

  const { mutate } = useSWRConfig();

  return {
    isLoading: status.isLoading || !state,
    context: {
      state: state,
      updateState: async (update: (state: ConceptSetContextState) => void) => {
        const newState = produce(state, update);
        if (!newState?.conceptSet) {
          throw new Error("Invalid null concept set update.");
        }

        setState(newState);
        if (!state?.conceptSet) {
          await source.createConceptSet(
            underlay.name,
            studyId,
            newState.conceptSet.criteria
          );
        } else {
          await source.updateConceptSet(studyId, newState.conceptSet);
        }

        mutate(
          (key: { type: string; studyId: string; list: boolean }) =>
            key.type === "conceptSet" &&
            key.studyId === studyId &&
            key.list === true,
          undefined,
          { revalidate: true }
        );
      },
    },
  };
}

export function createConceptSet(
  context: ConceptSetContextData,
  criteria: tanagra.Criteria
) {
  context.updateState((state) => {
    state.conceptSet = { id: "", underlayName: "", criteria };
  });
}

export function updateConceptSet(context: ConceptSetContextData, data: object) {
  context.updateState((state) => {
    if (!state?.conceptSet) {
      throw new Error("Attempted to update invalid concept set.");
    }

    state.conceptSet.criteria.data = data;
  });
}
