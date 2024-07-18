import App from "app";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import React from "react";

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
