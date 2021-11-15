import { render, screen, waitFor } from "@testing-library/react";
import React from "react";
import App from "./app";

test("render included datasets heading", async () => {
  render(<App underlayNames={["omop_test"]} />);
  await waitFor(() =>
    expect(screen.getByText(/datasets/i)).toBeInTheDocument()
  );
});
