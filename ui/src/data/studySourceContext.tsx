import { StudySource } from "data/source";
import { createContext, useContext } from "react";

type StudySourceContextData = {
  source: StudySource;
};

export const StudySourceContext = createContext<StudySourceContextData | null>(
  null
);

export function useStudySource() {
  const context = useContext(StudySourceContext);
  if (!context) {
    throw new Error(
      "Attempting to use study source context when not provided."
    );
  }
  return context.source;
}
