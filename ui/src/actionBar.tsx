import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import IconButton from "@mui/material/IconButton";
import Typography from "@mui/material/Typography";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { Link as RouterLink } from "react-router-dom";

type ActionBarProps = {
  title: string;
  subtitle?: string | JSX.Element;
  backAction?: (() => void) | string | null; // null hides the back button.
  titleControls?: JSX.Element;
  rightControls?: JSX.Element;
};

function backActionProps(backAction?: (() => void) | string | null) {
  if (backAction === null) {
    return {};
  }
  if (typeof backAction === "function") {
    return {
      onClick: () => backAction(),
    };
  }
  return {
    component: RouterLink,
    to: backAction ?? "..",
  };
}

export default function ActionBar(props: ActionBarProps) {
  return (
    <GridLayout
      cols
      fillCol={0}
      rowAlign="middle"
      sx={{
        px: 4,
        py: 2,
        backgroundColor: (theme) => theme.palette.background.paper,
        borderBottomColor: (theme) => theme.palette.divider,
        borderBottomStyle: "solid",
        borderBottomWidth: "1px",
      }}
    >
      <GridLayout
        cols={2}
        rows={props.subtitle ? 2 : undefined}
        rowAlign="middle"
        height="auto"
      >
        <IconButton
          aria-label="back"
          {...backActionProps(props.backAction)}
          sx={{
            mr: 2,
            visibility: props.backAction === null ? "hidden" : "visible",
            "&.MuiIconButton-root": {
              backgroundColor: (theme) => theme.palette.primary.main,
              color: (theme) => theme.palette.primary.contrastText,
            },
            "&.MuiIconButton-root:hover": {
              backgroundColor: (theme) => theme.palette.primary.dark,
            },
          }}
        >
          <ArrowBackIcon />
        </IconButton>
        <GridLayout rows height="auto">
          <GridLayout cols spacing={1} rowAlign="middle" height="auto">
            <Typography
              variant="h6"
              sx={{
                textOverflow: "ellipsis",
                whiteSpace: "nowrap",
                overflow: "hidden",
              }}
            >
              {props.title}
            </Typography>
            {props.titleControls}
          </GridLayout>
        </GridLayout>

        {props.subtitle ? <GridBox /> : null}
        {props.subtitle ? (
          <Typography variant="body2">{props.subtitle}</Typography>
        ) : null}
      </GridLayout>
      {props.rightControls ?? <GridBox />}
    </GridLayout>
  );
}
