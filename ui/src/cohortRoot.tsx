import Snackbar from "@mui/material/Snackbar";
import { CohortContext, useNewCohortContext } from "cohortContext";
import Loading from "components/loading";
import { SyntheticEvent, useState } from "react";
import { Outlet } from "react-router-dom";

export default function CohortRoot() {
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
        <Outlet />
        <Snackbar
          open={open}
          autoHideDuration={5000}
          onClose={handleClose}
          message={message}
        />
      </CohortContext.Provider>
    </Loading>
  );
}
