import { Palette, Theme } from "@mui/material/styles";

// Only allow the palette entries that are regular color palettes.
export type ColorsFromPalette = Pick<Palette, "primary" | "warning" | "error">;

// The current MUI version doesn't support "contained" IconButtons like Button
// does so add reusable styling to emulate it.
export function containedIconButtonSx(palette: keyof ColorsFromPalette) {
  return {
    "&.MuiIconButton-root": {
      backgroundColor: (theme: Theme) => theme.palette[palette].main,
      color: (theme: Theme) => theme.palette[palette].contrastText,
    },
    "&.MuiIconButton-root:hover": {
      backgroundColor: (theme: Theme) => theme.palette[palette].dark,
    },
  };
}
