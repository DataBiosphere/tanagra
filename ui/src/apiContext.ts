import { useAuth } from "auth/provider";
import { getEnvironment, isTestEnvironment } from "environment";
import {
  FakeAnnotationsAPI,
  FakeCohortsAPI,
  FakeExportAPI,
  FakeFeatureSetsAPI,
  FakeReviewsAPI,
  FakeStudiesAPI,
  FakeUnderlaysApi,
  FakeUsersAPI,
} from "fakeApis";
import * as tanagra from "tanagra-api";

function getAccessToken() {
  // For local dev: get the bearer token from the iframe url param.
  // For all other envs: check parent window's localStorage for auth token.
  return (
    (getEnvironment().REACT_APP_GET_LOCAL_AUTH_TOKEN
      ? new URLSearchParams(window.location.href.split("?")[1]).get("token")
      : window.parent.localStorage.getItem("tanagraAccessToken")) ?? ""
  );
}

function useApiForEnvironment<Real, Fake>(
  real: { new (c: tanagra.Configuration): Real },
  fake: { new (): Fake }
) {
  const { getAuthToken } = useAuth();
  const env = getEnvironment();

  if (isTestEnvironment()) {
    getAuthToken ?? getAccessToken();
    return new fake();
  }

  const config: tanagra.ConfigurationParameters = {
    basePath: env.REACT_APP_BACKEND_HOST || "",
    accessToken: getAuthToken ?? getAccessToken(),
  };
  return new real(new tanagra.Configuration(config));
}

export function useUnderlaysApi() {
  return useApiForEnvironment(tanagra.UnderlaysApi, FakeUnderlaysApi);
}

export function useStudiesApi() {
  return useApiForEnvironment(tanagra.StudiesApi, FakeStudiesAPI);
}

export function useCohortsApi() {
  return useApiForEnvironment(tanagra.CohortsApi, FakeCohortsAPI);
}

export function useFeatureSetsApi() {
  return useApiForEnvironment(tanagra.FeatureSetsApi, FakeFeatureSetsAPI);
}

export function useReviewsApi() {
  return useApiForEnvironment(tanagra.ReviewsApi, FakeReviewsAPI);
}

export function useAnnotationsApi() {
  return useApiForEnvironment(tanagra.AnnotationsApi, FakeAnnotationsAPI);
}

export function useExportApi() {
  return useApiForEnvironment(tanagra.ExportApi, FakeExportAPI);
}

export function useUsersApi() {
  return useApiForEnvironment(tanagra.UsersApi, FakeUsersAPI);
}
