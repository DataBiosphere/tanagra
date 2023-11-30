export type SZAttribute = {
  dataType: SZDataType;
  displayFieldName?: string;
  isComputeDisplayHint?: boolean;
  name: string;
  runtimeDataType?: SZDataType;
  runtimeSqlFunctionWrapper?: string;
  valueFieldName?: string;
};

export type SZBigQuery = {
  dataLocation: string;
  indexData: SZIndexData;
  queryProjectId: string;
  sourceData: SZSourceData;
};

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
};

export type SZHierarchy = {
  childIdFieldName: string;
  childParentIdPairsSqlFile: string;
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
  criteriaRelationship: SZCriteriaRelationship;
  occurrenceEntity: string;
  primaryRelationship: SZPrimaryRelationship;
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

export type SZService = {
  bigQuery: SZBigQuery;
  underlay: string;
};

export type SZSourceData = {
  datasetId: string;
  projectId: string;
  sqlSubstitutions?: { [key: string]: string };
};

export type SZTextSearch = {
  attributes?: string[];
  idFieldName?: string;
  idTextPairsSqlFile?: string;
  textFieldName?: string;
};

export type SZUnderlay = {
  criteriaOccurrenceEntityGroups: string[];
  entities: string[];
  groupItemsEntityGroups: string[];
  metadata: SZMetadata;
  name: string;
  primaryEntity: string;
  uiConfigFile: string;
};
