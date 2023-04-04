import { DataEntry } from "data/types";
import { createContext, useContext } from "react";

export type OccurrenceData = {
  [x: string]: DataEntry[];
};

export type CohortReviewContextData = {
  occurrences: OccurrenceData;

  searchData: <T extends object>(plugin: string) => T;
  updateSearchData: <T extends object>(
    plugin: string,
    fn: (data: T) => void
  ) => void;
};

export const CohortReviewContext = createContext<
  CohortReviewContextData | undefined
>(undefined);

export function useCohortReviewContext() {
  const context = useContext(CohortReviewContext);
  if (!context) {
    throw new Error("Attempting to use instance context when not provided.");
  }
  return context;
}
