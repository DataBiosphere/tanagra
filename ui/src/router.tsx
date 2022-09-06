import Button from "@mui/material/Button";
import ConceptSetEdit from "conceptSetEdit";
import Edit from "edit";
import { PathError } from "hooks";
import Overview from "overview";
import { ErrorBoundary } from "react-error-boundary";
import { Route, Routes, useNavigate } from "react-router-dom";
import { UnderlaySelect } from "underlaySelect";
import { Datasets } from "./datasets";

export function AppRouter() {
  return (
    <ErrorBoundary FallbackComponent={NotFound}>
      <Routes>
        <Route index element={<UnderlaySelect />} />
        <Route path=":underlayName">
          <Route index element={<Datasets />} />
          <Route path="cohorts/:cohortId">
            <Route index element={<Overview />} />
            <Route path="edit/:groupId/:criteriaId">
              <Route index element={<Edit />} />
            </Route>
          </Route>
          <Route path="conceptSets/:conceptSetId">
            <Route index element={<ConceptSetEdit />} />
          </Route>
        </Route>
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

export function cohortURL(cohortId: string) {
  return "cohorts/" + cohortId;
}

export function conceptSetURL(conceptSetId: string) {
  return "conceptSets/" + conceptSetId;
}

export function criteriaURL(groupId: string, criteriaId: string) {
  return `edit/${groupId}/${criteriaId}`;
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
