import { additionalRoutes, authRoutes, coreRoutes } from "appRoutes";
import {
  AuthProvider,
  AuthProviderProps,
  CheckAuthorization,
  isAuthEnabled,
} from "auth/provider";
import { ErrorPage } from "components/errorPage";
import { getEnvironment } from "environment";
import { useCallback, useEffect } from "react";
import {
  createHashRouter,
  generatePath,
  useLocation,
  useParams,
} from "react-router-dom";
import { useNavigate } from "util/searchState";

export function createAppRouter(authProps: AuthProviderProps) {
  const authEnabled = isAuthEnabled();
  return createHashRouter([
    {
      path: "/",
      element: authEnabled ? <AuthProvider {...authProps} /> : undefined,
      errorElement: <ErrorPage />,
      children: [
        {
          element: <CheckAuthorization />,
          children: [
            ...(authEnabled ? authRoutes() : []),
            ...coreRoutes(),
            ...additionalRoutes(),
          ],
        },
      ],
    },
  ]);
}

// Used when navigating back from a root Tanagra page.
export function useExitAction() {
  const env = getEnvironment();
  const location = useLocation();
  const params = useBaseParams();
  const navigate = useNavigate();

  return useCallback(() => {
    const match = location.pathname.match(/^(.*\/export\/).+$/);
    if (match) {
      navigate(match[1]);
    } else {
      if (env.REACT_APP_USE_EXIT_URL) {
        navigate(exitURL(params));
      } else {
        window.parent.postMessage(
          { message: "CLOSE" },
          env.REACT_APP_POST_MESSAGE_ORIGIN ?? window.location.origin
        );
      }
    }
  }, [location, params, navigate]);
}

export function useIsSecondBlock() {
  return !!useLocation().pathname.match(/\/second\//);
}

function debounce(callback: () => void, delay: number) {
  let timer: NodeJS.Timeout;
  return function () {
    clearTimeout(timer);
    timer = setTimeout(() => {
      callback();
    }, delay);
  };
}

export function useActivityListener() {
  const listener = debounce(
    () =>
      window.parent.postMessage(
        "USER_ACTIVITY_DETECTED",
        getEnvironment().REACT_APP_POST_MESSAGE_ORIGIN ?? window.location.origin
      ),
    1000
  );

  const activityEvents = ["mousedown", "keypress", "scroll", "click"];
  useEffect(() => {
    activityEvents.forEach((e) => window.addEventListener(e, listener));
    return () => {
      activityEvents.forEach((e) => window.removeEventListener(e, listener));
    };
  }, [listener]);
}

function useMessageListener<T>(message: string, callback: (event: T) => void) {
  const listener = useCallback(
    (event) => {
      if (
        event.origin != window.window.location.origin ||
        typeof event.data !== "object" ||
        event.data.message != message
      ) {
        return;
      }
      callback(event.data);
    },
    [message, callback]
  );

  useEffect(() => {
    window.addEventListener("message", listener);
    return () => {
      window.removeEventListener("message", listener);
    };
  }, [listener]);
}

export function useExitActionListener(callback: () => void) {
  useMessageListener("CLOSE", () => callback());
}

export const RETURN_URL_PLACEHOLDER = "T_RETURN_URL";

export type RedirectEvent = {
  redirectURL: string;
  returnPath: string;
};

export function useRedirectListener(
  callback: (redirectURL: string, returnPath: string) => void
) {
  useMessageListener<RedirectEvent>("REDIRECT", (event) =>
    callback(event.redirectURL, event.returnPath)
  );
}

export function redirect(redirectURL: string, returnPath: string) {
  window.parent.postMessage(
    { message: "REDIRECT", redirectURL, returnPath },
    window.location.origin
  );
}

export function exitURL(params: BaseParams) {
  const url = getEnvironment().REACT_APP_EXIT_URL;
  if (url) {
    return generatePath(url, params);
  }
  return generatePath("/underlays/:underlayName/studies/:studyId", params);
}

export type BaseParams = {
  underlayName: string;
  studyId: string;
};

export function useBaseParams(): BaseParams {
  const { underlayName, studyId } = useParams<BaseParams>();
  return {
    underlayName: underlayName ?? "",
    studyId: studyId ?? "",
  };
}

// TODO(tjennison): This is becoming spaghetti. Consider alternative ways to set
// this up or perhaps alternative libraries.
function absolutePrefix(params: BaseParams) {
  return generatePath(
    "/tanagra/underlays/:underlayName/studies/:studyId/",
    params
  );
}

export function underlayURL(underlayName: string) {
  return "underlays/" + underlayName;
}

export function cohortURL(
  cohortId: string,
  groupSectionId?: string,
  groupId?: string
) {
  return `cohorts/${cohortId}/${groupSectionId ?? "first"}/${
    groupId ?? "none"
  }`;
}

export function absoluteCohortURL(
  params: BaseParams,
  cohortId: string,
  groupSectionId?: string,
  groupId?: string
) {
  return absolutePrefix(params) + cohortURL(cohortId, groupSectionId, groupId);
}

export function featureSetURL(featureSetId: string) {
  return `featureSets/${featureSetId}`;
}

export function absoluteFeatureSetURL(
  params: BaseParams,
  featureSetId: string
) {
  return absolutePrefix(params) + featureSetURL(featureSetId);
}

export function addFeatureSetCriteriaURL() {
  return "tAddFeatureSet";
}

export function addCohortCriteriaURL() {
  return "tAddCohort";
}

export function absoluteExportURL(params: BaseParams) {
  return absolutePrefix(params) + "export";
}

export function absoluteExportSetsURL(params: BaseParams) {
  return absolutePrefix(params) + "sets";
}

export function criteriaURL() {
  return "edit";
}

export function newCriteriaURL(configId: string) {
  return configId;
}

export function featureSetCriteriaURL(criteriaId: string) {
  return "edit/" + criteriaId;
}

export function absoluteCohortReviewListURL(
  params: BaseParams,
  cohortId: string,
  reviewId?: string
) {
  return `${absolutePrefix(params)}reviews/${cohortId}/${reviewId ?? ""}`;
}

export function getCurrentUrl(): string {
  const url = window.location.href;
  return url.slice(url.indexOf("#") + 1);
}
