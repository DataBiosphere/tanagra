import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import * as React from "react";
import { Link as RouterLink } from "react-router-dom";
import { Cohort } from "./cohort";

type ActionBarProps = {
  title: string;
  backUrl?: string;
  cohort?: Cohort;
};

export default function ActionBar(props: ActionBarProps) {
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
          {props.cohort ? (
            <Typography variant="h6" className="underlay-name">
              Dataset: {props.cohort.underlayName}
            </Typography>
          ) : null}
        </Toolbar>
      </AppBar>
      <Toolbar /> {/*Prevent content from flowing under the AppBar.*/}
    </Box>
  );
}
