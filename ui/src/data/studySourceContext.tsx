import {
  LoginAccessType,
  useAccessTokenProvider,
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
  const tokenProvider = useAccessTokenProvider(LoginAccessType.REDIRECT_URL);
  // TODO(tjennison): Move "fake" logic into a separate source instead of APIs.
  const studiesApi = useStudiesApi(tokenProvider) as tanagra.StudiesApi;
  const cohortsApi = useCohortsApi(tokenProvider) as tanagra.CohortsApi;
  const conceptSetsApi = useConceptSetsApi(
    tokenProvider
  ) as tanagra.ConceptSetsApi;
  const reviewsApi = useReviewsApi(tokenProvider) as tanagra.ReviewsApi;
  const annotationsApi = useAnnotationsApi(
    tokenProvider
  ) as tanagra.AnnotationsApi;
  const usersApi = useUsersApi(tokenProvider) as tanagra.UsersApi;

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
