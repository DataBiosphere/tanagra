import * as tanagra from "tanagra-api";
export type DataKey = string | number;
export type DataValue = string | number | boolean | Date;

export type DataEntry = {
  key: DataKey;
  [x: string]: DataValue;
};

export type CohortReview = {
  id: string;
  displayName: string;
  description?: string;
  size: number;
  cohort: tanagra.Cohort;
  created: Date;
};
