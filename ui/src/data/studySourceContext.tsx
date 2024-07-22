import {
  getAccessToken,
  getAnnotationsApiContext,
  getCohortsApiContext,
  getConceptSetsApiContext,
  getReviewsApiContext,
  getStudiesApiContext,
  getUsersApiContext,
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
  const studiesApi = useContext(
    getStudiesApiContext(getAccessToken())
  ) as tanagra.StudiesApi;
  const cohortsApi = useContext(
    getCohortsApiContext(getAccessToken())
  ) as tanagra.CohortsApi;
  const conceptSetsApi = useContext(
    getConceptSetsApiContext(getAccessToken())
  ) as tanagra.ConceptSetsApi;
  const reviewsApi = useContext(
    getReviewsApiContext(getAccessToken())
  ) as tanagra.ReviewsApi;
  const annotationsApi = useContext(
    getAnnotationsApiContext(getAccessToken())
  ) as tanagra.AnnotationsApi;
  const usersApi = useContext(
    getUsersApiContext(getAccessToken())
  ) as tanagra.UsersApi;

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
