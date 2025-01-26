export type SZAttribute = {
  dataType: SZDataType;
  displayFieldName?: string;
  displayHintRangeMax?: number;
  displayHintRangeMin?: number;
  isComputeDisplayHint?: boolean;
  isDataTypeRepeated?: boolean;
  isSuppressedForExport?: boolean;
  name: string;
  runtimeDataType?: SZDataType;
  runtimeSqlFunctionWrapper?: string;
  sourceQuery?: SZSourceQuery;
  valueFieldName?: string;
};

export type SZAttributeSearch = {
  attributes: string[];
  includeEntityMainColumns?: boolean;
  includeNullValues?: boolean;
};

export type SZBigQuery = {
  dataLocation: string;
  exportBucketNames?: string[];
  exportDatasetIds?: string[];
  indexData: SZIndexData;
  queryProjectId: string;
  sourceData: SZSourceData;
};

export enum SZCorePlugin {
  ATTRIBUTE = "ATTRIBUTE",
  ENTITY_GROUP = "ENTITY_GROUP",
  FILTERABLE_GROUP = "FILTERABLE_GROUP",
  MULTI_ATTRIBUTE = "MULTI_ATTRIBUTE",
  OUTPUT_UNFILTERED = "OUTPUT_UNFILTERED",
  SURVEY = "SURVEY",
  TEXT_SEARCH = "TEXT_SEARCH",
  UNHINTED_VALUE = "UNHINTED_VALUE",
}

export type SZCriteriaOccurrence = {
  criteriaEntity: string;
  name: string;
  occurrenceEntities: SZOccurrenceEntity[];
  primaryCriteriaRelationship: SZPrimaryCriteriaRelationship;
};

export type SZCriteriaRelationship = {
  criteriaEntityIdFieldName?: string;
  foreignKeyAttributeOccurrenceEntity?: string;
  idPairsSqlFile?: string;
  occurrenceEntityIdFieldName?: string;
};

export type SZCriteriaSelector = {
  display: SZCriteriaSelectorDisplay;
  displayName: string;
  filterBuilder: string;
  isEnabledForCohorts: boolean;
  isEnabledForDataFeatureSets: boolean;
  modifiers: SZCriteriaSelectorModifier[];
  name: string;
  plugin: string;
  pluginConfig: string;
  pluginConfigFile: string;
  supportsTemporalQueries: boolean;
};

export type SZCriteriaSelectorDisplay = {
  category: string;
  tags: string[];
};

export type SZCriteriaSelectorModifier = {
  displayName: string;
  name: string;
  plugin: string;
  pluginConfig: string;
  pluginConfigFile: string;
  supportsTemporalQueries: boolean;
};

export enum SZDataType {
  BOOLEAN = "BOOLEAN",
  DATE = "DATE",
  DOUBLE = "DOUBLE",
  INT64 = "INT64",
  STRING = "STRING",
  TIMESTAMP = "TIMESTAMP",
}

export type SZDataflow = {
  dataflowLocation: string;
  gcsTempDirectory?: string;
  serviceAccountEmail: string;
  usePublicIps?: boolean;
  vpcSubnetworkName?: string;
  workerMachineType?: string;
};

export type SZEntity = {
  allInstancesSqlFile: string;
  attributes: SZAttribute[];
  description?: string;
  displayName?: string;
  hierarchies?: SZHierarchy[];
  idAttribute: string;
  name: string;
  optimizeGroupByAttributes?: string[];
  optimizeSearchByAttributes?: SZAttributeSearch[];
  sourceQueryTableName?: string;
  temporalQuery?: SZTemporalQuery;
  textSearch?: SZTextSearch;
};

export type SZGroupItems = {
  foreignKeyAttributeItemsEntity?: string;
  groupEntity: string;
  groupEntityIdFieldName?: string;
  idPairsSqlFile?: string;
  itemsEntity: string;
  itemsEntityIdFieldName?: string;
  name: string;
  rollupCountsSql?: SZRollupCountsSql;
  useSourceIdPairsSql?: boolean;
};

export type SZHierarchy = {
  childIdFieldName: string;
  childParentIdPairsSqlFile: string;
  cleanHierarchyNodesWithZeroCounts?: boolean;
  keepOrphanNodes?: boolean;
  maxDepth: number;
  name?: string;
  parentIdFieldName: string;
  rootIdFieldName?: string;
  rootNodeIds?: number[];
  rootNodeIdsSqlFile?: string;
};

export type SZIndexData = {
  datasetId: string;
  projectId: string;
  tablePrefix?: string;
};

export type SZIndexer = {
  bigQuery: SZBigQuery;
  dataflow: SZDataflow;
  underlay: string;
};

export type SZMetadata = {
  description?: string;
  displayName: string;
  properties?: { [key: string]: string };
};

export type SZOccurrenceEntity = {
  attributesWithInstanceLevelHints: string[];
  attributesWithRollupInstanceLevelHints: string[];
  criteriaRelationship: SZCriteriaRelationship;
  occurrenceEntity: string;
  primaryRelationship: SZPrimaryRelationship;
};

export type SZPrepackagedCriteria = {
  criteriaSelector: string;
  displayName: string;
  name: string;
  pluginData: string;
  pluginDataFile: string;
};

export type SZPrimaryCriteriaRelationship = {
  criteriaEntityIdFieldName: string;
  idPairsSqlFile: string;
  primaryEntityIdFieldName: string;
};

export type SZPrimaryRelationship = {
  foreignKeyAttributeOccurrenceEntity?: string;
  idPairsSqlFile?: string;
  occurrenceEntityIdFieldName?: string;
  primaryEntityIdFieldName?: string;
};

export type SZRollupCountsSql = {
  entityIdFieldName: string;
  rollupCountFieldName: string;
  sqlFile: string;
};

export type SZService = {
  bigQuery: SZBigQuery;
  underlay: string;
};

export type SZSourceData = {
  datasetId: string;
  projectId: string;
  sqlSubstitutions?: { [key: string]: string };
};

export type SZSourceQuery = {
  displayFieldName?: string;
  displayFieldTable?: string;
  displayFieldTableJoinFieldName?: string;
  valueFieldName?: string;
};

export type SZTemporalQuery = {
  visitDateAttribute: string;
  visitIdAttribute: string;
};

export type SZTextSearch = {
  attributes?: string[];
  idFieldName?: string;
  idTextPairsSqlFile?: string;
  textFieldName?: string;
};

export type SZUnderlay = {
  criteriaOccurrenceEntityGroups: string[];
  criteriaSelectors: string[];
  entities: string[];
  groupItemsEntityGroups: string[];
  metadata: SZMetadata;
  name: string;
  prepackagedDataFeatures: string[];
  primaryEntity: string;
  uiConfigFile: string;
  visualizations: string[];
};

export type SZVisualizationConfig = {
  dataConfig: string;
  dataConfigFile: string;
  name: string;
  plugin: string;
  pluginConfig: string;
  pluginConfigFile: string;
  title: string;
};

