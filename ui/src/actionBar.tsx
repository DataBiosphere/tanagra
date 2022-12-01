import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import SettingsIcon from "@mui/icons-material/Settings";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
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
    <Box className="action-bar">
      <AppBar
        position="fixed"
        sx={{
          zIndex: (theme) => theme.zIndex.drawer + 1,
          borderBottomColor: (theme) => theme.palette.divider,
          borderBottomStyle: "solid",
          borderBottomWidth: "1px",
        }}
      >
        <Toolbar>
          <IconButton
            color="primary"
            aria-label="back"
            component={RouterLink}
            to={props.backURL ?? ".."}
            sx={{
              visibility: props.backURL === null ? "hidden" : "visible",
            }}
          >
            <ArrowBackIcon />
          </IconButton>
          <Typography
            variant="h1"
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
          <Button
            variant="contained"
            startIcon={<SettingsIcon />}
            component={RouterLink}
            to="/sdAdmin"
          >
            SD Admin
          </Button>
          {underlay ? (
            <Typography variant="h4" className="underlay-name" sx={{ ml: 1 }}>
              Dataset: {underlay.name}
            </Typography>
          ) : null}
        </Toolbar>
      </AppBar>
    </Box>
  );
}
