import { TreeGridColumn } from "components/treegrid";
import { Filter } from "data/filter";
import * as underlayConfig from "tanagra-underlay/underlayConfig";

export type PrepackagedConceptSet = {
  id: string;
  name: string;
  category?: string;
  tags?: string[];
  entity: string;
  filter?: Filter;
};

export type Underlay = {
  name: string;
  uiConfiguration: UIConfiguration;
  underlayConfig: underlayConfig.SZUnderlay;
  criteriaOccurrences: underlayConfig.SZCriteriaOccurrence[];
  groupItems: underlayConfig.SZGroupItems[];
  entities: underlayConfig.SZEntity[];
  criteriaSelectors: underlayConfig.SZCriteriaSelector[];
  prepackagedDataFeatures: underlayConfig.SZPrepackagedCriteria[];
  visualizations: underlayConfig.SZVisualization[];
};

export type UIConfiguration = {
  featureConfig?: FeatureConfig;
  demographicChartConfigs: DemographicChartConfig;
  criteriaSearchConfig: CriteriaSearchConfig;
  cohortReviewConfig: CohortReviewConfig;
};

export type FeatureConfig = {
  disableExportButton?: boolean;
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
  participantIdAttribute: string;
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
