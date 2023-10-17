import Snackbar from "@mui/material/Snackbar";
import {
  CohortContext,
  cohortUndoRedo,
  useNewCohortContext,
} from "cohortContext";
import Loading from "components/loading";
import { SyntheticEvent, useState } from "react";
import { Outlet } from "react-router-dom";
import { useBaseParams } from "router";
import { UndoRedoContext } from "undoRedoToolbar";

export default function CohortRoot() {
  const params = useBaseParams();

  const [open, setOpen] = useState(false);
  const [message, setMessage] = useState("");

  const status = useNewCohortContext((message: string) => {
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
      <CohortContext.Provider value={status.context}>
        <UndoRedoContext.Provider
          value={cohortUndoRedo(params, status.context)}
        >
          <Outlet />
          <Snackbar
            open={open}
            autoHideDuration={5000}
            onClose={handleClose}
            message={message}
          />
        </UndoRedoContext.Provider>
      </CohortContext.Provider>
    </Loading>
  );
}
