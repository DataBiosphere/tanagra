import CheckBoxIcon from "@mui/icons-material/CheckBox";
import CheckBoxOutlineBlankIcon from "@mui/icons-material/CheckBoxOutlineBlank";
import IconButton from "@mui/material/IconButton";

export type CheckboxProps = {
  checked?: boolean;
  onChange?: () => void;
  size?: "small" | "medium" | "large";
  fontSize?: "small" | "medium" | "large" | "inherit";
  name?: string;
};

export default function Checkbox({
  checked,
  onChange,
  size,
  fontSize,
  name,
}: CheckboxProps) {
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
      {checked ? (
        <CheckBoxIcon fontSize={fontSize} color="primary" />
      ) : (
        <CheckBoxOutlineBlankIcon
          fontSize={fontSize}
          sx={{ fill: "inherit" }}
        />
      )}
    </IconButton>
  );
}
