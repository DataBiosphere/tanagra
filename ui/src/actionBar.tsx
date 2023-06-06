import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import IconButton from "@mui/material/IconButton";
import Typography from "@mui/material/Typography";
import { GridBox } from "layout/gridBox";
import GridLayout from "layout/gridLayout";
import { Link as RouterLink } from "react-router-dom";

type ActionBarProps = {
  title: string;
  subtitle?: string | JSX.Element;
  height?: number | string;
  backURL?: string | null; // null hides the back button.
  extraControls?: JSX.Element;
};

export default function ActionBar(props: ActionBarProps) {
  return (
    <GridLayout
      rows
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
        cols={4}
        rows={props.subtitle ? 2 : undefined}
        fillCol={2}
        rowAlign="middle"
        height="auto"
      >
        <IconButton
          aria-label="back"
          component={RouterLink}
          to={props.backURL ?? ".."}
          sx={{
            mr: 2,
            visibility: props.backURL === null ? "hidden" : "visible",
          }}
        >
          <ArrowBackIcon />
        </IconButton>
        <GridLayout rows height="auto">
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
        </GridLayout>
        <GridBox />
        {props.extraControls ?? <GridBox />}

        {props.subtitle ? <GridBox /> : null}
        {props.subtitle ? (
          <Typography variant="body2">{props.subtitle}</Typography>
        ) : null}
        {props.subtitle ? <GridBox /> : null}
      </GridLayout>
    </GridLayout>
  );
}
