import Button from "@mui/material/Button";
import { AddCohortCriteria, AddConceptSetCriteria } from "addCriteria";
import { CohortReview } from "cohortReview/cohortReview";
import { CohortReviewList } from "cohortReview/cohortReviewList";
import CohortRoot from "cohortRoot";
import ConceptSetEdit from "conceptSetEdit";
import ConceptSetRoot from "conceptSetRoot";
import { SourceContextRoot } from "data/sourceContext";
import { Datasets } from "datasets";
import Edit from "edit";
import NewConceptSet from "newConceptSet";
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
      path: prefix + "export?/",
      element: <SourceContextRoot />,
      children: [
        {
          index: true,
          element: <Datasets />,
        },
        {
          element: <CohortRoot />,
          children: [
            {
              path: "cohorts/:cohortId/:groupSectionId",
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
                  path: "edit/:groupId",
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
          path: "conceptSets",
          element: <ConceptSetRoot />,
          children: [
            {
              path: "add",
              children: [
                {
                  index: true,
                  element: <AddConceptSetCriteria />,
                },
                {
                  path: ":configId",
                  element: <NewConceptSet />,
                },
              ],
            },
            {
              path: "edit/:conceptSetId",
              element: <ConceptSetEdit />,
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
          element: <SourceContextRoot />,
          children: [
            {
              path: "underlays/:underlayName",
              element: <StudiesList />,
            },
            {
              path: "underlays/:underlayName/studies/:studyId",
              element: <StudyOverview />,
            },
            {
              path: "underlays/:underlayName/studies/:studyId/*",
              element: <TanagraContainer />,
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
        window.parent.postMessage({ message: "CLOSE" }, window.location.origin);
      }
    }
  }, [location, params, navigate]);
}

export function useExitActionListener(callback: () => void) {
  const listener = useCallback(
    (event) => {
      if (
        event.origin != window.window.location.origin ||
        typeof event.data !== "object" ||
        event.data.message != "CLOSE"
      ) {
        return;
      }
      callback();
    },
    [callback]
  );

  useEffect(() => {
    window.addEventListener("message", listener);
    return () => {
      window.removeEventListener("message", listener);
    };
  }, [listener]);
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
const prefix = "tanagra/underlays/:underlayName/studies/:studyId/";

function absolutePrefix(params: BaseParams) {
  return generatePath("/" + prefix, params);
}

export function underlayURL(underlayName: string) {
  return "underlays/" + underlayName;
}

export function cohortURL(cohortId: string, groupSectionId?: string) {
  return "cohorts/" + cohortId + "/" + (groupSectionId ?? "first");
}

export function absoluteCohortURL(
  params: BaseParams,
  cohortId: string,
  groupSectionId?: string
) {
  return absolutePrefix(params) + cohortURL(cohortId, groupSectionId);
}

export function absoluteExportURL(params: BaseParams) {
  return absolutePrefix(params) + "export";
}

export function absoluteConceptSetURL(
  params: BaseParams,
  conceptSetId: string
) {
  return absolutePrefix(params) + "conceptSets/edit/" + conceptSetId;
}

export function conceptSetURL(conceptSetId: string) {
  return "conceptSets/edit/" + conceptSetId;
}

export function absoluteNewConceptSetURL(params: BaseParams) {
  return absolutePrefix(params) + newConceptSetURL();
}

export function newConceptSetURL() {
  return `conceptSets/add`;
}

export function criteriaURL(groupId: string) {
  return `edit/${groupId}`;
}

export function newCriteriaURL(configId: string) {
  return `add/${configId}`;
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
