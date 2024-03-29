import { isValid } from "util/valid";
import { DataKey, DataValue, GroupByCount } from "./types";

export enum FilterType {
  Unary = "UNARY",
  Array = "ARRAY",
  EntityGroup = "ENTITY_GROUP",
  Attribute = "ATTRIBUTE",
  Text = "TEXT",
  Relationship = "RELATIONSHIP",
}

type BaseFilter = {
  type: FilterType;
};

export enum UnaryFilterOperator {
  Not,
}

export type UnaryFilter = BaseFilter & {
  operator: UnaryFilterOperator;
  operand: Filter;
};

export function isUnaryFilter(filter: Filter): filter is UnaryFilter {
  return filter.type == FilterType.Unary;
}

export type ArrayFilterOperator = {
  // Currently only AND (all) and OR (1+) equivalent values are supported but
  // this could allow for various XOR and x-of-y type operations in the future.
  // As a convenience, both min and max being undefined is equivalent to an AND.
  min?: number;
  max?: number;
};

export function isArrayFilter(filter: Filter): filter is ArrayFilter {
  return filter.type == FilterType.Array;
}

export type ArrayFilter = BaseFilter & {
  operator: ArrayFilterOperator;
  operands: Filter[];
};

export function makeArrayFilter(
  operator: ArrayFilterOperator,
  filters: (Filter | null)[]
): ArrayFilter | null {
  const operands = filters.filter(isValid);
  if (operands.length === 0) {
    return null;
  }
  return {
    type: FilterType.Array,
    operator,
    operands,
  };
}

export type AttributeFilter = BaseFilter & {
  attribute: string;
  values?: DataValue[];
  ranges?: { min: number; max: number }[];
  nonNull?: boolean;
};

export function isAttributeFilter(filter: Filter): filter is AttributeFilter {
  return filter.type == FilterType.Attribute;
}

export type TextFilter = BaseFilter & {
  text: string;
  attribute?: string;
};

export function isTextFilter(filter: Filter): filter is TextFilter {
  return filter.type == FilterType.Text;
}

export type RelationshipFilter = BaseFilter & {
  entityId: string;
  subfilter: Filter;
  groupByCount?: GroupByCount;
};

export function isRelationshipFilter(
  filter: Filter
): filter is RelationshipFilter {
  return filter.type == FilterType.Relationship;
}

export type EntityGroupFilter = BaseFilter & {
  entityGroupId: string;
  entityId: string;
  keys: DataKey[];
};

export function isEntityGroupFilter(
  filter: Filter
): filter is EntityGroupFilter {
  return filter.type == FilterType.EntityGroup;
}

export type Filter =
  | UnaryFilter
  | ArrayFilter
  | AttributeFilter
  | EntityGroupFilter
  | TextFilter
  | RelationshipFilter;
