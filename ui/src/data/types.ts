import { standardDateString } from "util/date";
export type DataKey = string | number;
export type DataValue = null | string | number | boolean | Date;

export type DataEntry = {
  key: DataKey;
  [x: string]: DataValue;
};

export function compareDataValues(a?: DataValue, b?: DataValue) {
  if (a === b) {
    return 0;
  }
  if (!a) {
    return -1;
  }
  if (!b) {
    return 1;
  }

  if (a < b) {
    return -1;
  }
  if (a > b) {
    return 1;
  }
  return 0;
}

export function stringifyDataValue(v?: DataValue): string {
  if (v === null) {
    return "NULL";
  } else if (v === undefined) {
    return "";
  } else if (v instanceof Date) {
    return standardDateString(v);
  }
  return String(v);
}

export enum ComparisonOperator {
  Equal = "EQUAL",
  GreaterThanEqual = "LESS_THAN_EQUAL",
  LessThanEqual = "GREATER_THAN_EQUAL",
  Between = "BETWEEN",
}

export type GroupByCount = {
  attributes: string[];
  operator: ComparisonOperator;
  value: number;
};
