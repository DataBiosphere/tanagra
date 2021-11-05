import Dialog from "@mui/material/Dialog";
import DialogContent from "@mui/material/DialogContent";
import DialogContentText from "@mui/material/DialogContentText";
import DialogTitle from "@mui/material/DialogTitle";
import { EntityInstancesApiContext } from "apiContext";
import Loading from "loading";
import { ReactElement, useCallback, useContext, useState } from "react";
import { useAsync } from "react-async";
import { Cohort } from "./cohort";

type SqlDialogProps = {
  cohort?: Cohort;
};

export function useSqlDialog(
  props: SqlDialogProps
): [ReactElement, () => void] {
  const [open, setOpen] = useState(false);
  const show = useCallback(() => {
    setOpen(true);
  }, []);

  const api = useContext(EntityInstancesApiContext);

  const sqlState = useAsync<string>({
    promiseFn: useCallback(() => {
      if (open && props.cohort) {
        const params = props.cohort.generateQueryParameters();
        if (params) {
          return api
            .generateDatasetSqlQuery({
              entityName: props.cohort.entityName,
              underlayName: props.cohort.underlayName,
              generateDatasetSqlQueryRequest: {
                entityDataset: params,
              },
            })
            .then((res) => {
              if (res?.query) {
                return res.query;
              }
              throw new Error("Service returned an empty query");
            });
        }
        return new Promise<string>((resolve) => {
          resolve("No criteria have been selected");
        });
      }
      return new Promise<string>((resolve) => {
        resolve("");
      });
    }, [api, props.cohort, open]),
  });

  return [
    // eslint-disable-next-line react/jsx-key
    <Dialog
      open={open}
      onClose={() => {
        setOpen(false);
      }}
      scroll={"paper"}
      fullWidth
      maxWidth="xl"
      aria-labelledby="scroll-dialog-title"
      aria-describedby="scroll-dialog-description"
    >
      <DialogTitle id="scroll-dialog-title">SQL Query</DialogTitle>
      <DialogContent dividers={true}>
        <DialogContentText
          id="scroll-dialog-description"
          tabIndex={-1}
          sx={{ fontFamily: "Monospace" }}
        >
          <Loading status={sqlState}>{sqlState.data}</Loading>
        </DialogContentText>
      </DialogContent>
    </Dialog>,
    show,
  ];
}
