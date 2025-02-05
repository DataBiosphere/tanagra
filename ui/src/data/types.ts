import * as tanagraUnderlay from "tanagra-underlay/underlayConfig";
import { standardDateString } from "util/date";

export type DataKey = string | number | bigint;
export type DataValue = null | string | number | bigint | boolean | Date;

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

export function convertAttributeStringToDataValue(
  value: string,
  attribute: tanagraUnderlay.SZAttribute
): DataValue {
  switch (attribute.dataType) {
    case tanagraUnderlay.SZDataType.INT64:
      return BigInt(value);
    case tanagraUnderlay.SZDataType.DOUBLE:
      return Number(value);
    case tanagraUnderlay.SZDataType.BOOLEAN:
      return (
        value.localeCompare("true", undefined, { sensitivity: "base" }) === 0
      );
    case tanagraUnderlay.SZDataType.DATE:
    case tanagraUnderlay.SZDataType.TIMESTAMP:
      return new Date(value);
    case tanagraUnderlay.SZDataType.STRING:
      return value;
  }

  throw new Error(
    `Unknown data type "${attribute.dataType}" in ${JSON.stringify(attribute)}.`
  );
}
