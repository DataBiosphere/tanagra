import * as tanagraUI from "tanagra-ui";

export type DataKey = string | number;
export type DataValue = null | string | number | boolean | Date;

export type DataEntry = {
  key: DataKey;
  [x: string]: DataValue;
};

export type CohortReview = {
  id: string;
  displayName: string;
  description?: string;
  size: number;
  cohort: tanagraUI.UICohort;
  created: Date;
  createdBy: string;
  lastModified: Date;
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
    return v.toDateString();
  }
  return String(v);
}
