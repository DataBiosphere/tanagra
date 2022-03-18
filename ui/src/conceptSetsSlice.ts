import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { Criteria, generateId } from "cohort";

export type ConceptSet = {
  id: string;
  underlayName: string;
  criteria: Criteria;
};

const initialState: ConceptSet[] = [];

const conceptSetsSlice = createSlice({
  name: "conceptSets",
  initialState,
  reducers: {
    insertConceptSet: {
      reducer: (state, action: PayloadAction<ConceptSet>) => {
        state.push(action.payload);
      },
      prepare: (underlayName: string, criteria: Criteria) => ({
        payload: {
          id: generateId(),
          underlayName,
          criteria,
        },
      }),
    },

    deleteConceptSet: (
      state,
      action: PayloadAction<{
        conceptSetId: string;
      }>
    ) => {
      state.splice(
        state.findIndex((cs) => cs.id === action.payload.conceptSetId),
        1
      );
    },

    updateConceptSetData: (
      state,
      action: PayloadAction<{
        conceptSetId: string;
        data: unknown;
      }>
    ) => {
      const conceptSet = state.find(
        (cs) => cs.id === action.payload.conceptSetId
      );
      if (conceptSet) {
        conceptSet.criteria.data = action.payload.data;
      }
    },
  },
});

export const { insertConceptSet, deleteConceptSet, updateConceptSetData } =
  conceptSetsSlice.actions;

export default conceptSetsSlice.reducer;
