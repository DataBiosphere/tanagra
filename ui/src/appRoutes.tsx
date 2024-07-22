import { CohortRevision } from "activityLog/cohortRevision";
import { AddCohort } from "addCohort";
import { AddCohortCriteria, AddFeatureSetCriteria } from "addCriteria";
import {
  Auth0ProviderWithOutlet,
  LoginPage,
  LogoutPage,
} from "auth/auth0OAuth";
import { CohortReview } from "cohortReview/cohortReview";
import { CohortReviewList } from "cohortReview/cohortReviewList";
import CohortRoot from "cohortRoot";
import { StudySourceContextRoot } from "data/studySourceContext";
import { UnderlaySourceContextRoot } from "data/underlaySourceContext";
import Edit from "edit";
import { getEnvironment } from "environment";
import { ErrorPage } from "errorhandler";
import { Export } from "export";
import { AddFeatureSet } from "featureSet/addFeatureSet";
import { FeatureSet } from "featureSet/featureSet";
import { FeatureSetEdit } from "featureSet/featureSetEdit";
import FeatureSetRoot from "featureSet/featureSetRoot";
import { NewFeatureSet } from "featureSet/newFeatureSet";
import NewCriteria from "newCriteria";
import { Overview } from "overview";
import { StudiesList } from "sampleApp/studiesList";
import { StudyOverview } from "sampleApp/studyOverview";
import { TanagraContainer } from "sampleApp/tanagraContainer";
import { UnderlaySelect } from "sampleApp/underlaySelect";

export function underlaySourceContextRootRoutes() {
  return [
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
            ...cohortRootRoutes(),
            ...featureSetRootRoutes(),
            ...cohortRevisionRoutes(),
          ],
        },
      ],
    },
  ];
}

function cohortRootRoutes() {
  return [
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
              path: "second?/add",
              children: [
                {
                  index: true,
                  element: <AddCohortCriteria />,
                },
                {
                  path: ":configId",
                  element: <NewCriteria />,
                },
                {
                  path: "tAddFeatureSet",
                  element: <AddFeatureSet />,
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
  ];
}

function featureSetRootRoutes() {
  return [
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
                {
                  path: "tAddCohort",
                  element: <AddCohort />,
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
  ];
}

function cohortRevisionRoutes() {
  return [
    {
      path: "activityLog/cohorts/:cohortId/:revisionId",
      element: <CohortRevision />,
    },
  ];
}

export function additionalRoutes() {
  switch (getEnvironment().REACT_APP_ADDITIONAL_ROUTES) {
    case "none":
      return [];
  }

  return [
    {
      path: "/",
      element: <Auth0ProviderWithOutlet />,
      errorElement: <ErrorPage />,
      children: [
        {
          index: true,
          element: <UnderlaySelect />,
        },
        ...authRoutes(),
        ...studySourceContextRootRoute(),
      ],
    },
  ];
}

function studySourceContextRootRoute() {
  return [
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
  ];
}

function authRoutes() {
  return [
    {
      path: "login",
      element: <LoginPage />,
    },
    {
      path: "logout",
      element: <LogoutPage />,
    },
  ];
}
