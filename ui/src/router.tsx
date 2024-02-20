import Button from "@mui/material/Button";
import { CohortRevision } from "activityLog/cohortRevision";
import { AddCohortCriteria, AddFeatureSetCriteria } from "addCriteria";
import { CohortReview } from "cohortReview/cohortReview";
import { CohortReviewList } from "cohortReview/cohortReviewList";
import CohortRoot from "cohortRoot";
import { StudySourceContextRoot } from "data/studySourceContext";
import { UnderlaySourceContextRoot } from "data/underlaySourceContext";
import Edit from "edit";
import { Export } from "export";
import { FeatureSet } from "featureSet/featureSet";
import { FeatureSetEdit } from "featureSet/featureSetEdit";
import FeatureSetRoot from "featureSet/featureSetRoot";
import { NewFeatureSet } from "featureSet/newFeatureSet";
import NewCriteria from "newCriteria";
import { Overview } from "overview";
import { useCallback, useEffect } from "react";
import {
  createHashRouter,
  generatePath,
  isRouteErrorResponse,
  useLocation,
  useNavigate,
  useParams,
  useRouteError,
} from "react-router-dom";
import { StudiesList } from "sampleApp/studiesList";
import { StudyOverview } from "sampleApp/studyOverview";
import { TanagraContainer } from "sampleApp/tanagraContainer";
import { UnderlaySelect } from "sampleApp/underlaySelect";

export function createAppRouter() {
  return createHashRouter([
    {
      path: "tanagra/underlays/:underlayName",
      element: <UnderlaySourceContextRoot />,
      children: [
        {
          path: "studies/:studyId/export?/",
          element: <StudySourceContextRoot />,
          children: [
            {
              index: true,
              element: <Export />,
            },
            {
              element: <CohortRoot />,
              children: [
                {
                  path: "cohorts/:cohortId/:groupSectionId/:groupId",
                  children: [
                    {
                      index: true,
                      element: <Overview />,
                    },
                    {
                      path: "add",
                      children: [
                        {
                          index: true,
                          element: <AddCohortCriteria />,
                        },
                        {
                          path: ":configId",
                          element: <NewCriteria />,
                        },
                      ],
                    },
                    {
                      path: "edit",
                      element: <Edit />,
                    },
                  ],
                },
                {
                  path: "reviews/:cohortId/:reviewId?",
                  children: [
                    {
                      index: true,
                      element: <CohortReviewList />,
                    },
                    {
                      path: "review",
                      element: <CohortReview />,
                    },
                  ],
                },
              ],
            },
            {
              element: <FeatureSetRoot />,
              children: [
                {
                  path: "featureSets/:featureSetId",
                  children: [
                    {
                      index: true,
                      element: <FeatureSet />,
                    },
                    {
                      path: "add",
                      children: [
                        {
                          index: true,
                          element: <AddFeatureSetCriteria />,
                        },
                        {
                          path: ":configId",
                          element: <NewFeatureSet />,
                        },
                      ],
                    },
                    {
                      path: "edit/:criteriaId",
                      element: <FeatureSetEdit />,
                    },
                  ],
                },
              ],
            },
            {
              path: "activityLog/cohorts/:cohortId/:revisionId",
              element: <CohortRevision />,
            },
          ],
        },
      ],
    },
    ...additionalRoutes(),
  ]);
}

function additionalRoutes() {
  switch (process.env.REACT_APP_ADDITIONAL_ROUTES) {
    case "none":
      return [];
  }

  return [
    {
      path: "/",
      errorElement: <ErrorPage />,
      children: [
        {
          index: true,
          element: <UnderlaySelect />,
        },
        {
          element: <StudySourceContextRoot />,
          children: [
            {
              path: "underlays/:underlayName",
              element: <UnderlaySourceContextRoot />,
              children: [
                {
                  index: true,
                  element: <StudiesList />,
                },
                {
                  path: "studies/:studyId",
                  element: <StudyOverview />,
                },
                {
                  path: "studies/:studyId/*",
                  element: <TanagraContainer />,
                },
              ],
            },
          ],
        },
      ],
    },
  ];
}

// Used when navigating back from a root Tanagra page.
export function useExitAction() {
  const location = useLocation();
  const params = useBaseParams();
  const navigate = useNavigate();

  return useCallback(() => {
    const match = location.pathname.match(/^(.*\/export\/).+$/);
    if (match) {
      navigate(match[1]);
    } else {
      if (process.env.REACT_APP_USE_EXIT_URL) {
        navigate(exitURL(params));
      } else {
        window.parent.postMessage(
          { message: "CLOSE" },
          process.env.REACT_APP_POST_MESSAGE_ORIGIN ?? window.location.origin
        );
      }
    }
  }, [location, params, navigate]);
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
  const url = process.env.REACT_APP_EXIT_URL;
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
  return `add/${configId}`;
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

// TODO(tjennison): Make a prettier error page.
function ErrorPage() {
  const error = useRouteError();
  const navigate = useNavigate();

  let message = String(error);
  if (isRouteErrorResponse(error)) {
    message = `${error.status}: ${error.statusText}`;
  } else if (typeof error === "object" && !!error && "message" in error) {
    message = (error as Error).message;
  }

  return (
    <>
      <p>{message}</p>
      <Button
        variant="contained"
        onClick={() => {
          navigate("/");
        }}
      >
        Return Home
      </Button>
    </>
  );
}

export function getCurrentUrl(): string {
  const url = window.location.href;
  return url.slice(url.indexOf("#") + 1);
}
