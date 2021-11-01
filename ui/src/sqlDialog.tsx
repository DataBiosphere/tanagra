import Dialog from "@mui/material/Dialog";
import DialogContent from "@mui/material/DialogContent";
import DialogContentText from "@mui/material/DialogContentText";
import DialogTitle from "@mui/material/DialogTitle";
import { EntityInstancesApiContext } from "apiContext";
import Loading from "loading";
import {
  ReactElement,
  useCallback,
  useContext,
  useEffect,
  useState,
} from "react";
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

  const [sql, setSql] = useState<string | null>(null);
  const [error, setError] = useState<Error | null>(null);

  const api = useContext(EntityInstancesApiContext);

  useEffect(() => {
    if (open && props.cohort) {
      const params = props.cohort.generateQueryParameters();
      if (params) {
        api
          .generateDatasetSqlQuery({
            entityName: props.cohort.entityName,
            underlayName: props.cohort.underlayName,
            generateDatasetSqlQueryRequest: {
              entityDataset: params,
            },
          })
          .then(
            (res) => {
              if (res?.query) {
                setSql(res.query);
                setError(null);
              } else {
                setError(new Error("Service returned an empty query"));
              }
            },
            (error) => {
              setError(error);
            }
          );
      } else {
        setError(new Error("No criteria have been selected"));
      }
    }
  }, [api, props.cohort, open]);

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
          {error?.message || sql || <Loading />}
        </DialogContentText>
      </DialogContent>
    </Dialog>,
    show,
  ];
}
