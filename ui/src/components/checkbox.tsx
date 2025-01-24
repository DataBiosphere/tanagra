import CheckBoxIcon from "@mui/icons-material/CheckBox";
import CheckBoxOutlineBlankIcon from "@mui/icons-material/CheckBoxOutlineBlank";
import IconButton from "@mui/material/IconButton";
import { useTheme, alpha } from "@mui/material/styles";
import { cloneElement, ReactElement } from "react";

export type CheckboxProps = {
  checked?: boolean;
  faded?: boolean;
  disabled?: boolean;
  onChange?: () => void;
  size?: "small" | "medium" | "large";
  fontSize?: "small" | "medium" | "large" | "inherit";
  name?: string;
  checkedIcon?: ReactElement;
  uncheckedIcon?: ReactElement;
};

const defaultCheckedIcon = <CheckBoxIcon />;
const defaultUncheckedIcon = <CheckBoxOutlineBlankIcon />;

export default function Checkbox({
  checked,
  faded,
  disabled,
  onChange,
  size,
  fontSize,
  name,
  checkedIcon = defaultCheckedIcon,
  uncheckedIcon = defaultUncheckedIcon,
}: CheckboxProps) {
  const theme = useTheme();

  return (
    <IconButton
      role={"checkbox"}
      size={size}
      name={name}
      disabled={disabled}
      onClick={() => {
        if (onChange) {
          onChange();
        }
      }}
    >
      {checked
        ? cloneElement(checkedIcon, {
            size: size,
            fontSize: fontSize,
            sx: {
              fill: alpha(
                faded
                  ? theme.palette.primary.light
                  : theme.palette.primary.main,
                !disabled ? 1 : theme.palette.action.disabledOpacity
              ),
            },
          })
        : cloneElement(uncheckedIcon, {
            size: size,
            fontSize: fontSize,
            sx: {
              fill: !disabled ? "inherit" : theme.palette.action.disabled,
            },
          })}
    </IconButton>
  );
}
