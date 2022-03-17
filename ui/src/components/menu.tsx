import {
  default as BaseMenu,
  MenuProps as BaseMenuProps,
} from "@mui/material/Menu";
import { MouseEvent, ReactElement, useState } from "react";

type MenuProps = Omit<
  BaseMenuProps,
  "anchorEl" | "open" | "onClick" | "onClose"
>;

export function useMenu({
  children,
  ...props
}: MenuProps,
clickNclose = true): [
  ReactElement,
  (e: MouseEvent<HTMLElement | undefined>) => void
] {
  const [anchorEl, setAnchorEl] = useState<HTMLElement | undefined>();
  const show = (e: MouseEvent<HTMLElement | undefined>) =>
    setAnchorEl(e.currentTarget);

  return [
    // eslint-disable-next-line react/jsx-key
    <BaseMenu
      {...props}
      anchorEl={anchorEl}
      open={!!anchorEl}
      onClick={() => {if (clickNclose) {setAnchorEl(undefined)}}}
      onClose={() => setAnchorEl(undefined)}
    >
      {children}
    </BaseMenu>,
    show,
  ];
}
