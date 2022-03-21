import { render } from "@testing-library/react";
import { PathError } from "hooks";
import { Route, StaticRouter } from "react-router-dom";
import { createUrl, useParentUrl } from "./router";

test("successful createUrl", () => {
  expect(createUrl({ underlayName: "test-underlay", cohortId: "testId" })).toBe(
    "/test-underlay/cohorts/testId"
  );
});

test("unsuccessful createUrl", () => {
  expect(() => createUrl({ cohortId: "testId" })).toThrow(PathError);
});

function ParentUrl() {
  return <>{String(useParentUrl())}</>;
}

test("defined useParentUrl", () => {
  render(
    <StaticRouter location="/test-underlay/cohorts/testId">
      <Route exact path="/:underlayName/cohorts/:cohortId">
        <ParentUrl />
      </Route>
    </StaticRouter>
  );
  expect(document.body.textContent).toBe("/test-underlay");
});

test("undefined useParentUrl", () => {
  render(
    <StaticRouter location="/">
      <Route exact path="/">
        <ParentUrl />
      </Route>
    </StaticRouter>
  );
  expect(document.body.textContent).toBe("undefined");
});
