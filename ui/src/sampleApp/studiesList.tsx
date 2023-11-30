import DeleteIcon from "@mui/icons-material/Delete";
import Button from "@mui/material/Button";
import IconButton from "@mui/material/IconButton";
import Link from "@mui/material/Link";
import Empty from "components/empty";
import Loading from "components/loading";
import { useSimpleDialog } from "components/simpleDialog";
import { useTextInputDialog } from "components/textInputDialog";
import { TreeGrid, TreeGridData } from "components/treegrid";
import { useStudySource } from "data/studySourceContext";
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
  const studySource = useStudySource();

  const userState = useSWRImmutable({ type: "user" }, async () => {
    return await studySource.getUser();
  });

  const listStudies = useCallback(async () => {
    const domain = userState.data?.email?.split("@")[1];
    return await studySource.listStudies({
      createdBy: domain ? "@" + domain : undefined,
    });
  }, [studySource, userState.data?.email]);

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
      await studySource.createStudy(name);
      return listStudies();
    });
  };

  const onDeleteStudy = (studyId: string) => {
    studiesState.mutate(async () => {
      await studySource.deleteStudy(studyId);
      return listStudies();
    });
  };

  const [newStudyDialog, showNewStudyDialog] = useTextInputDialog();

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
  }, [studySource, studiesState.data]);

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
            onClick={() =>
              showNewStudyDialog({
                title: "New study",
                textLabel: "Study name",
                buttonLabel: "Create",
                onConfirm: onCreateNewStudy,
              })
            }
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
