import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import FileDownloadIcon from "@mui/icons-material/Download";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import * as React from "react";
import { Link as RouterLink } from "react-router-dom";
import { Cohort } from "./cohort";
import { useSqlDialog } from "./sqlDialog";

type ActionBarProps = {
  title: string;
  backUrl?: string;
  cohort: Cohort;
};

export default function ActionBar(props: ActionBarProps) {
  const [dialog, showSqlDialog] = useSqlDialog({ cohort: props.cohort });

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
          <Typography variant="h6" className="underlay-name">
            Dataset: {props.cohort.underlayName}
          </Typography>
          <IconButton component="span" color="inherit" onClick={showSqlDialog}>
            <FileDownloadIcon />
          </IconButton>
          {dialog}
        </Toolbar>
      </AppBar>
      <Toolbar /> {/*Prevent content from flowing under the AppBar.*/}
    </Box>
  );
}
