import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import { useAppSelector } from "hooks";
import * as React from "react";
import { Link as RouterLink, useParams } from "react-router-dom";
import { useParentUrl } from "router";
import UndoRedo from "./components/UndoRedo";

type ActionBarProps = {
  title: string;
};

export default function ActionBar(props: ActionBarProps) {
  const { underlayName } = useParams<{ underlayName: string }>();
  const underlay = useAppSelector((state) =>
    state.underlays.find((underlay) => underlay.name === underlayName)
  );

  const backUrl = useParentUrl();

  return (
    <Box sx={{ flexGrow: 1 }} className="action-bar">
      <AppBar position="fixed">
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="back"
            component={RouterLink}
            to={backUrl || "/"}
            sx={{ visibility: backUrl ? "visible" : "hidden" }}
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
