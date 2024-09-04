import { Router } from "@remix-run/router";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { App, AppWithRouter } from "app";
import { AuthProviderProps, logInText } from "auth/provider";
import { AuthContextType as FakeAuthContextType } from "auth/provider";
import { getEnvironment } from "environment";
import React from "react";
import { FakeProfile, makeFakeAuth } from "auth/fakeProvider";
import { RouterProvider } from "react-router-dom";
import { createAppRouter } from "router";

function renderApp(fakeAuthCtx?: FakeAuthContextType) {
  let appRouter: Router;
  if (fakeAuthCtx) {
    const env = getEnvironment();
    env.REACT_APP_AUTH0_DOMAIN = "fake-domain"
    env.REACT_APP_AUTH0_CLIENT_ID = "fake-client"
    appRouter = createAppRouter({authCtx: fakeAuthCtx});
    render(<AppWithRouter {...appRouter} />);

  } else {
     appRouter = createAppRouter({} as AuthProviderProps);
    render(<App />);

  }

  return appRouter
}

test("render included datasets heading", async () => {
  renderApp();
  await waitFor(() => {
    expect(screen.getByText(/underlay_name/i)).toBeInTheDocument();
  });
  userEvent.click(screen.getByText("underlay_name"));
  await waitFor(() =>
    expect(screen.getByText(/add study/i)).toBeInTheDocument()
  );
});

test("with-auth: signed out redirects to login screen", () => {
  renderApp( makeFakeAuth({
    expired: true
  }));
  expect(screen.getByText(logInText)).toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: logInText })
  ).toBeInTheDocument();
});

test("with-auth: signed in renders the home page", async () => {
  renderApp(makeFakeAuth({
    expired: false, profile: FakeProfile
  }));
  await waitFor(() => {
    expect(screen.getByText(/underlay_name/i)).toBeInTheDocument();
  });
});

test("with-auth: login page while signed in redirects to home page", () => {
  const appRouter =   renderApp(makeFakeAuth({
    expired: false, profile: FakeProfile
  }));
  appRouter.navigate({ pathname: "/login" });
  render(<RouterProvider router={appRouter} />);
  expect(appRouter.state.location.pathname).toBe("/");
});

test("with-auth: signed out with bad url redirects to login screen", () => {
  const appRouter =   renderApp(makeFakeAuth({
    expired: true
  }));
  appRouter.navigate({ pathname: "/badurl" });
  render(<RouterProvider router={appRouter} />);
  expect(screen.getByText(logInText)).toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: logInText, hidden: true })
  ).toBeInTheDocument();
});
