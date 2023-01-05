import ErrorIcon from "@mui/icons-material/Error";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import React, { ReactNode, useEffect, useRef, useState } from "react";

type Status = {
  error?: Error;
  isLoading?: boolean;
  mutate?: () => void;
};

type Props = {
  size?: string;
  status?: Status;
  children?: React.ReactNode;
};

export default function Loading(props: Props) {
  const [visible, setVisible] = useState(false);
  const timerRef = useRef<number>();

  useEffect(() => {
    if (!props.status?.isLoading) {
      return;
    }

    // Show the small spinner immediately since it's used inline and the delay
    // causes extra shifting of the surrounding elements.
    if (props.size === "small") {
      setVisible(true);
      return;
    }

    timerRef.current = window.setTimeout(() => {
      setVisible(true);
    }, 800);

    return () => {
      setVisible(false);
      clearTimeout(timerRef.current);
    };
  }, [props.status?.isLoading]);

  if (props.status && !props.status.isLoading && !props.status.error) {
    return <>{props.children}</>;
  }

  return (
    <Box className={props.size === "small" ? "loading-small" : "loading"}>
      {showStatus(visible, props.status, props.size)}
    </Box>
  );
}

function showStatus(
  visible: boolean,
  status?: Status,
  size?: string
): ReactNode {
  if (status?.error && !status?.isLoading) {
    const errorMessage = status.error.message
      ? `An error has occurred: ${status.error.message}`
      : "An unknown error has occurred.";
    if (size === "small") {
      return (
        <Tooltip title={errorMessage} arrow={true}>
          <ErrorIcon />
        </Tooltip>
      );
    }

    return (
      <>
        <Typography variant="h2">Error</Typography>
        <Typography paragraph>{errorMessage}</Typography>
        <div>
          <Button onClick={status?.mutate} variant="contained">
            Reload
          </Button>
        </div>
      </>
    );
  }
  return visible ? (
    <CircularProgress
      style={size === "small" ? { width: "1em", height: "1em" } : {}}
    />
  ) : null;
}
