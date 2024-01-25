import CheckBoxIcon from "@mui/icons-material/CheckBox";
import CheckBoxOutlineBlankIcon from "@mui/icons-material/CheckBoxOutlineBlank";
import IconButton from "@mui/material/IconButton";
import { useTheme } from "@mui/material/styles";
import { cloneElement, ReactElement } from "react";

export type CheckboxProps = {
  checked?: boolean;
  faded?: boolean;
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
            ...(faded
              ? {
                  sx: {
                    fill: theme.palette.primary.light,
                  },
                }
              : { color: "primary" }),
          })
        : cloneElement(uncheckedIcon, {
            size: size,
            fontSize: fontSize,
            sx: {
              fill: "inherit",
            },
          })}
    </IconButton>
  );
}
