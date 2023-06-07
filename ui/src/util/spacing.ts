import { Theme } from "@mui/material/styles";

export function spacing(theme: Theme, value?: string | number) {
  return typeof value === "number" ? theme.spacing(value) : value;
}
