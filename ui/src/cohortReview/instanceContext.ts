import { DataEntry } from "data/types";
import { createContext, useContext } from "react";

export type OccurrenceData = {
  [x: string]: DataEntry[];
};

export type InstanceContextData = {
  occurrences: OccurrenceData;
};

export const InstanceContext = createContext<InstanceContextData | undefined>(
  undefined
);

export function useInstanceContext() {
  const context = useContext(InstanceContext);
  if (!context) {
    throw new Error("Attempting to use instance context when not provided.");
  }
  return context;
}
