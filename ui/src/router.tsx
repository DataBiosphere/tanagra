import Button from "@mui/material/Button";
import { AddCriteria } from "addCriteria";
import { CohortReviewList } from "cohortReview/cohortReviewList";
import CohortRoot from "cohortRoot";
import ConceptSetEdit from "conceptSetEdit";
import ConceptSetRoot from "conceptSetRoot";
import Edit from "edit";
import NewConceptSet from "newConceptSet";
import NewCriteria from "newCriteria";
import { Overview } from "overview";
import {
  createHashRouter,
  generatePath,
  isRouteErrorResponse,
  Outlet,
  useNavigate,
  useParams,
  useRouteError,
} from "react-router-dom";
import { SdAdmin } from "sd-admin/sdAdmin";
import { Studies } from "sd-admin/studies";
import { StudiesList } from "studiesList";
import { UnderlaySelect } from "underlaySelect";
import { Datasets } from "./datasets";

export function createAppRouter() {
  return createHashRouter([
    {
      path: prefix,
      element: <Outlet />,
      children: [
        {
          element: <CohortRoot />,
          children: [
            {
              path: "cohorts/:cohortId/:groupId",
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
                      element: <AddCriteria />,
                    },
                    {
                      path: ":configId",
                      element: <NewCriteria />,
                    },
                  ],
                },
                {
                  path: "edit/:criteriaId",
                  element: <Edit />,
                },
              ],
            },
          ],
        },
        {
          element: <ConceptSetRoot />,
          children: [
            {
              path: "conceptSets/new/:configId",
              element: <NewConceptSet />,
            },
            {
              path: "conceptSets/edit/:conceptSetId",
              element: <ConceptSetEdit />,
            },
          ],
        },
        {
          path: "review/:cohortId",
          children: [
            {
              index: true,
              element: <CohortReviewList />,
            },
            {
              path: ":reviewId",
              element: <CohortReviewList />,
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

    case "sd":
      return [
        {
          path: "sdAdmin",
          element: <SdAdmin />,
        },
        {
          path: "studies",
          element: <Studies />,
        },
      ];
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
          path: "underlays/:underlayName",
          element: <StudiesList />,
        },
        {
          path: "underlays/:underlayName/studies/:studyId",
          element: <Datasets />,
        },
      ],
    },
  ];
}

// Used when navigating back from a root Tanagra page.
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

export function cohortURL(cohortId: string, groupId?: string) {
  return "cohorts/" + cohortId + "/" + (groupId ?? "first");
}

export function absoluteCohortURL(
  params: BaseParams,
  cohortId: string,
  groupId?: string
) {
  return absolutePrefix(params) + cohortURL(cohortId, groupId);
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

export function absoluteNewConceptSetURL(params: BaseParams, configId: string) {
  return absolutePrefix(params) + "conceptSets/new/" + configId;
}

export function newConceptSetURL(configId: string) {
  return `conceptSets/new/${configId}`;
}

export function criteriaURL(criteriaId: string) {
  return `edit/${criteriaId}`;
}

export function newCriteriaURL(configId: string) {
  return `add/${configId}`;
}

export function absoluteCohortReviewURL(
  params: BaseParams,
  cohortId: string,
  reviewId?: string
) {
  return `${absolutePrefix(params)}review/${cohortId}/${reviewId ?? ""}`;
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
