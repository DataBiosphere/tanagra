import Snackbar from "@mui/material/Snackbar";
import Loading from "components/loading";
import {
  FeatureSetContext,
  featureSetUndoRedo,
  useNewFeatureSetContext,
} from "featureSet/featureSetContext";
import { SyntheticEvent, useState } from "react";
import { Outlet } from "react-router-dom";
import { useBaseParams } from "router";
import { UndoRedoContext } from "undoRedoToolbar";

export default function FeatureSetRoot() {
  const params = useBaseParams();

  const [open, setOpen] = useState(false);
  const [message, setMessage] = useState("");

  const status = useNewFeatureSetContext((message: string) => {
    setMessage(message);
    setOpen(true);
  });

  const handleClose = (event: SyntheticEvent | Event, reason?: string) => {
    if (reason === "clickaway") {
      return;
    }

    setOpen(false);
  };

  return (
    <Loading status={status}>
      <FeatureSetContext.Provider value={status.context}>
        <UndoRedoContext.Provider
          value={featureSetUndoRedo(params, status.context)}
        >
          <Outlet />
          <Snackbar
            open={open}
            autoHideDuration={5000}
            onClose={handleClose}
            message={message}
          />
        </UndoRedoContext.Provider>
      </FeatureSetContext.Provider>
    </Loading>
  );
}
