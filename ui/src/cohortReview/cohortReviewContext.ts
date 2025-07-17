import { DataEntry } from "data/types";
import { createContext, useContext } from "react";

export type EntityData = {
  [x: string]: DataEntry[];
};

export type CohortReviewContextData = {
  rows: EntityData;
  totalCounts: { [x: string]: number };

  size: number;
  setSize: (
    size: number | ((_size: number) => number)
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
  ) => Promise<any[] | undefined>;

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
