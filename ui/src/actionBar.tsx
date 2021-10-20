import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import FileDownloadIcon from "@mui/icons-material/FileDownload";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import * as React from "react";
import { Link as RouterLink } from "react-router-dom";

type ActionBarProps = {
  title: string;
  backUrl?: string;
};

export default function ActionBar(props: ActionBarProps) {
  return (
    <Box sx={{ flexGrow: 1 }}>
      <AppBar position="static">
        <Toolbar>
          {!!props.backUrl ? (
            <IconButton
              size="large"
              color="inherit"
              aria-label="back"
              component={RouterLink}
              to={props.backUrl}
            >
              <ArrowBackIcon />
            </IconButton>
          ) : null}
          <Typography variant="h3" sx={{ flexGrow: 1 }}>
            {props.title}
          </Typography>
          <IconButton size="large" color="inherit">
            <FileDownloadIcon />
          </IconButton>
        </Toolbar>
      </AppBar>
    </Box>
  );
}
