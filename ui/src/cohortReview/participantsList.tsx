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
  useReviewSearchData,
} from "cohortReview/reviewHooks";
import Loading from "components/loading";
import { TreeGrid, TreeGridData, TreeGridId } from "components/treegrid";
import GridLayout from "layout/gridLayout";
import React, { ReactNode, useMemo, useState } from "react";

type ParticipantsListDialogProps = {
  count: number;
};

export function useParticipantsListDialog(
  props: ParticipantsListDialogProps
): [ReactNode, () => void] {
  const [open, setOpen] = useState(false);
  const show = () => setOpen(true);

  return [
    // eslint-disable-next-line react/jsx-key
    <ParticipantsListDialog
      {...props}
      open={open}
      hide={() => setOpen(false)}
    />,
    show,
  ];
}

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

  const [, updateSearchData] = useReviewSearchData();

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
    [annotationsState]
  );

  const data = useMemo(() => {
    const data: TreeGridData = {
      root: { data: {}, children: [] },
    };

    instancesState.data
      ?.slice(
        props.page * props.rowsPerPage,
        (props.page + 1) * props.rowsPerPage
      )
      .forEach((instance) => {
        const key = instance.data.key;
        data[key] = { data: { ...instance.data } };
        data.root?.children?.push(key);

        annotationsState.data?.forEach((a) => {
          const values = instance.annotations[a.id];
          if (values) {
            data[key].data[`t_${a.id}`] = values[values.length - 1].value;
          }
        });
      });

    return data;
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
                    updateSearchData((data) => {
                      data.instanceIndex = index;
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
