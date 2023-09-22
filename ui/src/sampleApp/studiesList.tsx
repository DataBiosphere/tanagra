import DeleteIcon from "@mui/icons-material/Delete";
import Button from "@mui/material/Button";
import IconButton from "@mui/material/IconButton";
import Link from "@mui/material/Link";
import Empty from "components/empty";
import Loading from "components/loading";
import { useSimpleDialog } from "components/simpleDialog";
import { useTextInputDialog } from "components/textInputDialog";
import { TreeGrid, TreeGridData } from "components/treegrid";
import { useSource } from "data/sourceContext";
import { DataKey } from "data/types";
import GridLayout from "layout/gridLayout";
import { useCallback, useMemo } from "react";
import { Header } from "sampleApp/header";
import useSWR from "swr";
import useSWRImmutable from "swr/immutable";
import { RouterLink } from "util/searchState";

const columns = [
  {
    key: "name",
    width: "100%",
    title: "Name",
  },
  {
    key: "created",
    width: 160,
    title: "Creation date",
  },
  {
    key: "controls",
    width: 80,
    title: "",
  },
];

export function StudiesList() {
  const source = useSource();

  const userState = useSWRImmutable({ type: "user" }, async () => {
    return await source.getUser();
  });

  const listStudies = useCallback(async () => {
    const domain = userState.data?.email?.split("@")[1];
    return await source.listStudies({
      createdBy: domain ? "@" + domain : undefined,
    });
  }, [source, userState.data?.email]);

  const studiesState = useSWR(
    () =>
      userState.data
        ? { type: "studies", email: userState.data.email }
        : undefined,
    async () => {
      return listStudies();
    }
  );

  const onCreateNewStudy = (name: string) => {
    studiesState.mutate(async () => {
      await source.createStudy(name);
      return listStudies();
    });
  };

  const onDeleteStudy = (studyId: string) => {
    studiesState.mutate(async () => {
      await source.deleteStudy(studyId);
      return listStudies();
    });
  };

  const [newStudyDialog, showNewStudyDialog] = useTextInputDialog({
    title: "New study",
    textLabel: "Study name",
    buttonLabel: "Create",
    onConfirm: onCreateNewStudy,
  });

  const [confirmDialog, showConfirmDialog] = useSimpleDialog();

  const data = useMemo(() => {
    const children: DataKey[] = [];
    const data: TreeGridData = {
      root: { data: {}, children },
    };

    studiesState.data?.forEach((study) => {
      const key = study.id;
      children.push(key);

      const item = {
        data: {
          name: (
            <Link
              variant="body1"
              color="inherit"
              underline="hover"
              component={RouterLink}
              to={"studies/" + study.id}
            >
              {study.displayName}
            </Link>
          ),
          created: study.created,
          controls: (
            <GridLayout colAlign="center">
              <IconButton
                onClick={() =>
                  showConfirmDialog({
                    title: `Delete ${study.displayName}?`,
                    text: `Are you sure you want to delete "${study.displayName}"? This action is permanent.`,
                    buttons: ["Cancel", "Delete"],
                    onButton: (button) => {
                      if (button === 1) {
                        onDeleteStudy(study.id);
                      }
                    },
                  })
                }
              >
                <DeleteIcon />
              </IconButton>
            </GridLayout>
          ),
        },
      };
      data[key] = item;
    });

    return data;
  }, [source, studiesState.data]);

  return (
    <GridLayout
      rows
      sx={{ backgroundColor: (theme) => theme.palette.background.paper }}
    >
      <Header />
      <Loading status={studiesState}>
        <GridLayout rows spacing={4}>
          {!!data?.root?.children?.length ? (
            <TreeGrid columns={columns} data={data} />
          ) : (
            <Empty
              maxWidth="90%"
              minHeight="400px"
              title="No studies created"
            />
          )}
          <Button
            onClick={showNewStudyDialog}
            variant="contained"
            sx={{ ml: 4 }}
          >
            Add study
          </Button>
        </GridLayout>
        {newStudyDialog}
        {confirmDialog}
      </Loading>
    </GridLayout>
  );
}
