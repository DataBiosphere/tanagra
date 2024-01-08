import { defaultSection, generateCohortFilter } from "cohort";
import { getReasonPhrase } from "http-status-codes";
import * as tanagra from "tanagra-api";
import * as tanagraUI from "tanagra-ui";
import * as tanagraUnderlay from "tanagra-underlay/underlayConfig";
import { Underlay } from "underlaysSlice";
import { isValid } from "util/valid";
import {
  ITEM_COUNT_ATTRIBUTE,
  ROLLUP_COUNT_ATTRIBUTE,
  SortDirection,
  SortOrder,
  VALUE_SUFFIX,
} from "./configuration";
import {
  ArrayFilter,
  Filter,
  isArrayFilter,
  isAttributeFilter,
  isEntityGroupFilter,
  isRelationshipFilter,
  isTextFilter,
  isUnaryFilter,
} from "./filter";
import { CohortReview, DataEntry, DataKey, DataValue } from "./types";

export type EntityNode = {
  data: DataEntry;
  entity: string;
  ancestors?: DataKey[];
  childCount?: number;
};

export type SearchEntityGroupOptions = {
  query?: string;
  parent?: DataKey;
  limit?: number;
  hierarchy?: boolean;
};

export type SearchEntityGroupResult = {
  nodes: EntityNode[];
};

export type SearchGroupingOptions = {
  limit?: number;
};

export type ListDataResponse = {
  data: DataEntry[];
  sql: string;
};

export type IntegerHint = {
  min: number;
  max: number;
};

export type EnumHintOption = {
  value: DataValue;
  name: string;
  count: number;
};

export type HintData = {
  attribute: string;
  integerHint?: IntegerHint;
  enumHintOptions?: EnumHintOption[];
};

export type FilterCountValue = {
  count: number;
  [x: string]: DataValue;
};

export type PropertyMap = {
  [key: string]: string;
};

export type ListStudiesFilter = {
  createdBy?: string;
};

export type Study = {
  id: string;
  displayName: string;
  created: Date;
  properties: PropertyMap;
};

export type AnnotationValue = {
  value: DataValue;
  instanceId: DataKey;
  current: boolean;
};

export type AnnotationEntry = {
  [x: DataKey]: AnnotationValue[];
};

export type ReviewInstance = {
  data: DataEntry;
  annotations: AnnotationEntry;
};

export enum AnnotationType {
  String = "STRING",
}

export type Annotation = {
  id: string;
  displayName: string;
  annotationType: AnnotationType;
  enumVals?: string[];
};

export type ExportModel = {
  id: string;
  displayName: string;
  description: string;

  inputs: { [key: string]: string };
  outputs: { [key: string]: string };
};

export type ExportRequestEntity = {
  requestedAttributes: string[];
  entityId: string;
  cohort: Filter;
  conceptSet: Filter | null;
};

export type ExportResult = {
  redirectURL?: string | null;
  outputs: { [key: string]: string };
};

export type User = {
  email: string;
};

export type FeatureSetOutput = {
  occurrence: string;
  excludedAttributes: string[];
};

export type FeatureSet = {
  id: string;
  name: string;
  underlayName: string;
  lastModified: Date;

  criteria: tanagraUI.UICriteria[];
  predefinedCriteria: string[];
  output: FeatureSetOutput[];
};

export type EntityGroupData = {
  id: string;
  entityId: string;
  relatedEntityId?: string;
  relatedEntityGroupId?: string;
  occurrenceEntityIds: string[];
  selectionEntity: tanagraUnderlay.SZEntity;
};

export interface UnderlaySource {
  underlay: Underlay;

  lookupEntityGroup(entityGroupId: string): EntityGroupData;
  lookupEntity(entityId: string): tanagraUnderlay.SZEntity;
  primaryEntity(): tanagraUnderlay.SZEntity;

  searchEntityGroup(
    requestedAttributes: string[],
    entityGroupId: string,
    sortOrder: SortOrder,
    options?: SearchEntityGroupOptions
  ): Promise<SearchEntityGroupResult>;

  listAttributes(occurrenceID: string): string[];

  listData(
    requestedAttributes: string[],
    occurrenceID: string,
    cohort: Filter,
    conceptSet: Filter | null,
    limit?: number
  ): Promise<ListDataResponse>;

  getHintData(
    occurrenceID: string,
    attributeID: string,
    relatedEntity?: string,
    relatedID?: DataKey
  ): Promise<HintData | undefined>;

  getAllHintData(
    occurrenceID: string,
    relatedEntity?: string,
    relatedID?: DataKey
  ): Promise<HintData[]>;

  filterCount(
    filter: Filter | null,
    groupByAttributes?: string[]
  ): Promise<FilterCountValue[]>;

  listExportModels(underlayName: string): Promise<ExportModel[]>;

  export(
    underlayName: string,
    studyId: string,
    modelId: string,
    returnURL: string,
    cohortIds: string[],
    entities: ExportRequestEntity[]
  ): Promise<ExportResult>;
}

export interface StudySource {
  listCohortReviews(studyId: string, cohortId: string): Promise<CohortReview[]>;

  createCohortReview(
    studyId: string,
    underlaySource: UnderlaySource,
    cohort: tanagraUI.UICohort,
    displayName: string,
    size: number
  ): Promise<CohortReview>;

  deleteCohortReview(studyId: string, cohortId: string, reviewId: string): void;

  renameCohortReview(
    studyId: string,
    cohortId: string,
    reviewId: string,
    displayName: string
  ): Promise<CohortReview>;

  getCohortReview(
    studyId: string,
    cohortId: string,
    reviewId: string
  ): Promise<CohortReview>;

  listReviewInstances(
    studyId: string,
    underlaySource: UnderlaySource,
    cohortId: string,
    reviewId: string,
    includeAttributes: string[]
  ): Promise<ReviewInstance[]>;

  getStudy(studyId: string): Promise<Study>;

  listStudies(filter?: ListStudiesFilter): Promise<Study[]>;

  createStudy(displayName: string): Promise<Study>;

  deleteStudy(studyId: string): void;

  // TODO(tjennison): Use internal types for cohorts and related objects instead
  // of V1 types from the service definition.
  getCohort(
    studyId: string,
    cohortId: string,
    cohortRevisionId?: string
  ): Promise<tanagraUI.UICohort>;

  listCohorts(studyId: string): Promise<tanagraUI.UICohort[]>;

  createCohort(
    underlayName: string,
    studyId: string,
    displayName?: string
  ): Promise<tanagraUI.UICohort>;

  updateCohort(studyId: string, cohort: tanagraUI.UICohort): void;

  deleteCohort(studyId: string, cohortId: string): void;

  getFeatureSet(studyId: string, featureSetId: string): Promise<FeatureSet>;

  listFeatureSets(studyId: string): Promise<FeatureSet[]>;

  createFeatureSet(
    underlayName: string,
    studyId: string,
    name: string
  ): Promise<FeatureSet>;

  updateFeatureSet(studyId: string, featureSet: FeatureSet): void;

  deleteFeatureSet(studyId: string, featureSetId: string): void;

  listAnnotations(studyId: string, cohortId: string): Promise<Annotation[]>;

  createAnnotation(
    studyId: string,
    cohortId: string,
    displayName: string,
    annotationType: AnnotationType,
    enumVals?: string[]
  ): void;

  updateAnnotation(
    studyId: string,
    cohortId: string,
    annotationId: string,
    displayName: string
  ): void;

  deleteAnnotation(
    studyId: string,
    cohortId: string,
    annotationId: string
  ): void;

  createUpdateAnnotationValue(
    studyId: string,
    cohortId: string,
    reviewId: string,
    annotationId: string,
    entityKey: DataKey,
    value: DataValue
  ): Promise<void>;

  deleteAnnotationValue(
    studyId: string,
    cohortId: string,
    reviewId: string,
    annotationId: string,
    entityKey: DataKey
  ): Promise<void>;

  getUser(): Promise<User>;
}

export class BackendUnderlaySource implements UnderlaySource {
  constructor(
    private underlaysApi: tanagra.UnderlaysApi,
    private exportApi: tanagra.ExportApi,
    public underlay: Underlay
  ) {}

  lookupEntityGroup(entityGroupId: string): EntityGroupData {
    const criteriaOccurrence = this.underlay.criteriaOccurrences.find(
      (co) => co.name === entityGroupId
    );
    if (criteriaOccurrence) {
      return {
        id: entityGroupId,
        entityId: criteriaOccurrence.criteriaEntity,
        occurrenceEntityIds: criteriaOccurrence.occurrenceEntities.map(
          (e) => e.occurrenceEntity
        ),
        selectionEntity: this.lookupEntity(criteriaOccurrence.criteriaEntity),
      };
    }

    const groupItems = this.underlay.groupItems.find(
      (gi) => gi.name === entityGroupId
    );
    if (groupItems) {
      const primaryEntityRelated =
        groupItems.itemsEntity === this.underlay.underlayConfig.primaryEntity;

      let relatedEntityId: string | undefined;
      let relatedEntityGroupId: string | undefined;

      if (!primaryEntityRelated) {
        relatedEntityId = !primaryEntityRelated
          ? groupItems.itemsEntity
          : undefined;

        const criteriaOccurrence = this.underlay.criteriaOccurrences.find(
          (co) => co.criteriaEntity === relatedEntityId
        );
        if (!criteriaOccurrence) {
          throw new Error(
            `Unable to find transitive entity group for ${entityGroupId}.`
          );
        }
        relatedEntityGroupId = criteriaOccurrence.name;
      }

      return {
        id: entityGroupId,
        entityId: groupItems.groupEntity,
        relatedEntityId,
        relatedEntityGroupId,
        occurrenceEntityIds: primaryEntityRelated ? [""] : [],
        selectionEntity: this.lookupEntity(
          relatedEntityId ?? groupItems.groupEntity
        ),
      };
    }

    throw new Error(`Unknown entity group ${entityGroupId}.`);
  }

  lookupEntity(entityId: string) {
    const findId =
      entityId.length > 0
        ? entityId
        : this.underlay.underlayConfig.primaryEntity;
    const entity = this.underlay.entities.find((e) => e.name === findId);
    if (!entity) {
      throw new Error(`Unknown entity ${entityId}.`);
    }
    return entity;
  }

  primaryEntity(): tanagraUnderlay.SZEntity {
    return this.lookupEntity("");
  }

  searchEntityGroup(
    requestedAttributes: string[],
    entityGroupId: string,
    sortOrder: SortOrder,
    options?: SearchEntityGroupOptions
  ): Promise<SearchEntityGroupResult> {
    let entityGroup = this.lookupEntityGroup(entityGroupId);
    if (entityGroup.relatedEntityGroupId && options?.hierarchy) {
      entityGroup = this.lookupEntityGroup(entityGroup.relatedEntityGroupId);
    }

    const entity =
      entityGroup.relatedEntityId && options?.parent
        ? this.lookupEntity(entityGroup.relatedEntityId)
        : this.lookupEntity(entityGroup.entityId);

    return parseAPIError(
      this.underlaysApi
        .listInstances(
          this.searchRequest(
            requestedAttributes,
            entityGroup,
            entity,
            sortOrder,
            options?.query,
            options?.parent,
            options?.limit
          )
        )
        .then((res) => ({
          nodes: processEntitiesResponse(entity, res),
        }))
    );
  }

  listAttributes(entityId: string): string[] {
    const findId =
      entityId.length > 0
        ? entityId
        : this.underlay.underlayConfig.primaryEntity;

    return (
      this.underlay.entities
        .find((e) => e.name === findId)
        ?.attributes?.map((a) => a.name)
        .filter(isValid)
        .filter((n) => !isInternalAttribute(n)) || []
    );
  }

  async listData(
    requestedAttributes: string[],
    entityId: string,
    cohort: Filter,
    conceptSet: Filter | null,
    limit?: number
  ): Promise<ListDataResponse> {
    const entity = this.lookupEntity(entityId);
    const ra = normalizeRequestedAttributes(
      requestedAttributes,
      entity.idAttribute
    );

    const res = await parseAPIError(
      this.underlaysApi.listInstances({
        entityName: entity.name,
        underlayName: this.underlay.name,
        query: this.makeQuery(ra, entityId, cohort, conceptSet, limit),
      })
    );

    const data = res.instances?.map((instance) =>
      makeDataEntry(entity.idAttribute, instance.attributes)
    );
    return {
      data: data ?? [],
      sql: res.sql ?? "",
    };
  }

  async getHintData(
    entityId: string,
    attributeId: string,
    relatedEntity?: string,
    relatedId?: DataKey
  ): Promise<HintData | undefined> {
    const res = await parseAPIError(
      this.underlaysApi.queryHints({
        entityName:
          entityId === ""
            ? this.underlay.underlayConfig.primaryEntity
            : entityId,
        underlayName: this.underlay.name,
        hintQuery:
          !!relatedEntity && !!relatedId
            ? {
                relatedEntity: {
                  name: relatedEntity,
                  id: literalFromDataValue(relatedId),
                },
              }
            : {},
      })
    );

    const hint = res.displayHints?.find(
      (hint) => hint?.attribute?.name === attributeId
    );

    return fromAPIDisplayHint(hint);
  }

  async getAllHintData(
    entityId: string,
    relatedEntity?: string,
    relatedId?: DataKey
  ): Promise<HintData[]> {
    const res = await parseAPIError(
      this.underlaysApi.queryHints({
        entityName:
          entityId === ""
            ? this.underlay.underlayConfig.primaryEntity
            : entityId,
        underlayName: this.underlay.name,
        hintQuery:
          !!relatedEntity && !!relatedId
            ? {
                relatedEntity: {
                  name: relatedEntity,
                  id: literalFromDataValue(relatedId),
                },
              }
            : {},
      })
    );

    return res.displayHints?.map((hint) => fromAPIDisplayHint(hint)) ?? [];
  }

  async filterCount(
    filter: Filter | null,
    groupByAttributes?: string[]
  ): Promise<FilterCountValue[]> {
    const data = await parseAPIError(
      this.underlaysApi.countInstances({
        underlayName: this.underlay.name,
        entityName: this.underlay.underlayConfig.primaryEntity,
        countQuery: {
          attributes: groupByAttributes,
          filter: generateFilter(this, filter) ?? undefined,
        },
      })
    );

    if (!data.instanceCounts) {
      throw new Error("Count API returned no counts.");
    }

    return data.instanceCounts.map((count) => {
      const value: FilterCountValue = {
        count: count.count ?? 0,
      };
      processAttributes(value, count.attributes);
      return value;
    });
  }

  public async listExportModels(underlayName: string): Promise<ExportModel[]> {
    return await parseAPIError(
      this.exportApi
        .listExportModels({
          underlayName,
        })
        .then((res) =>
          res.map((model) => ({
            id: model.name ?? "",
            displayName: model.displayName ?? "",
            description: model.description ?? "",
            inputs: model.inputs ?? {},
            outputs: model.outputs ?? {},
          }))
        )
    );
  }

  public async export(
    underlayName: string,
    studyId: string,
    modelId: string,
    returnURL: string,
    cohortIds: string[],
    entities: ExportRequestEntity[]
  ): Promise<ExportResult> {
    return await parseAPIError(
      this.exportApi
        .exportInstancesAndAnnotations({
          underlayName,
          exportRequest: {
            study: studyId,
            exportModel: modelId,
            redirectBackUrl: returnURL,
            includeAnnotations: true,
            cohorts: cohortIds,
            instanceQuerys: entities.map((e) => ({
              entity: this.lookupEntity(e.entityId).name,
              query: this.makeQuery(
                e.requestedAttributes,
                e.entityId,
                e.cohort,
                e.conceptSet,
                0
              ),
            })),
          },
        })
        .then((res) => ({
          redirectURL: res.redirectAwayUrl,
          outputs: res.outputs ?? {},
        }))
    );
  }

  private makeQuery(
    requestedAttributes: string[],
    entityId: string,
    cohort: Filter,
    conceptSet: Filter | null,
    limit?: number
  ): tanagra.Query {
    let cohortFilter = generateFilter(this, cohort);
    if (!cohortFilter) {
      throw new Error("Cohort filter is empty.");
    }

    if (entityId) {
      const primaryEntity = this.underlay.underlayConfig.primaryEntity;
      cohortFilter = {
        filterType: tanagra.FilterFilterTypeEnum.Relationship,
        filterUnion: {
          relationshipFilter: {
            entity: primaryEntity,
            subfilter: cohortFilter,
          },
        },
      };
    }

    let filter = cohortFilter;
    const conceptSetFilter = generateFilter(this, conceptSet);
    if (conceptSetFilter) {
      const combined = makeBooleanLogicFilter(
        tanagra.BooleanLogicFilterOperatorEnum.And,
        [cohortFilter, conceptSetFilter]
      );
      if (combined) {
        filter = combined;
      }
    }

    return {
      includeAttributes: requestedAttributes,
      filter,
      limit: limit === 0 ? undefined : limit ?? 50,
    };
  }

  private searchRequest(
    requestedAttributes: string[],
    entityGroup: EntityGroupData,
    entity: tanagraUnderlay.SZEntity,
    sortOrder: SortOrder,
    query?: string,
    parent?: DataValue,
    limit?: number
  ): tanagra.ListInstancesRequest {
    const hierarchy = entity.hierarchies?.[0]?.name;
    const operands: tanagra.Filter[] = [];

    if (entityGroup.relatedEntityId && parent) {
      const groupingEntity = this.lookupEntity(entityGroup.entityId);
      operands.push({
        filterType: tanagra.FilterFilterTypeEnum.Relationship,
        filterUnion: {
          relationshipFilter: {
            entity: entityGroup.entityId,
            subfilter: {
              filterType: tanagra.FilterFilterTypeEnum.Attribute,
              filterUnion: {
                attributeFilter: {
                  attribute: groupingEntity.idAttribute,
                  operator: tanagra.BinaryOperator.Equals,
                  value: literalFromDataValue(parent),
                },
              },
            },
          },
        },
      });
    } else {
      if (hierarchy && parent) {
        operands.push({
          filterType: tanagra.FilterFilterTypeEnum.Hierarchy,
          filterUnion: {
            hierarchyFilter: {
              hierarchy,
              operator: tanagra.HierarchyFilterOperatorEnum.ChildOf,
              value: literalFromDataValue(parent),
            },
          },
        });
      } else if (isValid(query)) {
        if (query !== "") {
          operands.push({
            filterType: tanagra.FilterFilterTypeEnum.Text,
            filterUnion: {
              textFilter: {
                matchType: tanagra.TextFilterMatchTypeEnum.ExactMatch,
                text: query,
              },
            },
          });
        }
      } else if (hierarchy) {
        operands.push({
          filterType: tanagra.FilterFilterTypeEnum.Hierarchy,
          filterUnion: {
            hierarchyFilter: {
              hierarchy,
              operator: tanagra.HierarchyFilterOperatorEnum.IsRoot,
              value: literalFromDataValue(true),
            },
          },
        });
      }

      if (hierarchy) {
        operands.push({
          filterType: tanagra.FilterFilterTypeEnum.Hierarchy,
          filterUnion: {
            hierarchyFilter: {
              hierarchy: hierarchy,
              operator: tanagra.HierarchyFilterOperatorEnum.IsMember,
              value: literalFromDataValue(true),
            },
          },
        });
      }
    }

    const req = {
      entityName: entity.name,
      underlayName: this.underlay.name,
      query: {
        includeAttributes: normalizeRequestedAttributes(
          requestedAttributes,
          entity.idAttribute
        ),
        includeHierarchyFields: hierarchy
          ? {
              hierarchies: [hierarchy],
              fields: [
                tanagra.QueryIncludeHierarchyFieldsFieldsEnum.Path,
                tanagra.QueryIncludeHierarchyFieldsFieldsEnum.NumChildren,
              ],
            }
          : undefined,
        includeRelationshipFields:
          !entityGroup.relatedEntityId ||
          entity.name === entityGroup.relatedEntityId
            ? [
                {
                  relatedEntity: this.underlay.underlayConfig.primaryEntity,
                  hierarchies: hierarchy ? [hierarchy] : undefined,
                },
              ]
            : undefined,
        filter:
          makeBooleanLogicFilter(
            tanagra.BooleanLogicFilterOperatorEnum.And,
            operands
          ) ?? undefined,
        orderBys: [makeOrderBy(this.underlay, entity, sortOrder)],
        limit,
        pageSize: limit,
      },
    };
    return req;
  }
}

export class BackendStudySource implements StudySource {
  constructor(
    private studiesApi: tanagra.StudiesApi,
    private cohortsApi: tanagra.CohortsApi,
    private conceptSetsApi: tanagra.ConceptSetsApi,
    private reviewsApi: tanagra.ReviewsApi,
    private annotationsApi: tanagra.AnnotationsApi,
    private usersApi: tanagra.UsersApi
  ) {}

  public async listCohortReviews(
    studyId: string,
    cohortId: string
  ): Promise<CohortReview[]> {
    return parseAPIError(
      this.reviewsApi
        .listReviews({ studyId, cohortId })
        .then((res) => res.map((r) => fromAPICohortReview(r)))
    );
  }

  public createCohortReview(
    studyId: string,
    underlaySource: UnderlaySource,
    cohort: tanagraUI.UICohort,
    displayName: string,
    size: number
  ): Promise<CohortReview> {
    return parseAPIError(
      this.reviewsApi
        .createReview({
          studyId,
          cohortId: cohort.id,
          reviewCreateInfo: {
            displayName,
            size,
            filter:
              generateFilter(
                underlaySource,
                generateCohortFilter(underlaySource, cohort)
              ) ?? {},
          },
        })
        .then((r) => fromAPICohortReview(r))
    );
  }

  public async deleteCohortReview(
    studyId: string,
    cohortId: string,
    reviewId: string
  ) {
    await parseAPIError(
      this.reviewsApi.deleteReview({ studyId, cohortId, reviewId })
    );
  }

  public async renameCohortReview(
    studyId: string,
    cohortId: string,
    reviewId: string,
    displayName: string
  ): Promise<CohortReview> {
    return parseAPIError(
      this.reviewsApi
        .updateReview({
          studyId,
          cohortId,
          reviewId,
          reviewUpdateInfo: {
            displayName,
          },
        })
        .then((r) => fromAPICohortReview(r))
    );
  }

  public async getCohortReview(
    studyId: string,
    cohortId: string,
    reviewId: string
  ): Promise<CohortReview> {
    return parseAPIError(
      this.reviewsApi
        .getReview({ studyId, cohortId, reviewId })
        .then((r) => fromAPICohortReview(r))
    );
  }

  public async listReviewInstances(
    studyId: string,
    underlaySource: UnderlaySource,
    cohortId: string,
    reviewId: string,
    includeAttributes: string[]
  ): Promise<ReviewInstance[]> {
    const primaryEntity = underlaySource.primaryEntity();
    return parseAPIError(
      this.reviewsApi
        .listReviewInstancesAndAnnotations({
          studyId,
          cohortId,
          reviewId,
          reviewQuery: {
            includeAttributes,
          },
        })
        .then((res) => {
          return res.instances == null
            ? []
            : res.instances.map((i) =>
                fromAPIReviewInstance(reviewId, primaryEntity.idAttribute, i)
              );
        })
    );
  }

  public getStudy(studyId: string): Promise<Study> {
    return parseAPIError(
      this.studiesApi.getStudy({ studyId }).then((study) => processStudy(study))
    );
  }

  public listStudies(filter?: ListStudiesFilter): Promise<Study[]> {
    return parseAPIError(
      this.studiesApi
        .listStudies({
          createdBy: filter?.createdBy,
          limit: 250,
        })
        .then((studies) => studies.map((study) => processStudy(study)))
    );
  }

  public createStudy(displayName: string): Promise<Study> {
    return parseAPIError(
      this.studiesApi
        .createStudy({
          studyCreateInfo: {
            displayName,
          },
        })
        .then((res) => processStudy(res))
    );
  }

  public async deleteStudy(studyId: string) {
    parseAPIError(
      this.studiesApi.deleteStudy({
        studyId,
      })
    );
  }

  public async getCohort(
    studyId: string,
    cohortId: string,
    cohortRevisionId?: string
  ): Promise<tanagraUI.UICohort> {
    return parseAPIError(
      this.cohortsApi
        .getCohort({ studyId, cohortId, cohortRevisionId })
        .then((c) => fromAPICohort(c))
    );
  }

  public listCohorts(studyId: string): Promise<tanagraUI.UICohort[]> {
    return parseAPIError(
      this.cohortsApi
        .listCohorts({ studyId })
        .then((res) => res.map((c) => fromAPICohort(c)))
    );
  }

  public createCohort(
    underlayName: string,
    studyId: string,
    displayName?: string
  ): Promise<tanagraUI.UICohort> {
    return parseAPIError(
      this.cohortsApi
        .createCohort({
          studyId,
          cohortCreateInfo: {
            underlayName,
            displayName:
              displayName ?? `Untitled cohort ${new Date().toLocaleString()}`,
          },
        })
        .then((c) => fromAPICohort(c))
    );
  }

  public async updateCohort(studyId: string, cohort: tanagraUI.UICohort) {
    await parseAPIError(
      this.cohortsApi.updateCohort({
        studyId,
        cohortId: cohort.id,
        cohortUpdateInfo: {
          displayName: cohort.name,
          criteriaGroupSections: toAPICriteriaGroupSections(
            cohort.groupSections
          ),
        },
      })
    );
  }

  public async deleteCohort(studyId: string, cohortId: string) {
    await parseAPIError(
      this.cohortsApi.deleteCohort({
        studyId,
        cohortId,
      })
    );
  }

  public async getFeatureSet(
    studyId: string,
    featureSetId: string
  ): Promise<FeatureSet> {
    return parseAPIError(
      this.conceptSetsApi
        .getConceptSet({ studyId, conceptSetId: featureSetId })
        .then((cs) => fromAPIFeatureSet(cs))
    );
  }

  public async listFeatureSets(studyId: string): Promise<FeatureSet[]> {
    return parseAPIError(
      this.conceptSetsApi
        .listConceptSets({ studyId })
        .then((res) => res.map((cs) => fromAPIFeatureSet(cs)))
    );
  }

  public async createFeatureSet(
    underlayName: string,
    studyId: string,
    displayName: string
  ): Promise<FeatureSet> {
    return parseAPIError(
      this.conceptSetsApi
        .createConceptSet({
          studyId,
          conceptSetCreateInfo: {
            underlayName,
            displayName,
          },
        })
        .then((cs) => fromAPIFeatureSet(cs))
    );
  }

  public async updateFeatureSet(studyId: string, featureSet: FeatureSet) {
    await parseAPIError(
      this.conceptSetsApi.updateConceptSet({
        studyId,
        conceptSetId: featureSet.id,
        conceptSetUpdateInfo: {
          displayName: featureSet.name,
          criteria: [
            ...featureSet.criteria.map((c) => toAPICriteria(c)),
            ...featureSet.predefinedCriteria.map((c) => ({
              id: c,
              displayName: "",
              pluginName: "",
              predefinedId: c,
              selectionData: "",
              uiConfig: "",
              tags: {},
            })),
          ],
          entityOutputs: featureSet.output.map((o) => ({
            entity: o.occurrence,
            excludeAttributes: o.excludedAttributes,
          })),
        },
      })
    );
  }

  public async deleteFeatureSet(studyId: string, featureSetId: string) {
    await parseAPIError(
      this.conceptSetsApi.deleteConceptSet({
        studyId,
        conceptSetId: featureSetId,
      })
    );
  }

  public listAnnotations(
    studyId: string,
    cohortId: string
  ): Promise<Annotation[]> {
    return parseAPIError(
      this.annotationsApi
        .listAnnotationKeys({ studyId, cohortId })
        .then((res) => res.map((a) => fromAPIAnnotation(a)))
    );
  }

  public async createAnnotation(
    studyId: string,
    cohortId: string,
    displayName: string,
    annotationType: AnnotationType,
    enumVals?: string[]
  ) {
    await parseAPIError(
      this.annotationsApi.createAnnotationKey({
        studyId,
        cohortId,
        annotationCreateInfo: {
          displayName,
          dataType: toAPIAnnotationType(annotationType),
          enumVals,
        },
      })
    );
  }

  public async updateAnnotation(
    studyId: string,
    cohortId: string,
    annotationId: string,
    displayName: string
  ) {
    await parseAPIError(
      this.annotationsApi.updateAnnotationKey({
        studyId,
        cohortId,
        annotationId,
        annotationUpdateInfo: {
          displayName,
        },
      })
    );
  }

  public async deleteAnnotation(
    studyId: string,
    cohortId: string,
    annotationId: string
  ) {
    await parseAPIError(
      this.annotationsApi.deleteAnnotationKey({
        studyId,
        cohortId,
        annotationId,
      })
    );
  }

  public async createUpdateAnnotationValue(
    studyId: string,
    cohortId: string,
    reviewId: string,
    annotationId: string,
    entityKey: DataKey,
    value: DataValue
  ): Promise<void> {
    parseAPIError(
      this.annotationsApi.updateAnnotationValue({
        studyId,
        cohortId,
        reviewId,
        annotationId,
        instanceId: String(entityKey),
        literal: literalFromDataValue(value),
      })
    );
  }

  public async deleteAnnotationValue(
    studyId: string,
    cohortId: string,
    reviewId: string,
    annotationId: string,
    entityKey: DataKey
  ): Promise<void> {
    return await parseAPIError(
      this.annotationsApi.deleteAnnotationValues({
        studyId,
        cohortId,
        reviewId,
        annotationId,
        instanceId: String(entityKey),
      })
    );
  }

  public async getUser(): Promise<User> {
    return parseAPIError(
      this.usersApi.getMe({}).then((res) => ({
        email: res.email,
      }))
    );
  }
}

function isInternalAttribute(attribute: string): boolean {
  return attribute.startsWith("t_");
}

function literalFromDataValue(value: DataValue): tanagra.Literal {
  let dataType = tanagra.DataType.Int64;
  if (typeof value === "string") {
    dataType = tanagra.DataType.String;
  } else if (typeof value === "boolean") {
    dataType = tanagra.DataType.Boolean;
  } else if (value instanceof Date) {
    dataType = tanagra.DataType.Date;
  }

  return {
    dataType,
    valueUnion: {
      int64Val: typeof value === "number" ? value : undefined,
      stringVal: typeof value === "string" ? value : undefined,
      boolVal: typeof value === "boolean" ? value : undefined,
      dateVal: value instanceof Date ? value.toISOString() : undefined,
    },
  };
}

function dataValueFromLiteral(value?: tanagra.Literal | null): DataValue {
  if (!value) {
    return null;
  }
  switch (value.dataType) {
    case tanagra.DataType.Int64:
      return value.valueUnion?.int64Val ?? null;
    case tanagra.DataType.String:
      return value.valueUnion?.stringVal ?? null;
    case tanagra.DataType.Date:
      return value.valueUnion?.dateVal
        ? new Date(value.valueUnion.dateVal)
        : null;
    case tanagra.DataType.Boolean:
      return value.valueUnion?.boolVal ?? null;
    case tanagra.DataType.Double:
      return value.valueUnion?.doubleVal ?? null;
    case tanagra.DataType.Timestamp:
      return value.valueUnion?.timestampVal
        ? new Date(value.valueUnion.timestampVal)
        : null;
  }

  throw new Error(`Unknown data type "${value?.dataType}".`);
}

function convertSortDirection(dir: SortDirection) {
  return dir === SortDirection.Desc
    ? tanagra.OrderByDirection.Descending
    : tanagra.OrderByDirection.Ascending;
}

function processEntitiesResponse(
  entity: tanagraUnderlay.SZEntity,
  response: tanagra.InstanceListResult
): EntityNode[] {
  const nodes: EntityNode[] = [];
  if (response.instances) {
    response.instances.forEach((instance) => {
      const data = makeDataEntry(entity.idAttribute, instance.attributes);

      let ancestors: DataKey[] | undefined;
      const path = instance.hierarchyFields?.[0]?.path;
      if (isValid(path)) {
        if (path === "") {
          ancestors = [];
        } else {
          ancestors = path
            .split(".")
            .map((id) => (typeof data.key === "number" ? +id : id));
        }
      }

      instance.relationshipFields?.forEach((fields) => {
        if (isValid(fields.count)) {
          if (
            entity.hierarchies?.length &&
            fields.hierarchy === entity.hierarchies?.[0]?.name
          ) {
            data[ROLLUP_COUNT_ATTRIBUTE] = fields.count;
          } else if (!fields.hierarchy) {
            data[ITEM_COUNT_ATTRIBUTE] = fields.count;
          }
        }
      });

      nodes.push({
        data: data,
        entity: entity.name,
        ancestors: ancestors,
        childCount: instance.hierarchyFields?.[0]?.numChildren,
      });
    });
  }
  return nodes;
}

function makeDataEntry(
  primaryKey: string,
  attributes?: { [key: string]: tanagra.ValueDisplay }
): DataEntry {
  const data: DataEntry = {
    key: 0,
  };

  processAttributes(data, attributes);

  const key = dataValueFromLiteral(attributes?.[primaryKey]?.value ?? null);
  if (typeof key !== "string" && typeof key !== "number") {
    throw new Error(
      `Key attribute "${primaryKey}" not found in entity instance ${data}`
    );
  }
  data.key = key;
  return data;
}

function generateFilter(
  underlaySource: UnderlaySource,
  filter: Filter | null
): tanagra.Filter | null {
  if (!filter) {
    return null;
  }

  if (isArrayFilter(filter)) {
    const operands = filter.operands
      .map((o) => generateFilter(underlaySource, o))
      .filter(isValid);
    if (operands.length === 0) {
      return null;
    }

    return makeBooleanLogicFilter(arrayFilterOperator(filter), operands);
  }
  if (isUnaryFilter(filter)) {
    const operand = generateFilter(underlaySource, filter.operand);
    if (!operand) {
      return null;
    }

    return {
      filterType: tanagra.FilterFilterTypeEnum.BooleanLogic,
      filterUnion: {
        booleanLogicFilter: {
          operator: tanagra.BooleanLogicFilterOperatorEnum.Not,
          subfilters: [operand],
        },
      },
    };
  }

  if (isRelationshipFilter(filter)) {
    const subfilter = generateFilter(underlaySource, filter.subfilter);

    return {
      filterType: tanagra.FilterFilterTypeEnum.Relationship,
      filterUnion: {
        relationshipFilter: {
          entity: filter.entityId,
          subfilter: subfilter,
          groupByCountAttribute: filter.groupByCount?.attribute,
          groupByCountOperator: filter.groupByCount
            ? toAPIBinaryOperator(filter.groupByCount.operator)
            : undefined,
          groupByCountValue: filter.groupByCount?.value,
        },
      },
    };
  }

  if (isEntityGroupFilter(filter)) {
    const entity = underlaySource.lookupEntity(filter.entityId);

    const classificationFilter = (key: DataKey): tanagra.Filter => {
      if (entity.hierarchies?.length) {
        return {
          filterType: tanagra.FilterFilterTypeEnum.Hierarchy,
          filterUnion: {
            hierarchyFilter: {
              hierarchy: entity.hierarchies[0].name,
              operator:
                tanagra.HierarchyFilterOperatorEnum.DescendantOfInclusive,
              value: literalFromDataValue(key),
            },
          },
        };
      }

      return {
        filterType: tanagra.FilterFilterTypeEnum.Attribute,
        filterUnion: {
          attributeFilter: {
            attribute: entity.idAttribute,
            operator: tanagra.BinaryOperator.Equals,
            value: literalFromDataValue(key),
          },
        },
      };
    };

    const operands = filter.keys.map(classificationFilter);

    let subfilter: tanagra.Filter | undefined;
    if (operands.length > 0) {
      subfilter =
        makeBooleanLogicFilter(
          tanagra.BooleanLogicFilterOperatorEnum.Or,
          operands
        ) ?? undefined;
    }

    return {
      filterType: tanagra.FilterFilterTypeEnum.Relationship,
      filterUnion: {
        relationshipFilter: {
          entity: entity.name,
          subfilter,
        },
      },
    };
  }
  if (isAttributeFilter(filter)) {
    if (filter.nonNull) {
      return null;
    }
    if (filter.ranges?.length) {
      return makeBooleanLogicFilter(
        tanagra.BooleanLogicFilterOperatorEnum.Or,
        filter.ranges.map(({ min, max }) =>
          makeBooleanLogicFilter(tanagra.BooleanLogicFilterOperatorEnum.And, [
            {
              filterType: tanagra.FilterFilterTypeEnum.Attribute,
              filterUnion: {
                attributeFilter: {
                  attribute: filter.attribute,
                  operator: tanagra.BinaryOperator.LessThanOrEqual,
                  value: literalFromDataValue(max),
                },
              },
            },
            {
              filterType: tanagra.FilterFilterTypeEnum.Attribute,
              filterUnion: {
                attributeFilter: {
                  attribute: filter.attribute,
                  operator: tanagra.BinaryOperator.GreaterThanOrEqual,
                  value: literalFromDataValue(min),
                },
              },
            },
          ])
        )
      );
    }
    if (filter.values?.length) {
      return makeBooleanLogicFilter(
        tanagra.BooleanLogicFilterOperatorEnum.Or,
        filter.values.map((value) => ({
          filterType: tanagra.FilterFilterTypeEnum.Attribute,
          filterUnion: {
            attributeFilter: {
              attribute: filter.attribute,
              operator: tanagra.BinaryOperator.Equals,
              value: literalFromDataValue(value),
            },
          },
        }))
      );
    }

    return null;
  }

  if (isTextFilter(filter)) {
    if (filter.text.length === 0) {
      return null;
    }

    return {
      filterType: tanagra.FilterFilterTypeEnum.Text,
      filterUnion: {
        textFilter: {
          matchType: tanagra.TextFilterMatchTypeEnum.ExactMatch,
          text: filter.text,
          attribute: filter.attribute,
        },
      },
    };
  }

  throw new Error(`Unknown filter type: ${JSON.stringify(filter)}`);
}

function arrayFilterOperator(
  filter: ArrayFilter
): tanagra.BooleanLogicFilterOperatorEnum {
  if (!isValid(filter.operator.min) && !isValid(filter.operator.max)) {
    return tanagra.BooleanLogicFilterOperatorEnum.And;
  }
  if (filter.operator.min === 1 && !isValid(filter.operator.max)) {
    return tanagra.BooleanLogicFilterOperatorEnum.Or;
  }

  throw new Error("Only AND and OR equivalent operators are supported.");
}

function processAttributes(
  obj: { [x: string]: DataValue },
  attributes?: { [x: string]: tanagra.ValueDisplay }
) {
  for (const k in attributes) {
    const v = attributes[k];
    const value = dataValueFromLiteral(v.value);
    obj[k] = v.display ?? value;
    obj[k + VALUE_SUFFIX] = value;
  }
}

function makeBooleanLogicFilter(
  operator: tanagra.BooleanLogicFilterOperatorEnum,
  operands: (tanagra.Filter | null)[]
) {
  const subfilters = operands.filter(isValid);
  if (!subfilters || subfilters.length === 0) {
    return null;
  }
  if (subfilters.length === 1) {
    return subfilters[0];
  }
  return {
    filterType: tanagra.FilterFilterTypeEnum.BooleanLogic,
    filterUnion: {
      booleanLogicFilter: {
        operator,
        subfilters,
      },
    },
  };
}

function makeOrderBy(
  underlay: Underlay,
  entity: tanagraUnderlay.SZEntity,
  sortData: SortOrder
) {
  const orderBy: tanagra.QueryOrderBys = {
    direction: convertSortDirection(sortData?.direction),
  };

  if (sortData.attribute === ROLLUP_COUNT_ATTRIBUTE) {
    orderBy.relationshipField = {
      relatedEntity: underlay.underlayConfig.primaryEntity,
      hierarchy: entity.hierarchies?.[0]?.name,
    };
  } else if (sortData.attribute === ITEM_COUNT_ATTRIBUTE) {
    orderBy.relationshipField = {
      relatedEntity: underlay.underlayConfig.primaryEntity,
    };
  } else {
    orderBy.attribute = sortData.attribute;
  }

  return orderBy;
}

function normalizeRequestedAttributes(
  attributes: string[],
  requiredAttribute?: string
) {
  const a = attributes
    .filter((a) => a !== ROLLUP_COUNT_ATTRIBUTE && a !== ITEM_COUNT_ATTRIBUTE)
    .map((a) => {
      const i = a.indexOf(VALUE_SUFFIX);
      if (i != -1) {
        return a.substring(0, i);
      }
      return a;
    });

  if (requiredAttribute) {
    a.push(requiredAttribute);
  }

  return [...new Set(a)];
}

function fromAPICohort(cohort: tanagra.Cohort): tanagraUI.UICohort {
  return {
    id: cohort.id,
    name: cohort.displayName,
    underlayName: cohort.underlayName,
    lastModified: cohort.lastModified,
    groupSections: fromAPICriteriaGroupSections(cohort.criteriaGroupSections),
  };
}

function fromAPICriteriaGroupSections(
  sections?: tanagra.CriteriaGroupSection[]
): tanagraUI.UIGroupSection[] {
  if (!sections?.length) {
    return [defaultSection()];
  }

  return sections.map((section) => ({
    id: section.id,
    name: section.displayName,
    filter: {
      kind:
        section.operator === tanagra.CriteriaGroupSectionOperatorEnum.And
          ? tanagraUI.UIGroupSectionFilterKindEnum.All
          : tanagraUI.UIGroupSectionFilterKindEnum.Any,
      excluded: section.excluded,
    },
    groups: section.criteriaGroups.map((group) => ({
      id: group.id,
      entity: group.entity,
      criteria: group.criteria.map((criteria) => fromAPICriteria(criteria)),
    })),
  }));
}

function fromAPICriteria(criteria: tanagra.Criteria): tanagraUI.UICriteria {
  return {
    id: criteria.id,
    type: criteria.pluginName,
    data: JSON.parse(criteria.selectionData),
    config: JSON.parse(criteria.uiConfig),
  };
}

function fromAPIFeatureSet(conceptSet: tanagra.ConceptSet): FeatureSet {
  return {
    id: conceptSet.id,
    underlayName: conceptSet.underlayName,
    name: conceptSet.displayName,
    lastModified: conceptSet.lastModified,

    criteria: conceptSet.criteria
      .filter((c) => !c.predefinedId)
      .map((c) => fromAPICriteria(c)),
    predefinedCriteria: conceptSet.criteria
      .map((c) => c.predefinedId)
      .filter(isValid),
    output: conceptSet.entityOutputs.map((eo) => ({
      occurrence: eo.entity,
      excludedAttributes: eo.excludeAttributes ?? [],
    })),
  };
}

function toAPICriteriaGroupSections(
  groupSections: tanagraUI.UIGroupSection[]
): tanagra.CriteriaGroupSection[] {
  return groupSections.map((section) => ({
    id: section.id,
    displayName: section.name ?? "",
    operator:
      section.filter.kind === tanagraUI.UIGroupSectionFilterKindEnum.All
        ? tanagra.CriteriaGroupSectionOperatorEnum.And
        : tanagra.CriteriaGroupSectionOperatorEnum.Or,
    excluded: section.filter.excluded,
    criteriaGroups: section.groups.map((group) => ({
      id: group.id,
      displayName: "",
      entity: group.entity,
      criteria: group.criteria.map((criteria) => toAPICriteria(criteria)),
    })),
  }));
}

function toAPICriteria(criteria: tanagraUI.UICriteria): tanagra.Criteria {
  return {
    id: criteria.id,
    displayName: "",
    tags: {},
    pluginName: criteria.type,
    selectionData: JSON.stringify(criteria.data),
    uiConfig: JSON.stringify(criteria.config),
  };
}

function fromAPICohortReview(review: tanagra.Review): CohortReview {
  if (!review.cohort) {
    throw new Error(`Undefined cohort for review "${review.displayName}".`);
  }

  return {
    ...review,
    cohort: fromAPICohort(review.cohort),
  };
}

function fromAPIReviewInstance(
  reviewId: string,
  primaryKey: string,
  instance: tanagra.ReviewInstance
): ReviewInstance {
  const data = makeDataEntry(primaryKey, instance.attributes);

  const annotations: AnnotationEntry = {};
  for (const key in instance.annotations) {
    annotations[key] = instance.annotations[key].map((v) =>
      fromAPIAnnotationValue(v)
    );
  }

  return {
    data,
    annotations,
  };
}

function fromAPIAnnotationValue(
  value: tanagra.AnnotationValue
): AnnotationValue {
  return {
    value: dataValueFromLiteral(value.value),
    instanceId: value.instanceId,
    current: value.isPartOfSelectedReview,
  };
}

function fromAPIAnnotationType(dataType: string): AnnotationType {
  switch (dataType) {
    case "STRING":
      return AnnotationType.String;
  }

  throw new Error(`Unknown annotation data type ${dataType}.`);
}

function toAPIAnnotationType(annotationType: AnnotationType): tanagra.DataType {
  switch (annotationType) {
    case AnnotationType.String:
      return tanagra.DataType.String;
  }

  throw new Error(`Unhandled annotation type ${annotationType}.`);
}

function fromAPIAnnotation(annotation: tanagra.Annotation): Annotation {
  return {
    id: annotation.id,
    displayName: annotation.displayName,
    annotationType: fromAPIAnnotationType(annotation.dataType),
    enumVals: annotation.enumVals,
  };
}

function fromAPIDisplayHint(hint?: tanagra.DisplayHint): HintData {
  return {
    attribute: hint?.attribute?.name ?? "",
    integerHint: hint?.displayHint?.numericRangeHint
      ? {
          min: hint?.displayHint?.numericRangeHint.min ?? 0,
          max: hint?.displayHint?.numericRangeHint.max ?? 10000,
        }
      : undefined,
    enumHintOptions: hint?.displayHint?.enumHint?.enumHintValues
      ?.map((enumHintValue) => {
        if (!enumHintValue.enumVal) {
          return null;
        }

        const value = dataValueFromLiteral(enumHintValue.enumVal.value);
        return {
          value,
          name: enumHintValue.enumVal.display ?? String(value),
          count: enumHintValue.count ?? 0,
        };
      })
      ?.filter(isValid),
  };
}

function toAPIBinaryOperator(
  operator: tanagraUI.UIComparisonOperator
): tanagra.BinaryOperator {
  switch (operator) {
    case tanagraUI.UIComparisonOperator.Equal:
      return tanagra.BinaryOperator.Equals;
    case tanagraUI.UIComparisonOperator.GreaterThanEqual:
      return tanagra.BinaryOperator.GreaterThanOrEqual;
    case tanagraUI.UIComparisonOperator.LessThanEqual:
      return tanagra.BinaryOperator.LessThanOrEqual;
  }

  throw new Error(`Unhandled comparison operator: ${operator}`);
}

function parseAPIError<T>(p: Promise<T>) {
  return p.catch(async (response) => {
    if (!(response instanceof Response)) {
      throw response;
    }

    const text = await response.text();
    try {
      throw new Error(JSON.parse(text).message);
    } catch (e) {
      throw new Error(getReasonPhrase(response.status) + ": " + text);
    }
  });
}

function processStudy(study: tanagra.Study): Study {
  const properties: PropertyMap = {};
  study.properties?.forEach(({ key, value }) => (properties[key] = value));

  return {
    id: study.id,
    displayName: study.displayName ?? "Untitled study",
    created: study.created,
    properties,
  };
}
