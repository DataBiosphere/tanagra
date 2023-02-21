import {
  CohortsApiContext,
  ConceptSetsApiContext,
  EntityInstancesApiContext,
  HintsApiContext,
  ReviewsApiContext,
  StudiesApiContext,
} from "apiContext";
import { defaultGroup, generateCohortFilter, getCriteriaPlugin } from "cohort";
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
    conceptSet: Filter | null
  ): Promise<ListDataResponse>;

  getHintData(
    occurrenceID: string,
    attributeID: string
  ): Promise<HintData | undefined>;

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
  return useMemo(
    () =>
      new BackendSource(
        instancesApi,
        hintsApi,
        studiesApi,
        cohortsApi,
        conceptSetsApi,
        reviewsApi,
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
    private underlay: Underlay,
    public config: Configuration
  ) {}

  lookupOccurrence(occurrenceID: string): Occurrence {
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
    const occurrence = findByID(occurrenceID, this.config.occurrences);
    const classification = findByID(
      classificationID,
      occurrence.classifications
    );

    const query = !options?.parent ? options?.query || "" : undefined;

    const promises = [
      this.instancesApi.queryInstances(
        searchRequest(
          requestedAttributes,
          this.underlay,
          classification,
          undefined,
          query,
          options?.parent
        )
      ),
    ];

    if (options?.includeGroupings) {
      promises.push(
        ...(classification.groupings?.map((grouping) =>
          this.instancesApi.queryInstances(
            searchRequest(
              requestedAttributes,
              this.underlay,
              classification,
              grouping,
              query,
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
        .queryInstances({
          entityName: classification.entity,
          underlayName: this.underlay.name,
          queryV2: {
            includeAttributes:
              normalizeRequestedAttributes(requestedAttributes),
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
                        operator: tanagra.AttributeFilterV2OperatorEnum.Equals,
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
    conceptSet: Filter | null
  ): Promise<ListDataResponse> {
    const entity = findEntity(occurrenceID, this.config);

    const res = await parseAPIError(
      this.instancesApi.queryInstances({
        entityName: entity.entity,
        underlayName: this.underlay.name,
        queryV2: this.makeQuery(
          requestedAttributes,
          occurrenceID,
          cohort,
          conceptSet
        ),
      })
    );

    const data = res.instances?.map((instance) =>
      processEntityInstance(entity.key, instance)
    );
    return {
      data: data ?? [],
      sql: res.sql ?? "",
    };
  }

  async getHintData(
    occurrenceID: string,
    attributeID: string
  ): Promise<HintData | undefined> {
    if (occurrenceID) {
      return undefined;
    }

    let entity = this.config.primaryEntity.entity;
    if (occurrenceID) {
      entity = findByID(occurrenceID, this.config.occurrences).entity;
    }

    const res = await parseAPIError(
      this.hintsApi.queryHints({
        entityName: entity,
        underlayName: this.underlay.name,
        hintQueryV2: {},
      })
    );

    const displayHint = res.displayHints?.find(
      (hint) => hint?.attribute?.name === attributeID
    )?.displayHint;

    return {
      integerHint: displayHint?.numericRangeHint
        ? {
            min: displayHint?.numericRangeHint.min ?? 0,
            max: displayHint?.numericRangeHint.max ?? 10000,
          }
        : undefined,
      enumHintOptions: displayHint?.enumHint?.enumHintValues
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
          filter: generateFilter(this, filter, true) ?? undefined,
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
            filter:
              generateFilter(this, generateCohortFilter(cohort), true) ?? {},
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
          criteriaGroups: toAPICriteriaGroups(cohort.groups),
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
              getCriteriaPlugin(criteria).occurrenceID(),
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

  private makeQuery(
    requestedAttributes: string[],
    occurrenceID: string,
    cohort: Filter,
    conceptSet: Filter | null
  ): tanagra.QueryV2 {
    let cohortFilter = generateFilter(this, cohort, true);
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
    const conceptSetFilter = generateFilter(this, conceptSet, false);
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
      limit: 50,
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
      // The en-CA locale uses the ISO 8601 standard YYY-MM-DD.
      dateVal:
        value instanceof Date ? value.toLocaleDateString("en-CA") : undefined,
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
        ? Date.parse(value.valueUnion.dateVal)
        : null;
    case tanagra.DataTypeV2.Boolean:
      return value.valueUnion?.boolVal ?? null;
  }

  throw new Error(`Unknown data type "${value?.dataType}".`);
}

function searchRequest(
  requestedAttributes: string[],
  underlay: Underlay,
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
  } else if (query) {
    operands.push({
      filterType: tanagra.FilterV2FilterTypeEnum.Text,
      filterUnion: {
        textFilter: {
          matchType: tanagra.TextFilterV2MatchTypeEnum.ExactMatch,
          text: query,
        },
      },
    });
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
        grouping?.attributes ?? requestedAttributes
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
      includeRelationshipFields: !grouping
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
  attributeID: string,
  response: tanagra.InstanceListV2,
  hierarchy?: string,
  grouping?: string
): ClassificationNode[] {
  const nodes: ClassificationNode[] = [];
  if (response.instances) {
    response.instances.forEach((instance) => {
      const data = processEntityInstance(attributeID, instance);

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
        if (fields.hierarchy === hierarchy && isValid(fields.count)) {
          data[ROLLUP_COUNT_ATTRIBUTE] = fields.count;
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

function processEntityInstance(
  attributeID: string,
  instance: tanagra.InstanceV2
): DataEntry {
  const data: DataEntry = {
    key: 0,
  };

  processAttributes(data, instance.attributes);

  const key = dataValueFromLiteral(
    instance.attributes?.[attributeID]?.value ?? null
  );
  if (typeof key !== "string" && typeof key !== "number") {
    throw new Error(
      `Key attribute "${attributeID}" not found in entity instance ${data}`
    );
  }
  data.key = key;
  return data;
}

// TODO(tjennison): Move this to BackendSource and make it private once the
// count API uses have been converted.
export function generateFilter(
  source: Source,
  filter: Filter | null,
  fromPrimary: boolean
): tanagra.FilterV2 | null {
  if (!filter) {
    return null;
  }

  if (isArrayFilter(filter)) {
    const operands = filter.operands
      .map((o) => generateFilter(source, o, fromPrimary))
      .filter(isValid);
    if (operands.length === 0) {
      return null;
    }

    return makeBooleanLogicFilter(arrayFilterOperator(filter), operands);
  }
  if (isUnaryFilter(filter)) {
    const operand = generateFilter(source, filter.operand, fromPrimary);
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

  // Cohort filters need to be related to the primary entity but concept sets
  // filters don't because they're tied to particular occurrences.
  const [occurrenceFilter, entity] = generateOccurrenceFilter(source, filter);
  if (
    occurrenceFilter &&
    fromPrimary &&
    entity !== source.config.primaryEntity.entity
  ) {
    return {
      filterType: tanagra.FilterV2FilterTypeEnum.Relationship,
      filterUnion: {
        relationshipFilter: {
          entity,
          subfilter: occurrenceFilter,
        },
      },
    };
  }
  return occurrenceFilter;
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

function generateOccurrenceFilter(
  source: Source,
  filter: Filter
): [tanagra.FilterV2 | null, string] {
  if (isClassificationFilter(filter)) {
    const entity = findEntity(filter.occurrenceID, source.config);
    const classification = findByID(
      filter.classificationID,
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
            operator: tanagra.AttributeFilterV2OperatorEnum.Equals,
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

    return [
      {
        filterType: tanagra.FilterV2FilterTypeEnum.Relationship,
        filterUnion: {
          relationshipFilter: {
            entity: classification.entity,
            subfilter,
          },
        },
      },
      entity.entity,
    ];
  }
  if (isAttributeFilter(filter)) {
    const entity = findEntity(filter.occurrenceID, source.config);
    if (filter.ranges?.length) {
      return [
        makeBooleanLogicFilter(
          tanagra.BooleanLogicFilterV2OperatorEnum.Or,
          filter.ranges.map(({ min, max }) =>
            makeBooleanLogicFilter(
              tanagra.BooleanLogicFilterV2OperatorEnum.And,
              [
                {
                  filterType: tanagra.FilterV2FilterTypeEnum.Attribute,
                  filterUnion: {
                    attributeFilter: {
                      attribute: filter.attribute,
                      operator: tanagra.AttributeFilterV2OperatorEnum.LessThan,
                      value: literalFromDataValue(max),
                    },
                  },
                },
                {
                  filterType: tanagra.FilterV2FilterTypeEnum.Attribute,
                  filterUnion: {
                    attributeFilter: {
                      attribute: filter.attribute,
                      operator:
                        tanagra.AttributeFilterV2OperatorEnum.GreaterThan,
                      value: literalFromDataValue(min),
                    },
                  },
                },
              ]
            )
          )
        ),
        entity.entity,
      ];
    }
    if (filter.values?.length) {
      return [
        makeBooleanLogicFilter(
          tanagra.BooleanLogicFilterV2OperatorEnum.Or,
          filter.values.map((value) => ({
            filterType: tanagra.FilterV2FilterTypeEnum.Attribute,
            filterUnion: {
              attributeFilter: {
                attribute: filter.attribute,
                operator: tanagra.AttributeFilterV2OperatorEnum.Equals,
                value: literalFromDataValue(value),
              },
            },
          }))
        ),
        entity.entity,
      ];
    }

    return [null, ""];
  }
  throw new Error(`Unknown filter type: ${JSON.stringify(filter)}`);
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
  } else {
    orderBy.attribute = sortAttribute;
  }

  return orderBy;
}

function normalizeRequestedAttributes(attributes: string[]) {
  return [
    ...new Set(
      attributes
        .filter((a) => a !== ROLLUP_COUNT_ATTRIBUTE)
        .map((a) => {
          const i = a.indexOf(VALUE_SUFFIX);
          if (i != -1) {
            return a.substring(0, i);
          }
          return a;
        })
    ),
  ];
}

function fromAPICohort(cohort: tanagra.CohortV2): tanagra.Cohort {
  return {
    id: cohort.id,
    name: cohort.displayName,
    underlayName: cohort.underlayName,
    groups: fromAPICohortGroups(cohort.criteriaGroups),
  };
}

function fromAPICohortGroups(
  groups: tanagra.CriteriaGroupV2[]
): tanagra.Group[] {
  if (groups.length === 0) {
    return [defaultGroup()];
  }

  return groups.map((group) => ({
    id: group.id,
    name: group.displayName,
    filter: {
      kind:
        group.operator === tanagra.CriteriaGroupV2OperatorEnum.And
          ? tanagra.GroupFilterKindEnum.All
          : tanagra.GroupFilterKindEnum.Any,
      excluded: group.excluded,
    },
    criteria: group.criteria.map((criteria) => fromAPICriteria(criteria)),
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

function toAPICohort(cohort: tanagra.Cohort): tanagra.CohortV2 {
  return {
    id: cohort.id,
    displayName: cohort.name,
    underlayName: cohort.underlayName,
    created: new Date(),
    createdBy: "",
    lastModified: new Date(),
    criteriaGroups: toAPICriteriaGroups(cohort.groups),
  };
}

function toAPICriteriaGroups(
  groups: tanagra.Group[]
): tanagra.CriteriaGroupV2[] {
  return groups.map((group) => ({
    id: group.id,
    displayName: group.name ?? "",
    operator:
      group.filter.kind === tanagra.GroupFilterKindEnum.All
        ? tanagra.CriteriaGroupV2OperatorEnum.And
        : tanagra.CriteriaGroupV2OperatorEnum.Or,
    excluded: group.filter.excluded,
    criteria: group.criteria.map((criteria) => toAPICriteria(criteria)),
  }));
}

function toAPICriteria(criteria: tanagra.Criteria): tanagra.CriteriaV2 {
  return {
    id: criteria.id,
    displayName: "",
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
