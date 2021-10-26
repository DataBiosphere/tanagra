import CircularProgress from "@mui/material/CircularProgress";
import Grid from "@mui/material/Grid";
import React, { useEffect, useRef, useState } from "react";

export default function Loading() {
  const [visible, setVisible] = useState(false);
  const timerRef = useRef<number>();

  useEffect(() => {
    timerRef.current = window.setTimeout(() => {
      setVisible(true);
    }, 800);

    return () => {
      clearTimeout(timerRef.current);
    };
  }, []);

  return visible ? (
    <Grid container justifyContent="center" padding="1em">
      <Grid item xs="auto">
        <CircularProgress />
      </Grid>
    </Grid>
  ) : null;
}
