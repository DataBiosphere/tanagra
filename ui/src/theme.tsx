import { createTheme } from "@mui/material/styles";
import { TypographyStyleOptions } from "@mui/material/styles/createTypography";

declare module "@mui/material/styles" {
  interface Theme {
    highlightColor?: string;
    canvasColor?: string;
  }
  // allow configuration using `createTheme`
  interface ThemeOptions {
    highlightColor?: string;
    canvasColor?: string;
  }

  interface TypographyVariants {
    body1em: TypographyStyleOptions;
    body2em: TypographyStyleOptions;
    link: TypographyStyleOptions;
  }
  interface TypographyVariantsOptions {
    body1em?: TypographyStyleOptions;
    body2em?: TypographyStyleOptions;
    link?: TypographyStyleOptions;
  }

  interface TypeText {
    muted: string;
  }
}

declare module "@mui/material/Typography" {
  interface TypographyPropsVariantOverrides {
    body1em: true;
    body2em: true;
    link: true;
  }
}

export const theme = createTheme({
  highlightColor: "#FFD54F",
  canvasColor: "#F5F6F7",

  shape: {
    borderRadius: 16,
  },
  typography: {
    fontFamily: "Roboto",
    fontSize: 12,
    h1: {
      fontFamily: "Roboto",
      textTransform: "none",
      fontWeight: 400,
      fontSize: "6.5rem",
      letterSpacing: "-0.01em",
      lineHeight: "125%",
    },
    h2: {
      fontFamily: "Roboto",
      textTransform: "none",
      letterSpacing: "normal",
      fontSize: "4.5rem",
      lineHeight: "125%",
      fontWeight: 200,
    },
    h3: {
      fontFamily: "Roboto",
      textTransform: "none",
      letterSpacing: "normal",
      fontSize: "3rem",
      lineHeight: "125%",
      fontWeight: 300,
    },
    h4: {
      fontFamily: "Roboto",
      textTransform: "none",
      fontWeight: 400,
      fontSize: "2.25rem",
      lineHeight: "133%",
      letterSpacing: "0.0025em",
    },
    h5: {
      fontFamily: "Roboto",
      textTransform: "none",
      fontSize: "1.75rem",
      lineHeight: "128.6%",
      fontWeight: 500,
      letterSpacing: "normal",
    },
    h6: {
      fontFamily: "Roboto",
      textTransform: "none",
      fontSize: "1.25rem",
      lineHeight: "140%",
      fontWeight: 600,
      letterSpacing: "0.0015em",
    },
    subtitle1: {
      fontFamily: "Roboto",
      textTransform: "none",
      letterSpacing: "normal",
      fontSize: ".75rem",
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
    body1em: {
      fontFamily: "Roboto",
      textTransform: "none",
      lineHeight: "150%",
      fontSize: "1rem",
      letterSpacing: "0.0025em",
      fontWeight: 600,
    },
    body2: {
      fontFamily: "Roboto",
      textTransform: "none",
      fontWeight: 400,
      fontSize: ".875rem",
      lineHeight: "143%",
      letterSpacing: "0.0025em",
    },
    body2em: {
      fontFamily: "Roboto",
      textTransform: "none",
      fontSize: ".875rem",
      lineHeight: "143%",
      letterSpacing: "0.0025em",
      fontWeight: 600,
    },
    button: {
      fontFamily: "Roboto",
      textTransform: "none",
      fontWeight: 500,
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
    link: {
      fontFamily: "Roboto",
      textTransform: "none",
      color: "#4450c0",
      fontWeight: 400,
      lineHeight: "150%",
      fontSize: "1rem",
      textDecoration: "underline",
      letterSpacing: "0.0025em",
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
      muted: "rgba(0, 0, 0, 0.6)",
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
          borderRadius: "2.5rem",
        },
      },
    },
    MuiToggleButtonGroup: {
      defaultProps: {
        size: "small",
        color: "primary",
      },
    },
    MuiToggleButton: {
      defaultProps: {
        size: "small",
        color: "primary",
      },
      styleOverrides: {
        root: {
          textTransform: "none",
          borderRadius: "2.5rem",
          padding: "4px 10px 4px 10px",
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
      styleOverrides: {
        root: {
          borderRadius: "2.5rem",
        },
      },
    },
    MuiFilledInput: {
      defaultProps: {
        margin: "dense",
      },
      styleOverrides: {
        root: {
          borderRadius: "2.5rem",
        },
      },
    },
    MuiFormControl: {
      defaultProps: {
        margin: "dense",
        size: "small",
      },
      styleOverrides: {
        root: {
          margin: 0,
        },
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
      styleOverrides: {
        root: {
          borderRadius: "2.5rem",
        },
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
      styleOverrides: {
        root: {
          borderRadius: "2.5rem",
        },
      },
    },
    MuiTable: {
      defaultProps: {
        size: "small",
      },
    },
    MuiStep: {
      styleOverrides: {
        root: {
          padding: "0px",
        },
      },
    },
    MuiStepConnector: {
      styleOverrides: {
        root: {
          padding: "8px",
        },
      },
    },
  },
});

export default theme;
