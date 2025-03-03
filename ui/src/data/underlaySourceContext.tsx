import { UnderlaySource } from "data/source";
import { createContext, useContext } from "react";

type UnderlaySourceContextData = {
  source: UnderlaySource;
};

export const UnderlaySourceContext =
  createContext<UnderlaySourceContextData | null>(null);

export function useUnderlaySource() {
  const context = useContext(UnderlaySourceContext);
  if (!context) {
    throw new Error(
      "Attempting to use underlay source context when not provided."
    );
  }
  return context.source;
}
