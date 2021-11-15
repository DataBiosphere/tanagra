import {
  default as BaseMenu,
  MenuProps as BaseMenuProps,
} from "@mui/material/Menu";
import { MouseEvent, ReactElement, useCallback, useState } from "react";

type MenuProps = Omit<
  BaseMenuProps,
  "anchorEl" | "open" | "onClick" | "onClose"
>;

export function useMenu({
  children,
  ...props
}: MenuProps): [
  ReactElement,
  (e: MouseEvent<HTMLElement | undefined>) => void
] {
  const [anchorEl, setAnchorEl] = useState<HTMLElement | undefined>();
  const show = useCallback((e) => setAnchorEl(e.currentTarget), []);

  return [
    // eslint-disable-next-line react/jsx-key
    <BaseMenu
      {...props}
      anchorEl={anchorEl}
      open={!!anchorEl}
      onClick={() => setAnchorEl(undefined)}
      onClose={() => setAnchorEl(undefined)}
    >
      {children}
    </BaseMenu>,
    show,
  ];
}
