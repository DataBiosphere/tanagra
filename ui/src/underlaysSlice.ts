import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { CriteriaConfig } from "cohort";
import * as tanagra from "tanagra-api";

export type PrepackagedConceptSet = {
  id: string;
  name: string;
  entity: string;
  filter?: tanagra.Filter;
};

export type Underlay = {
  name: string;
  primaryEntity: string;
  entities: tanagra.Entity[];
  criteriaConfigs: CriteriaConfig[];
  prepackagedConceptSets: PrepackagedConceptSet[];
};

const initialState: Underlay[] = [];

const underlaysSlice = createSlice({
  name: "underlays",
  initialState,
  reducers: {
    setUnderlays: (state, action: PayloadAction<Underlay[]>) => {
      return action.payload;
    },
  },
});

export const { setUnderlays } = underlaysSlice.actions;

export default underlaysSlice.reducer;
