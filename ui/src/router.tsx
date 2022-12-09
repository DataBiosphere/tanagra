import Button from "@mui/material/Button";
import { AddCriteria } from "addCriteria";
import { CohortReviewList } from "cohortReview/cohortReviewList";
import ConceptSetEdit from "conceptSetEdit";
import Edit from "edit";
import { PathError } from "hooks";
import NewConceptSet from "newConceptSet";
import NewCriteria from "newCriteria";
import { Overview } from "overview";
import { ErrorBoundary } from "react-error-boundary";
import { Route, Routes, useNavigate } from "react-router-dom";
import { SdAdmin } from "sd-admin/sdAdmin";
import { UnderlaySelect } from "underlaySelect";
import { Datasets } from "./datasets";

export function AppRouter() {
  return (
    <ErrorBoundary FallbackComponent={NotFound}>
      <Routes>
        <Route index element={<UnderlaySelect />} />
        <Route path=":underlayName">
          <Route index element={<Datasets />} />
          <Route path="cohorts/:cohortId/:groupId">
            <Route index element={<Overview />} />
            <Route path="add">
              <Route index element={<AddCriteria />} />
              <Route path=":configId" element={<NewCriteria />} />
            </Route>
            <Route path="edit/:criteriaId" element={<Edit />} />
          </Route>
          <Route path="conceptSets/new/:configId" element={<NewConceptSet />} />
          <Route
            path="conceptSets/edit/:conceptSetId"
            element={<ConceptSetEdit />}
          />
          <Route path="review/:cohortId">
            <Route index element={<CohortReviewList />} />
            <Route path=":reviewId" element={<CohortReviewList />} />
          </Route>
        </Route>
        <Route path="sdAdmin" element={<SdAdmin />} />
        <Route
          path="*"
          element={<NotFound error={new PathError("Invalid URL.")} />}
        />
      </Routes>
    </ErrorBoundary>
  );
}

// TODO(tjennison): This is becoming spaghetti. Consider alternative ways to set
// this up or perhaps alternative libraries.
export function underlayURL(underlayName: string) {
  return underlayName;
}

export function cohortURL(cohortId: string, groupId?: string) {
  return "cohorts/" + cohortId + "/" + (groupId ?? "first");
}

export function absoluteCohortURL(underlayName: string, cohortId: string) {
  return `/${underlayName}/${cohortURL(cohortId)}`;
}

export function conceptSetURL(conceptSetId: string) {
  return "conceptSets/edit/" + conceptSetId;
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

export function cohortReviewURL(
  underlayName: string,
  cohortId: string,
  reviewId?: string
) {
  return `/${underlayName}/review/${cohortId}/${reviewId ?? ""}`;
}

// TODO(tjennison): Make a prettier 404 page.
function NotFound(props: { error: Error; resetErrorBoundary?: () => void }) {
  const navigate = useNavigate();
  return (
    <>
      <p>404: {props.error.message}</p>
      <Button
        variant="contained"
        onClick={() => {
          navigate("/");
          props.resetErrorBoundary?.();
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
