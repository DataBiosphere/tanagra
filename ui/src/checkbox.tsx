import CheckBoxIcon from "@mui/icons-material/CheckBox";
import CheckBoxOutlineBlankIcon from "@mui/icons-material/CheckBoxOutlineBlank";
import IconButton from "@mui/material/IconButton";

export type CheckboxProps = {
  checked?: boolean;
  onChange?: () => void;
  size?: "small" | "medium" | "large";
  fontSize?: "small" | "medium" | "large" | "inherit";
};

export default function Checkbox({
  checked,
  onChange,
  size,
  fontSize,
}: CheckboxProps) {
  return (
    <IconButton
      role={"checkbox"}
      size={size}
      onClick={() => {
        if (onChange) {
          onChange();
        }
      }}
    >
      {checked ? (
        <CheckBoxIcon fontSize={fontSize} />
      ) : (
        <CheckBoxOutlineBlankIcon fontSize={fontSize} />
      )}
    </IconButton>
  );
}
