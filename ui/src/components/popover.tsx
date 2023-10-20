import {
  default as BasePopover,
  PopoverProps as BasePopoverProps,
} from "@mui/material/Popover";
import { MouseEvent, ReactElement, useState } from "react";

type PopoverProps = Omit<BasePopoverProps, "anchorEl" | "open" | "onClose">;

export function usePopover({
  children,
  ...props
}: PopoverProps): [
  ReactElement,
  (e: MouseEvent<HTMLElement | undefined>) => void
] {
  const [anchorEl, setAnchorEl] = useState<HTMLElement | undefined>();
  const show = (e: MouseEvent<HTMLElement | undefined>) =>
    setAnchorEl(e.currentTarget);

  return [
    // eslint-disable-next-line react/jsx-key
    <BasePopover
      {...props}
      anchorEl={anchorEl}
      open={!!anchorEl}
      onClose={() => setAnchorEl(undefined)}
      anchorOrigin={{
        vertical: "bottom",
        horizontal: "left",
      }}
    >
      {children}
    </BasePopover>,
    show,
  ];
}
