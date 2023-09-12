import Typography from "@mui/material/Typography";
import GridLayout from "layout/gridLayout";
import React from "react";

type EmptyProps = {
  title?: string;
  subtitle?: string | JSX.Element;
  image?: string;
  maxWidth?: string | number;
  minHeight?: string | number;
};

export default function Empty(props: EmptyProps) {
  return (
    <GridLayout
      cols
      rowAlign="middle"
      colAlign="center"
      sx={{ px: 2, py: 1, minHeight: props.minHeight }}
    >
      <GridLayout colAlign="center" height="auto" sx={{ textAlign: "center" }}>
        {props.image && (
          <img src={props.image} style={{ marginBottom: "20px" }} />
        )}
        {props.title && (
          <Typography variant="body1em">{props.title}</Typography>
        )}
        {props.subtitle && (
          <Typography variant="body1">{props.subtitle}</Typography>
        )}
      </GridLayout>
    </GridLayout>
  );
}
