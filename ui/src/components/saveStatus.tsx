import CloudDoneIcon from "@mui/icons-material/CloudDone";
import LoopIcon from "@mui/icons-material/Loop";
import Typography from "@mui/material/Typography";
import GridLayout from "layout/gridLayout";
import { isValid } from "util/valid";

export type SaveStatusProps = {
  saving?: boolean;
  lastModified?: Date;
};

export function SaveStatus(props: SaveStatusProps) {
  if (!isValid(props.saving) || !isValid(props.lastModified)) {
    return null;
  }

  return (
    <GridLayout cols spacing={0.5} rowAlign="middle">
      <Typography variant="body1">
        {props.saving
          ? "Saving..."
          : `Last saved: ${props.lastModified.toLocaleString()}`}
      </Typography>
      {props.saving ? (
        <LoopIcon fontSize="small" sx={{ display: "block" }} />
      ) : (
        <CloudDoneIcon fontSize="small" sx={{ display: "block" }} />
      )}
    </GridLayout>
  );
}
