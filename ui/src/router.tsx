import Button from "@mui/material/Button";
import { AddCriteria } from "addCriteria";
import ConceptSetEdit from "conceptSetEdit";
import Edit from "edit";
import { GroupOverview } from "groupOverview";
import { PathError } from "hooks";
import NewConceptSet from "newConceptSet";
import NewCriteria from "newCriteria";
import { Overview } from "overview";
import { ErrorBoundary } from "react-error-boundary";
import { Route, Routes, useNavigate } from "react-router-dom";
import { UnderlaySelect } from "underlaySelect";
import { Datasets } from "./datasets";
import { SdAdmin } from "./sdAdmin";

export function AppRouter() {
  return (
    <ErrorBoundary FallbackComponent={NotFound}>
      <Routes>
        <Route index element={<UnderlaySelect />} />
        <Route path=":underlayName">
          <Route index element={<Datasets />} />
          <Route path="cohorts/:cohortId/:groupId" element={<Overview />}>
            <Route index element={<GroupOverview />} />
            <Route path="add" element={<AddCriteria />} />
            <Route path="new/:configId" element={<NewCriteria />} />
            <Route path="edit/:criteriaId" element={<Edit />} />
          </Route>
          <Route path="conceptSets/new/:configId" element={<NewConceptSet />} />
          <Route
            path="conceptSets/edit/:conceptSetId"
            element={<ConceptSetEdit />}
          />
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

export function underlayURL(underlayName: string) {
  return underlayName;
}

export function cohortURL(cohortId: string, groupId: string) {
  return "cohorts/" + cohortId + "/" + groupId;
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
  return `new/${configId}`;
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
