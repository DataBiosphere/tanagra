import {
  loginAccessType,
  useAccessTokenProvider,
  useAnnotationsApiContext,
  useCohortsApiContext,
  useConceptSetsApiContext,
  useReviewsApiContext,
  useStudiesApiContext,
  useUsersApiContext,
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
  const tokenProvider = useAccessTokenProvider(loginAccessType.RedirectUrl);
  // TODO(tjennison): Move "fake" logic into a separate source instead of APIs.
  const studiesApi = useContext(
    useStudiesApiContext(tokenProvider)
  ) as tanagra.StudiesApi;
  const cohortsApi = useContext(
    useCohortsApiContext(tokenProvider)
  ) as tanagra.CohortsApi;
  const conceptSetsApi = useContext(
    useConceptSetsApiContext(tokenProvider)
  ) as tanagra.ConceptSetsApi;
  const reviewsApi = useContext(
    useReviewsApiContext(tokenProvider)
  ) as tanagra.ReviewsApi;
  const annotationsApi = useContext(
    useAnnotationsApiContext(tokenProvider)
  ) as tanagra.AnnotationsApi;
  const usersApi = useContext(
    useUsersApiContext(tokenProvider)
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
