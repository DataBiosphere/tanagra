import Button from "@mui/material/Button";
import MenuItem from "@mui/material/MenuItem";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useMenu } from "components/menu";

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
  const onMenuItem = vitest.fn();
  render(<MenuButton onMenuItem={onMenuItem} />);

  await userEvent.click(screen.getByRole("button"));

  await userEvent.click(screen.getByText("menu-item"));
  expect(onMenuItem).toHaveBeenCalled();

  expect(screen.queryByText("menu-item")).not.toBeInTheDocument();
});
