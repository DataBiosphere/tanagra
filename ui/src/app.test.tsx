import { render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { Provider } from "react-redux";
import { store } from "store";
import App from "./app";

test("render included datasets heading", async () => {
  render(
    <Provider store={store}>
      <App entityName={"person"} />
    </Provider>
  );
  await waitFor(() =>
    expect(screen.getByText(/datasets/i)).toBeInTheDocument()
  );
});
