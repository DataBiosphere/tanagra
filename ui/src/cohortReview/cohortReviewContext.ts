import { DataEntry } from "data/types";
import { createContext, useContext } from "react";

export type EntityData = {
  [x: string]: DataEntry[];
};

export type CohortReviewContextData = {
  rows: EntityData;

  searchState: <T extends object>(plugin: string) => T;
  updateSearchState: <T extends object>(
    plugin: string,
    fn: (state: T) => void
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
