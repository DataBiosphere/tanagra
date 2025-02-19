import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import Loading from "components/loading";

test("loading", async () => {
  const { rerender } = render(
    <Loading status={{ isLoading: true }}>loaded</Loading>
  );
  await screen.findByRole("progressbar");

  expect(screen.queryByText("Reload")).not.toBeInTheDocument();
  expect(screen.queryByText("loaded")).not.toBeInTheDocument();

  const mutate = vitest.fn();
  rerender(
    <Loading status={{ error: new Error("test-error"), mutate: mutate }} />
  );
  expect(screen.getByText("test-error")).toBeInTheDocument();

  await userEvent.click(screen.getByText("Reload"));
  expect(mutate).toHaveBeenCalled();

  rerender(<Loading status={{ isLoading: true }} />);
  await screen.findByRole("progressbar");

  rerender(<Loading status={{}}>loaded</Loading>);
  screen.getByText("loaded");
});
