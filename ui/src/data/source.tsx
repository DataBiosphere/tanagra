import { EntityInstancesApiContext } from "apiContext";
import { useUnderlay } from "hooks";
import { useContext, useMemo } from "react";
import * as tanagra from "tanagra-api";
import { isValid } from "util/valid";
import {
  Classification,
  Configuration,
  DataEntry,
  DataKey,
  DataValue,
  findByID,
  Grouping,
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
};

export type SearchClassificationResult = {
  nodes: ClassificationNode[];
};

export interface Source {
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
      new BackendSource(
        context,
        underlay.name,
        underlay.uiConfiguration.dataConfig
      ),
    [underlay]
  );
}

export class BackendSource implements Source {
  constructor(
    private entityInstancesApi: tanagra.EntityInstancesApi,
    private underlay: string,
    private config: Configuration
  ) {}

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
    return Promise.all([
      this.entityInstancesApi.searchEntityInstances(
        searchRequest(
          ra,
          this.underlay,
          classification,
          undefined,
          query,
          options?.parent
        )
      ),
      ...(classification.groupings?.map((grouping) =>
        this.entityInstancesApi.searchEntityInstances(
          searchRequest(
            ra,
            this.underlay,
            classification,
            grouping,
            query,
            options?.parent
          )
        )
      ) || []),
    ]).then((res) => {
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
        underlayName: this.underlay,
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
}

function makePathAttribute(attribute: string) {
  return "t_path_" + attribute;
}

function makeNumChildrenAttribute(attribute: string) {
  return "t_numChildren_" + attribute;
}

function attributeValueFromDataValue(value: DataValue): tanagra.AttributeValue {
  switch (typeof value) {
    case "string":
      return {
        stringVal: value,
      };
    case "number":
      return {
        int64Val: value,
      };
    case "boolean":
      return {
        boolVal: value,
      };
  }
  throw new Error(`Unknown data value type ${typeof value}.`);
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
