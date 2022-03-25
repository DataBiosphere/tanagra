import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Typography from "@mui/material/Typography";
import React, { ReactNode, useEffect, useRef, useState } from "react";

type Status = {
  error?: Error;
  isPending: boolean;
  reload: () => void;
};

type Props = {
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

  return <Box className="loading">{showStatus(visible, props.status)}</Box>;
}

function showStatus(visible: boolean, status?: Status): ReactNode {
  if (status?.error && !status?.isPending) {
    return (
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
          <Button onClick={status.reload} variant="contained">
            Reload
          </Button>
        </div>
      </>
    );
  }
  return visible ? <CircularProgress /> : null;
}
