import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import React from "react";
import Loading from "./loading";

test("loading", async () => {
  const { rerender } = render(
    <Loading status={{ isPending: true }}>loaded</Loading>
  );
  await screen.findByRole("progressbar");

  expect(screen.queryByText("Reload")).not.toBeInTheDocument();
  expect(screen.queryByText("loaded")).not.toBeInTheDocument();

  const reload = jest.fn();
  rerender(
    <Loading status={{ error: new Error("test-error"), reload: reload }} />
  );
  screen.findByText((_, node) => {
    const hasText = (node: Element | null) =>
      node?.textContent === "test_error";
    const nodeHasText = hasText(node);
    const childrenDontHaveText = node
      ? Array.from(node.children).every((child) => !hasText(child))
      : true;

    return nodeHasText && childrenDontHaveText;
  });

  userEvent.click(screen.getByText("Reload"));
  expect(reload).toHaveBeenCalled();

  rerender(<Loading status={{ isPending: true }} />);
  await screen.findByRole("progressbar");

  rerender(<Loading status={{}}>loaded</Loading>);
  screen.getByText("loaded");
});
