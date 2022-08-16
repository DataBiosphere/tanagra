import ErrorIcon from "@mui/icons-material/Error";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import React, { ReactNode, useEffect, useRef, useState } from "react";

type Status = {
  error?: Error;
  isPending?: boolean;
  reload?: () => void;
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
    if (!props.status?.isPending) {
      return;
    }

    timerRef.current = window.setTimeout(() => {
      setVisible(true);
    }, 800);

    return () => {
      setVisible(false);
      clearTimeout(timerRef.current);
    };
  }, [props.status?.isPending]);

  if (props.status && !props.status.isPending && !props.status.error) {
    return <>{props.children}</>;
  }

  return (
    <Box className={props.size === "small" ? "" : "loading"}>
      {showStatus(visible, props.status, props.size)}
    </Box>
  );
}

function showStatus(
  visible: boolean,
  status?: Status,
  size?: string
): ReactNode {
  if (status?.error && !status?.isPending) {
    return size === "small" ? (
      <Tooltip title={status.error?.message} arrow={true}>
        <ErrorIcon />
      </Tooltip>
    ) : (
      <>
        <Typography variant="h2">Error</Typography>
        {status.error?.message ? (
          <Typography paragraph>
            An error has occurred: <b>{status.error.message}</b>
          </Typography>
        ) : (
          <Typography paragraph>An unknown error has occurred.</Typography>
        )}
        <div>
          <Button onClick={status?.reload} variant="contained">
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
