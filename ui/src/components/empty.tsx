import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import React from "react";

type EmptyProps = {
  title?: string;
  subtitle?: string;
  image?: string;
  maxWidth?: string | number;
  minHeight?: string | number;
};

export default function Empty(props: EmptyProps) {
  return (
    <Stack
      alignItems="center"
      justifyContent="center"
      sx={{ width: "100%", height: "100%", minHeight: props.minHeight }}
    >
      <Stack
        alignItems="center"
        sx={{ maxWidth: props.maxWidth, textAlign: "center" }}
      >
        {props.image && (
          <img src={props.image} style={{ marginBottom: "20px" }} />
        )}
        {props.title && (
          <Typography variant="body1em">{props.title}</Typography>
        )}
        {props.subtitle && (
          <Typography variant="body1">{props.subtitle}</Typography>
        )}
      </Stack>
    </Stack>
  );
}
