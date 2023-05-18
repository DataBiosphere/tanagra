import { createTheme } from "@mui/material/styles";

declare module "@mui/material/styles" {
  interface Theme {
    highlightColor?: string;
  }
  // allow configuration using `createTheme`
  interface ThemeOptions {
    highlightColor?: string;
  }
}

export const theme = createTheme({
  highlightColor: "#FFD54F",

  shape: {
    borderRadius: 8,
  },
  typography: {
    fontFamily: "Inter, sans-serif",
    fontSize: 12,
    h1: {
      fontFamily: "Roboto",
      textTransform: "none",
      fontWeight: 400,
      fontSize: "2.5rem",
      letterSpacing: "-0.01em",
      lineHeight: "125%",
    },
    h2: {
      fontFamily: "Roboto",
      textTransform: "none",
      letterSpacing: "normal",
      fontSize: "2rem",
      lineHeight: "125%",
      fontWeight: 200,
    },
    h3: {
      fontFamily: "Roboto",
      textTransform: "none",
      letterSpacing: "normal",
      fontSize: "1.5rem",
      lineHeight: "125%",
      fontWeight: 300,
    },
    h4: {
      fontFamily: "Roboto",
      textTransform: "none",
      fontWeight: 400,
      fontSize: "1.2rem",
      lineHeight: "133%",
      letterSpacing: "0.0025em",
    },
    subtitle1: {
      fontFamily: "Roboto",
      textTransform: "none",
      letterSpacing: "normal",
      fontSize: "1.15rem",
      lineHeight: "128.6%",
      fontWeight: 500,
    },
    body1: {
      fontFamily: "Roboto",
      textTransform: "none",
      fontWeight: 400,
      lineHeight: "150%",
      fontSize: "1rem",
      letterSpacing: "0.0025em",
    },
    body2: {
      fontFamily: "Roboto",
      textTransform: "none",
      fontWeight: 400,
      fontSize: ".875rem",
      lineHeight: "143%",
      letterSpacing: "0.0025em",
    },
    overline: {
      fontFamily: "Roboto",
      fontWeight: 300,
      lineHeight: "150%",
      fontSize: ".75rem",
      letterSpacing: "0.03em",
      textTransform: "uppercase",
    },
  },
  palette: {
    mode: "light",
    primary: {
      main: "#4450c0",
      light: "#e3e5f6",
      dark: "#131964",
      contrastText: "#ffffff",
    },
    background: {
      default: "#F5F5F5",
      paper: "#ffffff",
    },
    text: {
      primary: "#2e3539",
      secondary: "#171b1d",
      disabled: "#c7cbcc",
    },
    error: {
      main: "#d14545",
      light: "#fde9e9",
      dark: "#b00010",
      contrastText: "#ffffff",
    },
    warning: {
      main: "#f3bb29",
      light: "#fdf6e4",
      dark: "#674c07",
      contrastText: "#4f3715",
    },
    info: {
      main: "#eaedee",
      light: "#eaedee",
      dark: "#eaedee",
      contrastText: "#000000",
    },
    success: {
      main: "#148750",
      light: "#e6f5ee",
      dark: "#006d3f",
      contrastText: "#ffffff",
    },
    divider: "rgba(131,139,143,0.5)",
    action: {
      selected: "#34A853",
      selectedOpacity: 0.2,
    },
  },
  components: {
    MuiButton: {
      defaultProps: {
        size: "small",
        disableElevation: true,
      },
      styleOverrides: {
        root: {
          textTransform: "none",
        },
      },
    },
    MuiPaper: {
      defaultProps: {
        elevation: 0,
      },
    },
    MuiAppBar: {
      defaultProps: {
        elevation: 0,
        color: "transparent",
      },
      styleOverrides: {
        root: {
          backgroundColor: "white",
        },
      },
    },
    MuiToolbar: {
      defaultProps: {
        variant: "dense",
      },
    },
    MuiChip: {
      defaultProps: {
        size: "small",
        color: "info",
      },
      styleOverrides: {
        root: {
          borderRadius: "4px",
          height: "20px",
        },
        label: {
          fontFamily: "Roboto",
          fontWeight: 400,
          lineHeight: "150%",
          fontSize: ".75rem",
          letterSpacing: "0.04em",
          textTransform: "uppercase",
          padding: "1px 8px 0px 8px",
        },
      },
    },
    MuiCheckbox: {
      defaultProps: {
        color: "primary",
      },
    },
    MuiTextField: {
      defaultProps: {
        margin: "dense",
      },
    },
    MuiFilledInput: {
      defaultProps: {
        margin: "dense",
      },
    },
    MuiFormControl: {
      defaultProps: {
        margin: "dense",
        size: "small",
      },
    },
    MuiFormHelperText: {
      defaultProps: {
        margin: "dense",
      },
    },
    MuiIconButton: {
      defaultProps: {
        color: "primary",
        size: "small",
      },
    },
    MuiInputBase: {
      defaultProps: {
        margin: "dense",
      },
    },
    MuiInputLabel: {
      defaultProps: {
        margin: "dense",
      },
    },
    MuiListItem: {
      defaultProps: {
        dense: true,
      },
    },
    MuiOutlinedInput: {
      defaultProps: {
        margin: "dense",
      },
    },
    MuiTable: {
      defaultProps: {
        size: "small",
      },
    },
  },
});

export default theme;
