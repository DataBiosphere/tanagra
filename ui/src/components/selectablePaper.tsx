import Paper from "@mui/material/Paper";
import { PropsWithChildren } from "react";

export default function SelectablePaper({
  children,
  selected,
}: PropsWithChildren<{ selected: boolean }>) {
  return (
    <Paper
      elevation={0}
      sx={{
        width: "100%",
        overflow: "hidden",
        position: "relative",
        borderStyle: "solid",
        borderWidth: "1px",
        ...(selected
          ? {
              backgroundColor: "#F1F2FA",
              borderColor: "#BEC2E9",
            }
          : {
              borderColor: (theme) => theme.palette.background.paper,
            }),
      }}
    >
      {children}
    </Paper>
  );
}
