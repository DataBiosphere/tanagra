import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import React from "react";
import { Provider } from "react-redux";
import { store } from "store";
import App from "./app";

test("render included datasets heading", async () => {
  render(
    <Provider store={store}>
      <App />
    </Provider>
  );
  await waitFor(() => {
    expect(screen.getByText(/select dataset/i)).toBeInTheDocument();
    expect(screen.getByText(/underlay_name/i)).toBeInTheDocument();
  });

  userEvent.click(screen.getByText("underlay_name"));
  await waitFor(() => expect(screen.getByText(/studies/i)).toBeInTheDocument());
});
