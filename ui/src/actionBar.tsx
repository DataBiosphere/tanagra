import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import { useUnderlay } from "hooks";
import * as React from "react";
import { Link as RouterLink } from "react-router-dom";

type ActionBarProps = {
  title: string;
  backUrl?: string;
};

export default function ActionBar(props: ActionBarProps) {
  const underlay = useUnderlay();

  return (
    <Box sx={{ flexGrow: 1 }} className="action-bar">
      <AppBar position="fixed">
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="back"
            component={RouterLink}
            to={props.backUrl || "/"}
            sx={{ visibility: props.backUrl ? "visible" : "hidden" }}
          >
            <ArrowBackIcon />
          </IconButton>
          <Typography variant="h3" sx={{ flexGrow: 1 }}>
            {props.title}
          </Typography>
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
