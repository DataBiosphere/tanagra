import {
  useAnnotationsApi,
  useCohortsApi,
  useConceptSetsApi,
  useReviewsApi,
  useStudiesApi,
  useUsersApi,
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
  const studiesApi = useStudiesApi() as tanagra.StudiesApi;
  const cohortsApi = useCohortsApi() as tanagra.CohortsApi;
  const conceptSetsApi = useConceptSetsApi() as tanagra.ConceptSetsApi;
  const reviewsApi = useReviewsApi() as tanagra.ReviewsApi;
  const annotationsApi = useAnnotationsApi() as tanagra.AnnotationsApi;
  const usersApi = useUsersApi() as tanagra.UsersApi;

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
