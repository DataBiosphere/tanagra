import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import TablePagination from "@mui/material/TablePagination";
import {
  useReviewAnnotations,
  useReviewInstances,
  useReviewParams,
  useReviewSearchState,
} from "cohortReview/reviewHooks";
import Loading from "components/loading";
import { TreeGrid, TreeGridId } from "components/treeGrid";
import GridLayout from "layout/gridLayout";
import React, { useMemo } from "react";

export type ParticipantsListDialogProps = {
  count: number;
};

export function ParticipantsListDialog(
  props: ParticipantsListDialogProps & { open: boolean; hide: () => void }
) {
  const [page, setPage] = React.useState(0);
  const [rowsPerPage, setRowsPerPage] = React.useState(25);

  const handleChangePage = (
    event: React.MouseEvent<HTMLButtonElement> | null,
    newPage: number
  ) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (
    event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  return (
    <Dialog
      maxWidth="lg"
      PaperProps={{
        sx: {
          height: "100%",
        },
      }}
      aria-labelledby="participants-list-dialog-title"
      open={props.open}
      onClose={props.hide}
    >
      <DialogTitle id="participants-list-dialog-title">
        Participants
      </DialogTitle>
      <DialogContent sx={{ height: "100%" }}>
        <GridLayout rows fillRow={0}>
          <ParticipantsList
            page={page}
            rowsPerPage={rowsPerPage}
            hide={props.hide}
          />
          <TablePagination
            component="div"
            count={props.count}
            page={page}
            onPageChange={handleChangePage}
            rowsPerPage={rowsPerPage}
            onRowsPerPageChange={handleChangeRowsPerPage}
          />
        </GridLayout>
      </DialogContent>
      <DialogActions>
        <Button variant="contained" onClick={props.hide}>
          Done
        </Button>
      </DialogActions>
    </Dialog>
  );
}

type ParticipantsListProps = {
  page: number;
  rowsPerPage: number;
  hide: () => void;
};

function ParticipantsList(props: ParticipantsListProps) {
  const params = useReviewParams();
  const instancesState = useReviewInstances();
  const annotationsState = useReviewAnnotations();

  const [, updateSearchState] = useReviewSearchState();

  const columns = useMemo(
    () => [
      ...params.uiConfig.participantsListColumns,
      ...(annotationsState.data
        ? annotationsState.data.map((a) => ({
            key: `t_${a.id}`,
            width: params.uiConfig.annotationColumnWidth ?? 100,
            title: a.displayName,
          }))
        : []),
    ],
    [annotationsState, params.uiConfig]
  );

  const data = useMemo(() => {
    const children: TreeGridId[] = [];
    const rows = new Map();

    instancesState.data
      ?.slice(
        props.page * props.rowsPerPage,
        (props.page + 1) * props.rowsPerPage
      )
      .forEach((instance) => {
        const key = instance.data.key;
        rows.set(key, { data: { ...instance.data } });
        children.push(key);

        annotationsState.data?.forEach((a) => {
          const values = instance.annotations.get(a.id);
          if (values) {
            const valueData = rows.get(key)?.data;
            if (valueData) {
              valueData[`t_${a.id}`] = values[values.length - 1].value;
            }
          }
        });
      });

    return {
      rows,
      children,
    };
  }, [instancesState, annotationsState, props.page, props.rowsPerPage]);

  return (
    <Loading status={instancesState}>
      <Loading status={annotationsState}>
        <TreeGrid
          data={data}
          columns={columns}
          rowCustomization={(id: TreeGridId) => {
            return [
              {
                column: 0,
                onClick: () => {
                  if (!instancesState.data) {
                    return;
                  }

                  const index = instancesState.data.findIndex(
                    (i) => i.data.key === id
                  );
                  if (index >= 0) {
                    updateSearchState((state) => {
                      state.instanceIndex = index;
                    });
                    props.hide();
                  }
                },
              },
            ];
          }}
        />
      </Loading>
    </Loading>
  );
}
