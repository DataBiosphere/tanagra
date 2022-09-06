import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import { useAppSelector } from "hooks";
import * as React from "react";
import { Link as RouterLink, useLocation, useParams } from "react-router-dom";
import UndoRedo from "./components/UndoRedo";

type ActionBarProps = {
  title: string;
};

export default function ActionBar(props: ActionBarProps) {
  const { underlayName } = useParams<{ underlayName: string }>();
  const underlay = useAppSelector((state) =>
    state.present.underlays.find((underlay) => underlay.name === underlayName)
  );

  const location = useLocation();

  return (
    <Box sx={{ flexGrow: 1 }} className="action-bar">
      <AppBar
        position="fixed"
        sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="back"
            component={RouterLink}
            to={".."}
            sx={{
              visibility: location.pathname === "/" ? "hidden" : "visible",
            }}
          >
            <ArrowBackIcon />
          </IconButton>
          <Typography variant="h4" sx={{ flexGrow: 1 }}>
            {props.title}
          </Typography>
          <UndoRedo />
          {underlay ? (
            <Typography variant="h6" className="underlay-name">
              Dataset: {underlay.name}
            </Typography>
          ) : null}
        </Toolbar>
      </AppBar>
      <Toolbar /> {/*Prevent content from flowing under the AppBar.*/}
    </Box>
  );
}
