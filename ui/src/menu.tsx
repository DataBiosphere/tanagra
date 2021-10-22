import ListItemIcon from "@mui/material/ListItemIcon";
import ListItemText from "@mui/material/ListItemText";
import {
  default as BaseMenu,
  MenuProps as BaseMenuProps,
} from "@mui/material/Menu";
import MenuItem from "@mui/material/MenuItem";
import { MouseEvent, ReactElement, useCallback, useState } from "react";

interface ItemProps {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  key?: any;
  text?: string;
  icon?: ReactElement;
  onClick?: () => void;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  props?: { [name: string]: any };
}

interface MenuProps
  extends Omit<BaseMenuProps, "anchorEl" | "open" | "onClick" | "onClose"> {
  //items: ReactNode;
  items: ItemProps[];
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
      {props.items.map((item, index) => (
        <MenuItem
          {...item.props}
          key={item.key || index}
          onClick={item.onClick}
        >
          {item.icon ? <ListItemIcon>{item.icon}</ListItemIcon> : null}
          {item.text ? <ListItemText>{item.text}</ListItemText> : null}
        </MenuItem>
      ))}
    </BaseMenu>,
    show,
  ];
}
