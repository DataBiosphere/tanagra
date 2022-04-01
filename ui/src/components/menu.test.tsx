import Button from "@mui/material/Button";
import MenuItem from "@mui/material/MenuItem";
import {
  render,
  screen,
  waitForElementToBeRemoved,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useMenu } from "components/menu";
import React from "react";

function MenuButton(props: { onMenuItem: () => void }) {
  const [menu, show] = useMenu({
    children: <MenuItem onClick={props.onMenuItem}>menu-item</MenuItem>,
  });
  return (
    <>
      <Button onClick={show}>show</Button>
      {menu}
    </>
  );
}

test("show/hide menu", async () => {
  const onMenuItem = jest.fn();
  render(<MenuButton onMenuItem={onMenuItem} />);

  userEvent.click(screen.getByRole("button"));

  userEvent.click(screen.getByText("menu-item"));
  expect(onMenuItem).toHaveBeenCalled();

  waitForElementToBeRemoved(screen.queryByText("menu-item"));
});
