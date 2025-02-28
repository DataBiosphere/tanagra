import {
  useStudiesApi,
  useCohortsApi,
  useFeatureSetsApi,
  useReviewsApi,
  useAnnotationsApi,
  useUsersApi,
} from "apiContext";
import { useMemo } from "react";
import { Outlet } from "react-router-dom";
import * as tanagra from "tanagra-api";
import { BackendStudySource } from "data/source";
import { StudySourceContext } from "data/studySourceContext";

export function StudySourceContextRoot() {
  // TODO(tjennison): Move "fake" logic into a separate source instead of APIs.
  const studiesApi = useStudiesApi() as tanagra.StudiesApi;
  const cohortsApi = useCohortsApi() as tanagra.CohortsApi;
  const featureSetsApi = useFeatureSetsApi() as tanagra.FeatureSetsApi;
  const reviewsApi = useReviewsApi() as tanagra.ReviewsApi;
  const annotationsApi = useAnnotationsApi() as tanagra.AnnotationsApi;
  const usersApi = useUsersApi() as tanagra.UsersApi;

  const context = useMemo(() => {
    return {
      source: new BackendStudySource(
        studiesApi,
        cohortsApi,
        featureSetsApi,
        reviewsApi,
        annotationsApi,
        usersApi
      ),
    };
  }, [
    studiesApi,
    cohortsApi,
    featureSetsApi,
    reviewsApi,
    annotationsApi,
    usersApi,
  ]);

  return (
    <StudySourceContext.Provider value={context}>
      <Outlet />
    </StudySourceContext.Provider>
  );
}
