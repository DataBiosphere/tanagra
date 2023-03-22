import { isValid } from "util/valid";
import { DataKey, DataValue } from "./types";

export enum FilterType {
  Unary = "UNARY",
  Array = "ARRAY",
  Classification = "CLASSIFICATION",
  Attribute = "ATTRIBUTE",
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
  occurrenceID: string;
  attribute: string;
  values?: DataValue[];
  ranges?: { min: number; max: number }[];
};

export function isAttributeFilter(filter: Filter): filter is AttributeFilter {
  return filter.type == FilterType.Attribute;
}

export type ClassificationFilter = BaseFilter & {
  type?: FilterType;
  occurrenceID: string;
  classificationID: string;
  keys: DataKey[];
};

export function isClassificationFilter(
  filter: Filter
): filter is ClassificationFilter {
  return filter.type == FilterType.Classification;
}

export type Filter =
  | UnaryFilter
  | ArrayFilter
  | AttributeFilter
  | ClassificationFilter;
