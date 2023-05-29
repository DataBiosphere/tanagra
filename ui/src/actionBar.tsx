import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import AppBar from "@mui/material/AppBar";
import IconButton from "@mui/material/IconButton";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import { useAppSelector } from "hooks";
import { Link as RouterLink, useParams } from "react-router-dom";

type ActionBarProps = {
  title: string;
  backURL?: string | null; // null hides the back button.
  extraControls?: JSX.Element;
};

export default function ActionBar(props: ActionBarProps) {
  const { underlayName } = useParams<{ underlayName: string }>();
  const underlay = useAppSelector((state) =>
    state.present.underlays.find((underlay) => underlay.name === underlayName)
  );

  return (
    <AppBar
      position="fixed"
      sx={{
        zIndex: (theme) => theme.zIndex.drawer + 1,
        borderBottomColor: (theme) => theme.palette.divider,
        borderBottomStyle: "solid",
        borderBottomWidth: "1px",
      }}
    >
      <Toolbar disableGutters>
        <IconButton
          color="primary"
          aria-label="back"
          component={RouterLink}
          to={props.backURL ?? ".."}
          sx={{
            mx: 1,
            visibility: props.backURL === null ? "hidden" : "visible",
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
        <Typography
          variant="h6"
          sx={{
            flexGrow: 1,
            textOverflow: "ellipsis",
            whiteSpace: "nowrap",
            overflow: "hidden",
          }}
        >
          {props.title}
        </Typography>
        {props.extraControls}
        {underlay ? (
          <Typography variant="body1" sx={{ mx: 1 }}>
            Dataset: {underlay.name}
          </Typography>
        ) : null}
      </Toolbar>
    </AppBar>
  );
}
