import Button from "@mui/material/Button";
import ConceptSetEdit from "conceptSetEdit";
import Edit from "edit";
import { PathError } from "hooks";
import Overview from "overview";
import { ErrorBoundary } from "react-error-boundary";
import {
  matchPath,
  Route,
  Switch,
  useHistory,
  useParams,
} from "react-router-dom";
import { UnderlaySelect } from "underlaySelect";
import { Datasets } from "./datasets";

const pages = {
  "/": UnderlaySelect,
  "/:underlayName": Datasets,
  "/:underlayName/cohorts/:cohortId": Overview,
  "/:underlayName/cohorts/:cohortId/edit/:groupId/:criteriaId": Edit,
  "/:underlayName/conceptSets/:conceptSetId": ConceptSetEdit,
};

export function AppRouter() {
  return (
    <ErrorBoundary FallbackComponent={NotFound}>
      <Switch>
        {Object.entries(pages).map(([page, component]) => (
          <Route exact key={page} path={page} component={component} />
        ))}
        <Route path="/">
          <NotFound error={new PathError("Invalid URL.")} />
        </Route>
      </Switch>
    </ErrorBoundary>
  );
}

// useParentUrl returns the second longest matching page, assuming that the
// longest is the current page since longer ones wouldn't match.
export function useParentUrl(): string | undefined {
  const history = useHistory();
  const page = Object.keys(pages)
    .filter((page) => matchPath(history.location.pathname, { path: page }))
    .sort((a, b) => b.length - a.length)[1];
  return replaceParams(page, false, useParams());
}

export type UrlParams = {
  underlayName?: string;
  cohortId?: string;
  groupId?: string;
  criteriaId?: string;
  conceptSetId?: string;
};

export function createUrl(params: UrlParams): string {
  for (const page of Object.keys(pages)) {
    const url = replaceParams(page, true, params);
    if (url) {
      return url;
    }
  }
  throw new PathError(`No path matches "${JSON.stringify(params)}".`);
}

function replaceParams(
  path: string,
  validate: boolean,
  params: Record<string, string>
) {
  let count = 0;
  const url = path?.replaceAll(/:(\w+)/g, (_, match) => {
    count = params[match] && count >= 0 ? count + 1 : -1;
    return params[match];
  });
  return !validate || count == Object.keys(params).length ? url : undefined;
}

// TODO(tjennison): Make a prettier 404 page.
function NotFound(props: { error: Error; resetErrorBoundary?: () => void }) {
  const history = useHistory();
  return (
    <>
      <p>404: {props.error.message}</p>
      <Button
        variant="contained"
        onClick={() => {
          history.push("/");
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
