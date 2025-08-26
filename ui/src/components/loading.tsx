import ErrorIcon from "@mui/icons-material/Error";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import { Theme, useTheme } from "@mui/material/styles";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import HourglassBottomIcon from "@mui/icons-material/HourglassBottom";
import HourglassEmptyIcon from "@mui/icons-material/HourglassEmpty";
import HourglassFullIcon from "@mui/icons-material/HourglassFull";
import errorImage from "images/error.png";
import { GridBox } from "layout/gridBox";
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
  noProgress?: boolean;

  // Override default isLoading from status.
  isLoading?: boolean;
  showLoadingMessage?: boolean;
};

export default function Loading(props: Props) {
  const theme = useTheme();
  const [visible, setVisible] = useState(false);
  const [showText, setShowText] = useState(false);
  const timerRef = useRef<number>();
  const textTimer = useRef<number>();

  const isLoading =
    props.isLoading ??
    (props.status?.isLoading ||
      (props.showProgressOnMutate && props.status?.isMutating));

  useEffect(() => {
    if (!isLoading) {
      return;
    }

    textTimer.current = window.setTimeout(() => {
      setShowText(true);
    }, 10000);

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
      setShowText(false);
      clearTimeout(timerRef.current);
      clearTimeout(textTimer.current);
    };
  }, [isLoading, props]);

  if (props.status && !isLoading && !props.status.error) {
    return <>{props.children}</>;
  }

  return (
    <Box>
      {showStatus(
        theme,
        visible,
        showText,
        isLoading,
        props.status,
        props.size ?? "large",
        props.disableReloadButton,
        props.noProgress,
        props.showLoadingMessage
      )}
    </Box>
  );
}

function showStatus(
  theme: Theme,
  visible: boolean,
  showText: boolean,
  isLoading?: boolean,
  status?: Status,
  size?: string,
  disableReloadButton?: boolean,
  noProgress?: boolean,
  showLoadingMessage?: boolean
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
  return !noProgress && visible ? (
    size === "small" ? (
      <Box sx={{ position: "relative", display: "inline-flex" }}>
        <CircularProgress size="1.35rem" />

        {showLoadingMessage && showText && (
          <Box
            sx={{
              top: 1,
              left: 0,
              bottom: 0,
              right: 0,
              position: "absolute",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
            title="Just a moment while we politely interrogate a very large database. It has a lot to say."
          >
            <HourglassAnimation />
          </Box>
        )}
      </Box>
    ) : (
      <GridBox sx={{ p: 3, textAlign: "center" }}>
        <CircularProgress
          sx={{
            display: "block",
            m: "auto",
          }}
        />

        {showLoadingMessage && showText && (
          <p style={{ fontWeight: "bold" }}>
            Just a moment while we politely interrogate a very large database.
            It has a lot to say.
          </p>
        )}
      </GridBox>
    )
  ) : null;
}

function HourglassAnimation() {
  const icons = [
    <HourglassEmptyIcon key={0} color="primary" fontSize="inherit" />,
    <HourglassBottomIcon key={1} color="primary" fontSize="inherit" />,
    <HourglassFullIcon key={2} color="primary" fontSize="inherit" />,
  ];

  const [iconIndex, setIconIndex] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setIconIndex((prevState) => (prevState + 1) % icons.length);
    }, 1000);
    return () => clearInterval(interval);
  }, [icons.length]);

  return icons[iconIndex];
}
