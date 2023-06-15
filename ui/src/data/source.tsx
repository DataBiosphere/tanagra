import {
  AnnotationsApiContext,
  CohortsApiContext,
  ConceptSetsApiContext,
  EntityInstancesApiContext,
  ExportApiContext,
  HintsApiContext,
  ReviewsApiContext,
  StudiesApiContext,
} from "apiContext";
import {
  defaultSection,
  generateCohortFilter,
  getCriteriaPlugin,
} from "cohort";
import { useUnderlay } from "hooks";
import { getReasonPhrase } from "http-status-codes";
import { useContext, useMemo } from "react";
import * as tanagra from "tanagra-api";
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

export interface Source {
  config: Configuration;

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

  exportData(
    requestedAttributes: string[],
    occurrenceID: string,
    cohort: Filter,
    conceptSet: Filter | null,
    limit?: number
  ): Promise<string>;

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
    cohort: tanagra.Cohort,
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

  listStudies(): Promise<Study[]>;

  createStudy(displayName: string): Promise<Study>;

  deleteStudy(studyId: string): void;

  // TODO(tjennison): Use internal types for cohorts and related objects instead
  // of V1 types from the service definition.
  getCohort(studyId: string, cohortId: string): Promise<tanagra.Cohort>;

  listCohorts(studyId: string): Promise<tanagra.Cohort[]>;

  createCohort(
    underlayName: string,
    studyId: string,
    displayName: string
  ): Promise<tanagra.Cohort>;

  updateCohort(studyId: string, cohort: tanagra.Cohort): void;

  getConceptSet(
    studyId: string,
    conceptSetId: string
  ): Promise<tanagra.ConceptSet>;

  listConceptSets(studyId: string): Promise<tanagra.ConceptSet[]>;

  createConceptSet(
    underlayName: string,
    studyId: string,
    criteria: tanagra.Criteria
  ): Promise<tanagra.ConceptSet>;

  updateConceptSet(studyId: string, conceptSet: tanagra.ConceptSet): void;

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

  exportAnnotationValues(studyId: string, cohortId: string): Promise<string>;

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
}

// TODO(tjennison): Create the source once and put it into the context instead
// of recreating it. Move "fake" logic into a separate source instead of APIs.
export function useSource(): Source {
  const underlay = useUnderlay();
  const instancesApi = useContext(
    EntityInstancesApiContext
  ) as tanagra.InstancesV2Api;
  const hintsApi = useContext(HintsApiContext) as tanagra.HintsV2Api;
  const studiesApi = useContext(StudiesApiContext) as tanagra.StudiesV2Api;
  const cohortsApi = useContext(CohortsApiContext) as tanagra.CohortsV2Api;
  const conceptSetsApi = useContext(
    ConceptSetsApiContext
  ) as tanagra.ConceptSetsV2Api;
  const reviewsApi = useContext(ReviewsApiContext) as tanagra.ReviewsV2Api;
  const annotationsApi = useContext(
    AnnotationsApiContext
  ) as tanagra.AnnotationsV2Api;
  const exportApi = useContext(ExportApiContext) as tanagra.ExportApi;
  return useMemo(
    () =>
      new BackendSource(
        instancesApi,
        hintsApi,
        studiesApi,
        cohortsApi,
        conceptSetsApi,
        reviewsApi,
        annotationsApi,
        exportApi,
        underlay,
        underlay.uiConfiguration.dataConfig
      ),
    [underlay]
  );
}

export class BackendSource implements Source {
  constructor(
    private instancesApi: tanagra.InstancesV2Api,
    private hintsApi: tanagra.HintsV2Api,
    private studiesApi: tanagra.StudiesV2Api,
    private cohortsApi: tanagra.CohortsV2Api,
    private conceptSetsApi: tanagra.ConceptSetsV2Api,
    private reviewsApi: tanagra.ReviewsV2Api,
    private annotationsApi: tanagra.AnnotationsV2Api,
    private exportApi: tanagra.ExportApi,
    private underlay: Underlay,
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
      this.instancesApi.listInstances(
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
          this.instancesApi.listInstances(
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
      this.instancesApi
        .listInstances({
          entityName: classification.entity,
          underlayName: this.underlay.name,
          queryV2: {
            includeAttributes: normalizeRequestedAttributes(
              requestedAttributes,
              classification.entityAttribute
            ),
            includeHierarchyFields: !!classification.hierarchy
              ? {
                  hierarchies: [classification.hierarchy],
                  fields: [
                    tanagra.QueryV2IncludeHierarchyFieldsFieldsEnum.Path,
                    tanagra.QueryV2IncludeHierarchyFieldsFieldsEnum.NumChildren,
                  ],
                }
              : undefined,
            filter: {
              filterType: tanagra.FilterV2FilterTypeEnum.Relationship,
              filterUnion: {
                relationshipFilter: {
                  entity: grouping.entity,
                  subfilter: {
                    filterType: tanagra.FilterV2FilterTypeEnum.Attribute,
                    filterUnion: {
                      attributeFilter: {
                        attribute: classification.entityAttribute,
                        operator: tanagra.BinaryOperatorV2.Equals,
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
      this.instancesApi.listInstances({
        entityName: entity.entity,
        underlayName: this.underlay.name,
        queryV2: this.makeQuery(ra, occurrenceID, cohort, conceptSet, limit),
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

  async exportData(
    requestedAttributes: string[],
    occurrenceID: string,
    cohort: Filter,
    conceptSet: Filter | null,
    limit?: number
  ): Promise<string> {
    const entity = findEntity(occurrenceID, this.config);

    const res = await parseAPIError(
      this.instancesApi.exportInstances({
        entityName: entity.entity,
        underlayName: this.underlay.name,
        queryV2: this.makeQuery(
          requestedAttributes,
          occurrenceID,
          cohort,
          conceptSet,
          limit
        ),
      })
    );

    if (!res.gcsSignedUrl) {
      throw new Error("Invalid GCS signed URL.");
    }

    return res.gcsSignedUrl;
  }

  async getHintData(
    occurrenceID: string,
    attributeID: string,
    relatedEntity?: string,
    relatedID?: DataKey
  ): Promise<HintData | undefined> {
    const res = await parseAPIError(
      this.hintsApi.queryHints({
        entityName: findEntity(occurrenceID, this.config).entity,
        underlayName: this.underlay.name,
        hintQueryV2:
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
      this.hintsApi.queryHints({
        entityName: findEntity(occurrenceID, this.config).entity,
        underlayName: this.underlay.name,
        hintQueryV2:
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
      this.instancesApi.countInstances({
        underlayName: this.underlay.name,
        entityName: this.config.primaryEntity.entity,
        countQueryV2: {
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
    cohort: tanagra.Cohort,
    displayName: string,
    size: number
  ): Promise<CohortReview> {
    return parseAPIError(
      this.reviewsApi
        .createReview({
          studyId,
          cohortId: cohort.id,
          reviewCreateInfoV2: {
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
          reviewUpdateInfoV2: {
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
          reviewQueryV2: {
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

  public listStudies(): Promise<Study[]> {
    return parseAPIError(
      this.studiesApi
        .listStudies({})
        .then((studies) => studies.map((study) => processStudy(study)))
    );
  }

  public createStudy(displayName: string): Promise<Study> {
    return parseAPIError(
      this.studiesApi
        .createStudy({
          studyCreateInfoV2: {
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
  ): Promise<tanagra.Cohort> {
    return parseAPIError(
      this.cohortsApi
        .getCohort({ studyId, cohortId })
        .then((c) => fromAPICohort(c))
    );
  }

  public listCohorts(studyId: string): Promise<tanagra.Cohort[]> {
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
  ): Promise<tanagra.Cohort> {
    return parseAPIError(
      this.cohortsApi
        .createCohort({
          studyId,
          cohortCreateInfoV2: { underlayName, displayName },
        })
        .then((c) => fromAPICohort(c))
    );
  }

  public async updateCohort(studyId: string, cohort: tanagra.Cohort) {
    await parseAPIError(
      this.cohortsApi.updateCohort({
        studyId,
        cohortId: cohort.id,
        cohortUpdateInfoV2: {
          displayName: cohort.name,
          criteriaGroupSections: toAPICriteriaGroupSections(
            cohort.groupSections
          ),
        },
      })
    );
  }

  public async getConceptSet(
    studyId: string,
    conceptSetId: string
  ): Promise<tanagra.ConceptSet> {
    return parseAPIError(
      this.conceptSetsApi
        .getConceptSet({ studyId, conceptSetId })
        .then((c) => fromAPIConceptSet(c))
    );
  }

  public listConceptSets(studyId: string): Promise<tanagra.ConceptSet[]> {
    return parseAPIError(
      this.conceptSetsApi
        .listConceptSets({ studyId })
        .then((res) => res.map((c) => fromAPIConceptSet(c)))
    );
  }

  public createConceptSet(
    underlayName: string,
    studyId: string,
    criteria: tanagra.Criteria
  ): Promise<tanagra.ConceptSet> {
    return parseAPIError(
      this.conceptSetsApi
        .createConceptSet({
          studyId,
          conceptSetCreateInfoV2: {
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
    conceptSet: tanagra.ConceptSet
  ) {
    await parseAPIError(
      this.conceptSetsApi.updateConceptSet({
        studyId,
        conceptSetId: conceptSet.id,
        conceptSetUpdateInfoV2: {
          criteria: toAPICriteria(conceptSet.criteria),
        },
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
        annotationCreateInfoV2: {
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

  public async exportAnnotationValues(
    studyId: string,
    cohortId: string
  ): Promise<string> {
    const res = await parseAPIError(
      this.annotationsApi.exportAnnotationValues({ studyId, cohortId })
    );

    if (!res.gcsSignedUrl) {
      throw new Error("Invalid GCS signed URL.");
    }

    return res.gcsSignedUrl;
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
        literalV2: literalFromDataValue(value),
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

  private makeQuery(
    requestedAttributes: string[],
    occurrenceID: string,
    cohort: Filter,
    conceptSet: Filter | null,
    limit?: number
  ): tanagra.QueryV2 {
    let cohortFilter = generateFilter(this, cohort);
    if (!cohortFilter) {
      throw new Error("Cohort filter is empty.");
    }

    if (occurrenceID) {
      const primaryEntity = this.config.primaryEntity.entity;
      cohortFilter = {
        filterType: tanagra.FilterV2FilterTypeEnum.Relationship,
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
        tanagra.BooleanLogicFilterV2OperatorEnum.And,
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

function literalFromDataValue(value: DataValue): tanagra.LiteralV2 {
  let dataType = tanagra.DataTypeV2.Int64;
  if (typeof value === "string") {
    dataType = tanagra.DataTypeV2.String;
  } else if (typeof value === "boolean") {
    dataType = tanagra.DataTypeV2.Boolean;
  } else if (value instanceof Date) {
    dataType = tanagra.DataTypeV2.Date;
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

function dataValueFromLiteral(value?: tanagra.LiteralV2 | null): DataValue {
  if (!value) {
    return null;
  }
  switch (value.dataType) {
    case tanagra.DataTypeV2.Int64:
      return value.valueUnion?.int64Val ?? null;
    case tanagra.DataTypeV2.String:
      return value.valueUnion?.stringVal ?? null;
    case tanagra.DataTypeV2.Date:
      return value.valueUnion?.dateVal
        ? new Date(value.valueUnion.dateVal)
        : null;
    case tanagra.DataTypeV2.Boolean:
      return value.valueUnion?.boolVal ?? null;
    case tanagra.DataTypeV2.Double:
      return value.valueUnion?.doubleVal ?? null;
    case tanagra.DataTypeV2.Timestamp:
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

  const operands: tanagra.FilterV2[] = [];
  if (classification.hierarchy && !grouping) {
    operands.push({
      filterType: tanagra.FilterV2FilterTypeEnum.Hierarchy,
      filterUnion: {
        hierarchyFilter: {
          hierarchy: classification.hierarchy,
          operator: tanagra.HierarchyFilterV2OperatorEnum.IsMember,
          value: literalFromDataValue(true),
        },
      },
    });
  }

  if (classification.hierarchy && parent) {
    operands.push({
      filterType: tanagra.FilterV2FilterTypeEnum.Hierarchy,
      filterUnion: {
        hierarchyFilter: {
          hierarchy: classification.hierarchy,
          operator: tanagra.HierarchyFilterV2OperatorEnum.ChildOf,
          value: literalFromDataValue(parent),
        },
      },
    });
  } else if (isValid(query)) {
    if (query !== "") {
      operands.push({
        filterType: tanagra.FilterV2FilterTypeEnum.Text,
        filterUnion: {
          textFilter: {
            matchType: tanagra.TextFilterV2MatchTypeEnum.ExactMatch,
            text: query,
          },
        },
      });
    }
  } else if (classification.hierarchy && !grouping) {
    operands.push({
      filterType: tanagra.FilterV2FilterTypeEnum.Hierarchy,
      filterUnion: {
        hierarchyFilter: {
          hierarchy: classification.hierarchy,
          operator: tanagra.HierarchyFilterV2OperatorEnum.IsRoot,
          value: literalFromDataValue(true),
        },
      },
    });
  }

  const req = {
    entityName: entity,
    underlayName: underlay.name,
    queryV2: {
      includeAttributes: normalizeRequestedAttributes(
        grouping?.attributes ?? requestedAttributes,
        !grouping?.attributes ? classification.entityAttribute : undefined
      ),
      includeHierarchyFields:
        !!classification.hierarchy && !grouping
          ? {
              hierarchies: [classification.hierarchy],
              fields: [
                tanagra.QueryV2IncludeHierarchyFieldsFieldsEnum.Path,
                tanagra.QueryV2IncludeHierarchyFieldsFieldsEnum.NumChildren,
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
          tanagra.BooleanLogicFilterV2OperatorEnum.And,
          operands
        ) ?? undefined,
      orderBys: [makeOrderBy(underlay, classification, grouping)],
    },
  };
  return req;
}

function convertSortDirection(dir: SortDirection) {
  return dir === SortDirection.Desc
    ? tanagra.OrderByDirectionV2.Descending
    : tanagra.OrderByDirectionV2.Ascending;
}

function processEntitiesResponse(
  primaryKey: string,
  response: tanagra.InstanceListResultV2,
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
  attributes?: { [key: string]: tanagra.ValueDisplayV2 }
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
): tanagra.FilterV2 | null {
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
      filterType: tanagra.FilterV2FilterTypeEnum.BooleanLogic,
      filterUnion: {
        booleanLogicFilter: {
          operator: tanagra.BooleanLogicFilterV2OperatorEnum.Not,
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
      filterType: tanagra.FilterV2FilterTypeEnum.Relationship,
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

    const classificationFilter = (key: DataKey): tanagra.FilterV2 => {
      if (classification.hierarchy) {
        return {
          filterType: tanagra.FilterV2FilterTypeEnum.Hierarchy,
          filterUnion: {
            hierarchyFilter: {
              hierarchy: classification.hierarchy,
              operator:
                tanagra.HierarchyFilterV2OperatorEnum.DescendantOfInclusive,
              value: literalFromDataValue(key),
            },
          },
        };
      }

      return {
        filterType: tanagra.FilterV2FilterTypeEnum.Attribute,
        filterUnion: {
          attributeFilter: {
            attribute: classification.entityAttribute,
            operator: tanagra.BinaryOperatorV2.Equals,
            value: literalFromDataValue(key),
          },
        },
      };
    };

    const operands = filter.keys.map(classificationFilter);

    let subfilter: tanagra.FilterV2 | undefined;
    if (operands.length > 0) {
      subfilter =
        makeBooleanLogicFilter(
          tanagra.BooleanLogicFilterV2OperatorEnum.Or,
          operands
        ) ?? undefined;
    }

    return {
      filterType: tanagra.FilterV2FilterTypeEnum.Relationship,
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
        tanagra.BooleanLogicFilterV2OperatorEnum.Or,
        filter.ranges.map(({ min, max }) =>
          makeBooleanLogicFilter(tanagra.BooleanLogicFilterV2OperatorEnum.And, [
            {
              filterType: tanagra.FilterV2FilterTypeEnum.Attribute,
              filterUnion: {
                attributeFilter: {
                  attribute: filter.attribute,
                  operator: tanagra.BinaryOperatorV2.LessThanOrEqual,
                  value: literalFromDataValue(max),
                },
              },
            },
            {
              filterType: tanagra.FilterV2FilterTypeEnum.Attribute,
              filterUnion: {
                attributeFilter: {
                  attribute: filter.attribute,
                  operator: tanagra.BinaryOperatorV2.GreaterThanOrEqual,
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
        tanagra.BooleanLogicFilterV2OperatorEnum.Or,
        filter.values.map((value) => ({
          filterType: tanagra.FilterV2FilterTypeEnum.Attribute,
          filterUnion: {
            attributeFilter: {
              attribute: filter.attribute,
              operator: tanagra.BinaryOperatorV2.Equals,
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
      filterType: tanagra.FilterV2FilterTypeEnum.Text,
      filterUnion: {
        textFilter: {
          matchType: tanagra.TextFilterV2MatchTypeEnum.ExactMatch,
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
): tanagra.BooleanLogicFilterV2OperatorEnum {
  if (!isValid(filter.operator.min) && !isValid(filter.operator.max)) {
    return tanagra.BooleanLogicFilterV2OperatorEnum.And;
  }
  if (filter.operator.min === 1 && !isValid(filter.operator.max)) {
    return tanagra.BooleanLogicFilterV2OperatorEnum.Or;
  }

  throw new Error("Only AND and OR equivalent operators are supported.");
}

function processAttributes(
  obj: { [x: string]: DataValue },
  attributes?: { [x: string]: tanagra.ValueDisplayV2 }
) {
  for (const k in attributes) {
    const v = attributes[k];
    const value = dataValueFromLiteral(v.value);
    obj[k] = v.display ?? value;
    obj[k + VALUE_SUFFIX] = value;
  }
}

function makeBooleanLogicFilter(
  operator: tanagra.BooleanLogicFilterV2OperatorEnum,
  operands: (tanagra.FilterV2 | null)[]
) {
  const subfilters = operands.filter(isValid);
  if (!subfilters || subfilters.length === 0) {
    return null;
  }
  if (subfilters.length === 1) {
    return subfilters[0];
  }
  return {
    filterType: tanagra.FilterV2FilterTypeEnum.BooleanLogic,
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
  const orderBy: tanagra.QueryV2OrderBys = {
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

function fromAPICohort(cohort: tanagra.CohortV2): tanagra.Cohort {
  return {
    id: cohort.id,
    name: cohort.displayName,
    underlayName: cohort.underlayName,
    lastModified: cohort.lastModified,
    groupSections: fromAPICriteriaGroupSections(cohort.criteriaGroupSections),
  };
}

function fromAPICriteriaGroupSections(
  sections?: tanagra.CriteriaGroupSectionV3[]
): tanagra.GroupSection[] {
  if (!sections?.length) {
    return [defaultSection()];
  }

  return sections.map((section) => ({
    id: section.id,
    name: section.displayName,
    filter: {
      kind:
        section.operator === tanagra.CriteriaGroupSectionV3OperatorEnum.And
          ? tanagra.GroupSectionFilterKindEnum.All
          : tanagra.GroupSectionFilterKindEnum.Any,
      excluded: section.excluded,
    },
    groups: section.criteriaGroups.map((group) => ({
      id: group.id,
      entity: group.entity,
      criteria: group.criteria.map((criteria) => fromAPICriteria(criteria)),
    })),
  }));
}

function fromAPICriteria(criteria: tanagra.CriteriaV2): tanagra.Criteria {
  return {
    id: criteria.id,
    type: criteria.pluginName,
    data: JSON.parse(criteria.selectionData),
    config: JSON.parse(criteria.uiConfig),
  };
}

function fromAPIConceptSet(
  conceptSet: tanagra.ConceptSetV2
): tanagra.ConceptSet {
  return {
    id: conceptSet.id,
    underlayName: conceptSet.underlayName,
    criteria: conceptSet.criteria && fromAPICriteria(conceptSet.criteria),
  };
}

function toAPICriteriaGroupSections(
  groupSections: tanagra.GroupSection[]
): tanagra.CriteriaGroupSectionV3[] {
  return groupSections.map((section) => ({
    id: section.id,
    displayName: section.name ?? "",
    operator:
      section.filter.kind === tanagra.GroupSectionFilterKindEnum.All
        ? tanagra.CriteriaGroupSectionV3OperatorEnum.And
        : tanagra.CriteriaGroupSectionV3OperatorEnum.Or,
    excluded: section.filter.excluded,
    criteriaGroups: section.groups.map((group) => ({
      id: group.id,
      displayName: "",
      entity: group.entity,
      criteria: group.criteria.map((criteria) => toAPICriteria(criteria)),
    })),
  }));
}

function toAPICriteria(criteria: tanagra.Criteria): tanagra.CriteriaV2 {
  return {
    id: criteria.id,
    displayName: "",
    tags: {},
    pluginName: criteria.type,
    selectionData: JSON.stringify(criteria.data),
    uiConfig: JSON.stringify(criteria.config),
  };
}

function fromAPICohortReview(review: tanagra.ReviewV2): CohortReview {
  if (!review.cohort) {
    throw new Error(`Undefined cohort for review "${review.displayName}".`);
  }

  return {
    id: review.id,
    displayName: review.displayName,
    description: review.description,
    size: review.size,
    cohort: fromAPICohort(review.cohort),
    created: review.created,
  };
}

function fromAPIReviewInstance(
  reviewId: string,
  primaryKey: string,
  instance: tanagra.ReviewInstanceV2
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
  value: tanagra.AnnotationValueV2
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

function toAPIAnnotationType(
  annotationType: AnnotationType
): tanagra.DataTypeV2 {
  switch (annotationType) {
    case AnnotationType.String:
      return tanagra.DataTypeV2.String;
  }

  throw new Error(`Unhandled annotation type ${annotationType}.`);
}

function fromAPIAnnotation(annotation: tanagra.AnnotationV2): Annotation {
  return {
    id: annotation.id,
    displayName: annotation.displayName,
    annotationType: fromAPIAnnotationType(annotation.dataType),
    enumVals: annotation.enumVals,
  };
}

function fromAPIDisplayHint(hint?: tanagra.DisplayHintV2): HintData {
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
        };
      })
      ?.filter(isValid),
  };
}

function toAPIBinaryOperator(
  operator: tanagra.ComparisonOperator
): tanagra.BinaryOperatorV2 {
  switch (operator) {
    case tanagra.ComparisonOperator.Equal:
      return tanagra.BinaryOperatorV2.Equals;
    case tanagra.ComparisonOperator.GreaterThanEqual:
      return tanagra.BinaryOperatorV2.GreaterThanOrEqual;
    case tanagra.ComparisonOperator.LessThanEqual:
      return tanagra.BinaryOperatorV2.LessThanOrEqual;
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

function processStudy(study: tanagra.StudyV2): Study {
  const properties: PropertyMap = {};
  study.properties?.forEach(({ key, value }) => (properties[key] = value));

  return {
    id: study.id,
    displayName: study.displayName ?? "Untitled study",
    created: study.created,
    properties,
  };
}
