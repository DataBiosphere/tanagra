import Box from "@mui/material/Box";
import Paper from "@mui/material/Paper";
import { PropsWithChildren } from "react";

export default function SelectablePaper({
  children,
  selected,
}: PropsWithChildren<{ selected: boolean }>) {
  return (
    <Paper sx={{ width: "100%", overflow: "hidden", position: "relative" }}>
      {selected ? (
        <Box
          sx={{
            position: "absolute",
            top: "16px",
            bottom: "16px",
            left: "-4px",
            width: "8px",
            borderRadius: "4px",
            backgroundColor: (theme) => theme.palette.primary.main,
          }}
        />
      ) : undefined}
      {children}
    </Paper>
  );
}
