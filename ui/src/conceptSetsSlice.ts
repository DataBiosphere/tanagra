import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { generateId } from "cohort";
import * as tanagra from "tanagra-api";

export type ConceptSet = {
  id: string;
  underlayName: string;
  criteria: tanagra.Criteria;
};

const initialState: tanagra.ConceptSet[] = [];

const conceptSetsSlice = createSlice({
  name: "conceptSets",
  initialState,
  reducers: {
    insertConceptSet: {
      reducer: (state, action: PayloadAction<tanagra.ConceptSet>) => {
        state.push(action.payload);
      },
      prepare: (underlayName: string, criteria: tanagra.Criteria) => ({
        payload: {
          id: generateId(),
          name: criteria.name,
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
        data: object;
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
