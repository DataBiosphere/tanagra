import App from "app";
import { render, screen, waitFor } from "@testing-library/react";
import React from "react";
import {RouteObject, RouterProvider} from "react-router-dom";
import {createHashRouter} from 'react-router-dom';
import {signInText} from "auth/provider";
import {FakeAuthContext} from "auth/fakeProvider";
import {getEnvironment} from './environment';

function authTestRouter(expired: boolean,
    noProfile: boolean
) {
  const env = getEnvironment();
  env.REACT_APP_AUTH0_DOMAIN = "fake-domain"
  env.REACT_APP_AUTH0_CLIENT_ID = "fake-client"

  return createHashRouter([
    {
      path: "*",
      element: (
          <FakeAuthContext expired={expired}
              noProfile={noProfile} >
            <App/>
          </FakeAuthContext>
      ),
    },
  ]);

}

/*
test("render included datasets heading", async () => {
  render(<App />);
  await waitFor(() => {
    expect(screen.getByText(/underlay_name/i)).toBeInTheDocument();
  });

  userEvent.click(screen.getByText("underlay_name"));
  await waitFor(() =>
    expect(screen.getByText(/add study/i)).toBeInTheDocument()
  );
});
*/

test("signed out redirects to login screen", () => {
  const router = authTestRouter(/*expired= */ false, /* noProfile= */true);
  render(<RouterProvider router={router} />);

  expect(router.state.location.pathname).toBe("/login");
  expect(
      screen.getByRole("button", { name: signInText })
  ).toBeInTheDocument();
});

it("signed in renders the home page", () => {
  const router = authTestRouter(/*expired= */ false, /* noProfile= */false);
  expect(router.state.location.pathname).toBe("/");
  expect(screen.getByText(/Welcome to Workbench/i)).toBeInTheDocument();
});

/*
it("login page while signed in redirects to home page", () => {
  const router = authTestRouter(/*expired= * / false, /* noProfile= * /false);
  router.navigate({ pathname: "/login" });
  render(<RouterProvider router={router} />);
  expect(router.state.location.pathname).toBe("/");
});
*/

it("signed out with bad url redirects to login screen", () => {
  const router = authTestRouter(/*expired= */ false, /* noProfile= */true);
  expect(router.state.location.pathname).toBe("/login");
  expect(
      screen.getByRole("button", { name: signInText, hidden: true })
  ).toBeInTheDocument();
});

it("expired and loaded redirects to login screen", () => {
  const router = authTestRouter(/*expired= */ true, /* noProfile= */false);
  console.log("test + " + getEnvironment().REACT_APP_AUTH0_DOMAIN)
  expect(router.state.location.pathname).toBe("/login");
  expect(
      screen.getByLabelText(signInText)
  ).toBeInTheDocument();
});
