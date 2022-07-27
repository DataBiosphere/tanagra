import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { Configuration } from "data/configuration";
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
  uiConfiguration: UIConfiguration;
  prepackagedConceptSets: PrepackagedConceptSet[];
};

export type UIConfiguration = {
  dataConfig: Configuration;
  criteriaConfigs: CriteriaConfig[];
};

// CriteriaConfigs are used to initialize CriteriaPlugins and provide a list of
// possible criteria.
export interface CriteriaConfig {
  // The plugin type to use for this criteria.
  type: string;
  title: string;
  defaultName: string;

  // Plugin specific config.
  plugin: unknown;
}

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
