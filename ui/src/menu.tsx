import {
  default as BaseMenu,
  MenuProps as BaseMenuProps,
} from "@mui/material/Menu";
import {
  MouseEvent,
  ReactElement,
  ReactNode,
  useCallback,
  useState,
} from "react";

interface MenuProps
  extends Omit<BaseMenuProps, "anchorEl" | "open" | "onClick" | "onClose"> {
  items: ReactNode;
}

export function useMenu(
  props: MenuProps
): [ReactElement, (e: MouseEvent<HTMLElement | undefined>) => void] {
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
      {props.items}
    </BaseMenu>,
    show,
  ];
}
