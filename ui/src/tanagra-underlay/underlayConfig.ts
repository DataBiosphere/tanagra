type SZAttribute = {
  dataType: SZDataType;
  displayFieldName?: string;
  isComputeDisplayHint?: boolean;
  name: string;
  runtimeDataType?: SZDataType;
  runtimeSqlFunctionWrapper?: string;
  valueFieldName?: string;
};

type SZBigQuery = {
  dataLocation: string;
  indexData: SZIndexData;
  queryProjectId: string;
  sourceData: SZSourceData;
};

type SZCriteriaOccurrence = {
  criteriaEntity: string;
  name: string;
  occurrenceEntities: SZOccurrenceEntity[];
  primaryCriteriaRelationship: SZPrimaryCriteriaRelationship;
};

type SZCriteriaRelationship = {
  criteriaEntityIdFieldName?: string;
  foreignKeyAttributeOccurrenceEntity?: string;
  idPairsSqlFile?: string;
  occurrenceEntityIdFieldName?: string;
};

enum SZDataType = {
  BOOLEAN = "BOOLEAN";
  DATE = "DATE";
  DOUBLE = "DOUBLE";
  INT64 = "INT64";
  STRING = "STRING";
  TIMESTAMP = "TIMESTAMP";
};

type SZDataflow = {
  dataflowLocation: string;
  gcsTempDirectory?: string;
  serviceAccountEmail: string;
  usePublicIps?: boolean;
  vpcSubnetworkName?: string;
  workerMachineType?: string;
};

type SZEntity = {
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

type SZGroupItems = {
  foreignKeyAttributeItemsEntity?: string;
  groupEntity: string;
  groupEntityIdFieldName?: string;
  idPairsSqlFile?: string;
  itemsEntity: string;
  itemsEntityIdFieldName?: string;
  name: string;
};

type SZHierarchy = {
  childIdFieldName: string;
  childParentIdPairsSqlFile: string;
  keepOrphanNodes?: boolean;
  maxDepth: int;
  name?: string;
  parentIdFieldName: string;
  rootIdFieldName?: string;
  rootNodeIds?: long[];
  rootNodeIdsSqlFile?: string;
};

type SZIndexData = {
  datasetId: string;
  projectId: string;
  tablePrefix?: string;
};

type SZIndexer = {
  bigQuery: SZBigQuery;
  dataflow: SZDataflow;
  underlay: string;
};

type SZMetadata = {
  description?: string;
  displayName: string;
  properties?: [key: string]: string;
};

type SZOccurrenceEntity = {
  attributesWithInstanceLevelHints: string[];
  criteriaRelationship: SZCriteriaRelationship;
  occurrenceEntity: string;
  primaryRelationship: SZPrimaryRelationship;
};

type SZPrimaryCriteriaRelationship = {
  criteriaEntityIdFieldName: string;
  idPairsSqlFile: string;
  primaryEntityIdFieldName: string;
};

type SZPrimaryRelationship = {
  foreignKeyAttributeOccurrenceEntity?: string;
  idPairsSqlFile?: string;
  occurrenceEntityIdFieldName?: string;
  primaryEntityIdFieldName?: string;
};

type SZService = {
  bigQuery: SZBigQuery;
  underlay: string;
};

type SZSourceData = {
  datasetId: string;
  projectId: string;
  sqlSubstitutions?: [key: string]: string;
};

type SZTextSearch = {
  attributes?: string[];
  idFieldName?: string;
  idTextPairsSqlFile?: string;
  textFieldName?: string;
};

type SZUnderlay = {
  criteriaOccurrenceEntityGroups: string[];
  entities: string[];
  groupItemsEntityGroups: string[];
  metadata: SZMetadata;
  name: string;
  primaryEntity: string;
  uiConfigFile: string;
};

