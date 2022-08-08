import { EntityInstancesApiContext } from "apiContext";
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
  Grouping,
  Occurrence,
} from "./configuration";

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

export interface Source {
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

  getHintData(
    occurrenceID: string,
    attributeID: string
  ): Promise<HintData | undefined>;
}

// TODO(tjennison): Create the source once and put it into the context instead
// of recreating it. Move "fake" logic into a separate source instead of APIs.
export function useSource(): Source {
  const underlay = useUnderlay();
  const context = useContext(
    EntityInstancesApiContext
  ) as tanagra.EntityInstancesApi;
  return useMemo(
    () =>
      new BackendSource(context, underlay, underlay.uiConfiguration.dataConfig),
    [underlay]
  );
}

export class BackendSource implements Source {
  constructor(
    private entityInstancesApi: tanagra.EntityInstancesApi,
    private underlay: Underlay,
    private config: Configuration
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
}

function makePathAttribute(attribute: string) {
  return "t_path_" + attribute;
}

function makeNumChildrenAttribute(attribute: string) {
  return "t_numChildren_" + attribute;
}

// TODO(tjennison): Remove external uses of this function since they're not
// actually generic.
export function attributeValueFromDataValue(
  value: DataValue
): tanagra.AttributeValue {
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
    // TODO(tjennison): Use server side limits.
    response.instances.slice(0, 100).forEach((instance) => {
      let ancestors: DataKey[] | undefined;
      let childCount: number | undefined;
      const data: DataEntry = {
        key: 0,
      };

      for (const k in instance) {
        const v = instance[k];
        // TODO(tjennison): Add support for computed columns.
        if (k === "standard_concept") {
          data[k] = v ? "Standard" : "Source";
        } else if (!v) {
          data[k] = "";
        } else if (k === pathAttribute) {
          if (v.stringVal) {
            // TODO(tjennison): There's no way to tell if the IDs should
            // be numbers or strings and getting it wrong will cause
            // queries using them to fail because the types don't match.
            // Figure out a way to handle this generically.
            ancestors = v.stringVal.split(".").map((id) => +id);
          } else if (v.stringVal === "") {
            ancestors = [];
          }
        } else if (k === numChildrenAttribute) {
          childCount = v.int64Val;
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
        return;
      }
      data.key = key;

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
