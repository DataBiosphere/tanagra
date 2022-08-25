import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { Configuration } from "data/configuration";
import { Filter } from "data/filter";
import * as tanagra from "tanagra-api";

export type PrepackagedConceptSet = {
  id: string;
  name: string;
  occurrence: string;
  filter?: Filter;
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
  demographicChartConfigs: DemographicChartConfig;
};

export type DemographicChartConfig = {
  additionalSelectedAttributes: string[];
  groupByAttributes: string[];
  chartConfigs: ChartProperties[];
};

export type ChartProperties = {
  title: string;
  primaryProperties: ChartConfigProperty[];
  stackedProperty?: ChartConfigProperty;
};

export type ChartConfigProperty = {
  key: string;
  buckets?: Bucket[];
};

export type Bucket = {
  min?: number;
  max?: number;
  displayName: string;
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
