import { getCriteriaTitle } from "cohort";
import { Criteria, FeatureSet } from "data/source";
import { useStudySource } from "data/studySourceContext";
import { useUnderlaySource } from "data/underlaySourceContext";
import produce from "immer";
import { createContext, useContext, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { absoluteFeatureSetURL, BaseParams } from "router";
import useSWR, { useSWRConfig } from "swr";

type FeatureSetState = {
  past: FeatureSet[];
  present: FeatureSet;
  future: FeatureSet[];

  saving: boolean;
  showSnackbar: (message: string) => void;
};

type FeatureSetContextData = {
  state: FeatureSetState | null;
  updateState: (update: (state: FeatureSetState) => void) => void;
  updatePresent: (
    update: (
      present: FeatureSet,
      showSnackbar: (message: string) => void
    ) => void
  ) => void;
};

export const FeatureSetContext = createContext<FeatureSetContextData | null>(
  null
);

export function useFeatureSetContext() {
  const context = useContext(FeatureSetContext);
  if (!context) {
    throw new Error("Attempting to use featureSet context when not provided.");
  }
  return context;
}

export function useNewFeatureSetContext(
  showSnackbar: (message: string) => void
) {
  const studySource = useStudySource();
  const underlaySource = useUnderlaySource();
  const { studyId, featureSetId } =
    useParams<{ studyId: string; featureSetId: string }>();

  if (!studyId || !featureSetId) {
    throw new Error(
      "Cannot create featureSet context without study and featureSet IDs."
    );
  }

  const [state, setState] = useState<FeatureSetState | null>(null);

  const key = {
    type: "featureSet",
    studyId,
    featureSetId,
  };
  const status = useSWR(key, async () => {
    return await studySource.getFeatureSet(
      studyId,
      underlaySource,
      featureSetId
    );
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

  const updateFeatureSet = async (newState: FeatureSetState | null) => {
    if (!newState) {
      throw new Error("Invalid null featureSet update.");
    }
    setState(newState);
    await studySource.updateFeatureSet(studyId, newState.present);

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
      updateState: async (update: (state: FeatureSetState) => void) => {
        updateFeatureSet(
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
          present: FeatureSet,
          showSnackbar: (message: string) => void
        ) => void
      ) => {
        if (!state) {
          throw new Error("Attempting to update null featureSet.");
        }

        // Produce twice, otherwise edits to the present featureSet still end up
        // affecting the pushed featureSet as well.
        const pushed = produce(state, (state) => {
          state.past.push(state.present);
          state.future = [];
        });
        const newState = produce(pushed, (state) => {
          update(state.present, state.showSnackbar);
          state.saving = true;
        });

        await updateFeatureSet(newState);

        mutate(
          (key: { type: string; studyId: string; list: boolean }) =>
            key.type === "featureSet" &&
            key.studyId === studyId &&
            key.list === true,
          undefined,
          { revalidate: true }
        );
      },
    },
  };
}

export function featureSetUndoRedo(
  params: BaseParams,
  context: FeatureSetContextData
) {
  const featureSetURL = absoluteFeatureSetURL(
    params,
    context.state?.present?.id ?? ""
  );
  return {
    undoURL: context.state?.past?.length ? featureSetURL : "",
    redoURL: context.state?.future?.length ? featureSetURL : "",
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

export function insertFeatureSetCriteria(
  context: FeatureSetContextData,
  criteria: Criteria
) {
  context.updatePresent((present, showSnackbar) => {
    present.criteria.push(criteria);
    showSnackbar(`"${getCriteriaTitle(criteria)}" added`);
  });
}

export function insertPredefinedFeatureSetCriteria(
  context: FeatureSetContextData,
  criteria: string,
  title: string
) {
  context.updatePresent((present, showSnackbar) => {
    present.predefinedCriteria.push(criteria);
    showSnackbar(`"${title}" added`);
  });
}

export function updateFeatureSetCriteria(
  context: FeatureSetContextData,
  data: string,
  criteriaId?: string
) {
  context.updatePresent((present) => {
    const index = present.criteria.findIndex((c) => c.id === criteriaId);
    if (index >= 0) {
      present.criteria[index].data = data;
    }
  });
}

export function deleteFeatureSetCriteria(
  context: FeatureSetContextData,
  criteriaId: string
) {
  context.updatePresent((present) => {
    present.criteria = present.criteria.filter((c) => c.id != criteriaId);
  });
}

export function deletePredefinedFeatureSetCriteria(
  context: FeatureSetContextData,
  criteriaId: string
) {
  context.updatePresent((present) => {
    present.predefinedCriteria = present.predefinedCriteria.filter(
      (c) => c != criteriaId
    );
  });
}

export function updateFeatureSet(context: FeatureSetContextData, name: string) {
  context.updatePresent((present) => {
    present.name = name;
  });
}

export function setExcludedFeatureSetColumns(
  context: FeatureSetContextData,
  occurrence: string,
  columns: string[]
) {
  context.updatePresent((present) => {
    const output = present.output.find((o) => o.occurrence === occurrence);
    if (output) {
      output.excludedAttributes = columns;
    } else {
      present.output.push({
        occurrence,
        excludedAttributes: columns,
      });
    }
  });
}

export function toggleFeatureSetColumn(
  context: FeatureSetContextData,
  occurrence: string,
  column: string
) {
  context.updatePresent((present) => {
    const output = present.output.find((o) => o.occurrence === occurrence);
    if (output) {
      const index = output.excludedAttributes.findIndex((c) => c === column);
      if (index >= 0) {
        output.excludedAttributes.splice(index, 1);
      } else {
        output.excludedAttributes.push(column);
      }
    } else {
      present.output.push({
        occurrence,
        excludedAttributes: [column],
      });
    }
  });
}
