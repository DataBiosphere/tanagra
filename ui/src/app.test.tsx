import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import  { App, AppWithRouter } from "app";
import { signInText, useAuth } from "auth/provider";
import { getEnvironment } from "environment";
import React from "react";
import { createAppRouter } from "router";

afterEach(cleanup)

function renderAppReturnRouter(withAuth?: boolean,
  expired?: boolean,
                        noProfile?: boolean
) {
  if (withAuth) {
    const env = getEnvironment();
    env.REACT_APP_AUTH0_DOMAIN = "fake-domain"
    env.REACT_APP_AUTH0_CLIENT_ID = "fake-client"
  }
  const router = createAppRouter();
  render(<AppWithRouter appRouter={router} />);
  return router;
}


test("render included datasets heading", async () => {
  renderAppReturnRouter();
  await waitFor(() => {
    expect(screen.getByText(/underlay_name/i)).toBeInTheDocument();
  });

  userEvent.click(screen.getByText("underlay_name"));
  await waitFor(() =>
    expect(screen.getByText(/add study/i)).toBeInTheDocument()
  );
});

test("signed out redirects to login screen", () => {
  renderAppReturnRouter(/* withAuth= */ true);
  expect(screen.getByText(signInText)).toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: signInText })
  ).toBeInTheDocument();
});
