import { createTheme } from "@mui/material/styles";

export const theme = createTheme({
  typography: {
    fontFamily: "Inter, sans-serif",
    fontSize: 12,
    h1: {
      fontFamily: "'Inter', sans-serif",
      fontWeight: 400,
      fontSize: 32,
      lineHeight: "40px",
    },
    h2: {
      fontFamily: "'Red Hat Display', sans-serif",
      fontWeight: 500,
      fontSize: 24,
      lineHeight: "32px",
    },
    h3: {
      fontFamily: "'Red Hat Display', sans-serif",
      fontWeight: 400,
      fontSize: 20,
      lineHeight: "26px",
    },
    h4: {
      fontFamily: "'Inter', sans-serif",
      fontWeight: 400,
      fontSize: 16,
      lineHeight: "20px",
    },
    subtitle1: {
      fontFamily: "'Inter', sans-serif",
      fontWeight: 500,
      fontSize: 12,
      lineHeight: "20px",
    },
    body1: {
      fontFamily: "'Inter', sans-serif",
      fontSize: 12,
      lineHeight: "20px",
    },
    body2: {
      fontFamily: "'Inter', sans-serif",
      fontSize: 10,
      lineHeight: "14px",
    },
    overline: {
      fontFamily: "'Inter', sans-serif",
      fontWeight: 500,
      fontSize: 11,
      lineHeight: "16px",
    },
  },
  palette: {
    primary: {
      main: "#0A96AA",
      light: "#59c7dc",
      dark: "#00687b",
    },
    info: {
      main: "#EEEEEE",
    },
    background: {
      default: "#F5F5F5",
    },
    text: {
      primary: "#212121",
    },
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
          height: "16px",
        },
        label: {
          fontFamily: "'Inter', sans-serif",
          fontWeight: "500",
          fontSize: "11px",
          textTransform: "uppercase",
          letterSpacing: ".5px",
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
