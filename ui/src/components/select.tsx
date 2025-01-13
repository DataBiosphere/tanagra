import { Theme } from "@mui/material/styles";

export function uncontainedSelectSx() {
  return {
    color: (theme: Theme) => theme.palette.primary.main,
    "& .MuiOutlinedInput-input": {
      px: 0,
      py: "2px",
    },
    "& .MuiSelect-select": (theme: Theme) => ({
      ...theme.typography.body2,
    }),
    "& .MuiOutlinedInput-notchedOutline": {
      borderStyle: "none",
    },
  };
}
