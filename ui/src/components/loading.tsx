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
  isMutating?: boolean;
  mutate?: () => void;
};

type Props = {
  size?: string;
  status?: Status;
  children?: React.ReactNode;
  showProgressOnMutate?: boolean;
};

export default function Loading(props: Props) {
  const [visible, setVisible] = useState(false);
  const timerRef = useRef<number>();

  const isLoading =
    props.status?.isLoading ||
    (props.showProgressOnMutate && props.status?.isMutating);

  useEffect(() => {
    if (!isLoading) {
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
  }, [isLoading]);

  if (props.status && !isLoading && !props.status.error) {
    return <>{props.children}</>;
  }

  return (
    <Box sx={props.size !== "small" ? { px: 4, py: 2 } : {}}>
      {showStatus(visible, isLoading, props.status, props.size)}
    </Box>
  );
}

function showStatus(
  visible: boolean,
  isLoading?: boolean,
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
        <Typography variant="h2" sx={{ textAlign: "center" }}>
          Error
        </Typography>
        <Typography paragraph sx={{ textAlign: "center" }}>
          {errorMessage}
        </Typography>
        <div>
          <Button
            onClick={() => status?.mutate?.()}
            variant="contained"
            sx={{ display: "block", m: "auto" }}
          >
            Reload
          </Button>
        </div>
      </>
    );
  }
  return visible ? (
    <CircularProgress
      size={size === "small" ? "1em" : undefined}
      sx={
        size !== "small"
          ? {
              display: "block",
              m: "auto",
            }
          : {}
      }
    />
  ) : null;
}
