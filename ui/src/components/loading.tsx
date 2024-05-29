import ErrorIcon from "@mui/icons-material/Error";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import { Theme, useTheme } from "@mui/material/styles";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import errorImage from "images/error.png";
import GridLayout from "layout/gridLayout";
import React, { ReactNode, useEffect, useRef, useState } from "react";

type Status = {
  error?: Error;
  isLoading?: boolean;
  isMutating?: boolean;
  mutate?: () => void;
};

type Props = {
  size?: "small" | "medium" | "large";
  status?: Status;
  children?: React.ReactNode;
  showProgressOnMutate?: boolean;
  disableReloadButton?: boolean;
  immediate?: boolean;
};

export default function Loading(props: Props) {
  const theme = useTheme();
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
    if (props.immediate || props.size === "small") {
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
    <Box>
      {showStatus(
        theme,
        visible,
        isLoading,
        props.status,
        props.size ?? "large",
        props.disableReloadButton
      )}
    </Box>
  );
}

function showStatus(
  theme: Theme,
  visible: boolean,
  isLoading?: boolean,
  status?: Status,
  size?: string,
  disableReloadButton?: boolean
): ReactNode {
  if (status?.error && !status?.isLoading) {
    const defaultMessage = "Something went wrong";
    if (size === "small") {
      return (
        <Tooltip title={status.error.message ?? defaultMessage} arrow={true}>
          <ErrorIcon sx={{ display: "block" }} />
        </Tooltip>
      );
    }

    return (
      <GridLayout
        cols
        rowAlign="middle"
        colAlign="center"
        sx={{ px: 2, py: 1, minHeight: "200px" }}
      >
        <GridLayout
          colAlign="center"
          spacing={1}
          height="auto"
          sx={{ textAlign: "center" }}
        >
          {size === "large" ? (
            <img src={errorImage} style={{ width: "200px", height: "auto" }} />
          ) : null}
          <Typography variant="h6">{defaultMessage}</Typography>
          {status.error.message ? (
            <Typography variant="body1">{status.error.message}</Typography>
          ) : null}
          {!disableReloadButton ? (
            <Button onClick={() => status?.mutate?.()} variant="contained">
              Reload
            </Button>
          ) : null}
        </GridLayout>
      </GridLayout>
    );
  }
  return visible ? (
    <CircularProgress
      size={size === "small" ? theme.typography.body2.fontSize : undefined}
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
