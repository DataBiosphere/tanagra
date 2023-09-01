import { TreeGridColumn } from "components/treegrid";
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
  displayName: string;
  primaryEntity: string;
  entities: tanagra.EntityV2[];
  uiConfiguration: UIConfiguration;
};

export type UIConfiguration = {
  dataConfig: Configuration;
  criteriaConfigs: CriteriaConfig[];
  modifierConfigs: CriteriaConfig[];
  demographicChartConfigs: DemographicChartConfig;
  prepackagedConceptSets: PrepackagedConceptSet[];
  criteriaSearchConfig: CriteriaSearchConfig;
  cohortReviewConfig: CohortReviewConfig;
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

export type CriteriaSearchConfig = {
  criteriaTypeWidth: string;
  columns: TreeGridColumn[];
};

export type CohortReviewConfig = {
  primaryKey: string;
  annotationColumnWidth?: string | number;
  attributes: CohortReviewAttribute[];
  participantsListColumns: TreeGridColumn[];
  pages: CohortReviewPageConfig[];
};

export type CohortReviewAttribute = {
  title: string;
  key: string;
};

export type CohortReviewPageConfig = {
  type: string;
  id: string;
  title: string;

  // Plugin specific config.
  plugin: unknown;
};

// CriteriaConfigs are used to initialize CriteriaPlugins and provide a list of
// possible criteria.
export interface CriteriaConfig {
  // The plugin type to use for this criteria.
  type: string;
  id: string;
  title: string;
  conceptSet?: boolean;
  category?: string;
  tags?: string[];
  modifiers?: string[];

  // Plugin specific config.
  plugin: unknown;
}
