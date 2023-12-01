import {
  AnnotationsApiContext,
  CohortsApiContext,
  ConceptSetsApiContext,
  ReviewsApiContext,
  StudiesApiContext,
  UsersApiContext,
} from "apiContext";
import { BackendStudySource, StudySource } from "data/source";
import { createContext, useContext, useMemo } from "react";
import { Outlet } from "react-router-dom";
import * as tanagra from "tanagra-api";

type StudySourceContextData = {
  source: StudySource;
};

const StudySourceContext = createContext<StudySourceContextData | null>(null);

export function useStudySource() {
  const context = useContext(StudySourceContext);
  if (!context) {
    throw new Error(
      "Attempting to use study source context when not provided."
    );
  }
  return context.source;
}

export function StudySourceContextRoot() {
  // TODO(tjennison): Move "fake" logic into a separate source instead of APIs.
  const studiesApi = useContext(StudiesApiContext) as tanagra.StudiesApi;
  const cohortsApi = useContext(CohortsApiContext) as tanagra.CohortsApi;
  const conceptSetsApi = useContext(
    ConceptSetsApiContext
  ) as tanagra.ConceptSetsApi;
  const reviewsApi = useContext(ReviewsApiContext) as tanagra.ReviewsApi;
  const annotationsApi = useContext(
    AnnotationsApiContext
  ) as tanagra.AnnotationsApi;
  const usersApi = useContext(UsersApiContext) as tanagra.UsersApi;

  const context = useMemo(() => {
    return {
      source: new BackendStudySource(
        studiesApi,
        cohortsApi,
        conceptSetsApi,
        reviewsApi,
        annotationsApi,
        usersApi
      ),
    };
  }, []);

  return (
    <StudySourceContext.Provider value={context}>
      <Outlet />
    </StudySourceContext.Provider>
  );
}
