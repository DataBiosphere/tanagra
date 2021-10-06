import { render, screen } from "@testing-library/react";
import React from "react";
import App from "./app";

test("renders included partipants heading", () => {
  render(<App />);
  const linkElement = screen.getByText(/included participants/i);
  expect(linkElement).toBeInTheDocument();
});
