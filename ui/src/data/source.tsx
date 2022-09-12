import { EntityCountsApiContext, EntityInstancesApiContext } from "apiContext";
import { useUnderlay } from "hooks";
import { useContext, useMemo } from "react";
import * as tanagra from "tanagra-api";
import { Underlay } from "underlaysSlice";
import { isValid } from "util/valid";
import {
  Classification,
  Configuration,
  DataEntry,
  DataKey,
  DataValue,
  findByID,
  findEntity,
  Grouping,
  Occurrence,
} from "./configuration";
import {
  ArrayFilter,
  Filter,
  isArrayFilter,
  isAttributeFilter,
  isClassificationFilter,
  isUnaryFilter,
} from "./filter";

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

  generateSQLQuery(
    requestedAttributes: string[],
    occurrenceID: string,
    cohort: Filter,
    conceptSet: Filter | null
  ): Promise<string>;

  getHintData(
    occurrenceID: string,
    attributeID: string
  ): Promise<HintData | undefined>;

  filterCount(
    filter: Filter | null,
    groupByAttributes?: string[],
    additionalAttributes?: string[]
  ): Promise<FilterCountValue[]>;
}

// TODO(tjennison): Create the source once and put it into the context instead
// of recreating it. Move "fake" logic into a separate source instead of APIs.
export function useSource(): Source {
  const underlay = useUnderlay();
  const instancesApi = useContext(
    EntityInstancesApiContext
  ) as tanagra.EntityInstancesApi;
  const countsApi = useContext(
    EntityCountsApiContext
  ) as tanagra.EntityCountsApi;
  return useMemo(
    () =>
      new BackendSource(
        instancesApi,
        countsApi,
        underlay,
        underlay.uiConfiguration.dataConfig
      ),
    [underlay]
  );
}

export class BackendSource implements Source {
  constructor(
    private entityInstancesApi: tanagra.EntityInstancesApi,
    private entityCountsApi: tanagra.EntityCountsApi,
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

    const ra = [...requestedAttributes];
    if (classification?.hierarchical) {
      ra.push(
        makePathAttribute(classification.entityAttribute),
        makeNumChildrenAttribute(classification.entityAttribute)
      );
    }

    const query = !options?.parent ? options?.query || "" : undefined;
    const promises = [
      this.entityInstancesApi.searchEntityInstances(
        searchRequest(
          ra,
          this.underlay.name,
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
          this.entityInstancesApi.searchEntityInstances(
            searchRequest(
              ra,
              this.underlay.name,
              classification,
              grouping,
              query,
              options?.parent
            )
          )
        ) || [])
      );
    }

    return Promise.all(promises).then((res) => {
      const result: SearchClassificationResult = { nodes: [] };
      res?.forEach((r, i) => {
        result.nodes.push(
          ...processEntitiesResponse(
            classification.entityAttribute,
            r,
            i > 0 ? classification.groupings?.[i - 1].id : undefined
          )
        );
      });
      return result;
    });
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

    return this.entityInstancesApi
      .searchEntityInstances({
        entityName: classification.entity,
        underlayName: this.underlay.name,
        searchEntityInstancesRequest: {
          entityDataset: {
            entityVariable: classification.entity,
            selectedAttributes: requestedAttributes,
            limit: 100,
            filter: {
              relationshipFilter: {
                outerVariable: classification.entity,
                newVariable: grouping.entity,
                newEntity: grouping.entity,
                filter: {
                  binaryFilter: {
                    attributeVariable: {
                      variable: grouping.entity,
                      name: classification.entityAttribute,
                    },
                    operator: tanagra.BinaryFilterOperator.Equals,
                    attributeValue: attributeValueFromDataValue(root.data.key),
                  },
                },
              },
            },
          },
        },
      })
      .then((res) => ({
        nodes: processEntitiesResponse(classification.entityAttribute, res),
      }));
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

    const res = await this.entityInstancesApi.searchEntityInstances({
      entityName: entity.entity,
      underlayName: this.underlay.name,
      searchEntityInstancesRequest: {
        entityDataset: this.makeEntityDataset(
          requestedAttributes,
          occurrenceID,
          cohort,
          conceptSet
        ),
      },
    });

    const data = res.instances?.map((instance) =>
      processEntityInstance(entity.key, instance, (key: string) =>
        isInternalAttribute(key)
      )
    );
    return {
      data: data ?? [],
    };
  }

  async generateSQLQuery(
    requestedAttributes: string[],
    occurrenceID: string,
    cohort: Filter,
    conceptSet: Filter | null
  ): Promise<string> {
    const entity = findEntity(occurrenceID, this.config);

    const res = await this.entityInstancesApi.generateDatasetSqlQuery({
      entityName: entity.entity,
      underlayName: this.underlay.name,
      generateDatasetSqlQueryRequest: {
        entityDataset: this.makeEntityDataset(
          requestedAttributes,
          occurrenceID,
          cohort,
          conceptSet
        ),
      },
    });

    if (!res.query) {
      throw new Error("Service returned an empty query.");
    }
    return res.query;
  }

  getHintData(
    occurrenceID: string,
    attributeID: string
  ): Promise<HintData | undefined> {
    return new Promise((resolve) => {
      if (occurrenceID) {
        resolve(undefined);
      }

      const attributeHint = this.underlay.entities
        .find((e) => e.name === this.config.primaryEntity.entity)
        ?.attributes?.find(
          (attribute) => attribute.name === attributeID
        )?.attributeFilterHint;

      resolve({
        integerHint: attributeHint?.integerBoundsHint
          ? {
              min: attributeHint?.integerBoundsHint.min ?? 0,
              max: attributeHint?.integerBoundsHint.max ?? 10000,
            }
          : undefined,
        enumHintOptions: attributeHint?.enumHint?.enumHintValues?.map(
          (hint) => ({
            value: dataValueFromAttributeValue(hint.attributeValue),
            name: hint.displayName ?? "Unknown Value",
          })
        ),
      });
    });
  }

  async filterCount(
    filter: Filter | null,
    groupByAttributes?: string[],
    additionalAttributes?: string[]
  ): Promise<FilterCountValue[]> {
    const data = await this.entityCountsApi.searchEntityCounts({
      underlayName: this.underlay.name,
      entityName: this.config.primaryEntity.entity,
      searchEntityCountsRequest: {
        entityCounts: {
          entityVariable: this.config.primaryEntity.entity,
          additionalSelectedAttributes: additionalAttributes,
          groupByAttributes: groupByAttributes,
          filter: generateFilter(this, filter, true),
        },
      },
    });

    if (!data.counts) {
      throw new Error("Count API returned no counts.");
    }

    return data.counts.map((count) => {
      const value: FilterCountValue = {
        count: count.count ?? 0,
      };
      for (const attribute in count.definition) {
        value[attribute] = dataValueFromAttributeValue(
          count.definition[attribute]
        );
      }
      return value;
    });
  }

  private makeEntityDataset(
    requestedAttributes: string[],
    occurrenceID: string,
    cohort: Filter,
    conceptSet: Filter | null
  ): tanagra.EntityDataset {
    const entity = findEntity(occurrenceID, this.config);
    let cohortFilter = generateFilter(this, cohort, true);
    if (!cohortFilter) {
      throw new Error("Cohort filter is empty.");
    }

    if (occurrenceID) {
      const primaryEntity = this.config.primaryEntity.entity;
      cohortFilter = {
        relationshipFilter: {
          outerVariable: entity.entity,
          newVariable: primaryEntity,
          newEntity: primaryEntity,
          filter: cohortFilter,
        },
      };
    }

    let filter = cohortFilter;
    const conceptSetFilter = generateFilter(this, conceptSet, false);
    if (conceptSetFilter) {
      filter = {
        arrayFilter: {
          operator: tanagra.ArrayFilterOperator.And,
          operands: [cohortFilter, conceptSetFilter],
        },
      };
    }

    return {
      entityVariable: entity.entity,
      selectedAttributes: requestedAttributes,
      filter,
      limit: 50,
    };
  }
}

function isInternalAttribute(attribute: string): boolean {
  return attribute.startsWith("t_");
}

function makePathAttribute(attribute: string) {
  return "t_path_" + attribute;
}

function makeNumChildrenAttribute(attribute: string) {
  return "t_numChildren_" + attribute;
}

function attributeValueFromDataValue(value: DataValue): tanagra.AttributeValue {
  return {
    int64Val: typeof value === "number" ? value : undefined,
    stringVal: typeof value === "string" ? value : undefined,
    boolVal: typeof value === "boolean" ? value : undefined,
  };
}

function dataValueFromAttributeValue(
  value?: tanagra.AttributeValue | null
): DataValue {
  return value?.int64Val ?? value?.stringVal ?? value?.boolVal ?? -1;
}

function searchRequest(
  requestedAttributes: string[],
  underlay: string,
  classification: Classification,
  grouping?: Grouping,
  query?: string,
  parent?: DataValue
) {
  // TODO(tjennison): Make brands support the same columns as ingredients since
  // they're displayed in the same table instead of trying to route separate
  // attributes.
  if (grouping?.id === "brand") {
    requestedAttributes = [
      "concept_name",
      "concept_id",
      "standard_concept",
      "concept_code",
    ];
  }

  const operands: tanagra.Filter[] = [];
  if (classification.filter && !grouping) {
    operands.push(classification.filter);
  }

  const entity = grouping?.entity || classification.entity;

  if (parent) {
    operands.push({
      binaryFilter: {
        attributeVariable: {
          name: classification.entityAttribute,
          variable: entity,
        },
        operator: tanagra.BinaryFilterOperator.ChildOf,
        attributeValue: attributeValueFromDataValue(parent),
      },
    });
  } else if (query) {
    operands.push({
      textSearchFilter: {
        entityVariable: entity,
        term: query,
      },
    });
  } else if (classification?.hierarchical && !grouping) {
    operands.push({
      binaryFilter: {
        attributeVariable: {
          name: makePathAttribute(classification.entityAttribute),
          variable: entity,
        },
        operator: tanagra.BinaryFilterOperator.Equals,
        attributeValue: {
          stringVal: "",
        },
      },
    });
  }

  let filter: tanagra.Filter | undefined;
  if (operands.length > 0) {
    filter = {
      arrayFilter: {
        operands,
        operator: tanagra.ArrayFilterOperator.And,
      },
    };
  }

  const orderByAttribute =
    grouping?.defaultSort?.attribute ?? classification.defaultSort?.attribute;
  const orderByDirection =
    grouping?.defaultSort?.direction ?? classification.defaultSort?.direction;

  const req = {
    entityName: entity,
    underlayName: underlay,
    searchEntityInstancesRequest: {
      entityDataset: {
        entityVariable: entity,
        selectedAttributes: requestedAttributes,
        limit: 100,
        orderByAttribute: orderByAttribute,
        orderByDirection: orderByDirection as
          | tanagra.OrderByDirection
          | undefined,
        filter: filter,
      },
    },
  };
  return req;
}

function processEntitiesResponse(
  attributeID: string,
  response: tanagra.SearchEntityInstancesResponse,
  grouping?: string
): ClassificationNode[] {
  const pathAttribute = makePathAttribute(attributeID);
  const numChildrenAttribute = makeNumChildrenAttribute(attributeID);

  const nodes: ClassificationNode[] = [];
  if (response.instances) {
    response.instances.forEach((instance) => {
      let ancestors: DataKey[] | undefined;
      let childCount: number | undefined;

      const data = processEntityInstance(
        attributeID,
        instance,
        (key, value) => {
          if (key === pathAttribute) {
            if (value?.stringVal) {
              // TODO(tjennison): There's no way to tell if the IDs should
              // be numbers or strings and getting it wrong will cause
              // queries using them to fail because the types don't match.
              // Figure out a way to handle this generically.
              ancestors = value.stringVal.split(".").map((id) => +id);
            } else if (value?.stringVal === "") {
              ancestors = [];
            }
          } else if (key === numChildrenAttribute) {
            childCount = value?.int64Val;
          } else {
            return false;
          }
          return true;
        }
      );

      nodes.push({
        data: data,
        grouping: grouping,
        ancestors: ancestors,
        childCount: childCount,
      });
    });
  }
  return nodes;
}

type EntityInstance = { [key: string]: tanagra.AttributeValue | null };

function processEntityInstance(
  attributeID: string,
  instance: EntityInstance,
  attributeHandler: (
    key: string,
    value: tanagra.AttributeValue | null
  ) => boolean
): DataEntry {
  const data: DataEntry = {
    key: 0,
  };

  for (const k in instance) {
    const v = instance[k];
    // TODO(tjennison): Add support for computed columns.
    if (k === "standard_concept") {
      data[k] = v ? "Standard" : "Source";
    } else if (attributeHandler(k, v)) {
      continue;
    } else if (!v) {
      data[k] = "";
    } else if (isValid(v.int64Val)) {
      data[k] = v.int64Val;
    } else if (isValid(v.boolVal)) {
      data[k] = v.boolVal;
    } else if (isValid(v.stringVal)) {
      data[k] = v.stringVal;
    }
  }

  const key = data[attributeID];
  if (!key || typeof key === "boolean") {
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
): tanagra.Filter | null {
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

    return {
      arrayFilter: {
        operator: arrayFilterOperator(filter),
        operands: operands,
      },
    };
  }
  if (isUnaryFilter(filter)) {
    const operand = generateFilter(source, filter.operand, fromPrimary);
    if (!operand) {
      return null;
    }

    return {
      unaryFilter: {
        operator: tanagra.UnaryFilterOperator.Not,
        operand: operand,
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
      relationshipFilter: {
        outerVariable: source.config.primaryEntity.entity,
        newVariable: entity,
        newEntity: entity,
        filter: occurrenceFilter,
      },
    };
  }
  return occurrenceFilter;
}

function arrayFilterOperator(filter: ArrayFilter): tanagra.ArrayFilterOperator {
  if (!isValid(filter.operator.min) && !isValid(filter.operator.max)) {
    return tanagra.ArrayFilterOperator.And;
  }
  if (filter.operator.min === 1 && !isValid(filter.operator.max)) {
    return tanagra.ArrayFilterOperator.Or;
  }

  throw new Error("Only AND and OR equivalent operators are supported.");
}

function generateOccurrenceFilter(
  source: Source,
  filter: Filter
): [tanagra.Filter | null, string] {
  if (isClassificationFilter(filter)) {
    const entity = findEntity(filter.occurrenceID, source.config);
    const classification = findByID(
      filter.classificationID,
      entity.classifications
    );

    const operands = filter.keys.map((key) => ({
      binaryFilter: {
        attributeVariable: {
          variable: classification.entity,
          name: classification.entityAttribute,
        },
        operator: classification.hierarchical
          ? tanagra.BinaryFilterOperator.DescendantOfInclusive
          : tanagra.BinaryFilterOperator.Equals,
        attributeValue: {
          // TODO(tjennison): Handle other key types.
          int64Val: key as number,
        },
      },
    }));

    return [
      {
        relationshipFilter: {
          outerVariable: entity.entity,
          newVariable: classification.entity,
          newEntity: classification.entity,
          filter:
            operands.length > 0
              ? {
                  arrayFilter: {
                    operands: operands,
                    operator: tanagra.ArrayFilterOperator.Or,
                  },
                }
              : undefined,
        },
      },
      entity.entity,
    ];
  }
  if (isAttributeFilter(filter)) {
    const entity = findEntity(filter.occurrenceID, source.config);
    if (filter.ranges?.length) {
      return [
        {
          arrayFilter: {
            operands: filter.ranges.map(({ min, max }) => ({
              arrayFilter: {
                operands: [
                  {
                    binaryFilter: {
                      attributeVariable: {
                        variable: entity.entity,
                        name: filter.attribute,
                      },
                      operator: tanagra.BinaryFilterOperator.LessThan,
                      attributeValue: {
                        int64Val: max,
                      },
                    },
                  },
                  {
                    binaryFilter: {
                      attributeVariable: {
                        variable: entity.entity,
                        name: filter.attribute,
                      },
                      operator: tanagra.BinaryFilterOperator.GreaterThan,
                      attributeValue: {
                        int64Val: min,
                      },
                    },
                  },
                ],
                operator: tanagra.ArrayFilterOperator.And,
              },
            })),
            operator: tanagra.ArrayFilterOperator.Or,
          },
        },
        entity.entity,
      ];
    }
    if (filter.values?.length) {
      return [
        {
          arrayFilter: {
            operands: filter.values.map((value) => ({
              binaryFilter: {
                attributeVariable: {
                  variable: entity.entity,
                  name: filter.attribute,
                },
                operator: tanagra.BinaryFilterOperator.Equals,
                attributeValue: attributeValueFromDataValue(value),
              },
            })),
            operator: tanagra.ArrayFilterOperator.Or,
          },
        },
        entity.entity,
      ];
    }
    return [null, ""];
  }
  throw new Error(`Unknown filter type: ${JSON.stringify(filter)}`);
}
