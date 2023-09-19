import {
  defaultSection,
  generateCohortFilter,
  getCriteriaPlugin,
} from "cohort";
import { getReasonPhrase } from "http-status-codes";
import * as tanagra from "tanagra-api";
import * as tanagraUI from "tanagra-ui";
import { Underlay } from "underlaysSlice";
import { isValid } from "util/valid";
import {
  Classification,
  Configuration,
  findByID,
  findEntity,
  Grouping,
  ITEM_COUNT_ATTRIBUTE,
  Occurrence,
  ROLLUP_COUNT_ATTRIBUTE,
  SortDirection,
  VALUE_SUFFIX,
} from "./configuration";
import {
  ArrayFilter,
  Filter,
  isArrayFilter,
  isAttributeFilter,
  isClassificationFilter,
  isRelationshipFilter,
  isTextFilter,
  isUnaryFilter,
} from "./filter";
import { CohortReview, DataEntry, DataKey, DataValue } from "./types";

export type ClassificationNode = {
  data: DataEntry;
  grouping?: string;
  ancestors?: DataKey[];
  childCount?: number;
};

export type SearchClassificationOptions = {
  query?: string;
  parent?: DataKey;
  includeGroupings?: boolean;
};

export type SearchClassificationResult = {
  nodes: ClassificationNode[];
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

export type MergedDataEntry = {
  source: string;
  data: DataEntry;
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
  occurrenceID: string;
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

export interface Source {
  config: Configuration;
  underlay: Underlay;

  lookupOccurrence(occurrenceID: string): Occurrence;
  lookupClassification(
    occurrenceID: string,
    classificationID: string
  ): Classification;

  searchClassification(
    requestedAttributes: string[],
    occurrenceID: string,
    classificationID: string,
    options?: SearchClassificationOptions
  ): Promise<SearchClassificationResult>;

  searchGrouping(
    requestedAttributes: string[],
    occurrenceID: string,
    classificationID: string,
    root: ClassificationNode
  ): Promise<SearchClassificationResult>;

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

  mergeDataEntryLists(
    lists: [string, DataEntry[]][],
    maxCount: number
  ): MergedDataEntry[];

  listCohortReviews(studyId: string, cohortId: string): Promise<CohortReview[]>;

  createCohortReview(
    studyId: string,
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
  getCohort(studyId: string, cohortId: string): Promise<tanagraUI.UICohort>;

  listCohorts(studyId: string): Promise<tanagraUI.UICohort[]>;

  createCohort(
    underlayName: string,
    studyId: string,
    displayName: string
  ): Promise<tanagraUI.UICohort>;

  updateCohort(studyId: string, cohort: tanagraUI.UICohort): void;

  deleteCohort(studyId: string, cohortId: string): void;

  getConceptSet(
    studyId: string,
    conceptSetId: string
  ): Promise<tanagraUI.UIConceptSet>;

  listConceptSets(studyId: string): Promise<tanagraUI.UIConceptSet[]>;

  createConceptSet(
    underlayName: string,
    studyId: string,
    criteria: tanagraUI.UICriteria
  ): Promise<tanagraUI.UIConceptSet>;

  updateConceptSet(studyId: string, conceptSet: tanagraUI.UIConceptSet): void;

  deleteConceptSet(studyId: string, conceptSetId: string): void;

  listAnnotations(studyId: string, cohortId: string): Promise<Annotation[]>;

  createAnnotation(
    studyId: string,
    cohortId: string,
    displayName: string,
    annotationType: AnnotationType,
    enumVals?: string[]
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

  listExportModels(underlayName: string): Promise<ExportModel[]>;

  export(
    underlayName: string,
    studyId: string,
    modelId: string,
    returnURL: string,
    cohortIds: string[],
    entities: ExportRequestEntity[]
  ): Promise<ExportResult>;

  getUser(): Promise<User>;
}

export class BackendSource implements Source {
  constructor(
    private underlaysApi: tanagra.UnderlaysApi,
    private studiesApi: tanagra.StudiesApi,
    private cohortsApi: tanagra.CohortsApi,
    private conceptSetsApi: tanagra.ConceptSetsApi,
    private reviewsApi: tanagra.ReviewsApi,
    private annotationsApi: tanagra.AnnotationsApi,
    private exportApi: tanagra.ExportApi,
    private usersApi: tanagra.UsersApi,
    public underlay: Underlay,
    public config: Configuration
  ) {}

  lookupOccurrence(occurrenceID: string): Occurrence {
    if (!occurrenceID) {
      return { ...this.config.primaryEntity, id: "" };
    }

    return findByID(occurrenceID, this.config.occurrences);
  }

  lookupClassification(
    occurrenceID: string,
    classificationID: string
  ): Classification {
    return findByID(
      classificationID,
      this.lookupOccurrence(occurrenceID).classifications
    );
  }

  searchClassification(
    requestedAttributes: string[],
    occurrenceID: string,
    classificationID: string,
    options?: SearchClassificationOptions
  ): Promise<SearchClassificationResult> {
    const classification = this.lookupClassification(
      occurrenceID,
      classificationID
    );

    const promises = [
      this.underlaysApi.listInstances(
        searchRequest(
          requestedAttributes,
          this.underlay,
          occurrenceID,
          classification,
          undefined,
          options?.query,
          options?.parent
        )
      ),
    ];

    if (options?.includeGroupings) {
      promises.push(
        ...(classification.groupings?.map((grouping) =>
          this.underlaysApi.listInstances(
            searchRequest(
              requestedAttributes,
              this.underlay,
              occurrenceID,
              classification,
              grouping,
              options?.query,
              options?.parent
            )
          )
        ) || [])
      );
    }

    return parseAPIError(
      Promise.all(promises).then((res) => {
        const result: SearchClassificationResult = { nodes: [] };
        res?.forEach((r, i) => {
          result.nodes.push(
            ...processEntitiesResponse(
              classification.entityAttribute,
              r,
              i === 0 ? classification.hierarchy : undefined,
              i > 0 ? classification.groupings?.[i - 1].id : undefined
            )
          );
        });
        return result;
      })
    );
  }

  searchGrouping(
    requestedAttributes: string[],
    occurrenceID: string,
    classificationID: string,
    root: ClassificationNode
  ): Promise<SearchClassificationResult> {
    const occurrence = findByID(occurrenceID, this.config.occurrences);
    const classification = findByID(
      classificationID,
      occurrence.classifications
    );

    if (!root.grouping) {
      throw new Error(`Grouping undefined while searching from "${root}"`);
    }
    const grouping = findByID(root.grouping, classification.groupings);

    return parseAPIError(
      this.underlaysApi
        .listInstances({
          entityName: classification.entity,
          underlayName: this.underlay.name,
          query: {
            includeAttributes: normalizeRequestedAttributes(
              requestedAttributes,
              classification.entityAttribute
            ),
            includeHierarchyFields: !!classification.hierarchy
              ? {
                  hierarchies: [classification.hierarchy],
                  fields: [
                    tanagra.QueryIncludeHierarchyFieldsFieldsEnum.Path,
                    tanagra.QueryIncludeHierarchyFieldsFieldsEnum.NumChildren,
                  ],
                }
              : undefined,
            filter: {
              filterType: tanagra.FilterFilterTypeEnum.Relationship,
              filterUnion: {
                relationshipFilter: {
                  entity: grouping.entity,
                  subfilter: {
                    filterType: tanagra.FilterFilterTypeEnum.Attribute,
                    filterUnion: {
                      attributeFilter: {
                        attribute: classification.entityAttribute,
                        operator: tanagra.BinaryOperator.Equals,
                        value: literalFromDataValue(root.data.key),
                      },
                    },
                  },
                },
              },
            },
            orderBys: [makeOrderBy(this.underlay, classification, grouping)],
          },
        })
        .then((res) => ({
          nodes: processEntitiesResponse(
            classification.entityAttribute,
            res,
            classification.hierarchy
          ),
        }))
    );
  }

  listAttributes(occurrenceID: string): string[] {
    let entity = this.config.primaryEntity.entity;
    if (occurrenceID) {
      entity = findByID(occurrenceID, this.config.occurrences).entity;
    }

    return (
      this.underlay.entities
        .find((e) => e.name === entity)
        ?.attributes?.map((a) => a.name)
        .filter(isValid)
        .filter((n) => !isInternalAttribute(n)) || []
    );
  }

  async listData(
    requestedAttributes: string[],
    occurrenceID: string,
    cohort: Filter,
    conceptSet: Filter | null,
    limit?: number
  ): Promise<ListDataResponse> {
    const entity = findEntity(occurrenceID, this.config);
    const ra = normalizeRequestedAttributes(requestedAttributes, entity.key);

    const res = await parseAPIError(
      this.underlaysApi.listInstances({
        entityName: entity.entity,
        underlayName: this.underlay.name,
        query: this.makeQuery(ra, occurrenceID, cohort, conceptSet, limit),
      })
    );

    const data = res.instances?.map((instance) =>
      makeDataEntry(entity.key, instance.attributes)
    );
    return {
      data: data ?? [],
      sql: res.sql ?? "",
    };
  }

  async getHintData(
    occurrenceID: string,
    attributeID: string,
    relatedEntity?: string,
    relatedID?: DataKey
  ): Promise<HintData | undefined> {
    const res = await parseAPIError(
      this.underlaysApi.queryHints({
        entityName: findEntity(occurrenceID, this.config).entity,
        underlayName: this.underlay.name,
        hintQuery:
          !!relatedEntity && !!relatedID
            ? {
                relatedEntity: {
                  name: relatedEntity,
                  id: literalFromDataValue(relatedID),
                },
              }
            : {},
      })
    );

    const hint = res.displayHints?.find(
      (hint) => hint?.attribute?.name === attributeID
    );

    return fromAPIDisplayHint(hint);
  }

  async getAllHintData(
    occurrenceID: string,
    relatedEntity?: string,
    relatedID?: DataKey
  ): Promise<HintData[]> {
    const res = await parseAPIError(
      this.underlaysApi.queryHints({
        entityName: findEntity(occurrenceID, this.config).entity,
        underlayName: this.underlay.name,
        hintQuery:
          !!relatedEntity && !!relatedID
            ? {
                relatedEntity: {
                  name: relatedEntity,
                  id: literalFromDataValue(relatedID),
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
        entityName: this.config.primaryEntity.entity,
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

  public mergeDataEntryLists(
    lists: [string, DataEntry[]][],
    maxCount: number
  ): MergedDataEntry[] {
    const merged: MergedDataEntry[] = [];
    const sources = lists.map(
      ([source, data]) => new MergeSource(source, data)
    );
    // TODO(tjennison): Pass this as a parameter and base it on the criteria
    // config ordering.
    const countKey = ROLLUP_COUNT_ATTRIBUTE;

    while (true) {
      let maxSource: MergeSource | undefined;

      sources.forEach((source) => {
        if (!source.done()) {
          if (!maxSource) {
            maxSource = source;
          } else {
            const value = source.peek()[countKey];
            const maxValue = maxSource.peek()[countKey];
            if (
              (value && !maxValue) ||
              (value && maxValue && value > maxValue)
            ) {
              maxSource = source;
            }
          }
        }
      });
      if (!maxSource || merged.length === maxCount) {
        break;
      }
      merged.push({ source: maxSource.source, data: maxSource.pop() });
    }

    return merged;
  }

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
            filter: generateFilter(this, generateCohortFilter(cohort)) ?? {},
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
    cohortId: string,
    reviewId: string,
    includeAttributes: string[]
  ): Promise<ReviewInstance[]> {
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
                fromAPIReviewInstance(
                  reviewId,
                  this.config.primaryEntity.key,
                  i
                )
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
    cohortId: string
  ): Promise<tanagraUI.UICohort> {
    return parseAPIError(
      this.cohortsApi
        .getCohort({ studyId, cohortId })
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
    displayName: string
  ): Promise<tanagraUI.UICohort> {
    return parseAPIError(
      this.cohortsApi
        .createCohort({
          studyId,
          cohortCreateInfo: { underlayName, displayName },
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

  public async getConceptSet(
    studyId: string,
    conceptSetId: string
  ): Promise<tanagraUI.UIConceptSet> {
    return parseAPIError(
      this.conceptSetsApi
        .getConceptSet({ studyId, conceptSetId })
        .then((c) => fromAPIConceptSet(c))
    );
  }

  public listConceptSets(studyId: string): Promise<tanagraUI.UIConceptSet[]> {
    return parseAPIError(
      this.conceptSetsApi
        .listConceptSets({ studyId })
        .then((res) => res.map((c) => fromAPIConceptSet(c)))
    );
  }

  public createConceptSet(
    underlayName: string,
    studyId: string,
    criteria: tanagraUI.UICriteria
  ): Promise<tanagraUI.UIConceptSet> {
    return parseAPIError(
      this.conceptSetsApi
        .createConceptSet({
          studyId,
          conceptSetCreateInfo: {
            underlayName,
            criteria: toAPICriteria(criteria),
            entity: findEntity(
              getCriteriaPlugin(criteria).filterOccurrenceId(),
              this.config
            ).entity,
          },
        })
        .then((cs) => fromAPIConceptSet(cs))
    );
  }

  public async updateConceptSet(
    studyId: string,
    conceptSet: tanagraUI.UIConceptSet
  ) {
    await parseAPIError(
      this.conceptSetsApi.updateConceptSet({
        studyId,
        conceptSetId: conceptSet.id,
        conceptSetUpdateInfo: {
          criteria: toAPICriteria(conceptSet.criteria),
        },
      })
    );
  }

  public async deleteConceptSet(studyId: string, conceptSetId: string) {
    await parseAPIError(
      this.conceptSetsApi.deleteConceptSet({
        studyId,
        conceptSetId,
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
              entity: findEntity(e.occurrenceID, this.config).entity,
              query: this.makeQuery(
                e.requestedAttributes,
                e.occurrenceID,
                e.cohort,
                e.conceptSet
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

  public async getUser(): Promise<User> {
    return parseAPIError(
      this.usersApi.getMe({}).then((res) => ({
        email: res.email,
      }))
    );
  }

  private makeQuery(
    requestedAttributes: string[],
    occurrenceID: string,
    cohort: Filter,
    conceptSet: Filter | null,
    limit?: number
  ): tanagra.Query {
    let cohortFilter = generateFilter(this, cohort);
    if (!cohortFilter) {
      throw new Error("Cohort filter is empty.");
    }

    if (occurrenceID) {
      const primaryEntity = this.config.primaryEntity.entity;
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
      limit: limit ?? 50,
    };
  }
}

class MergeSource {
  constructor(public source: string, private data: DataEntry[]) {
    this.source = source;
    this.data = data;
    this.current = 0;
  }

  done() {
    return this.current === this.data.length;
  }

  peek() {
    return this.data[this.current];
  }

  pop(): DataEntry {
    const data = this.peek();
    this.current++;
    return data;
  }

  private current: number;
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

function searchRequest(
  requestedAttributes: string[],
  underlay: Underlay,
  occurrenceID: string,
  classification: Classification,
  grouping?: Grouping,
  query?: string,
  parent?: DataValue
) {
  const entity = grouping?.entity || classification.entity;

  const operands: tanagra.Filter[] = [];
  if (classification.hierarchy && !grouping) {
    operands.push({
      filterType: tanagra.FilterFilterTypeEnum.Hierarchy,
      filterUnion: {
        hierarchyFilter: {
          hierarchy: classification.hierarchy,
          operator: tanagra.HierarchyFilterOperatorEnum.IsMember,
          value: literalFromDataValue(true),
        },
      },
    });
  }

  if (classification.hierarchy && parent) {
    operands.push({
      filterType: tanagra.FilterFilterTypeEnum.Hierarchy,
      filterUnion: {
        hierarchyFilter: {
          hierarchy: classification.hierarchy,
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
  } else if (classification.hierarchy && !grouping) {
    operands.push({
      filterType: tanagra.FilterFilterTypeEnum.Hierarchy,
      filterUnion: {
        hierarchyFilter: {
          hierarchy: classification.hierarchy,
          operator: tanagra.HierarchyFilterOperatorEnum.IsRoot,
          value: literalFromDataValue(true),
        },
      },
    });
  }

  const req = {
    entityName: entity,
    underlayName: underlay.name,
    query: {
      includeAttributes: normalizeRequestedAttributes(
        grouping?.attributes ?? requestedAttributes,
        !grouping?.attributes ? classification.entityAttribute : undefined
      ),
      includeHierarchyFields:
        !!classification.hierarchy && !grouping
          ? {
              hierarchies: [classification.hierarchy],
              fields: [
                tanagra.QueryIncludeHierarchyFieldsFieldsEnum.Path,
                tanagra.QueryIncludeHierarchyFieldsFieldsEnum.NumChildren,
              ],
            }
          : undefined,
      includeRelationshipFields:
        !grouping && !!occurrenceID
          ? [
              {
                relatedEntity: underlay.primaryEntity,
                hierarchies: !!classification.hierarchy
                  ? [classification.hierarchy]
                  : undefined,
              },
            ]
          : undefined,
      filter:
        makeBooleanLogicFilter(
          tanagra.BooleanLogicFilterOperatorEnum.And,
          operands
        ) ?? undefined,
      orderBys: [makeOrderBy(underlay, classification, grouping)],
    },
  };
  return req;
}

function convertSortDirection(dir: SortDirection) {
  return dir === SortDirection.Desc
    ? tanagra.OrderByDirection.Descending
    : tanagra.OrderByDirection.Ascending;
}

function processEntitiesResponse(
  primaryKey: string,
  response: tanagra.InstanceListResult,
  hierarchy?: string,
  grouping?: string
): ClassificationNode[] {
  const nodes: ClassificationNode[] = [];
  if (response.instances) {
    response.instances.forEach((instance) => {
      const data = makeDataEntry(primaryKey, instance.attributes);

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
          if (fields.hierarchy === hierarchy) {
            data[ROLLUP_COUNT_ATTRIBUTE] = fields.count;
          } else if (!fields.hierarchy) {
            data[ITEM_COUNT_ATTRIBUTE] = fields.count;
          }
        }
      });

      nodes.push({
        data: data,
        grouping: grouping,
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

// TODO(tjennison): Move this to BackendSource and make it private once the
// count API uses have been converted.
export function generateFilter(
  source: Source,
  filter: Filter | null
): tanagra.Filter | null {
  if (!filter) {
    return null;
  }

  if (isArrayFilter(filter)) {
    const operands = filter.operands
      .map((o) => generateFilter(source, o))
      .filter(isValid);
    if (operands.length === 0) {
      return null;
    }

    return makeBooleanLogicFilter(arrayFilterOperator(filter), operands);
  }
  if (isUnaryFilter(filter)) {
    const operand = generateFilter(source, filter.operand);
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
    const entity = findEntity(filter.entityId, source.config);
    const subfilter = generateFilter(source, filter.subfilter);
    if (!subfilter) {
      return null;
    }

    return {
      filterType: tanagra.FilterFilterTypeEnum.Relationship,
      filterUnion: {
        relationshipFilter: {
          entity: entity.entity,
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

  if (isClassificationFilter(filter)) {
    const entity = findEntity(filter.occurrenceId, source.config);
    const classification = findByID(
      filter.classificationId,
      entity.classifications
    );

    const classificationFilter = (key: DataKey): tanagra.Filter => {
      if (classification.hierarchy) {
        return {
          filterType: tanagra.FilterFilterTypeEnum.Hierarchy,
          filterUnion: {
            hierarchyFilter: {
              hierarchy: classification.hierarchy,
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
            attribute: classification.entityAttribute,
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
          entity: classification.entity,
          subfilter,
        },
      },
    };
  }
  if (isAttributeFilter(filter)) {
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
  classification: Classification,
  grouping?: Grouping
) {
  const orderBy: tanagra.QueryOrderBys = {
    direction: convertSortDirection(
      grouping?.defaultSort?.direction ?? classification.defaultSort?.direction
    ),
  };

  const sortAttribute =
    grouping?.defaultSort?.attribute ?? classification.defaultSort?.attribute;
  if (sortAttribute === ROLLUP_COUNT_ATTRIBUTE) {
    orderBy.relationshipField = {
      relatedEntity: underlay.primaryEntity,
      hierarchy: classification.hierarchy,
    };
  } else if (sortAttribute === ITEM_COUNT_ATTRIBUTE) {
    orderBy.relationshipField = {
      relatedEntity: underlay.primaryEntity,
    };
  } else {
    orderBy.attribute = sortAttribute;
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

function fromAPIConceptSet(
  conceptSet: tanagra.ConceptSet
): tanagraUI.UIConceptSet {
  return {
    id: conceptSet.id,
    underlayName: conceptSet.underlayName,
    criteria: conceptSet.criteria && fromAPICriteria(conceptSet.criteria),
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
